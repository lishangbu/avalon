package io.github.lishangbu.avalon.catalog.domain


/**
 * 招式分类的领域视图。
 *
 * 这类数据对应 `catalog.move_category`，用于表达招式在更细粒度层面的效果类别，
 * 例如伤害、异常状态、回复、强制换人等，属于 Catalog 里的稳定参考数据。
 *
 * @property id 招式分类标识。
 * @property code 招式分类业务码。
 * @property name 招式分类展示名称。
 * @property description 招式分类说明，可为空。
 * @property sortingOrder 列表展示顺序。
 * @property enabled 当前招式分类是否启用。
 * @property version 乐观锁版本号。
 */
data class MoveCategory(
    val id: MoveCategoryId,
    val code: String,
    val name: String,
    val description: String?,
    val sortingOrder: Int,
    val enabled: Boolean,
    val version: Long,
)

/**
 * 招式分类的轻量摘要。
 *
 * @property id 招式分类标识。
 * @property code 招式分类业务码。
 * @property name 招式分类展示名称。
 */
data class MoveCategorySummary(
    val id: MoveCategoryId,
    val code: String,
    val name: String,
)

/**
 * 创建或更新招式分类时使用的输入草稿。
 *
 * @property code 招式分类业务码。
 * @property name 招式分类展示名称。
 * @property description 招式分类说明，可为空。
 * @property sortingOrder 列表展示顺序。
 * @property enabled 当前招式分类是否启用。
 */
data class MoveCategoryDraft(
    val code: String,
    val name: String,
    val description: String?,
    val sortingOrder: Int,
    val enabled: Boolean,
)