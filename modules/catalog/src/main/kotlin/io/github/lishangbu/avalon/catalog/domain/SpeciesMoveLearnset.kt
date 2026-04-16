package io.github.lishangbu.avalon.catalog.domain


/**
 * 物种招式学习关系的领域视图。
 *
 * 第一阶段先把“某个物种可以通过哪种方法学习哪个招式”收进 Catalog，
 * 方便 battle 与 progression 共享同一份参考事实。更细的世代差异、版本分组和自动补招式规则，
 * 后续再拆成更完整的 learnset 切片。当前先强约束 `LEVEL-UP` 类关系必须携带等级，
 * 其他学习方法则必须保持 `level` 为空。
 *
 * @property id 关系标识。
 * @property species 关联的物种摘要。
 * @property move 关联的招式摘要。
 * @property learnMethod 学习方法摘要。
 * @property level 关联学习方法所需的等级；当学习方法为 `LEVEL-UP` 时必须非空且大于 0，
 * 其他学习方法必须为空。
 * @property sortingOrder 同一物种下的展示顺序。
 * @property enabled 当前关联是否启用。
 * @property version 乐观锁版本号。
 */
data class SpeciesMoveLearnset(
    val id: SpeciesMoveLearnsetId,
    val species: SpeciesSummary,
    val move: MoveSummary,
    val learnMethod: MoveLearnMethodSummary,
    val level: Int?,
    val sortingOrder: Int,
    val enabled: Boolean,
    val version: Long,
)

/**
 * 创建或更新物种招式学习关系时使用的输入草稿。
 *
 * @property speciesId 物种定义标识。
 * @property moveId 招式定义标识。
 * @property learnMethodId 学习方法标识。
 * @property level 学习等级；当学习方法为 `LEVEL-UP` 时必须非空且大于 0，
 * 其他学习方法必须为空。
 * @property sortingOrder 同一物种下的展示顺序。
 * @property enabled 当前关联是否启用。
 */
data class SpeciesMoveLearnsetDraft(
    val speciesId: SpeciesId,
    val moveId: MoveId,
    val learnMethodId: MoveLearnMethodId,
    val level: Int?,
    val sortingOrder: Int,
    val enabled: Boolean,
)