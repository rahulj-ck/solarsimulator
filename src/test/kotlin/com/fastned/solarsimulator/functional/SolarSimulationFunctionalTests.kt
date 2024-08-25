package com.fastned.solarsimulator.functional

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fastned.solar.simulator.model.PowerPlant
import com.fastned.solar.simulator.model.SimulationResultResponse
import org.jooq.DSLContext
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import java.math.BigDecimal
import kotlin.random.Random

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class SolarSimulationFunctionalTests(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val objectMapper: ObjectMapper = ObjectMapper().registerModule(KotlinModule()),
    @Autowired private val dslContext: DSLContext,
) : AbstractFunctionalTests(
        mockMvc,
        objectMapper,
        dslContext,
    ) {
    @Test
    fun `get network state should return empty response if network is not loaded`() {
        val response = getNetworkStateResponseBody(5)
        assert(response.isEmpty())
    }

    @Test
    fun `get network output should return empty response if not network is not loaded`() {
        val response = getNetworkOutputResponseBody(5)
        assert(response.totalOutputInKwh == BigDecimal.ZERO)
    }

    @Test
    fun `load power plant network and verify energy output is zero for power plants younger than 60 days`() {
        val numberOfPowerPlants = Random.nextInt(100)
        val powerPlants =
            List(numberOfPowerPlants) {
                PowerPlant(generateRandomString(100), generateRandomAge())
            }

        val response = loadPowerPlants(powerPlants)
        assert(response.status == 205)

        val t = 25
        val networkStateResponse = getNetworkStateResponseBody(t)
        assert(networkStateResponse.size == numberOfPowerPlants)

        networkStateResponse.forEachIndexed { index, powerPlant ->
            assert(powerPlant.name == powerPlants[index].name)
            assert(powerPlant.age == powerPlants[index].age + t - 1)
            if (powerPlant.age <= 60) {
                assert(powerPlant.outputInKwh == BigDecimal.ZERO)
            }
            if (powerPlant.age > 9125) {
                assert(powerPlant.outputInKwh == BigDecimal.ZERO)
            }
        }
    }

    @Test
    fun `load power plant network and verify energy output is calculated correctly for power plants older than 60 days`() {
        val numberOfPowerPlants = Random.nextInt(100)
        val powerPlants =
            List(numberOfPowerPlants) {
                PowerPlant(generateRandomString(100), generateRandomAge() + 60)
            }

        val response = loadPowerPlants(powerPlants)
        assert(response.status == 205)

        val t = 20
        val networkStateResponse = getNetworkStateResponseBody(t)
        assert(networkStateResponse.size == numberOfPowerPlants)

        networkStateResponse.forEachIndexed { index, powerPlant ->
            assert(powerPlant.name == powerPlants[index].name)
            assert(powerPlant.age == powerPlants[index].age + t - 1)
            if (powerPlant.age in 61..9125) {
                assert(powerPlant.outputInKwh > BigDecimal.ZERO)
            }
        }
    }

    @Test
    fun `load power plant network, verify they are updated and the power output is calculated correctly for different values of T`() {
        val newPowerPlants = listOf(PowerPlant("Power plant 7", 55), PowerPlant("Power plant 6", 2), PowerPlant("Power plant 3", 88))
        loadPowerPlants(newPowerPlants)

        val t = 5
        val networkState = getNetworkStateResponseBody(t)
        assert(networkState.size == 3)
        assert(networkState.find { it.name == "Power plant 7" }?.let { it.age == 59 } ?: false)
        assert(networkState.find { it.name == "Power plant 7" }?.let { it.outputInKwh == BigDecimal.ZERO } ?: false)
        assert(networkState.find { it.name == "Power plant 6" }?.let { it.age == 6 } ?: false)
        assert(networkState.find { it.name == "Power plant 6" }?.let { it.outputInKwh == BigDecimal.ZERO } ?: false)

        assert(networkState.find { it.name == "Power plant 3" }?.let { it.age == 92 } ?: false)
    }

    @Test
    fun `load power plant network and verify that power plant older than 25 years is not operational`() {
        val powerPlants = listOf(PowerPlant("Power plant 10", 55), PowerPlant("Power plant 11", 2), PowerPlant("Power plant 12", 9126))
        val response = loadPowerPlants(powerPlants)
        assert(response.status == 205)

        val networkStateResponse = getNetworkStateResponseBody(5)
        println("networkStateResponse: $networkStateResponse")
        assert(networkStateResponse.size == 3)

        assert(networkStateResponse.find { it.name == "Power plant 12" }?.let { it.outputInKwh == BigDecimal.ZERO } ?: false)
    }

    @Test
    fun `load power plant networks with invalid age should return an error`() {
        val powerPlants = listOf(PowerPlant("Power plant 1", 55), PowerPlant("Power plant 2", -2))
        val response = loadPowerPlants(powerPlants)
        assert(response.status == 400)
        assert(response.contentAsString.contains("Power plant age cannot be negative"))
    }

    @Test
    fun `load power plant networks with empty name should return an error`() {
        val powerPlants = listOf(PowerPlant("Power plant 1", 55), PowerPlant("", 2))
        val response = loadPowerPlants(powerPlants)
        assert(response.status == 400)
        assert(response.contentAsString.contains("Power plant name cannot be empty"))
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

        assert(responseObj.network.size == 2)
        assert(responseObj.producedKwh == BigDecimal.ZERO)
        assert(responseObj.network[0].name == "Power plant 1")
        assert(responseObj.network[1].name == "Power plant 2")
        assert(responseObj.network.find { it.name == "Power plant 1" }?.let { it.age == 59 } ?: false)
        assert(responseObj.network.find { it.name == "Power plant 2" }?.let { it.age == 6 } ?: false)
    }

    @Test
    fun `upload power plants with invalid JSON should return an error`() {
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

    @Test
    fun `upload power plants with invalid age should return an error`() {
        val invalidJson =
            """
            [
                {
                    "name": "Power plant 1",
                    "age": 55
                },
                {
                    "name": "Power plant 2",
                    "age": -2
                }
            ]
            """.trimIndent()

        val response = uploadPowerPlants(invalidJson, 5)
        assert(response.status == 400)
        assert(response.contentAsString.contains("Power plant age cannot be negative"))
    }
}
