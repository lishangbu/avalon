package io.github.lishangbu.avalon.identity.access.domain.iam

/**
 * 创建或更新菜单时使用的输入草稿。
 *
 * @property parentId 父菜单标识；为 `null` 时表示根菜单。
 * @property disabled 菜单是否禁用。
 * @property extra 扩展字段，可为空。
 * @property icon 图标标识，可为空。
 * @property key 菜单稳定键值。
 * @property title 菜单展示标题。
 * @property visible 菜单是否可见。
 * @property path 路由路径，可为空。
 * @property routeName 前端路由名称，可为空。
 * @property redirect 默认跳转目标，可为空。
 * @property component 前端组件标识，可为空。
 * @property sortingOrder 菜单排序值。
 * @property pinned 是否固定。
 * @property showTab 是否展示标签页。
 * @property enableMultiTab 是否允许多标签实例。
 * @property type 菜单节点类型。
 * @property hidden 是否为隐藏路由。
 * @property hideChildrenInMenu 是否在导航中隐藏子菜单。
 * @property flatMenu 是否打平到父级导航。
 * @property activeMenu 当前路由激活时对应的菜单键，可为空。
 * @property external 是否为外部链接。
 * @property target 外部链接打开方式，可为空。
 */
data class MenuDraft(
    val parentId: MenuId?,
    val disabled: Boolean,
    val extra: String?,
    val icon: String?,
    val key: String,
    val title: String,
    val visible: Boolean,
    val path: String?,
    val routeName: String?,
    val redirect: String?,
    val component: String?,
    val sortingOrder: Int,
    val pinned: Boolean,
    val showTab: Boolean,
    val enableMultiTab: Boolean,
    val type: MenuType,
    val hidden: Boolean,
    val hideChildrenInMenu: Boolean,
    val flatMenu: Boolean,
    val activeMenu: String?,
    val external: Boolean,
    val target: String?,
)