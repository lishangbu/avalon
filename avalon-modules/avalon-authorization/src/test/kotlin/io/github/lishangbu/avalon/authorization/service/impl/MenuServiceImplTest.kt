package io.github.lishangbu.avalon.authorization.service.impl

import io.github.lishangbu.avalon.authorization.entity.Menu
import io.github.lishangbu.avalon.authorization.entity.Role
import io.github.lishangbu.avalon.authorization.entity.dto.MenuSpecification
import io.github.lishangbu.avalon.authorization.repository.AuthorizationFetchers
import io.github.lishangbu.avalon.authorization.repository.MenuRepository
import io.github.lishangbu.avalon.authorization.repository.RoleRepository
import io.github.lishangbu.avalon.jimmer.support.readOrNull
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.data.domain.Sort

class MenuServiceImplTest {
    private val menuRepository = mock(MenuRepository::class.java)
    private val roleRepository = mock(RoleRepository::class.java)
    private val service = MenuServiceImpl(menuRepository, roleRepository)
    private val menuTreeSort = Sort.by(Sort.Order.asc("sortingOrder"), Sort.Order.asc("id"))

    @Test
    fun returnsEmptyTreeWhenRoleCodesAreEmpty() {
        assertTrue(service.listMenuTreeByRoleCodes(emptyList()).isEmpty())
        verifyNoInteractions(menuRepository, roleRepository)
    }

    @Test
    fun buildsRoleBasedMenuTree() {
        val root = menu(1L, parentId = null, label = "Root", sortingOrder = 1)
        val child = menu(2L, parentId = 1L, label = "Child", sortingOrder = 2)
        `when`(menuRepository.findAllByRoleCodes(listOf("ADMIN"))).thenReturn(listOf(root, child))

        val result = service.listMenuTreeByRoleCodes(listOf("ADMIN"))

        assertEquals(1, result.size)
        assertEquals(1L, result.single().id)
        assertEquals(
            2L,
            result
                .single()
                .children!!
                .single()
                .id,
        )
    }

    @Test
    fun returnsEmptyListWhenMenuSourcesAreMissing() {
        val specification = mock(MenuSpecification::class.java)
        `when`(menuRepository.findAllByOrderBySortingOrderAscIdAsc()).thenReturn(emptyList())

        assertTrue(service.listAllMenuTree(specification).isEmpty())

        val root = menu(1L, parentId = null, label = "Root", sortingOrder = 1)
        `when`(menuRepository.findAllByOrderBySortingOrderAscIdAsc()).thenReturn(listOf(root))
        `when`(menuRepository.findAll(specification, menuTreeSort)).thenReturn(emptyList())

        assertTrue(service.listAllMenuTree(specification).isEmpty())
    }

    @Test
    fun listAllMenuTreeReturnsEmptyWhenMatchedMenuIsMissingFromSourceTree() {
        val specification = mock(MenuSpecification::class.java)
        val root = menu(1L, parentId = null, label = "Root", sortingOrder = 1)
        `when`(menuRepository.findAllByOrderBySortingOrderAscIdAsc()).thenReturn(listOf(root))
        `when`(menuRepository.findAll(specification, menuTreeSort)).thenReturn(listOf(menu(99L, parentId = 100L, label = "Missing")))

        val result = service.listAllMenuTree(specification)

        assertTrue(result.isEmpty())
    }

    @Test
    fun listAllMenuTreeHandlesCyclesWithoutInfiniteLoop() {
        val specification = mock(MenuSpecification::class.java)
        val cyclicMenu = menu(1L, parentId = 1L, label = "Cycle", sortingOrder = 1)
        `when`(menuRepository.findAllByOrderBySortingOrderAscIdAsc()).thenReturn(listOf(cyclicMenu))
        `when`(menuRepository.findAll(specification, menuTreeSort)).thenReturn(listOf(cyclicMenu))

        val result = service.listAllMenuTree(specification)

        assertTrue(result.isEmpty())
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
        `when`(menuRepository.findAll(specification, menuTreeSort)).thenReturn(listOf(child))

        val result = service.listAllMenuTree(specification)

        assertEquals(listOf(1L), result.map { it.id })
        val children = result.single().children!!
        assertEquals(listOf(2L), children.map { it.id })
        assertEquals(
            setOf(3L, 4L),
            children
                .single()
                .children!!
                .map { it.id }
                .toSet(),
        )
    }

