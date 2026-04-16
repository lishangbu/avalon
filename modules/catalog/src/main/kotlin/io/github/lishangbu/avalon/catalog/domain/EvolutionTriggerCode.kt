package io.github.lishangbu.avalon.catalog.domain

/**
 * 物种进化触发方式。
 *
 * 第一阶段先收敛一组稳定触发码，用于表达大多数进化边定义。
 * 更细的特殊条件和脚本化规则，后续再在独立切片中补充。
 */
enum class EvolutionTriggerCode {
    LEVEL,
    ITEM,
    TRADE,
    FRIENDSHIP,
    OTHER,
}