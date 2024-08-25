package com.fastned.solarsimulator.controller

import com.fastned.solar.simulator.api.SolarSimulatorApi
import com.fastned.solar.simulator.model.NetworkOutputResponse
import com.fastned.solar.simulator.model.PowerPlant
import com.fastned.solar.simulator.model.PowerPlantOutput
import com.fastned.solar.simulator.model.SimulationResultResponse
import com.fastned.solarsimulator.service.SolarSimulatorService
import org.springframework.core.io.Resource
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class SolarSimulatorController(
    private val solarSimulatorService: SolarSimulatorService,
) : SolarSimulatorApi {
    override fun getNetworkOutput(T: Int): ResponseEntity<NetworkOutputResponse> =
        ResponseEntity.ok(solarSimulatorService.getNetworkOutput(T))

    override fun getNetworkState(T: Int): ResponseEntity<List<PowerPlantOutput>> =
        ResponseEntity.ok(solarSimulatorService.getNetworkState(T))

    override fun loadPowerPlants(powerPlant: List<PowerPlant>): ResponseEntity<Unit> {
        solarSimulatorService.loadPowerPlants(powerPlant)
        return ResponseEntity.status(HttpStatus.RESET_CONTENT).build()
    }

    override fun uploadPowerPlants(
        T: Int,
        file: Resource,
    ): ResponseEntity<SimulationResultResponse> = ResponseEntity.ok(solarSimulatorService.uploadPowerPlants(T, file))
}
