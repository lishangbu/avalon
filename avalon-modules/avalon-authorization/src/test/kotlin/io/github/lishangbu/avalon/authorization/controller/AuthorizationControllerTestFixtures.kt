package io.github.lishangbu.avalon.authorization.controller

import io.github.lishangbu.avalon.authorization.entity.Menu
import io.github.lishangbu.avalon.authorization.entity.Role
import io.github.lishangbu.avalon.authorization.entity.User
import io.github.lishangbu.avalon.authorization.model.MenuTreeNode

internal fun userEntity(id: Long): User =
    User {
        this.id = id
        username = "user-$id"
        phone = "138001380$id"
        email = "user-$id@example.com"
        avatar = "avatar-$id.png"
        hashedPassword = "hashed-$id"
    }

internal fun roleEntity(id: Long): Role =
    Role {
        this.id = id
        code = "ROLE_$id"
        name = "Role $id"
        enabled = true
    }

internal fun menuEntity(id: Long): Menu =
    Menu {
        this.id = id
        parentId = null
        disabled = false
        extra = null
        icon = null
        key = "key-$id"
        label = "Menu $id"
        show = true
        path = "/$id"
        name = "menu-$id"
        redirect = null
        component = null
        sortingOrder = id.toInt()
        pinned = false
        showTab = true
        enableMultiTab = true
    }

internal fun treeNode(
    id: Long,
    parentId: Long? = null,
): MenuTreeNode =
    MenuTreeNode(
        id = id,
        parentId = parentId,
        disabled = false,
        extra = null,
        icon = null,
        key = "key-$id",
        label = "Menu $id",
        show = true,
        path = "/$id",
        name = "menu-$id",
        redirect = null,
        component = null,
        sortingOrder = id.toInt(),
        pinned = false,
        showTab = true,
        enableMultiTab = true,
    )
