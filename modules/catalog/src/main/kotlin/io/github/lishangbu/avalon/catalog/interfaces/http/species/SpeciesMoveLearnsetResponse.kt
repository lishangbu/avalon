package io.github.lishangbu.avalon.catalog.interfaces.http.species

import io.github.lishangbu.avalon.catalog.domain.SpeciesMoveLearnset
import io.github.lishangbu.avalon.catalog.interfaces.http.move.MoveLearnMethodSummaryResponse
import io.github.lishangbu.avalon.catalog.interfaces.http.move.MoveSummaryResponse
import io.github.lishangbu.avalon.catalog.interfaces.http.move.toResponse
import java.util.UUID

/**
 * 物种招式学习关系响应。
 *
 * @property id 关系主键。
 * @property species 关联的物种摘要。
 * @property move 关联的招式摘要。
 * @property learnMethod 关联的学习方法摘要。
 * @property level 学习等级；对应 `LEVEL-UP` 时会返回非空等级，其他学习方法返回空值。
 * @property sortingOrder 同一物种下的展示顺序。
 * @property enabled 当前关联是否启用。
 * @property version 乐观锁版本号。
 */
data class SpeciesMoveLearnsetResponse(
    val id: UUID,
    val species: SpeciesSummaryResponse,
    val move: MoveSummaryResponse,
    val learnMethod: MoveLearnMethodSummaryResponse,
    val level: Int?,
    val sortingOrder: Int,
    val enabled: Boolean,
    val version: Long,
)

/**
 * 将物种招式学习关系领域对象转换为接口响应。
 *
 * @return 可直接返回给调用方的物种招式学习关系明细。
 */
fun SpeciesMoveLearnset.toResponse(): SpeciesMoveLearnsetResponse =
    SpeciesMoveLearnsetResponse(
        id = id.value,
        species = species.toResponse(),
        move = move.toResponse(),
        learnMethod = learnMethod.toResponse(),
        level = level,
        sortingOrder = sortingOrder,
        enabled = enabled,
        version = version,
    )

