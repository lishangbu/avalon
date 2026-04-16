package io.github.lishangbu.avalon.catalog.domain

/**
 * 技能分类码。
 *
 * 当前只保留 battle 规则里最稳定的三类分类，
 * 避免把更细的运行时效果标签提前固化进 Catalog 写模型。
 */
enum class MoveCategoryCode {
    PHYSICAL,
    SPECIAL,
    STATUS,
}