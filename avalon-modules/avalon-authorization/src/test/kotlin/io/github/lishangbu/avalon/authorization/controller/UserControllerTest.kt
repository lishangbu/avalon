package io.github.lishangbu.avalon.authorization.controller

import io.github.lishangbu.avalon.authorization.entity.dto.CurrentUserView
import io.github.lishangbu.avalon.authorization.entity.dto.SaveUserInput
import io.github.lishangbu.avalon.authorization.entity.dto.UpdateUserInput
import io.github.lishangbu.avalon.authorization.entity.dto.UserSpecification
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
        val expectedUserInfo: CurrentUserView = currentUserView(1L)
        val pageable = PageRequest.of(0, 10)
        val specification = mock(UserSpecification::class.java)
        val page = Page(listOf(userView(1L)), 1, 1)
        val list = listOf(userView(2L))
        val found = userView(3L)
        val saved = userView(4L)
        val updated = userView(5L)
        val saveInput =
            SaveUserInput(
                username = "alice",
                phone = "13800138000",
                email = "alice@example.com",
                avatar = "avatar.png",
                hashedPassword = "hashed",
                roleIds = listOf("10", "11"),
            )
        val updateInput =
            UpdateUserInput(
                id = "5",
                username = "bob",
                phone = "13600136000",
                email = "bob@example.com",
                avatar = "bob.png",
                hashedPassword = "new-hash",
                roleIds = listOf("12"),
            )
        `when`(userService.getUserByUsername("alice")).thenReturn(expectedUserInfo)
        `when`(userService.getPageByCondition(specification, pageable)).thenReturn(page)
        `when`(userService.listByCondition(specification)).thenReturn(list)
        `when`(userService.getById(3L)).thenReturn(found)
        `when`(userService.save(saveInput)).thenReturn(saved)
        `when`(userService.update(updateInput)).thenReturn(updated)

        assertSame(expectedUserInfo, controller.getUserInfo(user))
        assertSame(page, controller.getUserPage(pageable, specification))
        assertSame(list, controller.listUsers(specification))
        assertSame(found, controller.getById(3L))
        assertSame(saved, controller.save(saveInput))
        assertSame(updated, controller.update(updateInput))
        controller.deleteById(9L)

        verify(userService).save(saveInput)
        verify(userService).update(updateInput)
        verify(userService).removeById(9L)
    }
}
