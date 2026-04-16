package io.github.lishangbu.avalon.catalog.interfaces.http.species

import io.github.lishangbu.avalon.catalog.domain.CatalogBadRequest
import io.github.lishangbu.avalon.catalog.domain.EvolutionTriggerCode
import io.github.lishangbu.avalon.catalog.domain.SpeciesEvolutionDraft
import io.github.lishangbu.avalon.catalog.domain.SpeciesId
import jakarta.validation.constraints.Min
import java.util.*
import java.util.UUID

/**
 * 创建或更新物种进化定义时使用的请求体。
 *
 * @property fromSpeciesId 进化起点物种主键。
 * @property toSpeciesId 进化终点物种主键。
 * @property triggerCode 进化触发方式业务码；映射时会统一转成大写。
 * @property minLevel 等级进化时的最低等级；非等级进化时可为空。
 * @property description 进化说明，可为空。
 * @property sortingOrder 同一物种下的展示顺序。
 * @property enabled 当前进化定义是否启用。
 */
data class UpsertSpeciesEvolutionRequest(
    val fromSpeciesId: UUID,
    val toSpeciesId: UUID,
    val triggerCode: String,
    @field:Min(1)
    val minLevel: Int? = null,
    val description: String? = null,
    val sortingOrder: Int = 0,
    val enabled: Boolean = true,
)

/**
 * 把物种进化请求转换为领域草稿。
 *
 * @return 可直接交给应用服务写入的物种进化草稿。
 * @throws IllegalArgumentException 当起终点物种相同，或等级进化缺少最低等级时抛出。
 */
fun UpsertSpeciesEvolutionRequest.toDraft(): SpeciesEvolutionDraft {
    val trigger = triggerCode.toEvolutionTriggerCode()
    if (fromSpeciesId == toSpeciesId) {
        throw CatalogBadRequest("fromSpeciesId must be different from toSpeciesId.")
    }
    if (trigger == EvolutionTriggerCode.LEVEL && minLevel == null) {
        throw CatalogBadRequest("minLevel is required when triggerCode is LEVEL.")
    }

    return SpeciesEvolutionDraft(
        fromSpeciesId = SpeciesId(fromSpeciesId),
        toSpeciesId = SpeciesId(toSpeciesId),
        triggerCode = trigger,
        minLevel = minLevel,
        description = description?.trim()?.takeIf { it.isNotEmpty() },
        sortingOrder = sortingOrder,
        enabled = enabled,
    )
}

private fun String.toEvolutionTriggerCode(): EvolutionTriggerCode {
    val normalized = trim().uppercase(Locale.ROOT)
    return try {
        EvolutionTriggerCode.valueOf(normalized)
    } catch (_: IllegalArgumentException) {
        val supportedCodes = EvolutionTriggerCode.values().joinToString(", ") { it.name }
        throw CatalogBadRequest("triggerCode must be one of: $supportedCodes.")
    }
}

