package io.github.lishangbu.avalon.identity.access.domain.iam

/**
 * 面向读取侧返回的树形菜单节点。
 *
 * @property id 菜单标识。
 * @property parentId 父菜单标识。
 * @property key 菜单稳定键值。
 * @property title 菜单展示标题。
 * @property visible 菜单是否可见。
 * @property path 路由路径，可为空。
 * @property routeName 前端路由名称，可为空。
 * @property component 前端组件标识，可为空。
 * @property icon 图标标识，可为空。
 * @property sortingOrder 菜单排序值。
 * @property type 菜单节点类型。
 * @property hidden 是否为隐藏路由。
 * @property disabled 菜单是否禁用。
 * @property external 是否为外部链接。
 * @property target 外部链接打开方式，可为空。
 * @property children 子菜单列表。
 */
data class MenuTreeNode(
    val id: MenuId,
    val parentId: MenuId?,
    val key: String,
    val title: String,
    val visible: Boolean,
    val path: String?,
    val routeName: String?,
    val component: String?,
    val icon: String?,
    val sortingOrder: Int,
    val type: MenuType,
    val hidden: Boolean,
    val disabled: Boolean,
    val external: Boolean,
    val target: String?,
    val children: List<MenuTreeNode>,
)