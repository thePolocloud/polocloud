package de.polocloud.node.communication.impl.node

import de.polocloud.common.communication.server.executor.GrpcServerExecutor
import de.polocloud.common.communication.server.registery.GrpcServerHandlerRegistry
import de.polocloud.database.DatabaseAccess
import de.polocloud.database.DatabaseCredentials
import de.polocloud.i18n.api.TranslationService
import de.polocloud.node.cluster.election.NodeElectionService
import de.polocloud.node.cluster.node.NodeData
import de.polocloud.node.cluster.node.NodeRepository
import de.polocloud.node.communication.interceptor.CliSessionInterceptor
import de.polocloud.node.services.ServiceProvider
import de.polocloud.proto.LeaderHeartbeatRequest
import de.polocloud.proto.NodeState
import de.polocloud.proto.RequestVoteRequest
import io.grpc.Context
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.File
import java.util.UUID

/**
 * Covers the caller-identity checks documented on [NodeServiceImpl.requestVote]/
 * [NodeServiceImpl.leaderHeartbeat]: being an admitted cluster member (a valid mTLS
 * client certificate signed by the cluster CA) is not by itself license to claim
 * candidacy/leadership on behalf of a *different* node id in the request payload.
 * Without this check, any node reachable on the cluster gRPC port could manipulate
 * another node's `votedFor`/`head` state just by forging its id in the request, while
 * still authenticating as itself.
 *
 * The authenticated caller identity is read from [CliSessionInterceptor.SUBJECT_CTX_KEY],
 * an ambient `io.grpc.Context` value populated by [CliSessionInterceptor] from the peer's
 * mTLS certificate CN — not from anything the request payload itself supplies — so tests
 * attach/detach that context directly instead of going through a real interceptor/TLS
 * handshake, exactly like a real call would arrive with the CN already resolved.
 *
 * Uses a real (throwaway) H2 database — same reasoning as [de.polocloud.node.communication.handler.cluster.CreateTokenServerHandlerTest]
 * — since the "is this caller a known node" check goes through [NodeRepository]. The
 * [NodeElectionService] is never started: every case covered here is rejected by the
 * identity check itself, before the request would ever reach election logic (which has
 * its own thorough coverage in [de.polocloud.node.cluster.election.ElectionStateTest]).
 */
class NodeServiceImplTest {

    companion object {
        private val dbPath = "build/tmp/polocloud-nodeserviceimpl-test-${UUID.randomUUID()}"

        @JvmStatic
        @BeforeAll
        fun setUp() {
            runCatching { TranslationService.init() }
            DatabaseAccess.initialize(DatabaseCredentials.H2(dbPath))
            check(DatabaseAccess.connect()) { "Failed to connect to the test H2 database" }
        }

        @JvmStatic
        @AfterAll
        fun tearDown() {
            DatabaseAccess.close()
            File(dbPath).parentFile?.listFiles { file -> file.name.startsWith(File(dbPath).name) }
                ?.forEach { it.delete() }
        }
    }

    private fun registeredNode(index: Int) = NodeData(
        id = UUID.randomUUID(),
        nodeIndex = index,
        groupName = "node",
        hostname = "10.0.0.$index",
        port = 4240 + index,
        state = NodeState.ONLINE,
        head = false,
        electedAt = null,
        term = 0,
        votedFor = null,
        version = "3",
        gitCommitHash = "abc",
        firstConnection = kotlin.time.Clock.System.now(),
        lastConnection = kotlin.time.Clock.System.now(),
        maxMemory = 0,
    ).also { NodeRepository.save(it) }

    private fun service() = NodeServiceImpl(
        executor = GrpcServerExecutor(GrpcServerHandlerRegistry()),
        serviceProvider = ServiceProvider(),
        electionService = NodeElectionService(),
    )

    /** Runs [block] with [subject] published as the authenticated caller identity, mirroring what [CliSessionInterceptor] does for a real mTLS call. */
    private fun <T> asCaller(subject: String?, block: () -> T): T {
        val context = if (subject != null) {
            Context.current().withValue(CliSessionInterceptor.SUBJECT_CTX_KEY, subject)
        } else {
            Context.current()
        }
        val previous = context.attach()
        try {
            return block()
        } finally {
            context.detach(previous)
        }
    }

    @Test
    fun `requestVote rejects a candidateId that does not match the caller's authenticated identity`() = runBlocking {
        val caller = registeredNode(1)
        val impersonated = UUID.randomUUID() // never even registered — the check must fire before any repository lookup on it

        val response = asCaller(caller.id.toString()) {
            runBlocking {
                service().requestVote(
                    RequestVoteRequest.newBuilder().setTerm(1).setCandidateId(impersonated.toString()).build()
                )
            }
        }

        assertFalse(response.voteGranted)
    }

    @Test
    fun `requestVote rejects a call with no authenticated identity at all`() = runBlocking {
        val someCandidate = registeredNode(2)

        val response = asCaller(null) {
            runBlocking {
                service().requestVote(
                    RequestVoteRequest.newBuilder().setTerm(1).setCandidateId(someCandidate.id.toString()).build()
                )
            }
        }

        assertFalse(response.voteGranted)
    }

    @Test
    fun `leaderHeartbeat rejects a leaderId that does not match the caller's authenticated identity`() = runBlocking {
        val caller = registeredNode(3)
        val impersonated = UUID.randomUUID()

        val response = asCaller(caller.id.toString()) {
            runBlocking {
                service().leaderHeartbeat(
                    LeaderHeartbeatRequest.newBuilder().setTerm(1).setLeaderId(impersonated.toString()).build()
                )
            }
        }

        assertFalse(response.success)
    }

    @Test
    fun `leaderHeartbeat rejects a call with no authenticated identity at all`() = runBlocking {
        val someLeader = registeredNode(4)

        val response = asCaller(null) {
            runBlocking {
                service().leaderHeartbeat(
                    LeaderHeartbeatRequest.newBuilder().setTerm(1).setLeaderId(someLeader.id.toString()).build()
                )
            }
        }

        assertFalse(response.success)
    }
}
