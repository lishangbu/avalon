package io.github.lishangbu.avalon.identity.access.application.iam.query

import io.github.lishangbu.avalon.shared.application.query.PageRequest

/**
 * 平铺菜单列表分页查询契约。
 *
 * 菜单树接口仍然直接返回完整树结构，这里只负责管理端平铺菜单列表的分页读取。
 *
 * @property pageRequest 当前查询使用的分页请求。
 */
data class MenuPageQuery(
    val pageRequest: PageRequest = PageRequest(),
)