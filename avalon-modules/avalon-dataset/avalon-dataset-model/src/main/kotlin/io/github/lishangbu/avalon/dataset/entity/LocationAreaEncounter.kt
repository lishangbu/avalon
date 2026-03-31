package io.github.lishangbu.avalon.dataset.entity

import io.github.lishangbu.avalon.jimmer.id.SnowflakeIdGenerator
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.GeneratedValue
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.JoinColumn
import org.babyfish.jimmer.sql.ManyToOne

@Entity
interface LocationAreaEncounter {
    /** ID */
    @Id
    @GeneratedValue(generatorType = SnowflakeIdGenerator::class)
    @JsonConverter(LongToStringConverter::class)
    val id: Long

    /** 概率 */
    val chance: Int?

    /** 最大等级 */
    val maxLevel: Int?

    /** 最小等级 */
    val minLevel: Int?

    /** 遭遇方式 */
    @ManyToOne
    val encounterMethod: EncounterMethod?

    /** 地点区域 */
    @ManyToOne
    val locationArea: LocationArea?

    /** 生物 */
    @ManyToOne
    @JoinColumn(name = "creature_id")
    val creature: Creature?

    /** 最大概率 */
    val maxChance: Int?
}
