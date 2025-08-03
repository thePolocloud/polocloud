package dev.httpmarco.polocloud.agent

fun polocloudVersion(): String {
    return System.getenv("polocloud-version")
}