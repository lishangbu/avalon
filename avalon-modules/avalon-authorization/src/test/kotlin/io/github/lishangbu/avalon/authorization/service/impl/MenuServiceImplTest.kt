package io.github.lishangbu.avalon.authorization.service.impl

import io.github.lishangbu.avalon.authorization.entity.Menu
import io.github.lishangbu.avalon.authorization.entity.Role
import io.github.lishangbu.avalon.authorization.entity.dto.MenuSpecification
import io.github.lishangbu.avalon.authorization.entity.dto.SaveMenuInput
import io.github.lishangbu.avalon.authorization.entity.dto.UpdateMenuInput
import io.github.lishangbu.avalon.authorization.repository.AuthorizationFetchers
import io.github.lishangbu.avalon.authorization.repository.MenuRepository
import io.github.lishangbu.avalon.authorization.repository.RoleRepository
import io.github.lishangbu.avalon.jimmer.support.readOrNull
import org.babyfish.jimmer.sql.ast.mutation.AssociatedSaveMode
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
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

class MenuServiceImplTest {
    private val menuRepository = mock(MenuRepository::class.java)
    private val roleRepository = mock(RoleRepository::class.java)
    private val service = MenuServiceImpl(menuRepository, roleRepository)

    @Test
    fun returnsEmptyTreeWhenRoleCodesAreEmpty() {
        assertTrue(service.listMenuTreeByRoleCodes(emptyList()).isEmpty())
        verifyNoInteractions(menuRepository, roleRepository)
    }

    @Test
    fun buildsRoleBasedMenuTree() {
        val root = menuView(1L, parentId = null, label = "Root", sortingOrder = 1)
        val child = menuView(2L, parentId = 1L, label = "Child", sortingOrder = 2)
        `when`(menuRepository.listViewsByRoleCodes(listOf("ADMIN"))).thenReturn(listOf(root, child))

        val result = service.listMenuTreeByRoleCodes(listOf("ADMIN"))

        assertEquals(1, result.size)
        assertEquals("1", result.single().id)
        assertEquals(
            "2",
            result
                .single()
                .children!!
                .single()
                .id,
        )
    }

    @Test
    fun delegatesManagementQueries() {
        val specification = MenuSpecification(key = "dashboard")
        val tree = listOf(menuTreeView(1L, children = listOf(menuTreeView(2L, parentId = 1L))))
        val list = listOf(menuView(1L), menuView(2L, parentId = 1L))
        `when`(menuRepository.listTreeViews()).thenReturn(tree)
        `when`(menuRepository.listViews(specification)).thenReturn(list)

        assertSame(tree, service.listTree())
        assertSame(list, service.listByCondition(specification))
    }

    @Test
    fun delegatesCrudOperations() {
        val found = menuView(9L, label = "Settings")
        val saved = menuView(10L, label = "Saved")
        val updated = menuView(11L, label = "Updated")
        `when`(menuRepository.loadViewById(9L)).thenReturn(found)
        `when`(menuRepository.hasChildren(12L)).thenReturn(false)
        `when`(menuRepository.save(anyMenu(), eq(SaveMode.INSERT_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())).thenReturn(menu(10L, label = "Saved"))
        `when`(menuRepository.loadViewById(10L)).thenReturn(saved)
        `when`(menuRepository.save(anyMenu())).thenReturn(menu(11L, label = "Updated"))
        `when`(menuRepository.loadViewById(11L)).thenReturn(updated)
        `when`(roleRepository.listWithMenus(null)).thenReturn(emptyList())

        assertSame(found, service.getById(9L))
        assertSame(saved, service.save(saveMenuInput(label = "New")))
        assertSame(updated, service.update(updateMenuInput(id = "11", label = "Changed")))
        service.removeById(12L)

        verify(menuRepository).loadViewById(9L)
        verify(menuRepository).loadViewById(10L)
        verify(menuRepository).loadViewById(11L)
        verify(menuRepository).hasChildren(12L)
        verify(menuRepository).deleteById(12L)
    }

    @Test
    fun saveRejectsMissingParentMenu() {
        `when`(menuRepository.findNullable(99L, AuthorizationFetchers.MENU)).thenReturn(null)

        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                service.save(saveMenuInput(parentId = 99L, label = "New"))
            }

        assertEquals("父菜单不存在", exception.message)
    }

