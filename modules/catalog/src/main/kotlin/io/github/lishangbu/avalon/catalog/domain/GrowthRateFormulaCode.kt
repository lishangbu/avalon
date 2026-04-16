package io.github.lishangbu.avalon.catalog.domain

/**
 * 成长率经验公式码。
 *
 * 当前先收敛为一组稳定公式类型，由消费方在本域内把公式码翻译成实际的
 * 经验值计算器，不在 Catalog 中提前内嵌计算实现。
 */
enum class GrowthRateFormulaCode {
    FAST,
    MEDIUM_FAST,
    MEDIUM_SLOW,
    SLOW,
    ERRATIC,
    FLUCTUATING,
}