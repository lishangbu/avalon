package io.github.lishangbu.avalon.authorization.service.impl

import io.github.lishangbu.avalon.authorization.entity.Role
import io.github.lishangbu.avalon.authorization.entity.addBy
import io.github.lishangbu.avalon.authorization.entity.dto.RoleSpecification
import io.github.lishangbu.avalon.authorization.entity.dto.SaveRoleInput
import io.github.lishangbu.avalon.authorization.entity.dto.UpdateRoleInput
import io.github.lishangbu.avalon.authorization.repository.AuthorizationFetchers
import io.github.lishangbu.avalon.authorization.repository.MenuRepository
import io.github.lishangbu.avalon.authorization.repository.RoleRepository
import io.github.lishangbu.avalon.jimmer.support.readOrNull
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.sql.ast.mutation.AssociatedSaveMode
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.Mockito.`when`
import org.springframework.data.domain.PageRequest

class RoleServiceImplTest {
    private val roleRepository = mock(RoleRepository::class.java)
    private val menuRepository = mock(MenuRepository::class.java)
    private val service = RoleServiceImpl(roleRepository, menuRepository)

    @Test
    fun delegatesPageListAndIdLookups() {
        val pageable = PageRequest.of(0, 10)
        val specification = mock(RoleSpecification::class.java)
        val page = Page(listOf(roleView(1L)), 1, 1)
        val roles = listOf(roleView(2L))
        val found = roleView(3L)
        `when`(roleRepository.pageViews(specification, pageable)).thenReturn(page)
        `when`(roleRepository.listViews(specification)).thenReturn(roles)
        `when`(roleRepository.loadViewById(3L)).thenReturn(found)

        assertSame(page, service.getPageByCondition(specification, pageable))
        assertSame(roles, service.listByCondition(specification))
        assertSame(found, service.getById(3L))
    }

