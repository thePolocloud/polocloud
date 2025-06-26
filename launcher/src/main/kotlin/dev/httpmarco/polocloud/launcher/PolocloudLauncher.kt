package dev.httpmarco.polocloud.launcher

import kotlin.io.path.createDirectories
import kotlin.io.path.toPath

fun main(args: Array<String>) {

    // own path of the launcher file
    val ownPath = PolocloudProcess::class.java.getProtectionDomain().codeSource.location.toURI().toPath()

    // we need to load the current version from the manifest data
    System.setProperty(VERSION_ENV_ID, readManifest(VERSION_ENV_ID, ownPath)!!)

    // generate lib folder
    LIB_DIRECTORY.createDirectories()

    // start the main context of the polocloud agent
    PolocloudProcess().start()
}