package io.github.lishangbu.avalon.catalog.domain


/**
 * 招式学习方法的领域视图。
 *
 * 这类数据表达招式被学习的渠道，例如 `LEVEL-UP`、`EGG`、`TUTOR`、`MACHINE`。
 * learnset 关系会引用这里的定义，避免把方法语义散落在各处字符串里；
 * 其中 `LEVEL-UP` 会被当作“必须携带等级”的学习方法码。
 *
 * @property id 招式学习方法标识。
 * @property code 招式学习方法业务码。
 * @property name 招式学习方法展示名称。
 * @property description 招式学习方法说明，可为空。
 * @property sortingOrder 列表展示顺序。
 * @property enabled 当前招式学习方法是否启用。
 * @property version 乐观锁版本号。
 */
data class MoveLearnMethod(
    val id: MoveLearnMethodId,
    val code: String,
    val name: String,
    val description: String?,
    val sortingOrder: Int,
    val enabled: Boolean,
    val version: Long,
)

/**
 * 招式学习方法的轻量摘要。
 *
 * @property id 招式学习方法标识。
 * @property code 招式学习方法业务码。
 * @property name 招式学习方法展示名称。
 */
data class MoveLearnMethodSummary(
    val id: MoveLearnMethodId,
    val code: String,
    val name: String,
)

/**
 * 创建或更新招式学习方法时使用的输入草稿。
 *
 * @property code 招式学习方法业务码。
 * @property name 招式学习方法展示名称。
 * @property description 招式学习方法说明，可为空。
 * @property sortingOrder 列表展示顺序。
 * @property enabled 当前招式学习方法是否启用。
 */
data class MoveLearnMethodDraft(
    val code: String,
    val name: String,
    val description: String?,
    val sortingOrder: Int,
    val enabled: Boolean,
)