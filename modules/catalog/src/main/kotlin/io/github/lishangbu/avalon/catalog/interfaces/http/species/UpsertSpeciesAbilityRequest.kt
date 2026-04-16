package io.github.lishangbu.avalon.catalog.interfaces.http.species

import io.github.lishangbu.avalon.catalog.domain.*
import java.util.*
import java.util.UUID

/**
 * 创建或更新物种特性关联时使用的请求体。
 *
 * @property speciesId 物种定义主键。
 * @property abilityId 特性定义主键。
 * @property slotCode 特性槽位业务码；映射时会统一转成大写。
 * @property sortingOrder 同一物种下的展示顺序。
 * @property enabled 当前关联是否启用。
 */
data class UpsertSpeciesAbilityRequest(
    val speciesId: UUID,
    val abilityId: UUID,
    val slotCode: String,
    val sortingOrder: Int = 0,
    val enabled: Boolean = true,
)

/**
 * 把物种特性关联请求转换为领域草稿。
 *
 * @return 可直接交给应用服务写入的物种特性关联草稿。
 */
fun UpsertSpeciesAbilityRequest.toDraft(): SpeciesAbilityDraft =
    SpeciesAbilityDraft(
        speciesId = SpeciesId(speciesId),
        abilityId = AbilityId(abilityId),
        slotCode = slotCode.toAbilitySlotCode(),
        sortingOrder = sortingOrder,
        enabled = enabled,
    )

private fun String.toAbilitySlotCode(): AbilitySlotCode {
    val normalized = trim().uppercase(Locale.ROOT)
    return try {
        AbilitySlotCode.valueOf(normalized)
    } catch (_: IllegalArgumentException) {
        val supportedCodes = AbilitySlotCode.values().joinToString(", ") { it.name }
        throw CatalogBadRequest("slotCode must be one of: $supportedCodes.")
    }
}

