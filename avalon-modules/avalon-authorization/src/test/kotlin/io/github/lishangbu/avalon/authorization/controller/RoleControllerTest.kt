package io.github.lishangbu.avalon.authorization.controller

import io.github.lishangbu.avalon.authorization.entity.dto.RoleSpecification
import io.github.lishangbu.avalon.authorization.entity.dto.SaveRoleInput
import io.github.lishangbu.avalon.authorization.entity.dto.UpdateRoleInput
import io.github.lishangbu.avalon.authorization.service.RoleService
import org.babyfish.jimmer.Page
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.data.domain.PageRequest

class RoleControllerTest {
    private val roleService = mock(RoleService::class.java)
    private val controller = RoleController(roleService)

    @Test
    fun delegatesRoleCrudOperations() {
        val pageable = PageRequest.of(0, 10)
        val specification = mock(RoleSpecification::class.java)
        val page = Page(listOf(roleView(1L)), 1, 1)
        val list = listOf(roleView(2L))
        val found = roleView(3L)
        val saved = roleView(4L)
        val updated = roleView(5L)
        val saveInput =
            SaveRoleInput(
                code = "ADMIN",
                name = "Administrator",
                enabled = true,
                menuIds = listOf("5", "6"),
            )
        val updateInput =
            UpdateRoleInput(
                id = "5",
                code = "AUDITOR",
                name = "Auditor",
                enabled = false,
                menuIds = listOf("7"),
            )
        `when`(roleService.getPageByCondition(specification, pageable)).thenReturn(page)
        `when`(roleService.listByCondition(specification)).thenReturn(list)
        `when`(roleService.getById(3L)).thenReturn(found)
        `when`(roleService.save(saveInput)).thenReturn(saved)
        `when`(roleService.update(updateInput)).thenReturn(updated)

        assertSame(page, controller.getRolePage(pageable, specification))
        assertSame(list, controller.listRoles(specification))
        assertSame(found, controller.getById(3L))
        assertSame(saved, controller.save(saveInput))
        assertSame(updated, controller.update(updateInput))
        controller.deleteById(8L)

        verify(roleService).save(saveInput)
        verify(roleService).update(updateInput)
        verify(roleService).removeById(8L)
    }
}
