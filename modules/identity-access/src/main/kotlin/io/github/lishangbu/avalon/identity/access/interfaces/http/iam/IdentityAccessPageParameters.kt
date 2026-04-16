package io.github.lishangbu.avalon.identity.access.interfaces.http.iam

import io.github.lishangbu.avalon.shared.application.query.PageRequest
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.ws.rs.DefaultValue
import jakarta.ws.rs.QueryParam

/**
 * IAM 管理端列表接口共用的分页查询参数。
 *
 * 该类型负责承接 `page/size` query param，并在 HTTP 边界内完成基础分页约束校验。
 *
 * @property page 页码，从 `1` 开始。
 * @property size 单页记录数，范围为 `1..100`。
 */
class IdentityAccessPageParameters {
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
     * 将 HTTP 分页参数转换为应用层分页请求。
     *
     * @return 应用层使用的分页请求。
     */
    fun toPageRequest(): PageRequest = PageRequest(page = page, size = size)
}