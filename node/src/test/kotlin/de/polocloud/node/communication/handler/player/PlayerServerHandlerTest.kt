package de.polocloud.node.communication.handler.player

import de.polocloud.common.communication.server.context.GrpcServerContext
import de.polocloud.database.DatabaseAccess
import de.polocloud.database.DatabaseCredentials
import de.polocloud.i18n.api.TranslationService
import de.polocloud.node.player.CloudPlayerRepository
import de.polocloud.proto.FindPlayerRequest
import de.polocloud.proto.ListPlayersRequest
import de.polocloud.proto.PlayerData
import de.polocloud.proto.RegisterPlayerRequest
import de.polocloud.proto.UnregisterPlayerRequest
import de.polocloud.proto.UpdatePlayerServerRequest
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.File
import java.util.UUID

/**
 * Covers the player RPC handlers ([RegisterPlayerServerHandler],
 * [UpdatePlayerServerServerHandler], [UnregisterPlayerServerHandler],
 * [FindPlayerServerHandler], [ListPlayersServerHandler]) — mirrors
 * `node/.../communication/handler/group/GroupServerHandlerTest.kt`'s shape, but against
 * a real throwaway H2 database rather than a fake repository, since
 * [CloudPlayerRepository] (unlike `GroupService`) is not itself injectable.
 */
class PlayerServerHandlerTest {

    companion object {
        private val dbPath = "build/tmp/polocloud-player-handler-test-${UUID.randomUUID()}"

        @JvmStatic
        @BeforeAll
        fun setUpDatabase() {
            runCatching { TranslationService.init() }
            DatabaseAccess.initialize(DatabaseCredentials.H2(dbPath))
            check(DatabaseAccess.connect()) { "Failed to connect to the test H2 database" }
        }

        @JvmStatic
        @AfterAll
        fun tearDownDatabase() {
            DatabaseAccess.close()
            File(dbPath).parentFile?.listFiles { file -> file.name.startsWith(File(dbPath).name) }
                ?.forEach { it.delete() }
        }
    }

    private fun playerData(id: UUID = UUID.randomUUID(), name: String = "Notch", currentProxy: String = "proxy-1") =
        PlayerData.newBuilder()
            .setId(id.toString())
            .setName(name)
            .setSkinValue("texture-value")
            .setSkinSignature("texture-signature")
            .setCurrentProxy(currentProxy)
            .build()

    // A "serviceSubject" in the context means the call arrived via ServiceGrpcEndpoint,
    // i.e. from a launched service/bridge — see CallerAuthorization.
    private fun callContext(serviceName: String) = GrpcServerContext().with("serviceSubject", serviceName)

    @Test
    fun `registerPlayer persists a new player`() = runBlocking {
        val data = playerData()
        val response = RegisterPlayerServerHandler().handle(
            RegisterPlayerRequest.newBuilder().setPlayer(data).build(),
            callContext("proxy-1"),
        )

        assertTrue(response.success)
        val saved = CloudPlayerRepository.findById(UUID.fromString(data.id))
        assertEquals("Notch", saved?.name)
        assertNull(saved?.currentServer)
    }

    @Test
    fun `registerPlayer rejects a proxy registering a player under another proxy's name`() {
        val data = playerData(currentProxy = "proxy-2")

        assertThrows(IllegalStateException::class.java) {
            runBlocking {
                RegisterPlayerServerHandler().handle(
                    RegisterPlayerRequest.newBuilder().setPlayer(data).build(),
                    callContext("proxy-1"),
                )
            }
        }
    }

    @Test
    fun `updatePlayerServer changes the current server and reports success`() = runBlocking {
        val data = playerData()
        RegisterPlayerServerHandler().handle(RegisterPlayerRequest.newBuilder().setPlayer(data).build(), callContext("proxy-1"))

        val response = UpdatePlayerServerServerHandler().handle(
            UpdatePlayerServerRequest.newBuilder().setPlayerId(data.id).setServer("lobby-1").build(),
            callContext("proxy-1"),
        )

        assertTrue(response.success)
        assertEquals("lobby-1", CloudPlayerRepository.findById(UUID.fromString(data.id))?.currentServer)
    }

