package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.MoveDamageClass
import io.github.lishangbu.avalon.dataset.entity.Stat
import io.github.lishangbu.avalon.dataset.entity.dto.StatSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateStatInput
import jakarta.annotation.Resource
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.springframework.transaction.annotation.Transactional

@Transactional
class StatRepositoryTest : AbstractRepositoryTest() {
    @Resource
    private lateinit var statRepository: StatRepository

    @Test
    fun shouldListAndCrudStat() {
        val condition = StatSpecification(internalName = "attack")

        val results = statRepository.listViews(condition)

        assertFalse(results.isEmpty())
        assertEquals("2", results.first().id)
        assertEquals("attack", results.first().internalName)
        assertEquals(true, results.first().readonly)
        assertEquals("2", results.first().moveDamageClass?.id)
        assertEquals("physical", results.first().moveDamageClass?.internalName)
        assertEquals("物理", results.first().moveDamageClass?.name)

        val existing = requireNotNull(statRepository.loadViewById(2L))
        assertEquals("attack", existing.internalName)
        assertEquals("物理", existing.moveDamageClass?.name)

        val saved =
            statRepository.save(
                Stat {
                    id = 990002L
                    internalName = "unit-stat"
                    name = "单元测试能力"
                    gameIndex = 99
                    battleOnly = false
                    readonly = false
                    moveDamageClass =
                        MoveDamageClass {
                            this.id = 2L
                        }
                },
                SaveMode.INSERT_ONLY,
            )

        val inserted = requireNotNull(statRepository.listViews(StatSpecification(internalName = "unit-stat")).firstOrNull())
        assertEquals(saved.id.toString(), inserted.id)
        assertEquals("单元测试能力", inserted.name)

        statRepository.save(inserted.toEntity { name = "更新后的能力" }, SaveMode.UPSERT)
        val updated = requireNotNull(statRepository.listViews(StatSpecification(internalName = "unit-stat")).firstOrNull())
        assertEquals("更新后的能力", updated.name)

        statRepository.deleteById(saved.id)
        assertTrue(statRepository.listViews(StatSpecification(internalName = "unit-stat")).isEmpty())
    }

    @Test
    fun shouldUpdateStatFromFlatInputPayload() {
        val command =
            UpdateStatInput(
                id = "8",
                name = "闪避",
                internalName = "evasion",
                gameIndex = 0,
                battleOnly = true,
                readonly = true,
                moveDamageClassId = "1",
            )

        statRepository.save(command.toEntity(), SaveMode.UPSERT)

        val updated = requireNotNull(statRepository.loadViewById(8L))
        assertEquals("8", updated.id)
        assertEquals("闪避", updated.name)
        assertEquals("evasion", updated.internalName)
        assertEquals(true, updated.battleOnly)
        assertEquals(true, updated.readonly)
        assertEquals("1", updated.moveDamageClass?.id)
    }

    private fun assertTrue(value: Boolean) {
        assertEquals(true, value)
    }
}
