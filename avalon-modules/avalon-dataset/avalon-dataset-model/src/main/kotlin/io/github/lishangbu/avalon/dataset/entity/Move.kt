package io.github.lishangbu.avalon.dataset.entity

import io.github.lishangbu.avalon.jimmer.id.SnowflakeIdGenerator
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter
import org.babyfish.jimmer.sql.*

@Entity
@Table(name = "move")
interface Move {
    @Id
    @GeneratedValue(generatorType = SnowflakeIdGenerator::class)
    @JsonConverter(LongToStringConverter::class)
    val id: Long

    val internalName: String?

    val name: String?

    @ManyToOne
    @JoinColumn(name = "type_id")
    val type: Type?

    val accuracy: Int?

    val effectChance: Int?

    val pp: Int?

    val priority: Int?

    val power: Int?

    @ManyToOne
    @JoinColumn(name = "move_damage_class_id")
    val moveDamageClass: MoveDamageClass?

    @ManyToOne
    @JoinColumn(name = "move_target_id")
    val moveTarget: MoveTarget?

    val text: String?

    val shortEffect: String?

    val effect: String?

    @ManyToOne
    @JoinColumn(name = "move_category_id")
    val moveCategory: MoveCategory?

    @ManyToOne
    @JoinColumn(name = "move_ailment_id")
    val moveAilment: MoveAilment?

    val minHits: Int?

    val maxHits: Int?

    val minTurns: Int?

    val maxTurns: Int?

    val drain: Int?

    val healing: Int?

    val critRate: Int?

    val ailmentChance: Int?

    val flinchChance: Int?

    val statChance: Int?
}
