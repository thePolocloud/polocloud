package dev.httpmarco.polocloud.agent.utils

import java.lang.management.ManagementFactory
import com.sun.management.OperatingSystemMXBean


fun maxRuntimeMemory(): Int {
    val osBean = ManagementFactory.getOperatingSystemMXBean() as OperatingSystemMXBean
    val totalPhysicalMemoryMB = osBean.totalMemorySize / (1024 * 1024)
    return totalPhysicalMemoryMB.toInt()
}

fun maxRuntimeCores(): Int {
    return Runtime.getRuntime().availableProcessors()
}
