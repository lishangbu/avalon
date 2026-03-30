package io.github.lishangbu.avalon.authorization.service.impl

import io.github.lishangbu.avalon.authorization.entity.User
import io.github.lishangbu.avalon.authorization.entity.dto.CurrentUserView
import io.github.lishangbu.avalon.authorization.entity.dto.SaveUserInput
import io.github.lishangbu.avalon.authorization.entity.dto.UpdateUserInput
import io.github.lishangbu.avalon.authorization.entity.dto.UserSpecification
import io.github.lishangbu.avalon.authorization.repository.AuthorizationFetchers
import io.github.lishangbu.avalon.authorization.repository.RoleRepository
import io.github.lishangbu.avalon.authorization.repository.UserRepository
import io.github.lishangbu.avalon.jimmer.support.readOrNull
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.sql.ast.mutation.AssociatedSaveMode
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
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
    fun wrapsCurrentUserViewForAccountLookup() {
        `when`(userRepository.loadByAccountWithRoles("alice")).thenReturn(user(1L, roles = listOf(role(1L, "ADMIN"))))

        val result = service.getUserByUsername("alice")

        assertNotNull(result)
        assertEquals("1", result!!.id)
        assertEquals("alice", result.username)
        assertEquals(setOf("ADMIN"), result.roles.mapNotNull { it.code }.toSet())
    }

    @Test
    fun returnsNullWhenAccountLookupMisses() {
        `when`(userRepository.loadByAccountWithRoles("missing")).thenReturn(null)

        val result = service.getUserByUsername("missing")

        assertNull(result)
    }

    @Test
    fun delegatesPageListAndIdLookups() {
        val pageable = PageRequest.of(0, 10)
        val specification = mock(UserSpecification::class.java)
        val page = Page(listOf(userView(1L)), 1, 1)
        val users = listOf(userView(2L))
        val found = userView(3L)
        `when`(userRepository.pageViews(specification, pageable)).thenReturn(page)
        `when`(userRepository.listViews(specification)).thenReturn(users)
        `when`(userRepository.loadViewById(3L)).thenReturn(found)

        assertSame(page, service.getPageByCondition(specification, pageable))
        assertSame(users, service.listByCondition(specification))
        assertSame(found, service.getById(3L))
    }

    @Test
    fun saveBindsRolesBeforePersisting() {
        val incoming =
            SaveUserInput(
                username = "alice",
                phone = "13800138000",
                email = "alice@example.com",
                avatar = "avatar.png",
                hashedPassword = "hashed",
                roleIds = listOf("10", "11"),
            )
        val boundRoles = listOf(role(10L, "ADMIN"), role(11L, "USER"))
        var persisted: User? = null
        `when`(roleRepository.findAllById(setOf(10L, 11L))).thenReturn(boundRoles)
        `when`(userRepository.save(anyUser(), eq(SaveMode.INSERT_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())).thenAnswer {
            persisted = it.getArgument<User>(0)
            user(
                id = 1L,
                username = "alice",
                phone = "13800138000",
                email = "alice@example.com",
                avatar = "avatar.png",
                hashedPassword = "hashed",
                roles = boundRoles,
            )
        }
        `when`(userRepository.loadViewById(1L)).thenReturn(
            userView(
                id = 1L,
                username = "alice",
                phone = "13800138000",
                email = "alice@example.com",
                avatar = "avatar.png",
                roles = boundRoles,
            ),
        )

        val saved = service.save(incoming)
        val prepared = requireNotNull(persisted)

        assertEquals("alice", prepared.username)
        assertEquals("hashed", prepared.hashedPassword)
        assertEquals(setOf("ADMIN", "USER"), prepared.roles.mapNotNull { it.code }.toSet())
        assertEquals("alice", saved.username)
        assertEquals(setOf("ADMIN", "USER"), saved.roles.mapNotNull { it.code }.toSet())
        verify(roleRepository).findAllById(setOf(10L, 11L))
        verify(userRepository).save(anyUser(), eq(SaveMode.INSERT_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())
        verify(userRepository).loadViewById(1L)
    }

    @Test
    fun saveKeepsRolesEmptyWhenInputProvidesEmptyRoleIds() {
        var persisted: User? = null
        `when`(userRepository.save(anyUser(), eq(SaveMode.INSERT_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())).thenAnswer {
            persisted = it.getArgument<User>(0)
            user(id = 2L, username = "alice", hashedPassword = "hashed")
        }
        `when`(userRepository.loadViewById(2L)).thenReturn(userView(id = 2L, username = "alice", roles = emptyList()))

        val saved =
            service.save(
                SaveUserInput(
                    username = "alice",
                    hashedPassword = "hashed",
                    roleIds = emptyList(),
                ),
            )
        val prepared = requireNotNull(persisted)

        assertEquals("alice", prepared.username)
        assertEquals("hashed", prepared.hashedPassword)
        assertEquals(emptyList<String>(), prepared.roles.mapNotNull { it.code })
        assertEquals("alice", saved.username)
        assertEquals(emptyList<String>(), saved.roles.mapNotNull { it.code })
        verifyNoInteractions(roleRepository)
        verify(userRepository).loadViewById(2L)
    }

    @Test
    fun updatePreservesExistingScalarFieldsWhenOptionalInputsAreNull() {
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
        var persisted: User? = null
        `when`(userRepository.findNullable(9L, AuthorizationFetchers.USER_WITH_ROLES)).thenReturn(existing)
        `when`(roleRepository.findAllById(setOf(20L))).thenReturn(listOf(existingRole))
        `when`(userRepository.save(anyUser())).thenAnswer {
            it.getArgument<User>(0).also { user -> persisted = user }
        }
        `when`(userRepository.loadViewById(9L)).thenReturn(
            userView(
                id = 9L,
                username = "old-user",
                phone = "13900000000",
                email = "old@example.com",
                avatar = "old.png",
                roles = listOf(existingRole),
            ),
        )

        val updated = service.update(UpdateUserInput(id = "9", roleIds = listOf("20")))
        val prepared = requireNotNull(persisted)

        assertEquals("old-user", prepared.username)
        assertEquals("13900000000", prepared.phone)
        assertEquals("old@example.com", prepared.email)
        assertEquals("old.png", prepared.avatar)
        assertEquals("old-hash", prepared.hashedPassword)
        assertEquals("old-user", updated.username)
        assertEquals("13900000000", updated.phone)
        assertEquals("old@example.com", updated.email)
        assertEquals("old.png", updated.avatar)
        assertEquals(listOf("AUDITOR"), updated.roles.mapNotNull { it.code })
        verify(userRepository).loadViewById(9L)
    }

    @Test
    fun updateClearsExistingRolesWhenInputLoadsEmptyRoleIds() {
        val existing =
            user(
                id = 9L,
                roles = listOf(role(20L, "AUDITOR")),
            )
        var persisted: User? = null
        `when`(userRepository.findNullable(9L, AuthorizationFetchers.USER_WITH_ROLES)).thenReturn(existing)
        `when`(userRepository.save(anyUser())).thenAnswer {
            it.getArgument<User>(0).also { user -> persisted = user }
        }
        `when`(userRepository.loadViewById(9L)).thenReturn(userView(id = 9L, roles = emptyList()))

        val updated =
            service.update(
                UpdateUserInput(
                    id = "9",
                    roleIds = emptyList(),
                ),
            )
        val prepared = requireNotNull(persisted)

        assertEquals(emptyList<String>(), (prepared.readOrNull { roles } ?: emptyList()).mapNotNull { it.code })
        assertEquals(emptyList<String>(), updated.roles.mapNotNull { it.code })
        verifyNoInteractions(roleRepository)
        verify(userRepository).loadViewById(9L)
    }

    @Test
    fun updateUsesIncomingFieldsAndRolesWhenExistingUserDoesNotExist() {
        val boundRole = role(30L, "MANAGER")
        val incoming =
            UpdateUserInput(
                id = "9",
                username = "new-user",
                phone = "13600000000",
                email = "new@example.com",
                avatar = "new.png",
                hashedPassword = "new-hash",
                roleIds = listOf("30"),
            )
        var persisted: User? = null
        `when`(userRepository.findNullable(9L, AuthorizationFetchers.USER_WITH_ROLES)).thenReturn(null)
        `when`(roleRepository.findAllById(setOf(30L))).thenReturn(listOf(boundRole))
        `when`(userRepository.save(anyUser())).thenAnswer {
            it.getArgument<User>(0).also { user -> persisted = user }
        }
        `when`(userRepository.loadViewById(9L)).thenReturn(
            userView(
                id = 9L,
                username = "new-user",
                phone = "13600000000",
                email = "new@example.com",
                avatar = "new.png",
                roles = listOf(boundRole),
            ),
        )

        val updated = service.update(incoming)
        val prepared = requireNotNull(persisted)

        assertEquals("new-user", prepared.username)
        assertEquals("13600000000", prepared.phone)
        assertEquals("new@example.com", prepared.email)
        assertEquals("new.png", prepared.avatar)
        assertEquals("new-hash", prepared.hashedPassword)
        assertEquals("new-user", updated.username)
        assertEquals("13600000000", updated.phone)
        assertEquals("new@example.com", updated.email)
        assertEquals("new.png", updated.avatar)
        assertEquals(listOf("MANAGER"), updated.roles.mapNotNull { it.code })
        verify(userRepository).findNullable(9L, AuthorizationFetchers.USER_WITH_ROLES)
        verify(roleRepository).findAllById(setOf(30L))
        verify(userRepository).loadViewById(9L)
    }

    @Test
    fun updateLeavesOptionalFieldsUnsetWhenExistingUserCannotBeFound() {
        var persisted: User? = null
        `when`(userRepository.findNullable(99L, AuthorizationFetchers.USER_WITH_ROLES)).thenReturn(null)
        `when`(userRepository.save(anyUser())).thenAnswer {
            it.getArgument<User>(0).also { user -> persisted = user }
        }
        `when`(userRepository.loadViewById(99L)).thenReturn(userView(id = 99L, roles = emptyList()))

        val updated = service.update(UpdateUserInput(id = "99", roleIds = emptyList()))
        val prepared = requireNotNull(persisted)

        assertEquals(99L, prepared.id)
        assertNull(prepared.readOrNull { username })
        assertNull(prepared.readOrNull { phone })
        assertNull(prepared.readOrNull { email })
        assertNull(prepared.readOrNull { avatar })
        assertNull(prepared.readOrNull { hashedPassword })
        assertEquals(emptyList<String>(), (prepared.readOrNull { roles } ?: emptyList()).mapNotNull { it.code })
        assertEquals("99", updated.id)
        verify(userRepository).findNullable(99L, AuthorizationFetchers.USER_WITH_ROLES)
        verifyNoInteractions(roleRepository)
        verify(userRepository).loadViewById(99L)
    }

    @Test
    fun removeDelegatesDeleteToRepository() {
        service.removeById(99L)

        verify(userRepository).deleteById(99L)
    }
}
