package com.fastned.solarsimulator.db.jooq

import com.fastned.solar.simulator.model.PowerPlant
import com.fastned.solarsimulator.db.PowerPlantRepository
import org.jooq.DSLContext
import org.jooq.TransactionalRunnable
import org.jooq.generated.Tables
import org.jooq.generated.tables.records.PowerPlantRecord
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Repository
class JOOQPowerPlantRepositoryImpl(
    val ctx: DSLContext,
) : PowerPlantRepository {
    override fun createBatch(entities: List<PowerPlant>): List<Int> {
        var insertedRecords = mutableListOf<Int>()
        this.ctx.transaction(
            TransactionalRunnable {
                ctx
                    .update(Tables.POWER_PLANT)
                    .set(Tables.POWER_PLANT.OPERATIONAL, false)
                    .execute()

                insertedRecords =
                    ctx
                        .batchInsert(
                            entities.map {
                                val setupOn = LocalDate.now().minusDays(it.age.toLong())
                                PowerPlantRecord().apply {
                                    this.name = it.name
                                    this.operational = true
                                    this.setupOn = setupOn.toString()
                                }
                            },
                        ).execute()
                        .toMutableList()
            },
        )
        return insertedRecords
    }

    override fun getAll(): List<PowerPlant> =
        ctx
            .selectFrom(Tables.POWER_PLANT)
            .where(Tables.POWER_PLANT.OPERATIONAL.eq(true))
            .fetch()
            .map {
                val createdDate = LocalDate.parse(it.setupOn)
                val age = ChronoUnit.DAYS.between(createdDate, LocalDate.now()).toInt()
                PowerPlant(
                    it.name,
                    age,
                )
            }

    override fun update(
        id: Int,
        entity: PowerPlant,
    ): Boolean {
        TODO("Not yet implemented")
    }

    override fun delete(id: Int): Boolean {
        TODO("Not yet implemented")
    }
}
