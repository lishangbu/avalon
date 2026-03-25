package io.github.lishangbu.avalon.authorization.service.impl

import io.github.lishangbu.avalon.authorization.entity.Role
import io.github.lishangbu.avalon.authorization.entity.addBy
import io.github.lishangbu.avalon.authorization.entity.dto.RoleSpecification
import io.github.lishangbu.avalon.authorization.repository.MenuRepository
import io.github.lishangbu.avalon.authorization.repository.RoleRepository
import org.babyfish.jimmer.Page
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
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
        `when`(roleRepository.findByIdWithMenus(3L)).thenReturn(found)

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
        `when`(roleRepository.findByIdWithMenus(3L)).thenReturn(existing)
        `when`(roleRepository.save(any())).thenAnswer { it.getArgument(0) }

        val updated = service.update(Role { id = 3L })

        assertEquals("AUDITOR", updated.code)
        assertEquals("Auditor", updated.name)
        assertEquals(false, updated.enabled)
        assertEquals(listOf("Reports"), updated.menus.mapNotNull { it.label })
    }

    @Test
    fun removeDelegatesDeleteToRepository() {
        service.removeById(42L)

        verify(roleRepository).deleteById(42L)
    }
}