    @Test
    fun saveBindsMenusBeforePersisting() {
        val incoming =
            SaveRoleInput(
                code = "ADMIN",
                name = "Administrator",
                enabled = true,
                menuIds = listOf("5", "6"),
            )
        val boundMenus = listOf(menu(5L, label = "Dashboard"), menu(6L, label = "Users"))
        var persisted: Role? = null
        `when`(menuRepository.findAllById(setOf(5L, 6L))).thenReturn(boundMenus)
        `when`(roleRepository.save(anyRole(), eq(SaveMode.INSERT_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())).thenAnswer {
            persisted = it.getArgument<Role>(0)
            role(
                id = 1L,
                code = "ADMIN",
                name = "Administrator",
                enabled = true,
                menus = boundMenus,
            )
        }
        `when`(roleRepository.loadViewById(1L)).thenReturn(
            roleView(
                id = 1L,
                code = "ADMIN",
                name = "Administrator",
                enabled = true,
                menus = boundMenus,
            ),
        )

        val saved = service.save(incoming)
        val prepared = requireNotNull(persisted)

        assertEquals("ADMIN", prepared.code)
        assertEquals("Administrator", prepared.name)
        assertEquals(true, prepared.enabled)
        assertEquals(setOf("Dashboard", "Users"), prepared.menus.mapNotNull { it.label }.toSet())
        assertEquals("ADMIN", saved.code)
        assertEquals("Administrator", saved.name)
        assertEquals(true, saved.enabled)
        assertEquals(setOf("Dashboard", "Users"), saved.menus.mapNotNull { it.label }.toSet())
        verify(roleRepository).loadViewById(1L)
    }

    @Test
    fun saveKeepsMenusEmptyWhenInputProvidesEmptyMenuIds() {
        var persisted: Role? = null
        `when`(roleRepository.save(anyRole(), eq(SaveMode.INSERT_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())).thenAnswer {
            persisted = it.getArgument<Role>(0)
            role(id = 2L, code = "GUEST", name = "Guest", enabled = true)
        }
        `when`(roleRepository.loadViewById(2L)).thenReturn(roleView(id = 2L, code = "GUEST", name = "Guest", enabled = true))

        val saved =
            service.save(
                SaveRoleInput(
                    code = "GUEST",
                    name = "Guest",
                    enabled = true,
                    menuIds = emptyList(),
                ),
            )
        val prepared = requireNotNull(persisted)

        assertEquals("GUEST", prepared.code)
        assertEquals("Guest", prepared.name)
        assertEquals(true, prepared.enabled)
        assertEquals(emptyList<String>(), prepared.menus.mapNotNull { it.label })
        assertEquals("GUEST", saved.code)
        assertEquals("Guest", saved.name)
        assertEquals(true, saved.enabled)
        assertEquals(emptyList<String>(), saved.menus.mapNotNull { it.label })
        verifyNoInteractions(menuRepository)
        verify(roleRepository).loadViewById(2L)
    }

    @Test
    fun updatePreservesExistingScalarFieldsWhenOptionalInputsAreNull() {
        val existingMenu = menu(7L, label = "Reports")
        val existing =
            Role {
                id = 3L
                code = "AUDITOR"
                name = "Auditor"
                enabled = false
                menus().addBy(existingMenu)
            }
        var persisted: Role? = null
        `when`(roleRepository.findNullable(3L, AuthorizationFetchers.ROLE_WITH_MENUS)).thenReturn(existing)
        `when`(menuRepository.findAllById(setOf(7L))).thenReturn(listOf(existingMenu))
        `when`(roleRepository.save(anyRole())).thenAnswer {
            it.getArgument<Role>(0).also { role -> persisted = role }
        }
        `when`(roleRepository.loadViewById(3L)).thenReturn(
            roleView(
                id = 3L,
                code = "AUDITOR",
                name = "Auditor",
                enabled = false,
                menus = listOf(existingMenu),
            ),
        )

        val updated = service.update(UpdateRoleInput(id = "3", menuIds = listOf("7")))
        val prepared = requireNotNull(persisted)

        assertEquals("AUDITOR", prepared.code)
        assertEquals("Auditor", prepared.name)
        assertEquals(false, prepared.enabled)
        assertEquals("AUDITOR", updated.code)
        assertEquals("Auditor", updated.name)
        assertEquals(false, updated.enabled)
        assertEquals(listOf("Reports"), updated.menus.mapNotNull { it.label })
        verify(roleRepository).loadViewById(3L)
    }

    @Test
    fun updateClearsExistingMenusWhenInputLoadsEmptyMenuIds() {
        val existing =
            Role {
                id = 3L
                code = "AUDITOR"
                name = "Auditor"
                enabled = false
                menus().addBy(menu(7L, label = "Reports"))
            }
        var persisted: Role? = null
        `when`(roleRepository.findNullable(3L, AuthorizationFetchers.ROLE_WITH_MENUS)).thenReturn(existing)
        `when`(roleRepository.save(anyRole())).thenAnswer {
            it.getArgument<Role>(0).also { role -> persisted = role }
        }
        `when`(roleRepository.loadViewById(3L)).thenReturn(roleView(id = 3L, code = "AUDITOR", name = "Auditor", enabled = false, menus = emptyList()))

        val updated =
            service.update(
                UpdateRoleInput(
                    id = "3",
                    menuIds = emptyList(),
                ),
            )
        val prepared = requireNotNull(persisted)

        assertEquals(emptyList<String>(), (prepared.readOrNull { menus } ?: emptyList()).mapNotNull { it.label })
        assertEquals(emptyList<String>(), updated.menus.mapNotNull { it.label })
        verifyNoInteractions(menuRepository)
        verify(roleRepository).loadViewById(3L)
    }

    @Test
    fun updateUsesIncomingFieldsAndMenusWhenExistingRoleDoesNotExist() {
        val boundMenu = menu(8L, label = "Audit")
        val incoming =
            UpdateRoleInput(
                id = "4",
                code = "SUPERVISOR",
                name = "Supervisor",
                enabled = true,
                menuIds = listOf("8"),
            )
        var persisted: Role? = null
        `when`(roleRepository.findNullable(4L, AuthorizationFetchers.ROLE_WITH_MENUS)).thenReturn(null)
        `when`(menuRepository.findAllById(setOf(8L))).thenReturn(listOf(boundMenu))
        `when`(roleRepository.save(anyRole())).thenAnswer {
            it.getArgument<Role>(0).also { role -> persisted = role }
        }
        `when`(roleRepository.loadViewById(4L)).thenReturn(
            roleView(
                id = 4L,
                code = "SUPERVISOR",
                name = "Supervisor",
                enabled = true,
                menus = listOf(boundMenu),
            ),
        )

        val updated = service.update(incoming)
        val prepared = requireNotNull(persisted)

        assertEquals("SUPERVISOR", prepared.code)
        assertEquals("Supervisor", prepared.name)
        assertEquals(true, prepared.enabled)
        assertEquals("SUPERVISOR", updated.code)
        assertEquals("Supervisor", updated.name)
        assertEquals(true, updated.enabled)
        assertEquals(listOf("Audit"), updated.menus.mapNotNull { it.label })
        verify(roleRepository).findNullable(4L, AuthorizationFetchers.ROLE_WITH_MENUS)
        verify(menuRepository).findAllById(setOf(8L))
        verify(roleRepository).loadViewById(4L)
    }

    @Test
    fun updateLeavesOptionalFieldsUnsetWhenExistingRoleCannotBeFound() {
        var persisted: Role? = null
        `when`(roleRepository.findNullable(99L, AuthorizationFetchers.ROLE_WITH_MENUS)).thenReturn(null)
        `when`(roleRepository.save(anyRole())).thenAnswer {
            it.getArgument<Role>(0).also { role -> persisted = role }
        }
        `when`(roleRepository.loadViewById(99L)).thenReturn(roleView(id = 99L, menus = emptyList()))

        val updated = service.update(UpdateRoleInput(id = "99", menuIds = emptyList()))
        val prepared = requireNotNull(persisted)

        assertEquals(99L, prepared.id)
        assertNull(prepared.readOrNull { code })
        assertNull(prepared.readOrNull { name })
        assertNull(prepared.readOrNull { enabled })
        assertEquals(emptyList<String>(), (prepared.readOrNull { menus } ?: emptyList()).mapNotNull { it.label })
        assertEquals("99", updated.id)
        verify(roleRepository).findNullable(99L, AuthorizationFetchers.ROLE_WITH_MENUS)
        verifyNoInteractions(menuRepository)
        verify(roleRepository).loadViewById(99L)
    }

    @Test
    fun removeDelegatesDeleteToRepository() {
        service.removeById(42L)

        verify(roleRepository).deleteById(42L)
    }
}
