package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.Stat
import io.github.lishangbu.avalon.dataset.entity.dto.SaveStatInput
import io.github.lishangbu.avalon.dataset.entity.dto.StatSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.StatView
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateStatInput
import io.github.lishangbu.avalon.dataset.repository.StatRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class StatServiceImplTest {
    @Test
    fun listByCondition_callsRepository() {
        val repository = FakeStatRepository()
        val statService = StatServiceImpl(repository)
        val specification = StatSpecification(id = "1", internalName = "speed")
        repository.listResult =
            listOf(
                StatView(
                    "2",
                    "attack",
                    "攻击",
                    2,
                    false,
                    StatView.TargetOf_moveDamageClass("2", "physical", "物理"),
                ),
            )

        val result = statService.listByCondition(specification)

        assertEquals(1, result.size)
        assertEquals("2", result.first().id)
        assertEquals("physical", result.first().moveDamageClass?.internalName)
        assertEquals("物理", result.first().moveDamageClass?.name)
        assertEquals(specification, repository.listCondition)
    }

    @Test
    fun save_usesRepository() {
        val repository = FakeStatRepository()
        val statService = StatServiceImpl(repository)
        val command = SaveStatInput("attack", "攻击", 2, false, "2")
        repository.saveResult =
            Stat {
                id = 2L
                internalName = "attack"
                name = "攻击"
                gameIndex = 2
                battleOnly = false
                moveDamageClassId = 2L
            }
        repository.findByIdResult =
            StatView(
                "2",
                "attack",
                "攻击",
                2,
                false,
                StatView.TargetOf_moveDamageClass("2", "physical", "物理"),
            )

        val result = statService.save(command)

        assertEquals("2", result.id)
        assertEquals("物理", result.moveDamageClass?.name)
        assertEquals("attack", repository.savedStat!!.internalName)
        assertEquals(2L, repository.savedStat!!.moveDamageClassId)
        assertEquals(2L, repository.findByIdValue)
    }

    @Test
    fun update_usesRepository() {
        val repository = FakeStatRepository()
        val statService = StatServiceImpl(repository)
        val command = UpdateStatInput("2", "attack", "攻击", 2, false, "2")
        repository.saveResult =
            Stat {
                id = 2L
                internalName = "attack"
                name = "攻击"
                gameIndex = 2
                battleOnly = false
                moveDamageClassId = 2L
            }
        repository.findByIdResult =
            StatView(
                "2",
                "attack",
                "攻击",
                2,
                false,
                StatView.TargetOf_moveDamageClass("2", "physical", "物理"),
            )

        val result = statService.update(command)

        assertEquals("2", result.id)
        assertEquals("physical", result.moveDamageClass?.internalName)
        assertEquals(2L, repository.savedStat!!.id)
        assertEquals(2L, repository.findByIdValue)
    }

    @Test
    fun removeById_callsRepository() {
        val repository = FakeStatRepository()
        val statService = StatServiceImpl(repository)
        statService.removeById(1L)
        assertEquals(1L, repository.deletedId)
    }

    private class FakeStatRepository : StatRepository {
        var listCondition: StatSpecification? = null
        var savedStat: Stat? = null
        var deletedId: Long? = null
        var findByIdValue: Long? = null

        var listResult: List<StatView> = emptyList()
        var saveResult: Stat = Stat()
        var findByIdResult: StatView? = null

        override fun findAll(specification: StatSpecification?): List<StatView> {
            listCondition = specification
            return listResult
        }

        override fun findById(id: Long): StatView? {
            findByIdValue = id
            return findByIdResult
        }

        override fun save(stat: Stat): Stat {
            savedStat = stat
            return saveResult
        }

        override fun deleteById(id: Long) {
            deletedId = id
        }
    }
}
