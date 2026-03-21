package io.github.lishangbu.avalon.dataset.entity

import io.github.lishangbu.avalon.jimmer.id.SnowflakeIdGenerator
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter
import org.babyfish.jimmer.sql.*

@Entity
@Table(name = "move_learn_method")
interface MoveLearnMethod {
    @Id
    @GeneratedValue(generatorType = SnowflakeIdGenerator::class)
    @JsonConverter(LongToStringConverter::class)
    val id: Long

    val internalName: String?

    val name: String?

    val description: String?
}
