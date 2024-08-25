package com.fastned.solarsimulator.service

import com.fastned.solar.simulator.model.PowerPlant
import org.springframework.stereotype.Component

@Component
class SolarSimulatorValidator {
    fun validateT(t: Int): Int {
        if (t <= 0) {
            throw IllegalArgumentException("Invalid input value")
        }
        return t
    }

    fun validateNetwork(powerPlants: List<PowerPlant>) {
        powerPlants.forEach { plant ->
            if (plant.name.isBlank()) {
                throw IllegalArgumentException("Power plant name cannot be empty")
            }
            if (plant.age < 0) {
                throw IllegalArgumentException("Power plant age cannot be negative")
            }
        }
    }
}
