package dev.httpmarco.polocloud.launcher

import kotlin.io.path.Path
import kotlin.io.path.createDirectory

fun main(args: Array<String>) {

    // own path of the launcher file
    val ownPath = Path(PolocloudProcess::class.java.getProtectionDomain().codeSource.location.toURI().toString())

    // we need to load the current version from the manifest data
    System.setProperty(VERSION_ENV_ID,readManifest(VERSION_ENV_ID, ownPath)!!)

    // generate lib folder
    LIB_DIRECTORY.createDirectory()

    // start the main context of the polocloud agent
    PolocloudProcess().start()
}