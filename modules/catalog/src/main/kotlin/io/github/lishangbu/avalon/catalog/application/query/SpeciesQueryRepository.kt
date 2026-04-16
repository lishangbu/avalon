package io.github.lishangbu.avalon.catalog.application.query

import io.github.lishangbu.avalon.catalog.domain.Species
import io.github.lishangbu.avalon.shared.application.query.Page

/**
 * 物种列表查询端口。
 *
 * Catalog 的写入仍通过领域仓储处理，这里只承载面向 application 用例的分页读取契约，
 * 避免把 offset 分页这类查询策略继续塞进领域仓储接口。
 */
interface SpeciesQueryRepository {
    /**
     * 按固定排序分页读取物种定义。
     *
     * @param query 物种分页查询条件。
     * @return 当前页物种结果与分页元数据。
     */
    suspend fun pageSpecies(query: SpeciesPageQuery): Page<Species>
}