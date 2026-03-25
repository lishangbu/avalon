package io.github.lishangbu.avalon.authorization.service.impl

import io.github.lishangbu.avalon.authorization.entity.Menu
import io.github.lishangbu.avalon.authorization.entity.OauthRegisteredClient
import io.github.lishangbu.avalon.authorization.entity.Role
import io.github.lishangbu.avalon.authorization.entity.User
import io.github.lishangbu.avalon.authorization.entity.addBy
import org.mockito.Mockito
import java.time.Instant

internal fun user(
    id: Long = 1L,
    username: String = "alice",
    phone: String = "13800138000",
    email: String = "alice@example.com",
    avatar: String = "avatar.png",
    hashedPassword: String = "hashed",
    roles: List<Role> = emptyList(),
): User =
    User {
        this.id = id
        this.username = username
        this.phone = phone
        this.email = email
        this.avatar = avatar
        this.hashedPassword = hashedPassword
        roles.forEach { role -> roles().addBy(role) }
    }

internal fun role(
    id: Long,
    code: String = "ROLE_$id",
    name: String = "Role $id",
    enabled: Boolean = true,
    menus: List<Menu> = emptyList(),
): Role =
    Role {
        this.id = id
        this.code = code
        this.name = name
        this.enabled = enabled
        menus.forEach { menu -> menus().addBy(menu) }
    }

internal fun menu(
    id: Long,
    parentId: Long? = null,
    label: String = "Menu $id",
    sortingOrder: Int = id.toInt(),
): Menu =
    Menu {
        this.id = id
        this.parentId = parentId
        this.extra = null
        this.icon = null
        this.label = label
        this.sortingOrder = sortingOrder
        this.key = "key-$id"
        this.path = "/$id"
        this.name = "menu-$id"
        this.redirect = null
        this.component = null
        this.show = true
        this.disabled = false
        this.pinned = false
        this.showTab = true
        this.enableMultiTab = true
    }

internal fun registeredClient(id: String): OauthRegisteredClient =
    OauthRegisteredClient {
        this.id = id
        clientId = id
        clientIdIssuedAt = Instant.parse("2026-03-25T00:00:00Z")
        clientSecret = "secret"
        clientName = "Client $id"
        clientAuthenticationMethods = "client_secret_basic"
        authorizationGrantTypes = "client_credentials"
        scopes = "read"
    }

@Suppress("UNCHECKED_CAST")
internal fun <T> any(): T = Mockito.any<T>()

@Suppress("UNCHECKED_CAST")
internal fun <T> same(value: T): T = Mockito.same(value)
