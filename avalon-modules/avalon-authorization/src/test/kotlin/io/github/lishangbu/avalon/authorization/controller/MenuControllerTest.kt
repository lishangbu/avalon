package io.github.lishangbu.avalon.authorization.controller

import io.github.lishangbu.avalon.authorization.entity.dto.MenuSpecification
import io.github.lishangbu.avalon.authorization.entity.dto.SaveMenuInput
import io.github.lishangbu.avalon.authorization.entity.dto.UpdateMenuInput
import io.github.lishangbu.avalon.authorization.service.MenuService
import io.github.lishangbu.avalon.oauth2.common.userdetails.UserInfo
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.security.core.authority.AuthorityUtils

class MenuControllerTest {
    private val menuService = mock(MenuService::class.java)
    private val controller = MenuController(menuService)

    @Test
    fun delegatesMenuTreeAndCrudOperations() {
        val user = UserInfo("alice", "{noop}pwd", AuthorityUtils.createAuthorityList("ADMIN", "USER"))
        val specification = MenuSpecification(key = "dashboard")
        val roleTree = listOf(menuTreeView(1L, children = listOf(menuTreeView(2L, parentId = 1L))))
        val tree = listOf(menuTreeView(1L, children = listOf(menuTreeView(2L, parentId = 1L))))
        val list = listOf(menuView(3L), menuView(4L, parentId = 3L))
        val found = menuView(3L)
        val saved = menuView(4L, parentId = 1L)
        val updated = menuView(5L)
        val saveInput =
            SaveMenuInput(
                parentId = "1",
                disabled = false,
                extra = "{\"a\":1}",
                icon = "ph:house",
                key = "dashboard",
                label = "仪表板",
                show = true,
                path = "/dashboard",
                name = "dashboard",
                redirect = "/dashboard/home",
                component = "dashboard/index",
                sortingOrder = 1,
                pinned = true,
                showTab = true,
                enableMultiTab = false,
            )
        val updateInput =
            UpdateMenuInput(
                id = "5",
                parentId = null,
                disabled = true,
                extra = null,
                icon = "ph:gear",
                key = "settings",
                label = "系统设置",
                show = false,
                path = "/settings",
                name = "settings",
                redirect = null,
                component = "system/settings/index",
                sortingOrder = 99,
                pinned = false,
                showTab = false,
                enableMultiTab = true,
            )
        `when`(menuService.listMenuTreeByRoleCodes(listOf("ADMIN", "USER"))).thenReturn(roleTree)
        `when`(menuService.listTree()).thenReturn(tree)
        `when`(menuService.listByCondition(specification)).thenReturn(list)
        `when`(menuService.getById(3L)).thenReturn(found)
        `when`(menuService.save(saveInput)).thenReturn(saved)
        `when`(menuService.update(updateInput)).thenReturn(updated)

        assertSame(roleTree, controller.listCurrentRoleMenuTree(user))
        assertSame(tree, controller.listTree())
        assertSame(list, controller.listByCondition(specification))
        assertSame(found, controller.getById(3L))
        assertSame(saved, controller.save(saveInput))
        assertSame(updated, controller.update(updateInput))
        controller.deleteById(6L)

        verify(menuService).listTree()
        verify(menuService).listByCondition(specification)
        verify(menuService).save(saveInput)
        verify(menuService).update(updateInput)
        verify(menuService).removeById(6L)
    }
}
