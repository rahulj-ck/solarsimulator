package com.fastned.solarsimulator.functional

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fastned.solar.simulator.model.PowerPlant
import com.fastned.solar.simulator.model.SimulationResultResponse
import com.fastned.solarsimulator.db.PowerPlantRepository
import org.jooq.DSLContext
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import java.math.BigDecimal

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class SolarSimulationFunctionalTests(
    @Autowired private val powerPlantRepository: PowerPlantRepository,
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val objectMapper: ObjectMapper = ObjectMapper().registerModule(KotlinModule()),
    @Autowired private val dslContext: DSLContext,
) : AbstractFunctionalTests(
        mockMvc,
        objectMapper,
        dslContext,
    ) {
    @Test
    fun `test getNetworkOutput`() {
        val t = 5
        getNetworkOutput(t)
        getNetworkOutputResponseBody(t)
    }

    @Test
    fun `test getNetworkState`() {
        val t = 5
        getNetworkState(t)
        getNetworkStateResponseBody(t)
    }

    @Test
    fun `test loadPowerPlants`() {
        val powerPlants = listOf(PowerPlant("Power plant 1", 55), PowerPlant("Power plant 2", 2))
        loadPowerPlants(powerPlants)

        val activeNetworks = powerPlantRepository.getAll()

        assert(powerPlantRepository.getAll().size == 2)

        val newPowerPlants = listOf(PowerPlant("Power plant 7", 55), PowerPlant("Power plant 6", 2), PowerPlant("Power plant 3", 88))
        loadPowerPlants(newPowerPlants)

        val activeNetwork = powerPlantRepository.getAll()
        assert(activeNetwork.size == 3)
        assert(activeNetwork.find { it.name == "Power plant 7" }?.let { it.age == 55 } ?: false)
        assert(activeNetwork.find { it.name == "Power plant 6" }?.let { it.age == 2 } ?: false)
        assert(activeNetwork.find { it.name == "Power plant 3" }?.let { it.age == 88 } ?: false)
        assert(activeNetwork.find { it.name == "Power plant 1" } == null)
        assert(activeNetwork.find { it.name == "Power plant 2" } == null)
    }

    @Test
    fun `upload power plants successfully`() {
        val jsonContent =
            """
            [
                {
                    "name": "Power plant 1",
                    "age": 55
                },
                {
                    "name": "Power plant 2",
                    "age": 2
                }
            ]
            """.trimIndent()

        val response = uploadPowerPlants(jsonContent, 5)

        val content = response.contentAsString
        val responseObj = objectMapper.readValue(content, SimulationResultResponse::class.java)

        println("Response: $responseObj")
        assert(responseObj.network.size == 2)
        assert(responseObj.producedKwh == BigDecimal.ZERO)
        assert(responseObj.network[0].name == "Power plant 1")
        assert(responseObj.network[1].name == "Power plant 2")
        assert(responseObj.network.find { it.name == "Power plant 1" }?.let { it.age == 59 } ?: false)
        assert(responseObj.network.find { it.name == "Power plant 2" }?.let { it.age == 6 } ?: false)
    }

    @Test
    fun `upload power plants with invalid JSON`() {
        val invalidJson =
            """
            [
                {
                    "name": "Power plant 1",
                    "age": 55
                },
                {
                    "name": "Power plant 2"
                }
            ]
            """.trimIndent()

        val response = uploadPowerPlants(invalidJson, 5)
        assert(response.status == 400)
        assert(response.contentAsString.contains("Invalid Json file format"))
    }
}
