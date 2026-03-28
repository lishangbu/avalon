package io.github.lishangbu.avalon.authorization.service.impl

import io.github.lishangbu.avalon.authorization.entity.User
import io.github.lishangbu.avalon.authorization.entity.addBy
import io.github.lishangbu.avalon.authorization.entity.dto.UserSpecification
import io.github.lishangbu.avalon.authorization.repository.AuthorizationFetchers
import io.github.lishangbu.avalon.authorization.repository.RoleRepository
import io.github.lishangbu.avalon.authorization.repository.UserRepository
import io.github.lishangbu.avalon.jimmer.support.readOrNull
import org.babyfish.jimmer.Page
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.Mockito.`when`
import org.springframework.data.domain.PageRequest

class UserServiceImplTest {
    private val userRepository = mock(UserRepository::class.java)
    private val roleRepository = mock(RoleRepository::class.java)
    private val service = UserServiceImpl(userRepository, roleRepository)

    @Test
    fun wrapsUserWithRolesForAccountLookup() {
        `when`(userRepository.findByAccountWithRoles("alice")).thenReturn(user(1L, roles = listOf(role(1L, "ADMIN"))))

        val result = service.getUserByUsername("alice")

        assertNotNull(result)
        assertEquals(1L, result!!.id)
        assertEquals("alice", result.username)
        assertEquals(setOf("ADMIN"), result.roles.mapNotNull { it.code }.toSet())
    }

    @Test
    fun returnsNullWhenAccountLookupMisses() {
        `when`(userRepository.findByAccountWithRoles("missing")).thenReturn(null)

        val result = service.getUserByUsername("missing")

        assertNull(result)
    }

    @Test
    fun delegatesPageListAndIdLookups() {
        val pageable = PageRequest.of(0, 10)
        val specification = mock(UserSpecification::class.java)
        val page = Page(listOf(user(1L)), 1, 1)
        val users = listOf(user(2L))
        val found = user(3L)
        `when`(userRepository.findAllWithRoles(specification, pageable)).thenReturn(page)
        `when`(userRepository.findAllWithRoles(specification)).thenReturn(users)
        `when`(userRepository.findNullable(3L, AuthorizationFetchers.USER_WITH_ROLES)).thenReturn(found)

        assertSame(page, service.getPageByCondition(specification, pageable))
        assertSame(users, service.listByCondition(specification))
        assertSame(found, service.getById(3L))
    }

    @Test
    fun saveBindsRolesBeforePersisting() {
        val incoming =
            User {
                username = "alice"
                phone = "13800138000"
                email = "alice@example.com"
                avatar = "avatar.png"
                hashedPassword = "hashed"
                roles().addBy(role(10L))
                roles().addBy(role(11L))
            }
        val boundRoles = listOf(role(10L, "ADMIN"), role(11L, "USER"))
        `when`(roleRepository.findAllById(setOf(10L, 11L))).thenReturn(boundRoles)
        `when`(userRepository.save(any())).thenAnswer { it.getArgument(0) }

        val saved = service.save(incoming)

        assertEquals("alice", saved.username)
        assertEquals("hashed", saved.hashedPassword)
        assertEquals(setOf("ADMIN", "USER"), saved.roles.mapNotNull { it.code }.toSet())
        verify(roleRepository).findAllById(setOf(10L, 11L))
        verify(userRepository).save(any())
    }

    @Test
    fun saveKeepsRolesEmptyWhenInputDoesNotLoadAnyRoleIds() {
        `when`(userRepository.save(any())).thenAnswer { it.getArgument(0) }

        val saved =
            service.save(
                User {
                    username = "alice"
                    hashedPassword = "hashed"
                },
            )

        assertEquals("alice", saved.username)
        assertEquals("hashed", saved.hashedPassword)
        assertNull(saved.readOrNull { roles })
        verifyNoInteractions(roleRepository)
    }

    @Test
    fun updatePreservesExistingFieldsWhenInputDoesNotLoadThem() {
        val existingRole = role(20L, "AUDITOR")
        val existing =
            user(
                id = 9L,
                username = "old-user",
                phone = "13900000000",
                email = "old@example.com",
                avatar = "old.png",
                hashedPassword = "old-hash",
                roles = listOf(existingRole),
            )
        `when`(userRepository.findNullable(9L, AuthorizationFetchers.USER_WITH_ROLES)).thenReturn(existing)
        `when`(userRepository.save(any())).thenAnswer { it.getArgument(0) }

        val updated = service.update(User { id = 9L })

        assertEquals("old-user", updated.username)
        assertEquals("13900000000", updated.phone)
        assertEquals("old@example.com", updated.email)
        assertEquals("old.png", updated.avatar)
        assertEquals("old-hash", updated.hashedPassword)
        assertEquals(listOf("AUDITOR"), updated.roles.mapNotNull { it.code })
    }

    @Test
    fun updateUsesIncomingFieldsAndRolesWhenExistingUserDoesNotExist() {
        val boundRole = role(30L, "MANAGER")
        val incoming =
            User {
                id = 9L
                username = "new-user"
                phone = "13600000000"
                email = "new@example.com"
                avatar = "new.png"
                hashedPassword = "new-hash"
                roles().addBy(role(30L))
            }
        `when`(userRepository.findNullable(9L, AuthorizationFetchers.USER_WITH_ROLES)).thenReturn(null)
        `when`(roleRepository.findAllById(setOf(30L))).thenReturn(listOf(boundRole))
        `when`(userRepository.save(any())).thenAnswer { it.getArgument(0) }

        val updated = service.update(incoming)

        assertEquals("new-user", updated.username)
        assertEquals("13600000000", updated.phone)
        assertEquals("new@example.com", updated.email)
        assertEquals("new.png", updated.avatar)
        assertEquals("new-hash", updated.hashedPassword)
        assertEquals(listOf("MANAGER"), updated.roles.mapNotNull { it.code })
        verify(userRepository).findNullable(9L, AuthorizationFetchers.USER_WITH_ROLES)
        verify(roleRepository).findAllById(setOf(30L))
    }

    @Test
    fun updateLeavesOptionalFieldsUnsetWhenExistingUserCannotBeFound() {
        `when`(userRepository.findNullable(99L, AuthorizationFetchers.USER_WITH_ROLES)).thenReturn(null)
        `when`(userRepository.save(any())).thenAnswer { it.getArgument(0) }

        val updated = service.update(User { id = 99L })

        assertEquals(99L, updated.id)
        assertNull(updated.readOrNull { username })
        assertNull(updated.readOrNull { phone })
        assertNull(updated.readOrNull { email })
        assertNull(updated.readOrNull { avatar })
        assertNull(updated.readOrNull { hashedPassword })
        assertNull(updated.readOrNull { roles })
        verify(userRepository).findNullable(99L, AuthorizationFetchers.USER_WITH_ROLES)
        verifyNoInteractions(roleRepository)
    }

    @Test
    fun updateDoesNotQueryExistingUserWhenIdIsNotLoaded() {
        `when`(userRepository.save(any())).thenAnswer { it.getArgument(0) }

        val updated =
            service.update(
                User {
                    username = "draft-user"
                },
            )

        assertEquals("draft-user", updated.username)
        verify(userRepository).save(any())
        verifyNoMoreInteractions(userRepository)
        verifyNoInteractions(roleRepository)
    }

    @Test
    fun removeDelegatesDeleteToRepository() {
        service.removeById(99L)

        verify(userRepository).removeById(99L)
    }
}
