package io.github.lishangbu.avalon.dataset.entity

import io.github.lishangbu.avalon.jimmer.id.SnowflakeIdGenerator
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter
import org.babyfish.jimmer.sql.Column
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.GeneratedValue
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.IdView
import org.babyfish.jimmer.sql.JoinColumn
import org.babyfish.jimmer.sql.ManyToOne
import org.babyfish.jimmer.sql.Table

@Entity
@Table(name = "stat")
interface Stat {
    @Id
    @GeneratedValue(generatorType = SnowflakeIdGenerator::class)
    @JsonConverter(LongToStringConverter::class)
    val id: Long

    @Column(name = "internal_name")
    val internalName: String?

    @Column(name = "name")
    val name: String?

    @Column(name = "game_index")
    val gameIndex: Int?

    @Column(name = "is_battle_only")
    val isBattleOnly: Boolean?

    @ManyToOne
    @JoinColumn(name = "move_damage_class_id")
    val moveDamageClass: MoveDamageClass?

    @IdView("moveDamageClass")
    @JsonConverter(LongToStringConverter::class)
    val moveDamageClassId: Long?
}