    @Test
    fun `updatePlayerServer reports failure for an unknown player`() = runBlocking {
        val response = UpdatePlayerServerServerHandler().handle(
            UpdatePlayerServerRequest.newBuilder().setPlayerId(UUID.randomUUID().toString()).setServer("lobby-1").build(),
            GrpcServerContext(),
        )

        assertFalse(response.success)
    }

    @Test
    fun `unregisterPlayer removes the player`() = runBlocking {
        val data = playerData()
        RegisterPlayerServerHandler().handle(RegisterPlayerRequest.newBuilder().setPlayer(data).build(), callContext("proxy-1"))

        val response = UnregisterPlayerServerHandler().handle(
            UnregisterPlayerRequest.newBuilder().setPlayerId(data.id).setKicked(false).build(),
            callContext("proxy-1"),
        )

        assertTrue(response.success)
        assertNull(CloudPlayerRepository.findById(UUID.fromString(data.id)))
    }

    @Test
    fun `unregisterPlayer rejects a proxy unregistering another proxy's player`() {
        val data = playerData(currentProxy = "proxy-1")
        runBlocking { RegisterPlayerServerHandler().handle(RegisterPlayerRequest.newBuilder().setPlayer(data).build(), callContext("proxy-1")) }

        assertThrows(IllegalStateException::class.java) {
            runBlocking {
                UnregisterPlayerServerHandler().handle(
                    UnregisterPlayerRequest.newBuilder().setPlayerId(data.id).setKicked(false).build(),
                    callContext("proxy-2"),
                )
            }
        }
    }

    @Test
    fun `findPlayer locates a player by id`() = runBlocking {
        val data = playerData()
        RegisterPlayerServerHandler().handle(RegisterPlayerRequest.newBuilder().setPlayer(data).build(), callContext("proxy-1"))

        val response = FindPlayerServerHandler().handle(
            FindPlayerRequest.newBuilder().setPlayerId(data.id).build(),
            GrpcServerContext(),
        )

        assertTrue(response.found)
        assertEquals("Notch", response.player.name)
    }

    @Test
    fun `findPlayer locates a player by name`() = runBlocking {
        val data = playerData(name = "UniqueFindByNameTarget")
        RegisterPlayerServerHandler().handle(RegisterPlayerRequest.newBuilder().setPlayer(data).build(), callContext("proxy-1"))

        val response = FindPlayerServerHandler().handle(
            FindPlayerRequest.newBuilder().setName("UniqueFindByNameTarget").build(),
            GrpcServerContext(),
        )

        assertTrue(response.found)
        assertEquals(data.id, response.player.id)
    }

    @Test
    fun `findPlayer reports not found for an unknown player`() = runBlocking {
        val response = FindPlayerServerHandler().handle(
            FindPlayerRequest.newBuilder().setName("does-not-exist-${UUID.randomUUID()}").build(),
            GrpcServerContext(),
        )

        assertFalse(response.found)
    }

    @Test
    fun `listPlayers includes every registered player`() = runBlocking {
        val first = playerData(name = "ListTargetOne")
        val second = playerData(name = "ListTargetTwo")
        RegisterPlayerServerHandler().handle(RegisterPlayerRequest.newBuilder().setPlayer(first).build(), callContext("proxy-1"))
        RegisterPlayerServerHandler().handle(RegisterPlayerRequest.newBuilder().setPlayer(second).build(), callContext("proxy-1"))

        val response = ListPlayersServerHandler().handle(ListPlayersRequest.getDefaultInstance(), GrpcServerContext())

        val names = response.playersList.map { it.name }
        assertTrue(names.contains("ListTargetOne"))
        assertTrue(names.contains("ListTargetTwo"))
    }
}
