package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.BerryFlavor
import io.github.lishangbu.avalon.dataset.entity.Nature
import io.github.lishangbu.avalon.dataset.entity.Stat
import io.github.lishangbu.avalon.dataset.entity.dto.NatureSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateNatureInput
import jakarta.annotation.Resource
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.transaction.annotation.Transactional

@Transactional
class NatureRepositoryTest : AbstractRepositoryTest() {
    @Resource
    private lateinit var natureRepository: NatureRepository

    @Test
    fun shouldListAndCrudNature() {
        val condition = NatureSpecification(internalName = "bold")

        val results = natureRepository.findAll(condition)

        assertFalse(results.isEmpty())
        assertEquals(2L, results.first().id)
        assertEquals("bold", results.first().internalName)
        assertEquals(2L, results.first().decreasedStat?.id)
        assertEquals("attack", results.first().decreasedStat?.internalName)
        assertEquals("辣", results.first().hatesBerryFlavor?.name)

        val neutral = requireNotNull(natureRepository.loadByIdWithAssociations(1L))
        assertEquals("hardy", neutral.internalName)
        assertNull(neutral.decreasedStat)
        assertNull(neutral.likesBerryFlavor)

        val saved =
            natureRepository.save(
                Nature {
                    internalName = "unit-nature"
                    name = "单元测试性格"
                    decreasedStat =
                        Stat {
                            id = 2L
                        }
                    increasedStat =
                        Stat {
                            id = 3L
                        }
                    hatesBerryFlavor =
                        BerryFlavor {
                            id = 1L
                        }
                    likesBerryFlavor =
                        BerryFlavor {
                            id = 5L
                        }
                },
                SaveMode.INSERT_ONLY,
            )

        val inserted = requireNotNull(natureRepository.loadByIdWithAssociations(saved.id))
        assertEquals("单元测试性格", inserted.name)
        assertEquals("攻击", inserted.decreasedStat?.name)
        assertEquals("酸", inserted.likesBerryFlavor?.name)

        natureRepository.save(
            Nature(inserted) {
                name = "更新后的性格"
                decreasedStat = null
                increasedStat = null
                hatesBerryFlavor = null
                likesBerryFlavor = null
            },
            SaveMode.UPSERT,
        )
        val updated = requireNotNull(natureRepository.loadByIdWithAssociations(saved.id))
        assertEquals("更新后的性格", updated.name)
        assertNull(updated.decreasedStat)
        assertNull(updated.likesBerryFlavor)

        natureRepository.deleteById(saved.id)
        assertNull(natureRepository.loadByIdWithAssociations(saved.id))
    }

    @Test
    fun shouldUpdateNatureFromFlatInputPayload() {
        val command =
            UpdateNatureInput(
                id = "7",
                internalName = "docile",
                name = "坦率",
                decreasedStatId = "2",
                increasedStatId = "3",
                hatesBerryFlavorId = "1",
                likesBerryFlavorId = "5",
            )

        natureRepository.save(command.toEntity(), SaveMode.UPSERT)

        val updated = requireNotNull(natureRepository.loadByIdWithAssociations(7L))
        assertEquals(7L, updated.id)
        assertEquals("docile", updated.internalName)
        assertEquals(2L, updated.decreasedStat?.id)
        assertEquals(3L, updated.increasedStat?.id)
        assertEquals(1L, updated.hatesBerryFlavor?.id)
        assertEquals(5L, updated.likesBerryFlavor?.id)
        assertTrue(updated.likesBerryFlavor?.name == "酸")
    }
}
