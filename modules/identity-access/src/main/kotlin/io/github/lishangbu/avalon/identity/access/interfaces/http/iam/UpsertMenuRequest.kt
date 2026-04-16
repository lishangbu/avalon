package io.github.lishangbu.avalon.identity.access.interfaces.http.iam

import io.github.lishangbu.avalon.identity.access.domain.iam.MenuDraft
import io.github.lishangbu.avalon.identity.access.domain.iam.MenuId
import io.github.lishangbu.avalon.identity.access.domain.iam.MenuType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.util.UUID

/**
 * 菜单创建或更新请求。
 *
 * @property parentId 父菜单主键；为 `null` 时表示根菜单。
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
 */
data class UpsertMenuRequest(
    val parentId: UUID? = null,
    val disabled: Boolean = false,
    val extra: String? = null,
    @field:Size(max = 128)
    val icon: String? = null,
    @field:NotBlank
    @field:Size(max = 128)
    val key: String,
    @field:NotBlank
    @field:Size(max = 128)
    val title: String,
    val visible: Boolean = true,
    @field:Size(max = 255)
    val path: String? = null,
    @field:Size(max = 128)
    val routeName: String? = null,
    @field:Size(max = 255)
    val redirect: String? = null,
    @field:Size(max = 255)
    val component: String? = null,
    val sortingOrder: Int = 0,
    val pinned: Boolean = false,
    val showTab: Boolean = true,
    val enableMultiTab: Boolean = false,
    val type: MenuType = MenuType.MENU,
    val hidden: Boolean = false,
    val hideChildrenInMenu: Boolean = false,
    val flatMenu: Boolean = false,
    @field:Size(max = 255)
    val activeMenu: String? = null,
    val external: Boolean = false,
    @field:Size(max = 32)
    val target: String? = null,
)

/**
 * 将菜单请求转换为领域草稿。
 */
fun UpsertMenuRequest.toDraft(): MenuDraft =
    MenuDraft(
        parentId = parentId?.let(::MenuId),
        disabled = disabled,
        extra = extra?.trim()?.takeIf { it.isNotEmpty() },
        icon = icon?.trim()?.takeIf { it.isNotEmpty() },
        key = key.trim(),
        title = title.trim(),
        visible = visible,
        path = path?.trim()?.takeIf { it.isNotEmpty() },
        routeName = routeName?.trim()?.takeIf { it.isNotEmpty() },
        redirect = redirect?.trim()?.takeIf { it.isNotEmpty() },
        component = component?.trim()?.takeIf { it.isNotEmpty() },
        sortingOrder = sortingOrder,
        pinned = pinned,
        showTab = showTab,
        enableMultiTab = enableMultiTab,
        type = type,
        hidden = hidden,
        hideChildrenInMenu = hideChildrenInMenu,
        flatMenu = flatMenu,
        activeMenu = activeMenu?.trim()?.takeIf { it.isNotEmpty() },
        external = external,
        target = target?.trim()?.takeIf { it.isNotEmpty() },
    )

