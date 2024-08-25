package com.fastned.solarsimulator.service

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fastned.solar.simulator.model.NetworkOutputResponse
import com.fastned.solar.simulator.model.PowerPlant
import com.fastned.solar.simulator.model.PowerPlantOutput
import com.fastned.solar.simulator.model.SimulationResultResponse
import com.fastned.solarsimulator.db.PowerPlantRepository
import com.fastned.solarsimulator.service.SolarSimulatorService.Constants.SUN_HOURS_PER_DAY
import com.fastned.solarsimulator.utils.SolarCumulativeOutputMap
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service
import java.io.IOException
import java.math.BigDecimal
import java.math.RoundingMode

@Service
class SolarSimulatorService(
    private val powerPlantDb: PowerPlantRepository,
    private val objectMapper: ObjectMapper,
    private val solarSimulatorValidator: SolarSimulatorValidator,
) {
    object Constants {
        val SUN_HOURS_PER_DAY: BigDecimal = BigDecimal("1000").divide(BigDecimal("365"), 10, RoundingMode.HALF_UP)
    }

    private fun calculatePowerOutput(
        age: Int,
        t: Int,
    ): BigDecimal {
        val solarOutput =
            SolarCumulativeOutputMap.solarOutputCumulativeMap.getOrDefault(t + age - 1, BigDecimal.ZERO).subtract(
                SolarCumulativeOutputMap.solarOutputCumulativeMap.getOrDefault(age - 1, BigDecimal.ZERO),
            )
        return solarOutput.multiply(SUN_HOURS_PER_DAY).setScale(10, RoundingMode.HALF_UP).stripTrailingZeros()
    }

    fun getNetworkState(t: Int): List<PowerPlantOutput> {
        solarSimulatorValidator.validateT(t)
        val powerPlants = powerPlantDb.getAll()
        return powerPlants.map {
            val powerOutput = calculatePowerOutput(it.age, t)
            PowerPlantOutput(it.name, it.age, powerOutput)
        }
    }

    fun getNetworkOutput(t: Int): NetworkOutputResponse {
        solarSimulatorValidator.validateT(t)
        val energyOutputs =
            powerPlantDb.getAll().map {
                calculatePowerOutput(it.age, t)
            }
        val totalEnergyOutput = energyOutputs.fold(BigDecimal.ZERO) { acc, powerOutput -> acc + powerOutput }
        return NetworkOutputResponse(totalEnergyOutput)
    }

    fun loadPowerPlants(powerPlant: List<PowerPlant>) {
        solarSimulatorValidator.validateNetwork(powerPlant)
        powerPlantDb.createBatch(powerPlant)
    }

    fun uploadPowerPlants(
        t: Int,
        file: Resource,
    ): SimulationResultResponse {
        val powerPlants =
            try {
                objectMapper.readValue(file.inputStream, Array<PowerPlant>::class.java).toList()
            } catch (e: JsonProcessingException) {
                throw IllegalArgumentException("Invalid Json file format", e)
            } catch (e: IOException) {
                throw IllegalArgumentException("Error reading file", e)
            }

        solarSimulatorValidator.validateT(t)
        solarSimulatorValidator.validateNetwork(powerPlants)

        val network = powerPlants.map { PowerPlant(it.name, it.age.plus(t - 1)) }
        val producedKwh =
            powerPlants.sumOf {
                calculatePowerOutput(it.age, t)
            }
        return SimulationResultResponse(producedKwh, network)
    }
}
