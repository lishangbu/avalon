package io.github.lishangbu.avalon.catalog.application.query

import io.github.lishangbu.avalon.shared.application.query.PageRequest

/**
 * 物种列表分页查询契约。
 *
 * 当前版本只开放固定排序下的基础分页，不提前暴露筛选条件或客户端可控排序，
 * 以保持 species 列表的读取协议尽量简单稳定；后续若要加入关键字、状态或类型筛选，
 * 在这里继续扩展即可。
 *
 * @property pageRequest 当前查询使用的分页请求。
 */
data class SpeciesPageQuery(
    val pageRequest: PageRequest = PageRequest(),
)