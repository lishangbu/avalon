package io.github.lishangbu.avalon.dataset.entity

import org.babyfish.jimmer.sql.*

@Entity
interface TypeEffectivenessEntry {
    /** ID */
    @Id
    @PropOverride(prop = "attackingTypeId", columnName = "attacking_type_id")
    @PropOverride(prop = "defendingTypeId", columnName = "defending_type_id")
    val id: TypeEffectivenessEntryId

    /**
     * 定点倍率百分比。
     *
     * 存储层统一使用放大 100 倍后的整数来避免浮点误差：
     * - 100 表示 1.00x
     * - 50 表示 0.50x
     * - 200 表示 2.00x
     */
    val multiplierPercent: Int?
}
