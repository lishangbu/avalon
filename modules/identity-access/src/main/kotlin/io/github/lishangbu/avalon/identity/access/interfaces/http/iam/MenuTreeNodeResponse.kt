package io.github.lishangbu.avalon.identity.access.interfaces.http.iam

import io.github.lishangbu.avalon.identity.access.domain.iam.MenuTreeNode
import io.github.lishangbu.avalon.identity.access.domain.iam.MenuType
import java.util.UUID

/**
 * 菜单树节点响应。
 *
 * @property id 菜单主键。
 * @property parentId 父菜单主键；根节点时为空。
 * @property key 菜单稳定键值。
 * @property title 菜单展示标题。
 * @property visible 菜单是否对常规导航可见。
 * @property path 路由路径，可为空。
 * @property routeName 前端路由名称，可为空。
 * @property component 前端组件标识，可为空。
 * @property icon 菜单图标标识，可为空。
 * @property sortingOrder 菜单排序值。
 * @property type 菜单节点类型。
 * @property hidden 是否以隐藏路由形式存在。
 * @property disabled 菜单是否禁用。
 * @property external 是否为外部链接。
 * @property target 外部链接打开方式，可为空。
 * @property children 当前节点的已排序子菜单列表。
 */
data class MenuTreeNodeResponse(
    val id: UUID,
    val parentId: UUID?,
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
    val children: List<MenuTreeNodeResponse>,
)

/**
 * 将菜单树节点转换为树形响应。
 */
fun MenuTreeNode.toResponse(): MenuTreeNodeResponse =
    MenuTreeNodeResponse(
        id = id.value,
        parentId = parentId?.value,
        key = key,
        title = title,
        visible = visible,
        path = path,
        routeName = routeName,
        component = component,
        icon = icon,
        sortingOrder = sortingOrder,
        type = type,
        hidden = hidden,
        disabled = disabled,
        external = external,
        target = target,
        children = children.map(MenuTreeNode::toResponse),
    )