    @Test
    fun updateRejectsCircularParentReference() {
        val root = menu(1L, parentId = null, label = "Root")
        val child =
            Menu(menu(2L, label = "Child")) {
                parent = root
            }
        val grandChild =
            Menu(menu(3L, label = "Grandchild")) {
                parent = child
            }
        `when`(menuRepository.findNullable(3L, AuthorizationFetchers.MENU)).thenReturn(grandChild)
        `when`(menuRepository.findNullable(2L, AuthorizationFetchers.MENU)).thenReturn(child)
        `when`(menuRepository.findNullable(1L, AuthorizationFetchers.MENU)).thenReturn(root)

        val exception =
            assertThrows(IllegalStateException::class.java) {
                service.update(updateMenuInput(id = "1", parentId = 3L, label = "Root"))
            }

        assertTrue(
            exception.message == "父菜单不能选择当前菜单或其子菜单" ||
                exception.message == "菜单层级存在循环引用",
        )
    }

    @Test
    fun removeByIdRejectsWhenChildrenStillExist() {
        `when`(menuRepository.hasChildren(1L)).thenReturn(true)

        val exception =
            assertThrows(IllegalStateException::class.java) {
                service.removeById(1L)
            }

        assertEquals("请先删除子菜单后再删除当前菜单", exception.message)
        verify(roleRepository, never()).listWithMenus(null)
        verify(menuRepository, never()).deleteById(1L)
    }

    @Test
    fun removeByIdDetachesRoleRelationsBeforeDelete() {
        val removableMenu = menu(12L, label = "Settings")
        val roleWithMenu = role(1L, menus = listOf(removableMenu, menu(13L, label = "Other")))
        val unaffectedRole = role(2L, menus = listOf(menu(14L, label = "Another")))
        var savedRole: Role? = null
        `when`(menuRepository.hasChildren(12L)).thenReturn(false)
        `when`(roleRepository.listWithMenus(null)).thenReturn(listOf(roleWithMenu, unaffectedRole))
        `when`(roleRepository.save(anyRole())).thenAnswer {
            it.getArgument<Role>(0).also { role -> savedRole = role }
        }

        service.removeById(12L)

        val detachedRole = requireNotNull(savedRole)
        val savedMenus = detachedRole.readOrNull { menus } ?: emptyList()
        assertFalse(savedMenus.any { it.id == 12L })
        verify(roleRepository).save(anyRole())
        verify(menuRepository).deleteById(12L)
    }
}

private fun saveMenuInput(
    parentId: Long? = null,
    label: String = "Menu",
): SaveMenuInput =
    SaveMenuInput(
        parentId = parentId?.toString(),
        disabled = false,
        extra = null,
        icon = null,
        key = "key-$label",
        label = label,
        show = true,
        path = "/${label.lowercase()}",
        name = label.lowercase(),
        redirect = null,
        component = "component/${label.lowercase()}",
        sortingOrder = 1,
        pinned = false,
        showTab = true,
        enableMultiTab = true,
    )

private fun updateMenuInput(
    id: String,
    parentId: Long? = null,
    label: String = "Menu",
): UpdateMenuInput =
    UpdateMenuInput(
        id = id,
        parentId = parentId?.toString(),
        disabled = false,
        extra = null,
        icon = null,
        key = "key-$label",
        label = label,
        show = true,
        path = "/${label.lowercase()}",
        name = label.lowercase(),
        redirect = null,
        component = "component/${label.lowercase()}",
        sortingOrder = 1,
        pinned = false,
        showTab = true,
        enableMultiTab = true,
    )
