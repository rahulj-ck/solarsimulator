package com.fastned.solarsimulator.db

import com.fastned.solar.simulator.model.PowerPlant

interface EntityRepository<T> {
    fun createBatch(entities: List<T>): List<Int>

    fun getAll(): List<T>

    fun update(
        id: Int,
        entity: T,
    ): Boolean

    fun delete(id: Int): Boolean
}

interface PowerPlantRepository : EntityRepository<PowerPlant>
