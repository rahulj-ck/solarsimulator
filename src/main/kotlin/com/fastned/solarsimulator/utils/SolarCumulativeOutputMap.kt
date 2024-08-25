package com.fastned.solarsimulator.utils

import java.math.BigDecimal
import java.math.RoundingMode

object SolarCumulativeOutputMap {
    const val MAX_POWER_PLANT_AGE = 9125
    val solarOutputCumulativeMap: Map<Int, BigDecimal> = generateSolarOutputMap()

    private fun generateSolarOutputMap(): Map<Int, BigDecimal> =
        (0..MAX_POWER_PLANT_AGE).fold(mutableMapOf()) { map, age ->
            val powerOutput =
                if (age <= 60) {
                    BigDecimal.ZERO
                } else {
                    val ageRatio = BigDecimal(age).divide(BigDecimal(365), 10, RoundingMode.HALF_UP)
                    BigDecimal(20).multiply(BigDecimal.ONE.subtract(ageRatio.multiply(BigDecimal(0.005))))
                }

            map[age] = map.getOrDefault(age - 1, BigDecimal.ZERO).add(powerOutput)
            map
        }
}
