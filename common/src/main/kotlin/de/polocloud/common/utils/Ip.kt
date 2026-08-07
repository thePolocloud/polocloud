package de.polocloud.common.utils

import org.slf4j.LoggerFactory
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.SocketException
import java.net.URI

private val logger = LoggerFactory.getLogger("de.polocloud.common.utils.Ip")

/**
 * Returns the local IPv4 address of the device.
 *
 * Iterates through all network interfaces and returns the first non-loopback IPv4 address found.
 *
 * @return Local IPv4 address as [String]
 * @throws IllegalArgumentException if no suitable local IP is found
 */
fun localIpAddress(): String {
    try {
        NetworkInterface.getNetworkInterfaces()?.toList()?.forEach { intf ->
            intf.inetAddresses?.toList()?.forEach { inetAddress ->
                if (!inetAddress.isLoopbackAddress && inetAddress is Inet4Address) {
                    return inetAddress.hostAddress
                }
            }
        }
    } catch (ex: SocketException) {
        logger.warn("Failed to enumerate network interfaces: {}", ex.message)
    }
    throw IllegalArgumentException("No local IPv4 address found")
}


fun publicIpAddress(timeoutMs: Int = 5000): String? {
    val services = listOf(
        "https://api.ipify.org",
        "https://checkip.amazonaws.com",
        "https://ifconfig.me/ip"
    )

    for (service in services) {
        try {
            val url = URI(service).toURL()
            (url.openConnection() as? HttpURLConnection)?.run {
                connectTimeout = timeoutMs
                readTimeout = timeoutMs
                requestMethod = "GET"
                inputStream.bufferedReader().use { reader ->
                    val ip = reader.readText().trim()
                    if (ip.isNotEmpty()) return ip
                }
            }
        } catch (e: Exception) {
            logger.debug("Failed to resolve public IP via {}: {}", service, e.message)
        }
    }
    logger.warn("Could not resolve a public IP address — all {} fallback services failed", services.size)
    return null
}
