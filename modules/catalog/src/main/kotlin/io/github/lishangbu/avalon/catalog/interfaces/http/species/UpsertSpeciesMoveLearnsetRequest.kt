package io.github.lishangbu.avalon.catalog.interfaces.http.species

import io.github.lishangbu.avalon.catalog.domain.MoveId
import io.github.lishangbu.avalon.catalog.domain.MoveLearnMethodId
import io.github.lishangbu.avalon.catalog.domain.SpeciesId
import io.github.lishangbu.avalon.catalog.domain.SpeciesMoveLearnsetDraft
import jakarta.validation.constraints.Min
import java.util.UUID

/**
 * 创建或更新物种招式学习关系时使用的请求体。
 *
 * @property speciesId 物种定义主键。
 * @property moveId 招式定义主键。
 * @property learnMethodId 学习方法主键；对应 `LEVEL-UP` 时会要求 `level` 同时填写。
 * @property level 学习等级；对应 `LEVEL-UP` 时必须非空且大于 0，其他学习方法必须为空。
 * @property sortingOrder 同一物种下的展示顺序。
 * @property enabled 当前关联是否启用。
 */
data class UpsertSpeciesMoveLearnsetRequest(
    val speciesId: UUID,
    val moveId: UUID,
    val learnMethodId: UUID,
    @field:Min(1)
    val level: Int? = null,
    val sortingOrder: Int = 0,
    val enabled: Boolean = true,
)

/**
 * 将物种招式学习关系请求转换为领域草稿。
 *
 * @return 可直接交给应用服务写入的物种招式学习关系草稿。
 */
fun UpsertSpeciesMoveLearnsetRequest.toDraft(): SpeciesMoveLearnsetDraft =
    SpeciesMoveLearnsetDraft(
        speciesId = SpeciesId(speciesId),
        moveId = MoveId(moveId),
        learnMethodId = MoveLearnMethodId(learnMethodId),
        level = level,
        sortingOrder = sortingOrder,
        enabled = enabled,
    )

