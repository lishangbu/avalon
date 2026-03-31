package io.github.lishangbu.avalon.dataset.entity

import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.PropOverride

@Entity
interface CreatureStat {
    /** ID */
    @Id
    @PropOverride(prop = "creatureId", columnName = "pokemon_id")
    @PropOverride(prop = "statId", columnName = "stat_id")
    val id: CreatureStatId

    /** 基础能力值 */
    val baseStat: Int?

    /** 努力值 */
    val effort: Int?
}
