package io.github.lishangbu.avalon.authorization.service.impl

import io.github.lishangbu.avalon.authorization.entity.dto.MenuSpecification
import io.github.lishangbu.avalon.authorization.repository.MenuRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`

class MenuServiceImplTest {
    private val menuRepository = mock(MenuRepository::class.java)
    private val service = MenuServiceImpl(menuRepository)

    @Test
    fun returnsEmptyTreeWhenRoleCodesAreEmpty() {
        assertTrue(service.listMenuTreeByRoleCodes(emptyList()).isEmpty())
        verifyNoInteractions(menuRepository)
    }

    @Test
    fun buildsRoleBasedMenuTree() {
        val root = menu(1L, parentId = null, label = "Root", sortingOrder = 1)
        val child = menu(2L, parentId = 1L, label = "Child", sortingOrder = 2)
        `when`(menuRepository.findAllByRoleCodes(listOf("ADMIN"))).thenReturn(listOf(root, child))

        val result = service.listMenuTreeByRoleCodes(listOf("ADMIN"))

        assertEquals(1, result.size)
        assertEquals(1L, result.single().id)
        assertEquals(2L, result.single().children!!.single().id)
    }

    @Test
    fun returnsEmptyListWhenMenuSourcesAreMissing() {
        val specification = mock(MenuSpecification::class.java)
        `when`(menuRepository.findAllByOrderBySortingOrderAscIdAsc()).thenReturn(emptyList())

        assertTrue(service.listAllMenuTree(specification).isEmpty())

        val root = menu(1L, parentId = null, label = "Root", sortingOrder = 1)
        `when`(menuRepository.findAllByOrderBySortingOrderAscIdAsc()).thenReturn(listOf(root))
        `when`(menuRepository.findAll(same(specification), any())).thenReturn(emptyList())

        assertTrue(service.listAllMenuTree(specification).isEmpty())
    }

    @Test
    fun listAllMenuTreeIncludesAncestorsAndDescendantsOfMatchedMenus() {
        val specification = mock(MenuSpecification::class.java)
        val root = menu(1L, parentId = null, label = "Root", sortingOrder = 1)
        val child = menu(2L, parentId = 1L, label = "Child", sortingOrder = 2)
        val grandChild = menu(3L, parentId = 2L, label = "Grandchild", sortingOrder = 3)
        val sibling = menu(4L, parentId = 2L, label = "Sibling", sortingOrder = 4)
        val unrelated = menu(5L, parentId = null, label = "Unrelated", sortingOrder = 5)
        `when`(menuRepository.findAllByOrderBySortingOrderAscIdAsc()).thenReturn(listOf(root, child, grandChild, sibling, unrelated))
        `when`(menuRepository.findAll(same(specification), any())).thenReturn(listOf(child))

        val result = service.listAllMenuTree(specification)

        assertEquals(listOf(1L), result.map { it.id })
        val children = result.single().children!!
        assertEquals(listOf(2L), children.map { it.id })
        assertEquals(setOf(3L, 4L), children.single().children!!.map { it.id }.toSet())
    }

    @Test
    fun delegatesCrudOperations() {
        val found = menu(9L, label = "Settings")
        val saved = menu(10L, label = "Saved")
        val updated = menu(11L, label = "Updated")
        `when`(menuRepository.findById(9L)).thenReturn(found)
        `when`(menuRepository.save(any())).thenReturn(saved).thenReturn(updated)

        assertSame(found, service.getById(9L))
        assertSame(saved, service.save(menu(10L, label = "New")))
        assertSame(updated, service.update(menu(11L, label = "Changed")))
        service.removeById(12L)

        verify(menuRepository).deleteById(12L)
    }
}
