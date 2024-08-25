package com.fastned.solarsimulator.functional

import com.fasterxml.jackson.databind.ObjectMapper
import com.fastned.solar.simulator.model.NetworkOutputResponse
import com.fastned.solar.simulator.model.PowerPlant
import com.fastned.solar.simulator.model.PowerPlantOutput
import org.jooq.DSLContext
import org.jooq.generated.Tables
import org.junit.jupiter.api.BeforeEach
import org.springframework.http.MediaType
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.io.File
import kotlin.random.Random

@ActiveProfiles("test")
abstract class AbstractFunctionalTests(
    private var mockMvc: MockMvc,
    private var objectMapper: ObjectMapper,
    private var dslContext: DSLContext,
) {
    @BeforeEach
    fun clearDatabase() {
        dslContext.deleteFrom(Tables.POWER_PLANT).execute()
    }

    fun getNetworkState(T: Int): MockHttpServletResponse {
        val url = "/solar-simulator/network/{id}"
        return mockMvc
            .perform(get(url, T))
            .andExpect(status().isOk)
            .andExpect(content().string(org.hamcrest.Matchers.notNullValue()))
            .andReturn()
            .response
    }

    fun getNetworkStateResponseBody(T: Int): List<PowerPlantOutput> =
        objectMapper.readValue(getNetworkState(T).contentAsString, Array<PowerPlantOutput>::class.java).toList()

    fun getNetworkOutput(T: Int): MockHttpServletResponse {
        val url = "/solar-simulator/output/{id}"
        return mockMvc
            .perform(get(url, T))
            .andReturn()
            .response
    }

    fun getNetworkOutputResponseBody(T: Int): NetworkOutputResponse =
        objectMapper.readValue(getNetworkOutput(T).contentAsString, NetworkOutputResponse::class.java)

    fun loadPowerPlants(powerPlants: List<PowerPlant>): MockHttpServletResponse {
        val url = "/solar-simulator/load"
        return mockMvc
            .perform(
                post(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(powerPlants)),
            ).andReturn()
            .response
    }

    fun uploadPowerPlants(
        jsonContent: String,
        t: Int,
    ): MockHttpServletResponse {
        val url = "/solar-simulator/upload"
        val jsonFile = File.createTempFile("temp", ".json")
        jsonFile.writeText(jsonContent)

        val response =
            mockMvc
                .perform(
                    multipart(url)
                        .file("file", jsonFile.readBytes())
                        .param("T", t.toString()),
                ).andReturn()
                .response
        jsonFile.delete()
        return response
    }

    fun generateRandomString(length: Int): String {
        val alphabet = ('a'..'z') + ('A'..'Z')
        return List(length) { alphabet.random() }.joinToString("")
    }

    fun generateRandomAge(): Int = Random.nextInt(1, 10000)
}
