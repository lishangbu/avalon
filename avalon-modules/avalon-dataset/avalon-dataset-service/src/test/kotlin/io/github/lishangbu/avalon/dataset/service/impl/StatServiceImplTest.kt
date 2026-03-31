package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.MoveDamageClass
import io.github.lishangbu.avalon.dataset.entity.Stat
import io.github.lishangbu.avalon.dataset.entity.dto.SaveStatInput
import io.github.lishangbu.avalon.dataset.entity.dto.StatSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.StatView
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateStatInput
import io.github.lishangbu.avalon.dataset.repository.StatRepository
import org.babyfish.jimmer.sql.ast.mutation.AssociatedSaveMode
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class StatServiceImplTest {
    private val repository = mock(StatRepository::class.java)
    private val service = StatServiceImpl(repository)

    @Test
    fun listByCondition_callsRepository() {
        val specification = StatSpecification(id = "1", internalName = "speed")
        `when`(repository.listViews(specification)).thenReturn(
            listOf(
                StatView(
                    "2",
                    "attack",
                    "攻击",
                    2,
                    false,
                    false,
                    StatView.TargetOf_moveDamageClass("2", "physical", "物理"),
                ),
            ),
        )

        val result = service.listByCondition(specification)

        assertEquals(1, result.size)
        assertEquals("2", result.first().id)
        assertEquals("physical", result.first().moveDamageClass?.internalName)
    }

    @Test
    fun save_usesInsertOnlyModeAndReloadsView() {
        `when`(repository.save(any<Stat>(), eq(SaveMode.INSERT_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())).thenReturn(statSavedEntity(2L))
        `when`(repository.loadViewById(2L)).thenReturn(statView("2", "attack", "攻击"))

        val result = service.save(SaveStatInput("attack", "攻击", 2, false, false, "2"))

        assertEquals("2", result.id)
        assertEquals("物理", result.moveDamageClass?.name)
        verify(repository).save(any<Stat>(), eq(SaveMode.INSERT_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())
        verify(repository).loadViewById(2L)
    }

    @Test
    fun update_usesUpsertModeAndReloadsView() {
        `when`(repository.findNullable(2L)).thenReturn(statSavedEntity(2L))
        `when`(repository.save(any<Stat>(), eq(SaveMode.UPDATE_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())).thenReturn(statSavedEntity(2L))
        `when`(repository.loadViewById(2L)).thenReturn(statView("2", "attack", "攻击"))

        val result = service.update(UpdateStatInput("2", "attack", "攻击", 2, false, false, "2"))

        assertEquals("2", result.id)
        assertEquals("physical", result.moveDamageClass?.internalName)
        verify(repository).save(any<Stat>(), eq(SaveMode.UPDATE_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())
        verify(repository).loadViewById(2L)
    }

    @Test
    fun removeById_callsRepository() {
        `when`(repository.findNullable(1L)).thenReturn(statSavedEntity(1L))

        service.removeById(1L)

        verify(repository).deleteById(1L)
    }

    @Test
    fun update_rejectsReadonlyStat() {
        `when`(repository.findNullable(2L)).thenReturn(statSavedEntity(2L, readonly = true))

        val error =
            assertThrows(IllegalStateException::class.java) {
                service.update(UpdateStatInput("2", "attack", "攻击", 2, false, true, "2"))
            }

        assertEquals("能力值已设为只读，禁止修改", error.message)
    }

    @Test
    fun removeById_rejectsReadonlyStat() {
        `when`(repository.findNullable(1L)).thenReturn(statSavedEntity(1L, readonly = true))

        val error =
            assertThrows(IllegalStateException::class.java) {
                service.removeById(1L)
            }

        assertEquals("能力值已设为只读，禁止删除", error.message)
    }
}

private fun statSavedEntity(
    id: Long,
    readonly: Boolean = false,
): Stat =
    Stat {
        this.id = id
        internalName = "attack"
        name = "攻击"
        gameIndex = 2
        battleOnly = false
        this.readonly = readonly
        moveDamageClass =
            MoveDamageClass {
                this.id = 2L
            }
    }

private fun statView(
    id: String,
    internalName: String,
    name: String,
    readonly: Boolean = false,
): StatView =
    StatView(
        id,
        internalName,
        name,
        2,
        false,
        readonly,
        StatView.TargetOf_moveDamageClass("2", "physical", "物理"),
    )
