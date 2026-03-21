package io.github.lishangbu.avalon.dataset.entity

import io.github.lishangbu.avalon.jimmer.id.SnowflakeIdGenerator
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter
import org.babyfish.jimmer.sql.*

@Entity
@Table(name = "item")
interface Item {
    @Id
    @GeneratedValue(generatorType = SnowflakeIdGenerator::class)
    @JsonConverter(LongToStringConverter::class)
    val id: Long

    val internalName: String?

    val name: String?

    val cost: Int?

    val flingPower: Int?

    @ManyToOne
    @JoinColumn(name = "item_fling_effect_id")
    val itemFlingEffect: ItemFlingEffect?

    @ManyToMany
    @JoinTable(
        name = "item_attribute_relation",
        joinColumnName = "item_id",
        inverseJoinColumnName = "item_attribute_id",
    )
    val itemAttributes: List<ItemAttribute>

    val shortEffect: String?

    val effect: String?

    val text: String?
}
