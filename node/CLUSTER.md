# Cluster architecture

This document describes how PoloCloud nodes form a cluster: how they join, how they
agree on a head, how they detect failure, and how they talk to each other. It covers
the `node/.../cluster/` package and the node-to-node parts of `node/.../communication/`.

## 1. Source of truth: the shared database

There is no gossip protocol and no replicated log for cluster/service state. Every
node reads and writes the same database, configured per node under `localNode.database`
in `config.json` (`LocalNodeConfiguration`). The backend is pluggable
(`de.polocloud:polocloud-database`): H2 (embedded, file-based), MariaDB, MySQL,
PostgreSQL, MongoDB, or Redis.

- **Single node / local testing**: the H2 default works out of the box — nothing to
  configure.
- **Real multi-node cluster**: every node's `localNode.database` **must** point at the
  same external database (e.g. one shared MySQL/MariaDB/PostgreSQL instance). Two nodes
  each running their own embedded H2 file are *not* a cluster — the join handshake
  itself depends on the joining node being able to immediately read the row the node it
  registered with just wrote (see §3), so it will fail with an unregistered-node error
  if the databases aren't actually the same one.

`NodeRepository` (`node/.../cluster/node/NodeRepository.kt`) is the table of every node
that has ever joined (`NodeData`: id, address, version, `state`, `head`, election
`term`/`votedFor`, resource capacity, timestamps). It is the cluster's membership list
and the input to both leader election and cluster-wide operations (service listing,
group shutdown, event relay) — all of which work by iterating
`NodeRepository.find(NodeState.ONLINE)` and calling each peer directly over gRPC,
tolerating individual failures (see §6).

## 2. Node lifecycle

`NodeState` (`common.proto`): `OFFLINE → STARTING → (SYNCING) → ONLINE → STOPPING →
STOPPED`, or `→ CRASHED` from `ONLINE` if heartbeats stop. Transitions are driven by
`LocalNodeContainer` (`markStarting`/`markOnline`/`markStopping`/`markStopped`), each
guarded by a predicate on the current state, and persisted via `NodeRepository.save`.

Stale terminal rows (`CRASHED`/`STOPPED` for over an hour) are deleted automatically by
`NodePruneService`, which only runs on the current head so peers don't race each other
deleting the same rows. There is no manual "remove node" command — decommissioning is
just leaving a node stopped and letting the pruner catch up.

## 3. Joining a cluster

1. An operator gets a registration token from an existing node — either the one
   auto-printed on that node's first boot (`cluster.node.identity.alert.token`,
   10-minute TTL) or a fresh one via `CreateToken` (`ClusterCommand`/`ClusterService`).
   Tokens are **held in memory per-node** (`RegistrationTokenManager`), not shared
   through the database, so a token is only valid against the specific node that
   minted it — the joining node must be pointed at that same node's address.
2. The joining node calls `NodeRegistrationService.RegisterNode` (plaintext, since no
   certificate exists yet) with its CSR, hostname/port, version, and reported max
   memory. The receiving node validates the token, signs the CSR with the cluster CA,
   and writes a new `NodeData` row (`RegistrationService.registerNode`).
3. The joining node saves the returned certificate + CA certificate
   (`NodeCertificateStorage`), then opens an mTLS channel back to the node it joined
   and adopts the **real** CA private key and the shared forwarding secret
   (`NodeIdentityService.adoptClusterCaKeyPair`/`adoptForwardingSecret`) — both
   best-effort; a transient failure here doesn't abort the join, but the node can't be
   promoted to head (or forward players correctly) until it succeeds, typically on the
   next restart.
4. From here on, all node-to-node traffic — including the rest of registration — runs
   over the mTLS port authenticated by the shared cluster CA
   (`NodeCertificateStorage`/`CertificateAuthority`).

Every node ends up holding the same CA key pair, specifically so leader election can
promote *any* node to head without breaking certificate issuance for the rest of the
cluster.

## 4. Leader election

One node is always (intended to be) the current **head** — the node responsible for
[`NodePruneService`] and other head-only duties. Election is a Raft-style leader
election, *not* a Raft-replicated log: actual cluster/service state already lives in
the shared database (§1), so this only needs to solve "exactly one head, and a
superseded head stops acting like one" — terms, majority votes, and a leader lease
heartbeat, without a log to replicate.

