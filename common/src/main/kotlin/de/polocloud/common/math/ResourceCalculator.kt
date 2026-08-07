package de.polocloud.common.math

import java.math.BigDecimal
import java.math.RoundingMode

fun convertBytesToMegabytes(bytes: Long?): Double {
    return BigDecimal(bytes ?: 0)
        .divide(BigDecimal(1024 * 1024), 2, RoundingMode.HALF_UP)
        .toDouble()
}