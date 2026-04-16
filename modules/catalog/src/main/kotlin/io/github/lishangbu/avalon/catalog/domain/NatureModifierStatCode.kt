package io.github.lishangbu.avalon.catalog.domain

/**
 * 性格可调整的数值类型。
 *
 * 当前只保留会被性格正负修正影响的五项数值，不把 `HP` 放进这个集合，
 * 避免调用方误以为性格可以直接修改生命值。
 */
enum class NatureModifierStatCode {
    ATTACK,
    DEFENSE,
    SPECIAL_ATTACK,
    SPECIAL_DEFENSE,
    SPEED,
}