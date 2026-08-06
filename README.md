<div align="center">

# ☁️ PoloCloud v3

[![License](https://img.shields.io/github/license/thePolocloud/polocloud?style=for-the-badge&color=b2204c)](LICENSE)
[![Modrinth](https://img.shields.io/badge/Modrinth-polocloud-1bd96a?logo=modrinth&style=for-the-badge)](https://modrinth.com/organization/polocloud)
[![Downloads](https://img.shields.io/github/downloads/thePolocloud/polocloud/total?style=for-the-badge&logo=github&color=2ea043)](https://github.com/thePolocloud/polocloud/releases)

</div>

**One cloud for your entire Minecraft network:** PoloCloud is a modular Minecraft cloud system - it runs, scales, and manages your servers from a single control plane.

Modular by design: a cloud daemon, an admin CLI, a plugin SDK, and ready-made addons for Velocity, Waterfall/BungeeCord, and Bukkit.

- [Features](#features)
- [Supported platforms](#supported-platforms)
- [User Guide](#user-guide)
  - [Requirements](#requirements)
  - [Getting Started](#getting-started)
  - [CLI](#cli)
  - [Clustering](#clustering)
- [Versioning](#versioning)
- [Development](#development)
  - [Prerequisites](#prerequisites)
  - [Commands](#commands)
  - [Architecture](#architecture)
  - [References](#references)
  - [📜 License](#-license)
  - [🤝 Community](#-community)

## Features

- **Cluster mode:** Raft-style leader election, heartbeat-based crash detection, automatic node pruning, cross-node event relay.
- **Security:** mTLS across all nodes via a shared cluster CA - token-based node joining, per-service certificates.
- **Shared database:** Pluggable - H2 by default; MySQL, MariaDB, PostgreSQL, MongoDB, or Redis for clusters (MySQL/MariaDB currently blocked by a [known issue](node/CLUSTER.md#9-known-gaps)).
- **Service management:** Auto-scaling up to each group's `minOnline`, ordered templates, task-based config patching, live status detection, JAVA and GO service runtimes.
- **Group management:** Per-group memory, start threshold, static mode, fallback priority, node whitelist, ordered templates.
- **Event system:** Typed events relayed across the cluster over gRPC.
- **Proxy bridge (Velocity):** Backend registration, fallback selection, tab-complete relay.
- **Addons:** sign-system and server-mobs for Bukkit; hub, notify, and proxy for Velocity (proxy also supports Waterfall/BungeeCord).
- **Updating:** Checks GitHub releases on boot; stages self-updates when `general.autoUpdate` is enabled or via the `update` command.

## Supported platforms

- **Velocity:** Full support via the bridge plugin plus the hub, notify, and proxy addons. The bridge covers backend registration, fallback selection, initial server, and tab-complete relay.
- **Waterfall / BungeeCord:** Proxy addon only - animated tab list, MOTD, player count, maintenance mode. Bridge plugin not available yet.
- **Bukkit / Spigot:** sign-system and server-mobs. Sign-system shows animated server signs; server-mobs adds NPC mobs with live service status.

## User Guide

There is no stable release yet - use the latest DEV build from the [releases page](https://github.com/thePolocloud/polocloud/releases).

### Requirements

- JRE 25+
- Internet access for the initial start

### Getting Started

> [!WARNING]
> Under active development, so expect breaking changes.
> Please feel free to report any issues you encounter.

1. **Download** `polocloud-runner-<version>.jar` (one file, no installation).
2. **Run the node:**

   ```sh
   java -jar polocloud-runner-<version>.jar
   ```

3. **Create your first group** in the node terminal:

   ```
   group setup
   ```

   Answer the wizard - the cloud starts the servers for you.

### CLI

Run the CLI in a separate terminal and connect to the node:

```sh
java -jar polocloud-runner-<version>.jar --cli
```

```
connect <token>
```

The token is in `config.json` under `cluster.cliAccess.registrationToken`. On a local setup `connect <token>` is enough - it assumes localhost.

### Clustering

Multiple nodes form a cluster with `cluster join`:

```sh
cluster join
```

Every node needs the same shared external database - H2 is single-node only. `cluster join` only checks for it, it does not set it up. Point `localNode.database` in `config.json` at the shared database first:

```json
{
  "localNode": {
    "database": "<shared external database settings>"
  }
}
```

Then restart the node and run `cluster join` again. See the [cluster architecture](node/CLUSTER.md) for how joining and leader election work.

## Versioning

PoloCloud versions look like `3.1.0` (stable) or `3.0.7-dev.42` (pre-release) - `major.minor.patch[-channel.build]`.

**Channels**, from least to most stable: `SNAPSHOT` (local dev builds) > `DEV` (CI builds from every push to `master`, tagged `vX.Y.Z-dev.<run>` and published as GitHub pre-releases) > `ALPHA` > `BETA` > `RELEASE` (stable, tagged `vX.Y.Z`).

Stability comes from the channel in the version string, not from GitHub's `draft`/`prerelease` flags.

## Development

### Prerequisites

- JDK 25+
- Git

### Commands

```sh
./gradlew build     # compiles all modules and builds the artifacts
./gradlew allTests  # runs the test suite of all modules
```

### Architecture

Nodes communicate over [gRPC with protobuf contracts](proto/src/main/proto), and cluster state lives in a shared, pluggable database. The [cluster architecture](node/CLUSTER.md) covers node lifecycle, leader election, heartbeats, crash detection, and cross-node operations.

### References

- [CODE_OF_CONDUCT](.github/CODE_OF_CONDUCT.md)
- [CONTRIBUTING GUIDELINE](.github/CONTRIBUTING.md)
- [SECURITY POLICY](.github/SECURITY.md)

<div align="center">

#

### 📜 License

Licensed under the [Apache License 2.0](LICENSE).

### 🤝 Community

<a href="https://discord.polocloud.de">
    <img alt="PoloCloud Discord" src="https://discord.com/api/guilds/1278460874679386244/widget.png?style=banner2">
</a>

</div>
