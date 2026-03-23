package io.github.lishangbu.avalon.dataset.entity

import io.github.lishangbu.avalon.jimmer.id.SnowflakeIdGenerator
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter
import org.babyfish.jimmer.sql.*

@Entity
@Table(name = "move_damage_class")
interface MoveDamageClass {
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

    /** 描述 */
    @Column(name = "description")
    val description: String?
}
