package io.github.lishangbu.avalon.authorization.model

import io.github.lishangbu.avalon.authorization.entity.Menu

/**
 * 菜单树节点
 *
 * 使用独立节点模型构建树结构，避免直接依赖实体接口
 */
data class MenuTreeNode(
    /** ID */
    val id: Long,
    /** 父节点 ID */
    val parentId: Long?,
    /** 禁用状态 */
    val disabled: Boolean?,
    /** 扩展信息 */
    val extra: String?,
    /** 图标 */
    val icon: String?,
    /** 密钥 */
    val key: String?,
    /** 标签 */
    val label: String?,
    /** 显示 */
    val show: Boolean?,
    /** 路径 */
    val path: String?,
    /** 名称 */
    val name: String?,
    /** 重定向 */
    val redirect: String?,
    /** 组件 */
    val component: String?,
    /** 排序顺序 */
    val sortingOrder: Int?,
    /** 固定 */
    val pinned: Boolean?,
    /** 显示标签页 */
    val showTab: Boolean?,
    /** 启用多标签页 */
    val enableMultiTab: Boolean?,
    /** 子节点列表 */
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
