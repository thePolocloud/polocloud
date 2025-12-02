package dev.httpmarco.polocloud.platforms

import com.google.gson.GsonBuilder
import dev.httpmarco.polocloud.common.json.RuntimeTypeAdapterFactory
import dev.httpmarco.polocloud.platforms.bridge.Bridge
import dev.httpmarco.polocloud.platforms.tasks.actions.PlatformAction
import dev.httpmarco.polocloud.platforms.tasks.actions.PlatformDirectoryDeleteAction
import dev.httpmarco.polocloud.platforms.tasks.actions.PlatformDownloadAction
import dev.httpmarco.polocloud.platforms.tasks.actions.PlatformExecuteCommandAction
import dev.httpmarco.polocloud.platforms.tasks.actions.PlatformFileDeleteAction
import dev.httpmarco.polocloud.platforms.tasks.actions.PlatformFileMoveAction
import dev.httpmarco.polocloud.platforms.tasks.actions.PlatformFilePropertyUpdateAction
import dev.httpmarco.polocloud.platforms.tasks.actions.PlatformFileReplacementAction
import dev.httpmarco.polocloud.platforms.tasks.actions.PlatformFileUnzipAction
import dev.httpmarco.polocloud.platforms.tasks.actions.PlatformFileWriteAction
import kotlin.io.path.Path

val PLATFORM_PATH = Path("local/metadata")
const val PLATFORM_METADATA_URL = "https://raw.githubusercontent.com/thePolocloud/polocloud/refs/heads/master/metadata/"

val PLATFORM_GSON =
    GsonBuilder().setPrettyPrinting().serializeNulls()
        .registerTypeHierarchyAdapter(PlatformVersion::class.java, PlatformVersionSerializer())
        .registerTypeAdapter(Platform::class.java, PlatformDeserializer())
        .registerTypeAdapterFactory(
            RuntimeTypeAdapterFactory
                .of(PlatformAction::class.java, "type") // "type" ist das Typ-Merkmal im JSON
                .registerSubtype(PlatformFileReplacementAction::class.java, "PlatformFileReplacementAction")
                .registerSubtype(PlatformFileWriteAction::class.java, "PlatformFileWriteAction")
                .registerSubtype(PlatformFilePropertyUpdateAction::class.java, "PlatformFilePropertyUpdateAction")
                .registerSubtype(PlatformFileUnzipAction::class.java, "PlatformFileUnzipAction")
                .registerSubtype(PlatformFileDeleteAction::class.java, "PlatformFileDeleteAction")
                .registerSubtype(PlatformExecuteCommandAction::class.java, "PlatformExecuteCommandAction")
                .registerSubtype(PlatformDirectoryDeleteAction::class.java, "PlatformDirectoryDeleteAction")
                .registerSubtype(PlatformFileMoveAction::class.java, "PlatformFileMoveAction")
                .registerSubtype(PlatformDownloadAction::class.java, "PlatformDownloadAction")
        )
        .create()
