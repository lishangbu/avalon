package io.github.lishangbu.avalon.catalog.domain

/**
 * 物种特性槽位类型。
 *
 * 第一阶段先把最常见的主特性、副特性和隐藏特性槽位收进 Catalog，
 * 便于内容维护端和运行时消费方共享同一套槽位语义。
 */
enum class AbilitySlotCode {
    PRIMARY,
    SECONDARY,
    HIDDEN,
}