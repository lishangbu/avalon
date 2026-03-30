package io.github.lishangbu.avalon.authorization.controller

import io.github.lishangbu.avalon.authorization.entity.Menu
import io.github.lishangbu.avalon.authorization.entity.OauthRegisteredClient
import io.github.lishangbu.avalon.authorization.entity.Role
import io.github.lishangbu.avalon.authorization.entity.User
import io.github.lishangbu.avalon.authorization.entity.addBy
import io.github.lishangbu.avalon.authorization.entity.dto.CurrentUserView
import io.github.lishangbu.avalon.authorization.entity.dto.MenuTreeView
import io.github.lishangbu.avalon.authorization.entity.dto.MenuView
import io.github.lishangbu.avalon.authorization.entity.dto.OauthRegisteredClientView
import io.github.lishangbu.avalon.authorization.entity.dto.RoleView
import io.github.lishangbu.avalon.authorization.entity.dto.UserView
import org.mockito.Mockito
import java.time.Instant

internal fun userEntity(
    id: Long,
    roles: List<Role> = emptyList(),
): User =
    User {
        this.id = id
        username = "user-$id"
        phone = "138001380$id"
        email = "user-$id@example.com"
        avatar = "avatar-$id.png"
        hashedPassword = "hashed-$id"
        roles()
        roles.forEach { role -> roles().addBy(role) }
    }

internal fun userView(id: Long): UserView = UserView(userEntity(id, roles = listOf(roleEntity(id))))

internal fun currentUserView(id: Long): CurrentUserView = CurrentUserView(userEntity(id, roles = listOf(roleEntity(id))))

internal fun roleEntity(
    id: Long,
    menus: List<Menu> = emptyList(),
): Role =
    Role {
        this.id = id
        code = "ROLE_$id"
        name = "Role $id"
        enabled = true
        menus()
        menus.forEach { menu -> menus().addBy(menu) }
    }

internal fun roleView(id: Long): RoleView = RoleView(roleEntity(id, menus = listOf(menuEntity(id))))

internal fun menuEntity(
    id: Long,
    parentId: Long? = null,
): Menu =
    Menu {
        this.id = id
        parentId?.let { parent = Menu { this.id = it } }
        disabled = false
        extra = null
        icon = null
        key = "key-$id"
        label = "Menu $id"
        show = true
        path = "/$id"
        name = "menu-$id"
        redirect = null
        component = "component/$id"
        sortingOrder = id.toInt()
        pinned = false
        showTab = true
        enableMultiTab = true
    }

internal fun menuView(
    id: Long,
    parentId: Long? = null,
): MenuView = MenuView(menuEntity(id, parentId))

internal fun menuTreeView(
    id: Long,
    parentId: Long? = null,
    children: List<MenuTreeView> = emptyList(),
): MenuTreeView =
    MenuTreeView(
        id = id.toString(),
        parentId = parentId?.toString(),
        disabled = false,
        extra = null,
        icon = null,
        key = "key-$id",
        label = "Menu $id",
        show = true,
        path = "/$id",
        name = "menu-$id",
        redirect = null,
        component = "component/$id",
        sortingOrder = id.toInt(),
        pinned = false,
        showTab = true,
        enableMultiTab = true,
        children = children,
    )

internal fun registeredClientEntity(id: String): OauthRegisteredClient =
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

internal fun registeredClientView(id: String): OauthRegisteredClientView =
    OauthRegisteredClientView(
        id = id,
        clientId = id,
        clientIdIssuedAt = Instant.parse("2026-03-25T00:00:00Z"),
        clientSecret = "secret",
        clientName = "Client $id",
        clientAuthenticationMethods = "client_secret_basic",
        authorizationGrantTypes = "client_credentials",
        scopes = "read",
    )

internal inline fun <reified T> any(): T = Mockito.any(T::class.java)
