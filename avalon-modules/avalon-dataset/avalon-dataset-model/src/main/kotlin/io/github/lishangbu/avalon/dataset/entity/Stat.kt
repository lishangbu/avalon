package io.github.lishangbu.avalon.dataset.entity

import io.github.lishangbu.avalon.jimmer.id.SnowflakeIdGenerator
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter
import org.babyfish.jimmer.sql.*

@Entity
@Table(name = "stat")
interface Stat {
    /** ID */
    @Id
    @GeneratedValue(generatorType = SnowflakeIdGenerator::class)
    @JsonConverter(LongToStringConverter::class)
    val id: Long

    /** 内部名称 */
    @Column(name = "internal_name")
    val internalName: String?

    /** 名称 */
    @Column(name = "name")
    val name: String?

    /** 游戏索引 */
    @Column(name = "game_index")
    val gameIndex: Int?

    /** 是否仅战斗可用 */
    @Column(name = "is_battle_only")
    val isBattleOnly: Boolean?

    /** 招式伤害分类 */
    @ManyToOne
    @JoinColumn(name = "move_damage_class_id")
    val moveDamageClass: MoveDamageClass?

    /** 招式伤害分类 ID */
    @IdView("moveDamageClass")
    @JsonConverter(LongToStringConverter::class)
    val moveDamageClassId: Long?
}
