package io.github.lishangbu.avalon.dataset.entity

import io.github.lishangbu.avalon.jimmer.id.SnowflakeIdGenerator
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter
import org.babyfish.jimmer.sql.*

@Entity
interface Stat {
    /** ID */
    @Id
    @GeneratedValue(generatorType = SnowflakeIdGenerator::class)
    @JsonConverter(LongToStringConverter::class)
    val id: Long

    /** 内部名称 */
    val internalName: String?

    /** 名称 */
    val name: String?

    /** 游戏索引 */
    val gameIndex: Int?

    /** 是否仅战斗可用 */
    val battleOnly: Boolean?

    /** 是否只读 */
    val readonly: Boolean

    /** 招式伤害分类 */
    @ManyToOne
    val moveDamageClass: MoveDamageClass?
}
