package io.github.lishangbu.avalon.authorization.service.impl

import io.github.lishangbu.avalon.authorization.entity.AuthenticationLog
import io.github.lishangbu.avalon.authorization.entity.Menu
import io.github.lishangbu.avalon.authorization.entity.OauthAuthorization
import io.github.lishangbu.avalon.authorization.entity.OauthAuthorizationConsent
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
import org.babyfish.jimmer.sql.ast.mutation.AssociatedSaveMode
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
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
        roles()
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
        menus()
        menus.forEach { menu -> menus().addBy(menu) }
    }

internal fun menu(
    id: Long,
    parentId: Long? = null,
    label: String = "Menu $id",
    sortingOrder: Int = id.toInt(),
    component: String = "component/$id",
): Menu =
    Menu {
        this.id = id
        parentId?.let { parent = Menu { this.id = it } }
        this.extra = null
        this.icon = null
        this.label = label
        this.sortingOrder = sortingOrder
        this.key = "key-$id"
        this.path = "/$id"
        this.name = "menu-$id"
        this.redirect = null
        this.component = component
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

internal fun userView(base: User): UserView = UserView(base)

internal fun currentUserView(base: User): CurrentUserView = CurrentUserView(base)

internal fun userView(
    id: Long = 1L,
    username: String = "alice",
    phone: String = "13800138000",
    email: String = "alice@example.com",
    avatar: String = "avatar.png",
    roles: List<Role> = emptyList(),
): UserView = UserView(user(id, username, phone, email, avatar, "hashed", roles))

internal fun roleView(base: Role): RoleView = RoleView(base)

internal fun roleView(
    id: Long,
    code: String = "ROLE_$id",
    name: String = "Role $id",
    enabled: Boolean = true,
    menus: List<Menu> = emptyList(),
): RoleView = RoleView(role(id, code, name, enabled, menus))

internal fun menuView(base: Menu): MenuView = MenuView(base)

internal fun menuView(
    id: Long,
    parentId: Long? = null,
    label: String = "Menu $id",
    sortingOrder: Int = id.toInt(),
    component: String = "component/$id",
): MenuView = MenuView(menu(id, parentId, label, sortingOrder, component))

internal fun menuTreeView(
    id: Long,
    parentId: Long? = null,
    label: String = "Menu $id",
    sortingOrder: Int = id.toInt(),
    component: String = "component/$id",
    children: List<MenuTreeView> = emptyList(),
): MenuTreeView =
    MenuTreeView(
        id = id.toString(),
        parentId = parentId?.toString(),
        disabled = false,
        extra = null,
        icon = null,
        key = "key-$id",
        label = label,
        show = true,
        path = "/$id",
        name = "menu-$id",
        redirect = null,
        component = component,
        sortingOrder = sortingOrder,
        pinned = false,
        showTab = true,
        enableMultiTab = true,
        children = children,
    )

internal fun registeredClientView(base: OauthRegisteredClient): OauthRegisteredClientView = OauthRegisteredClientView(base)

internal fun registeredClientView(
    id: String,
    clientId: String = id,
    clientIdIssuedAt: Instant = Instant.parse("2026-03-25T00:00:00Z"),
    clientSecret: String = "secret",
    clientSecretExpiresAt: Instant? = null,
    clientName: String = "Client $id",
    clientAuthenticationMethods: String = "client_secret_basic",
    authorizationGrantTypes: String = "client_credentials",
    redirectUris: String? = null,
    postLogoutRedirectUris: String? = null,
    scopes: String = "read",
    requireProofKey: Boolean? = null,
    requireAuthorizationConsent: Boolean? = null,
    jwkSetUrl: String? = null,
    tokenEndpointAuthenticationSigningAlgorithm: String? = null,
    x509CertificateSubjectDn: String? = null,
    authorizationCodeTimeToLive: String? = null,
    accessTokenTimeToLive: String? = null,
    accessTokenFormat: String? = null,
    deviceCodeTimeToLive: String? = null,
    reuseRefreshTokens: Boolean? = null,
    refreshTokenTimeToLive: String? = null,
    idTokenSignatureAlgorithm: String? = null,
    x509CertificateBoundAccessTokens: Boolean? = null,
): OauthRegisteredClientView =
    OauthRegisteredClientView(
        id = id,
        clientId = clientId,
        clientIdIssuedAt = clientIdIssuedAt,
        clientSecret = clientSecret,
        clientSecretExpiresAt = clientSecretExpiresAt,
        clientName = clientName,
        clientAuthenticationMethods = clientAuthenticationMethods,
        authorizationGrantTypes = authorizationGrantTypes,
        redirectUris = redirectUris,
        postLogoutRedirectUris = postLogoutRedirectUris,
        scopes = scopes,
        requireProofKey = requireProofKey,
        requireAuthorizationConsent = requireAuthorizationConsent,
        jwkSetUrl = jwkSetUrl,
        tokenEndpointAuthenticationSigningAlgorithm = tokenEndpointAuthenticationSigningAlgorithm,
        x509CertificateSubjectDn = x509CertificateSubjectDn,
        authorizationCodeTimeToLive = authorizationCodeTimeToLive,
        accessTokenTimeToLive = accessTokenTimeToLive,
        accessTokenFormat = accessTokenFormat,
        deviceCodeTimeToLive = deviceCodeTimeToLive,
        reuseRefreshTokens = reuseRefreshTokens,
        refreshTokenTimeToLive = refreshTokenTimeToLive,
        idTokenSignatureAlgorithm = idTokenSignatureAlgorithm,
        x509CertificateBoundAccessTokens = x509CertificateBoundAccessTokens,
    )

internal inline fun <reified T> any(): T = Mockito.any(T::class.java)

internal fun anyAuthenticationLog(): AuthenticationLog = any()

internal fun anyMenu(): Menu = any()

internal fun anyOauthAuthorization(): OauthAuthorization = any()

internal fun anyOauthAuthorizationConsent(): OauthAuthorizationConsent = any()

internal fun anyOauthRegisteredClient(): OauthRegisteredClient = any()

internal fun anyRole(): Role = any()

internal fun anyUser(): User = any()

@Suppress("UNCHECKED_CAST")
internal fun <T> eq(value: T): T = Mockito.eq(value) ?: value

@Suppress("UNCHECKED_CAST")
internal fun <T> isNull(): T? = Mockito.isNull<T>()

internal fun upsertMode(): SaveMode = eq(SaveMode.UPSERT)

internal fun replaceAssociatedMode(): AssociatedSaveMode = eq(AssociatedSaveMode.REPLACE)

@Suppress("UNCHECKED_CAST")
internal fun <T> same(value: T): T = Mockito.same(value)
