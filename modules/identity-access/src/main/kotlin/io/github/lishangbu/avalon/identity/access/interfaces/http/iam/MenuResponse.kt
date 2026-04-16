package io.github.lishangbu.avalon.identity.access.interfaces.http.iam

import io.github.lishangbu.avalon.identity.access.domain.iam.Menu
import io.github.lishangbu.avalon.identity.access.domain.iam.MenuType
import java.util.UUID

/**
 * 菜单响应。
 *
 * @property id 菜单主键。
 * @property parentId 父菜单主键；根菜单时为空。
 * @property disabled 菜单是否禁用。
 * @property extra 附加扩展字段，可为空。
 * @property icon 菜单图标标识，可为空。
 * @property key 菜单稳定键值。
 * @property title 菜单展示标题。
 * @property visible 菜单是否对常规导航可见。
 * @property path 路由路径，可为空。
 * @property routeName 前端路由名称，可为空。
 * @property redirect 默认跳转目标，可为空。
 * @property component 前端组件标识，可为空。
 * @property sortingOrder 菜单排序值。
 * @property pinned 是否固定在导航区域。
 * @property showTab 是否展示标签页。
 * @property enableMultiTab 是否允许多标签实例。
 * @property type 菜单节点类型。
 * @property hidden 是否以隐藏路由形式存在。
 * @property hideChildrenInMenu 是否在导航中隐藏子菜单。
 * @property flatMenu 是否打平到父级导航。
 * @property activeMenu 当前路由激活时对应的菜单键，可为空。
 * @property external 是否为外部链接。
 * @property target 外部链接打开方式，可为空。
 * @property version 乐观锁版本号。
 */
data class MenuResponse(
    val id: UUID,
    val parentId: UUID?,
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
    val version: Long,
)

/**
 * 将菜单领域对象转换为接口响应。
 */
fun Menu.toResponse(): MenuResponse =
    MenuResponse(
        id = id.value,
        parentId = parentId?.value,
        disabled = disabled,
        extra = extra,
        icon = icon,
        key = key,
        title = title,
        visible = visible,
        path = path,
        routeName = routeName,
        redirect = redirect,
        component = component,
        sortingOrder = sortingOrder,
        pinned = pinned,
        showTab = showTab,
        enableMultiTab = enableMultiTab,
        type = type,
        hidden = hidden,
        hideChildrenInMenu = hideChildrenInMenu,
        flatMenu = flatMenu,
        activeMenu = activeMenu,
        external = external,
        target = target,
        version = version,
    )