- **`ElectionState`** (`cluster/election/ElectionState.kt`) is the in-memory FSM:
  `FOLLOWER` / `CANDIDATE` / `LEADER`, a monotonic `term`, and `votedFor` (persisted on
  `NodeData` so a restart mid-election can't vote twice in the same term).
- A follower that hears no leader heartbeat within a randomized timeout (base + jitter,
  biased shorter for more senior nodes to reduce split votes) becomes a candidate:
  bumps its term, votes for itself, and sends `RequestVote` (over the same `NodeService`
  gRPC used for `RelayEvent`/`FetchClusterCa`) to every node in `NodeRepository`.
- **Quorum is computed against every registered node**, not just currently-reachable
  ones — a candidate that can only reach a minority of the cluster cannot win, even if
  every peer it *can* reach votes yes. This is why `NodePruneService` matters: nodes
  that are gone for good need to actually leave the membership table, or they
  permanently inflate the quorum denominator.
- On winning a majority, the candidate becomes leader, writes `head = true` /
  `electedAt` for itself (and clears it for whoever it replaced) in `NodeRepository`,
  and starts sending a `LeaderHeartbeat` to every peer once per
  `leaderHeartbeatIntervalMillis`.
- **Fencing**: any `RequestVote` or `LeaderHeartbeat` carrying a higher term than a
  node has seen immediately demotes that node to follower and adopts the new term —
  including a current leader, so a partitioned-then-reconnected stale head steps down
  as soon as it hears about a newer term, instead of continuing to act on stale
  authority.
- `NodeElectionService` (`cluster/election/NodeElectionService.kt`) is the per-node
  orchestrator wiring `ElectionState` to `NodeRepository` and the gRPC transport
  (`GrpcElectionRpcClient`). `NodeHeartBeatMonitor.onNodeCrashed` calls
  `NodeElectionService.onNodeCrashed`, which — if the crashed node was head — skips the
  rest of this node's own election timeout instead of waiting it out; the winner is
  still decided entirely by the term/vote exchange.
- A node leaving gracefully cannot elect its own successor (that would bypass quorum
  voting); it just stops sending leader heartbeats, and peers elect a replacement
  themselves within one timeout window (`NodeElectionService.onHeadNodeLeft`).

All of the above timing is configurable per node via `cluster.timing` in `config.json`
(`ClusterTimingConfiguration`) — see §7.

## 5. Heartbeats & crash detection

Separate from the election heartbeat (§4), each node saves its own resource heartbeat
(CPU/memory/TPS) every second (`NodeHeartBeatService`, `NodeHeartBeatRepository`,
table `nodes_heartbeats`). Old rows are pruned locally: recent history is kept in full,
older than 24h is thinned to one sample per 10 minutes.

`NodeHeartBeatMonitor` ticks periodically (`heartbeatMonitorTickMillis`) and, for every
`ONLINE` node, compares the newer of "latest heartbeat" and "last time we heard from it
for any reason" (`NodeData.lastConnection`) against `heartbeatCrashTimeoutMillis`. Using
`lastConnection` as a fallback matters: a node whose heartbeat scheduler never starts or
dies would otherwise never get a heartbeat row and stay `ONLINE` forever, silently able
to block election (as a phantom quorum member) and service placement. A node found
stale is marked `CRASHED`, which — if it was head — triggers the fast-path re-election
described in §4.

## 6. Cross-node operations

Everything here is **best-effort fan-out**: iterate `NodeRepository.find(ONLINE)`, call
each peer over a short-lived or cached mTLS channel, isolate individual failures so one
slow/dead peer never blocks the rest.

- **Live event relay** (`ClusterEventRelay`/`ClusterEventService`): local events (e.g.
  service lifecycle) are pushed to every peer's `NodeService.RelayEvent`, which
  re-broadcasts to that peer's own local subscribers only — one hop, no loops.
- **Cluster-wide service listing** (`ListServicesServerHandler`/`FindServicesServerHandler`,
  `PeerServiceQuery`): ask every peer for its *local* services and merge.
- **Cluster-wide group shutdown** (`ClusterGroupShutdown`): stop a group's services on
  every node before the group row itself is deleted, so replicas on other nodes don't
  end up orphaned and the group delete doesn't fail on a stale foreign-key reference.

## 7. Configuration reference

`config.json` → `cluster`:

| Field | Meaning |
|---|---|
| `registration` | Address the registration/cluster gRPC server binds to |
| `cliAccess` | IP allowlist + token for CLI access on the same port |
| `timing.heartbeatIntervalMillis` | How often a node saves its own resource heartbeat (default 1000) |
| `timing.heartbeatMonitorTickMillis` | How often `NodeHeartBeatMonitor` re-checks every ONLINE node (default 3000) |
| `timing.heartbeatCrashTimeoutMillis` | How stale a node's liveness reference may get before it's marked CRASHED (default 15000) |
| `timing.electionBaseTimeoutMillis` | Base Raft election timeout (default 5000) |
| `timing.electionJitterRangeMillis` | Random jitter added on top, to reduce split votes (default 4000) |
| `timing.leaderHeartbeatIntervalMillis` | How often the head sends its leader-lease heartbeat (default 1000) |

`localNode.database` selects the shared database backend (§1).

## 8. CLI

The `cluster` terminal command gives a live view of all of the above:

- `cluster` — summary: node count by state, current head, total group/service counts,
  aggregate memory usage across online nodes.
- `cluster list` — one line per node: state, host, live CPU/mem/TPS load.
- `cluster <name>` — detailed view of one node, including when it was elected head (if
  it is) and its last-received heartbeat.

## 9. Known gaps

Documented here rather than left implicit in the code, so they're easy to pick up
later:

- `CreateToken` (`CreateTokenServerHandler`) doesn't check that the caller is the head —
  any node reachable on the cluster gRPC endpoint can mint a fresh registration token
  for itself. Authorization of *who* may admit new nodes isn't centralized yet.
- Election/heartbeat/pruning intervals are per-node config, not cluster-wide-enforced —
  nothing currently stops different nodes in the same cluster from running with
  inconsistent timing values, which is only really safe if every node's `config.json`
  agrees.
