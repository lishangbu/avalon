package io.github.lishangbu.avalon.dataset.entity

import io.github.lishangbu.avalon.jimmer.id.SnowflakeIdGenerator
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter
import org.babyfish.jimmer.sql.*

@Entity
interface Move {
    /** ID */
    @Id
    @GeneratedValue(generatorType = SnowflakeIdGenerator::class)
    @JsonConverter(LongToStringConverter::class)
    val id: Long

    /** 内部名称 */
    val internalName: String?

    /** 名称 */
    val name: String?

    /** 属性 */
    @ManyToOne
    @JoinColumn(name = "type_id")
    val type: Type?

    /** 命中率 */
    val accuracy: Int?

    /** 效果触发概率 */
    val effectChance: Int?

    /** PP */
    val pp: Int?

    /** 优先级 */
    val priority: Int?

    /** 威力 */
    val power: Int?

    /** 招式伤害分类 */
    @ManyToOne
    @JoinColumn(name = "move_damage_class_id")
    val moveDamageClass: MoveDamageClass?

    /** 招式目标 */
    @ManyToOne
    @JoinColumn(name = "move_target_id")
    val moveTarget: MoveTarget?

    /** 文本 */
    val text: String?

    /** 简称效果 */
    val shortEffect: String?

    /** 效果 */
    val effect: String?

    /** 招式分类 */
    @ManyToOne
    @JoinColumn(name = "move_category_id")
    val moveCategory: MoveCategory?

    /** 招式异常状态 */
    @ManyToOne
    @JoinColumn(name = "move_ailment_id")
    val moveAilment: MoveAilment?

    /** 最小命中次数 */
    val minHits: Int?

    /** 最大命中次数 */
    val maxHits: Int?

    /** 最少回合数 */
    val minTurns: Int?

    /** 最多回合数 */
    val maxTurns: Int?

    /** 吸收 */
    val drain: Int?

    /** 治疗 */
    val healing: Int?

    /** 暴击速率 */
    val critRate: Int?

    /** 异常状态概率 */
    val ailmentChance: Int?

    /** 畏缩概率 */
    val flinchChance: Int?

    /** 能力值概率 */
    val statChance: Int?
}
