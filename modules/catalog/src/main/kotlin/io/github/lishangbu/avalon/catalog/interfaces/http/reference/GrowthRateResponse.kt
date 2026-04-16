package io.github.lishangbu.avalon.catalog.interfaces.http.reference

import io.github.lishangbu.avalon.catalog.domain.GrowthRate
import java.util.UUID

/**
 * 成长率明细响应。
 *
 * @property id 成长率主键。
 * @property code 成长率业务编码。
 * @property name 成长率展示名称。
 * @property formulaCode 经验公式业务码。
 * @property description 成长率说明，可为空。
 * @property sortingOrder 列表展示顺序。
 * @property enabled 当前成长率是否启用。
 * @property version 乐观锁版本号。
 */
data class GrowthRateResponse(
    val id: UUID,
    val code: String,
    val name: String,
    val formulaCode: String,
    val description: String?,
    val sortingOrder: Int,
    val enabled: Boolean,
    val version: Long,
)

/**
 * 把成长率定义转换为接口响应。
 *
 * @return 可直接返回给调用方的成长率明细。
 */
fun GrowthRate.toResponse(): GrowthRateResponse =
    GrowthRateResponse(
        id = id.value,
        code = code,
        name = name,
        formulaCode = formulaCode.name,
        description = description,
        sortingOrder = sortingOrder,
        enabled = enabled,
        version = version,
    )
