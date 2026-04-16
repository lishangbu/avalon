package io.github.lishangbu.avalon.catalog.domain


/**
 * 招式异常状态的领域视图。
 *
 * 这类数据对应 `catalog.move_ailment`，用于表达招式可能附带的状态异常语义，
 * 例如麻痹、睡眠、中毒等，供 Catalog、战斗和管理端共同读取。
 *
 * @property id 招式异常状态标识。
 * @property code 招式异常状态业务码。
 * @property name 招式异常状态展示名称。
 * @property description 招式异常状态说明，可为空。
 * @property sortingOrder 列表展示顺序。
 * @property enabled 当前招式异常状态是否启用。
 * @property version 乐观锁版本号。
 */
data class MoveAilment(
    val id: MoveAilmentId,
    val code: String,
    val name: String,
    val description: String?,
    val sortingOrder: Int,
    val enabled: Boolean,
    val version: Long,
)

/**
 * 招式异常状态的轻量摘要。
 *
 * @property id 招式异常状态标识。
 * @property code 招式异常状态业务码。
 * @property name 招式异常状态展示名称。
 */
data class MoveAilmentSummary(
    val id: MoveAilmentId,
    val code: String,
    val name: String,
)

/**
 * 创建或更新招式异常状态时使用的输入草稿。
 *
 * @property code 招式异常状态业务码。
 * @property name 招式异常状态展示名称。
 * @property description 招式异常状态说明，可为空。
 * @property sortingOrder 列表展示顺序。
 * @property enabled 当前招式异常状态是否启用。
 */
data class MoveAilmentDraft(
    val code: String,
    val name: String,
    val description: String?,
    val sortingOrder: Int,
    val enabled: Boolean,
)