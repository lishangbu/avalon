package io.github.lishangbu.avalon.authorization.controller

import io.github.lishangbu.avalon.authorization.entity.dto.UserSpecification
import io.github.lishangbu.avalon.authorization.model.UserWithRoles
import io.github.lishangbu.avalon.authorization.service.UserService
import io.github.lishangbu.avalon.oauth2.common.userdetails.UserInfo
import org.babyfish.jimmer.Page
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.data.domain.PageRequest
import org.springframework.security.core.authority.AuthorityUtils

class UserControllerTest {
    private val userService = mock(UserService::class.java)
    private val controller = UserController(userService)

    @Test
    fun delegatesUserInfoAndCrudOperations() {
        val user = UserInfo("alice", "{noop}pwd", AuthorityUtils.createAuthorityList("ROLE_ADMIN"))
        val expectedUserInfo = UserWithRoles(1L, "alice", "avatar.png", emptySet())
        val pageable = PageRequest.of(0, 10)
        val specification = mock(UserSpecification::class.java)
        val page = Page(listOf(userEntity(1L)), 1, 1)
        val list = listOf(userEntity(2L))
        val found = userEntity(3L)
        val saved = userEntity(4L)
        val updated = userEntity(5L)
        `when`(userService.getUserByUsername("alice")).thenReturn(expectedUserInfo)
        `when`(userService.getPageByCondition(specification, pageable)).thenReturn(page)
        `when`(userService.listByCondition(specification)).thenReturn(list)
        `when`(userService.getById(3L)).thenReturn(found)
        `when`(userService.save(saved)).thenReturn(saved)
        `when`(userService.update(updated)).thenReturn(updated)

        assertSame(expectedUserInfo, controller.getUserInfo(user))
        assertSame(page, controller.getUserPage(pageable, specification))
        assertSame(list, controller.listUsers(specification))
        assertSame(found, controller.getById(3L))
        assertSame(saved, controller.save(saved))
        assertSame(updated, controller.update(updated))
        controller.deleteById(9L)

        verify(userService).removeById(9L)
    }
}
