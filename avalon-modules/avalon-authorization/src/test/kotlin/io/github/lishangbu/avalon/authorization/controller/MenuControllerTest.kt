package io.github.lishangbu.avalon.authorization.controller

import io.github.lishangbu.avalon.authorization.entity.dto.MenuSpecification
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
        val specification = mock(MenuSpecification::class.java)
        val tree = listOf(treeNode(1L), treeNode(2L, parentId = 1L))
        val found = menuEntity(3L)
        val saved = menuEntity(4L)
        val updated = menuEntity(5L)
        `when`(menuService.listMenuTreeByRoleCodes(listOf("ADMIN", "USER"))).thenReturn(tree)
        `when`(menuService.listAllMenuTree(specification)).thenReturn(tree)
        `when`(menuService.getById(3L)).thenReturn(found)
        `when`(menuService.save(saved)).thenReturn(saved)
        `when`(menuService.update(updated)).thenReturn(updated)

        assertSame(tree, controller.listCurrentRoleMenuTree(user))
        assertSame(tree, controller.listAllMenuTree(specification))
        assertSame(found, controller.getById(3L))
        assertSame(saved, controller.save(saved))
        assertSame(updated, controller.update(updated))
        controller.deleteById(6L)

        verify(menuService).removeById(6L)
    }
}
