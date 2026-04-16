package io.github.lishangbu.avalon.catalog.interfaces.http.species

import io.github.lishangbu.avalon.catalog.application.query.SpeciesPageQuery
import io.github.lishangbu.avalon.shared.application.query.PageRequest
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.ws.rs.DefaultValue
import jakarta.ws.rs.QueryParam

/**
 * 物种列表分页查询参数。
 *
 * 该类型负责承接 `/api/catalog/species` 的 query param，并在 HTTP 边界内完成
 * 基础分页约束校验，随后再转换为 application 层查询契约。
 *
 * @property page 页码，从 `1` 开始。
 * @property size 单页记录数，范围为 `1..100`。
 */
class SpeciesPageParameters {
    @field:QueryParam("page")
    @field:DefaultValue("1")
    @field:Min(1)
    var page: Int = 1

    @field:QueryParam("size")
    @field:DefaultValue("20")
    @field:Min(1)
    @field:Max(100)
    var size: Int = 20

    /**
     * 将 HTTP 查询参数转换为 application 层分页查询契约。
     *
     * @return 供 Catalog 应用服务消费的物种分页查询。
     */
    fun toQuery(): SpeciesPageQuery =
        SpeciesPageQuery(
            pageRequest = PageRequest(page = page, size = size),
        )
}