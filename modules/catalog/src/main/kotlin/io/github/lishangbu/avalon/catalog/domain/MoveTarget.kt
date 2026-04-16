package io.github.lishangbu.avalon.catalog.domain


/**
 * 招式目标定义的领域视图。
 *
 * 这类数据用于表达一个招式可以指向谁，属于 Catalog 中的稳定参考数据。
 *
 * @property id 招式目标标识。
 * @property code 招式目标业务编码，供上下文和管理端引用。
 * @property name 招式目标展示名称。
 * @property description 招式目标说明，可为空。
 * @property sortingOrder 列表展示顺序。
 * @property enabled 当前招式目标是否启用。
 * @property version 乐观锁版本号。
 */
data class MoveTarget(
    val id: MoveTargetId,
    val code: String,
    val name: String,
    val description: String?,
    val sortingOrder: Int,
    val enabled: Boolean,
    val version: Long,
)

/**
 * 招式目标的轻量摘要。
 *
 * @property id 招式目标标识。
 * @property code 招式目标业务编码。
 * @property name 招式目标展示名称。
 */
data class MoveTargetSummary(
    val id: MoveTargetId,
    val code: String,
    val name: String,
)

/**
 * 创建或更新招式目标时使用的输入草稿。
 *
 * @property code 招式目标业务编码。
 * @property name 招式目标展示名称。
 * @property description 招式目标说明，可为空。
 * @property sortingOrder 列表展示顺序。
 * @property enabled 当前招式目标是否启用。
 */
data class MoveTargetDraft(
    val code: String,
    val name: String,
    val description: String?,
    val sortingOrder: Int,
    val enabled: Boolean,
)