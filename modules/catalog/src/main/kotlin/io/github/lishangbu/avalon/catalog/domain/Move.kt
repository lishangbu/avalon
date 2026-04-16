package io.github.lishangbu.avalon.catalog.domain


/**
 * 招式定义的领域视图。
 *
 * 第一阶段只维护对多个上下文都稳定成立的招式参考数据，例如属性、分类、
 * 目标、异常状态和管理端展示文本，后续更细的战斗逻辑仍由消费方解释。
 *
 * @property id 招式标识。
 * @property code 招式业务码，供上下文和管理端引用。
 * @property name 招式展示名称。
 * @property type 招式所属属性摘要。
 * @property categoryCode 招式伤害类别码。
 * @property moveCategory 招式更细粒度分类摘要，可为空。
 * @property moveAilment 招式异常状态摘要，可为空。
 * @property moveTarget 招式目标摘要，可为空。
 * @property description 招式说明，可为空。
 * @property effectChance 追加效果概率，可为空。
 * @property power 招式威力，可为空。
 * @property accuracy 招式命中率，可为空。
 * @property powerPoints 招式基础 PP。
 * @property priority 招式优先级。
 * @property text 招式文本，可为空。
 * @property shortEffect 招式短效果说明，可为空。
 * @property effect 招式效果说明，可为空。
 * @property sortingOrder 列表展示顺序。
 * @property enabled 当前招式是否启用。
 * @property version 乐观锁版本号。
 */
data class Move(
    val id: MoveId,
    val code: String,
    val name: String,
    val type: TypeDefinitionSummary,
    val categoryCode: MoveCategoryCode,
    val moveCategory: MoveCategorySummary?,
    val moveAilment: MoveAilmentSummary?,
    val moveTarget: MoveTargetSummary?,
    val description: String?,
    val effectChance: Int?,
    val power: Int?,
    val accuracy: Int?,
    val powerPoints: Int,
    val priority: Int,
    val text: String?,
    val shortEffect: String?,
    val effect: String?,
    val sortingOrder: Int,
    val enabled: Boolean,
    val version: Long,
)

/**
 * 招式的轻量摘要。
 *
 * 该模型主要出现在 learnset 等跨概念关联里，避免每次都展开完整招式明细。
 *
 * @property id 招式标识。
 * @property code 招式业务码。
 * @property name 招式展示名称。
 */
data class MoveSummary(
    val id: MoveId,
    val code: String,
    val name: String,
)

/**
 * 创建或更新招式时使用的输入草稿。
 *
 * @property code 招式业务码。
 * @property name 招式展示名称。
 * @property typeDefinitionId 招式属性主键。
 * @property categoryCode 招式伤害类别码。
 * @property moveCategoryId 招式分类主键，可为空。
 * @property moveAilmentId 招式异常状态主键，可为空。
 * @property moveTargetId 招式目标主键，可为空。
 * @property description 招式说明，可为空。
 * @property effectChance 追加效果概率，可为空。
 * @property power 招式威力，可为空。
 * @property accuracy 招式命中率，可为空。
 * @property powerPoints 招式基础 PP。
 * @property priority 招式优先级。
 * @property text 招式文本，可为空。
 * @property shortEffect 招式短效果说明，可为空。
 * @property effect 招式效果说明，可为空。
 * @property sortingOrder 列表展示顺序。
 * @property enabled 当前招式是否启用。
 */
data class MoveDraft(
    val code: String,
    val name: String,
    val typeDefinitionId: TypeDefinitionId,
    val categoryCode: MoveCategoryCode,
    val moveCategoryId: MoveCategoryId?,
    val moveAilmentId: MoveAilmentId?,
    val moveTargetId: MoveTargetId?,
    val description: String?,
    val effectChance: Int?,
    val power: Int?,
    val accuracy: Int?,
    val powerPoints: Int,
    val priority: Int,
    val text: String?,
    val shortEffect: String?,
    val effect: String?,
    val sortingOrder: Int,
    val enabled: Boolean,
)