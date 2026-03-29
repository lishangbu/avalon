package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.BerryFlavor
import io.github.lishangbu.avalon.dataset.entity.Nature
import io.github.lishangbu.avalon.dataset.entity.Stat
import io.github.lishangbu.avalon.dataset.entity.dto.NatureSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.NatureView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveNatureInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateNatureInput
import io.github.lishangbu.avalon.dataset.repository.NatureRepository
import org.babyfish.jimmer.sql.ast.mutation.AssociatedSaveMode
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class NatureServiceImplTest {
    private val repository = mock(NatureRepository::class.java)
    private val service = NatureServiceImpl(repository)

    @Test
    fun listByCondition_callsRepository() {
        val specification = NatureSpecification(id = "2", internalName = "bold")
        `when`(repository.listViews(specification)).thenReturn(listOf(natureView(2L)))

        val result = service.listByCondition(specification)

        assertEquals(1, result.size)
        assertEquals("2", result.first().id)
        assertEquals("attack", result.first().decreasedStat?.internalName)
    }

    @Test
    fun save_usesInsertOnlyModeAndReloadsView() {
        `when`(repository.save(any<Nature>(), eq(SaveMode.INSERT_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())).thenReturn(natureSavedEntity(2L))
        `when`(repository.loadViewById(2L)).thenReturn(natureView(2L))

        val result =
            service.save(
                SaveNatureInput(
                    internalName = "bold",
                    name = "大胆",
                    decreasedStatId = "2",
                    increasedStatId = "3",
                    hatesBerryFlavorId = "1",
                    likesBerryFlavorId = "5",
                ),
            )

        assertEquals("2", result.id)
        assertEquals("attack", result.decreasedStat?.internalName)
        verify(repository).save(any<Nature>(), eq(SaveMode.INSERT_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())
        verify(repository).loadViewById(2L)
    }

    @Test
    fun update_usesUpsertModeAndReloadsView() {
        `when`(repository.save(any<Nature>(), eq(SaveMode.UPSERT), eq(AssociatedSaveMode.REPLACE), isNull())).thenReturn(natureSavedEntity(2L))
        `when`(repository.loadViewById(2L)).thenReturn(natureView(2L))

        val result =
            service.update(
                UpdateNatureInput(
                    id = "2",
                    internalName = "bold",
                    name = "大胆",
                    decreasedStatId = "2",
                    increasedStatId = "3",
                    hatesBerryFlavorId = "1",
                    likesBerryFlavorId = "5",
                ),
            )

        assertEquals("2", result.id)
        assertEquals("防御", result.increasedStat?.name)
        verify(repository).save(any<Nature>(), eq(SaveMode.UPSERT), eq(AssociatedSaveMode.REPLACE), isNull())
        verify(repository).loadViewById(2L)
    }

    @Test
    fun removeById_callsRepository() {
        service.removeById(2L)

        verify(repository).deleteById(2L)
    }
}

private fun natureSavedEntity(id: Long): Nature =
    Nature {
        this.id = id
        internalName = "bold"
        name = "大胆"
        decreasedStat =
            Stat {
                this.id = 2L
            }
        increasedStat =
            Stat {
                this.id = 3L
            }
        hatesBerryFlavor =
            BerryFlavor {
                this.id = 1L
            }
        likesBerryFlavor =
            BerryFlavor {
                this.id = 5L
            }
    }

private fun natureWithAssociations(id: Long): Nature =
    Nature {
        this.id = id
        internalName = "bold"
        name = "大胆"
        decreasedStat =
            Stat {
                this.id = 2L
                internalName = "attack"
                name = "攻击"
            }
        increasedStat =
            Stat {
                this.id = 3L
                internalName = "defense"
                name = "防御"
            }
        hatesBerryFlavor =
            BerryFlavor {
                this.id = 1L
                internalName = "spicy"
                name = "辣"
            }
        likesBerryFlavor =
            BerryFlavor {
                this.id = 5L
                internalName = "sour"
                name = "酸"
            }
    }

private fun natureView(id: Long): NatureView = NatureView(natureWithAssociations(id))