    @Test
    fun delegatesCrudOperations() {
        val found = menu(9L, label = "Settings")
        val saved = menu(10L, label = "Saved")
        val updated = menu(11L, label = "Updated")
        `when`(menuRepository.findNullable(9L, AuthorizationFetchers.MENU)).thenReturn(found)
        `when`(menuRepository.findAll(MenuSpecification(parentId = 12L))).thenReturn(emptyList())
        `when`(menuRepository.save(any())).thenReturn(saved).thenReturn(updated)
        `when`(roleRepository.findAllWithMenus(null)).thenReturn(emptyList())

        assertSame(found, service.getById(9L))
        assertSame(saved, service.save(menu(10L, label = "New")))
        assertSame(updated, service.update(menu(11L, label = "Changed")))
        service.removeById(12L)

        verify(menuRepository).removeById(12L)
    }

    @Test
    fun saveRejectsMissingParentMenu() {
        `when`(menuRepository.findNullable(99L, AuthorizationFetchers.MENU)).thenReturn(null)

        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                service.save(menu(10L, parentId = 99L, label = "New"))
            }

        assertEquals("父菜单不存在", exception.message)
    }

    @Test
    fun updateRejectsCircularParentReference() {
        `when`(menuRepository.findNullable(3L, AuthorizationFetchers.MENU)).thenReturn(menu(3L, parentId = 2L, label = "Grandchild"))
        `when`(menuRepository.findNullable(2L, AuthorizationFetchers.MENU)).thenReturn(menu(2L, parentId = 1L, label = "Child"))
        `when`(menuRepository.findNullable(1L, AuthorizationFetchers.MENU)).thenReturn(menu(1L, parentId = null, label = "Root"))

        val exception =
            assertThrows(IllegalStateException::class.java) {
                service.update(menu(1L, parentId = 3L, label = "Root"))
            }

        assertEquals("父菜单不能选择当前菜单或其子菜单", exception.message)
    }

    @Test
    fun removeByIdRejectsWhenChildrenStillExist() {
        `when`(menuRepository.findAll(MenuSpecification(parentId = 1L))).thenReturn(
            listOf(menu(2L, parentId = 1L, label = "Child")),
        )

        val exception =
            assertThrows(IllegalStateException::class.java) {
                service.removeById(1L)
            }

        assertEquals("请先删除子菜单后再删除当前菜单", exception.message)
        verify(roleRepository, never()).findAllWithMenus(null)
        verify(menuRepository, never()).removeById(1L)
    }

    @Test
    fun removeByIdDetachesRoleRelationsBeforeDelete() {
        val removableMenu = menu(12L, label = "Settings")
        val roleWithMenu = role(1L, menus = listOf(removableMenu, menu(13L, label = "Other")))
        val unaffectedRole = role(2L, menus = listOf(menu(14L, label = "Another")))
        var savedRole: Role? = null
        `when`(menuRepository.findAll(MenuSpecification(parentId = 12L))).thenReturn(emptyList())
        `when`(roleRepository.findAllWithMenus(null)).thenReturn(listOf(roleWithMenu, unaffectedRole))
        `when`(roleRepository.save(any())).thenAnswer {
            it.getArgument<Role>(0).also { role -> savedRole = role }
        }

        service.removeById(12L)

        val detachedRole = requireNotNull(savedRole)
        val savedMenus = detachedRole.readOrNull { menus } ?: emptyList()
        assertFalse(savedMenus.any { it.id == 12L })
        verify(roleRepository).save(any())
        verify(menuRepository).removeById(12L)
    }
}
