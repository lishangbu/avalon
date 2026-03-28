package io.github.lishangbu.avalon.authorization.service.impl

import io.github.lishangbu.avalon.authorization.entity.Role
import io.github.lishangbu.avalon.authorization.entity.addBy
import io.github.lishangbu.avalon.authorization.entity.dto.RoleSpecification
import io.github.lishangbu.avalon.authorization.repository.AuthorizationFetchers
import io.github.lishangbu.avalon.authorization.repository.MenuRepository
import io.github.lishangbu.avalon.authorization.repository.RoleRepository
import io.github.lishangbu.avalon.jimmer.support.readOrNull
import org.babyfish.jimmer.Page
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
        val page = Page(listOf(role(1L)), 1, 1)
        val roles = listOf(role(2L))
        val found = role(3L)
        `when`(roleRepository.findAllWithMenus(specification, pageable)).thenReturn(page)
        `when`(roleRepository.findAllWithMenus(specification)).thenReturn(roles)
        `when`(roleRepository.findNullable(3L, AuthorizationFetchers.ROLE_WITH_MENUS)).thenReturn(found)

        assertSame(page, service.getPageByCondition(specification, pageable))
        assertSame(roles, service.listByCondition(specification))
        assertSame(found, service.getById(3L))
    }

    @Test
    fun saveBindsMenusBeforePersisting() {
        val incoming =
            Role {
                code = "ADMIN"
                name = "Administrator"
                enabled = true
                menus().addBy(menu(5L))
                menus().addBy(menu(6L))
            }
        val boundMenus = listOf(menu(5L, label = "Dashboard"), menu(6L, label = "Users"))
        `when`(menuRepository.findAllById(setOf(5L, 6L))).thenReturn(boundMenus)
        `when`(roleRepository.save(any())).thenAnswer { it.getArgument(0) }

        val saved = service.save(incoming)

        assertEquals("ADMIN", saved.code)
        assertEquals("Administrator", saved.name)
        assertEquals(true, saved.enabled)
        assertEquals(setOf("Dashboard", "Users"), saved.menus.mapNotNull { it.label }.toSet())
    }

    @Test
    fun saveKeepsMenusUnloadedWhenInputDoesNotLoadAnyMenuIds() {
        `when`(roleRepository.save(any())).thenAnswer { it.getArgument(0) }

        val saved =
            service.save(
                Role {
                    code = "GUEST"
                    name = "Guest"
                    enabled = true
                },
            )

        assertEquals("GUEST", saved.code)
        assertEquals("Guest", saved.name)
        assertEquals(true, saved.enabled)
        assertNull(saved.readOrNull { menus })
        verifyNoInteractions(menuRepository)
    }

    @Test
    fun updatePreservesExistingMenusWhenInputDoesNotLoadThem() {
        val existingMenu = menu(7L, label = "Reports")
        val existing =
            Role {
                id = 3L
                code = "AUDITOR"
                name = "Auditor"
                enabled = false
                menus().addBy(existingMenu)
            }
        `when`(roleRepository.findNullable(3L, AuthorizationFetchers.ROLE_WITH_MENUS)).thenReturn(existing)
        `when`(roleRepository.save(any())).thenAnswer { it.getArgument(0) }

        val updated = service.update(Role { id = 3L })

        assertEquals("AUDITOR", updated.code)
        assertEquals("Auditor", updated.name)
        assertEquals(false, updated.enabled)
        assertEquals(listOf("Reports"), updated.menus.mapNotNull { it.label })
    }

    @Test
    fun updateUsesIncomingFieldsAndMenusWhenExistingRoleDoesNotExist() {
        val boundMenu = menu(8L, label = "Audit")
        val incoming =
            Role {
                id = 4L
                code = "SUPERVISOR"
                name = "Supervisor"
                enabled = true
                menus().addBy(menu(8L))
            }
        `when`(roleRepository.findNullable(4L, AuthorizationFetchers.ROLE_WITH_MENUS)).thenReturn(null)
        `when`(menuRepository.findAllById(setOf(8L))).thenReturn(listOf(boundMenu))
        `when`(roleRepository.save(any())).thenAnswer { it.getArgument(0) }

        val updated = service.update(incoming)

        assertEquals("SUPERVISOR", updated.code)
        assertEquals("Supervisor", updated.name)
        assertEquals(true, updated.enabled)
        assertEquals(listOf("Audit"), updated.menus.mapNotNull { it.label })
        verify(roleRepository).findNullable(4L, AuthorizationFetchers.ROLE_WITH_MENUS)
        verify(menuRepository).findAllById(setOf(8L))
    }

    @Test
    fun updateLeavesOptionalFieldsUnsetWhenExistingRoleCannotBeFound() {
        `when`(roleRepository.findNullable(99L, AuthorizationFetchers.ROLE_WITH_MENUS)).thenReturn(null)
        `when`(roleRepository.save(any())).thenAnswer { it.getArgument(0) }

        val updated = service.update(Role { id = 99L })

        assertEquals(99L, updated.id)
        assertNull(updated.readOrNull { code })
        assertNull(updated.readOrNull { name })
        assertNull(updated.readOrNull { enabled })
        assertNull(updated.readOrNull { menus })
        verify(roleRepository).findNullable(99L, AuthorizationFetchers.ROLE_WITH_MENUS)
        verifyNoInteractions(menuRepository)
    }

    @Test
    fun updateDoesNotQueryExistingRoleWhenIdIsNotLoaded() {
        `when`(roleRepository.save(any())).thenAnswer { it.getArgument(0) }

        val updated =
            service.update(
                Role {
                    code = "DRAFT"
                },
            )

        assertEquals("DRAFT", updated.code)
        verify(roleRepository).save(any())
        verifyNoMoreInteractions(roleRepository)
        verifyNoInteractions(menuRepository)
    }

    @Test
    fun removeDelegatesDeleteToRepository() {
        service.removeById(42L)

        verify(roleRepository).removeById(42L)
    }
}
