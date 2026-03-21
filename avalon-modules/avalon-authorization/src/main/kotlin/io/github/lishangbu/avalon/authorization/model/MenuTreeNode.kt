package io.github.lishangbu.avalon.authorization.model

import io.github.lishangbu.avalon.authorization.entity.Menu

/**
 * 菜单树节点
 *
 * 使用独立节点模型构建树结构，避免直接继承实体接口。
 */
data class MenuTreeNode(
    val id: Long,
    val parentId: Long?,
    val disabled: Boolean?,
    val extra: String?,
    val icon: String?,
    val key: String?,
    val label: String?,
    val show: Boolean?,
    val path: String?,
    val name: String?,
    val redirect: String?,
    val component: String?,
    val sortingOrder: Int?,
    val pinned: Boolean?,
    val showTab: Boolean?,
    val enableMultiTab: Boolean?,
    var children: List<MenuTreeNode>? = null,
) {
    constructor(
        menu: Menu,
    ) : this(
        id = menu.id,
        parentId = menu.parentId,
        disabled = menu.disabled,
        extra = menu.extra,
        icon = menu.icon,
        key = menu.key,
        label = menu.label,
        show = menu.show,
        path = menu.path,
        name = menu.name,
        redirect = menu.redirect,
        component = menu.component,
        sortingOrder = menu.sortingOrder,
        pinned = menu.pinned,
        showTab = menu.showTab,
        enableMultiTab = menu.enableMultiTab,
    )
}
