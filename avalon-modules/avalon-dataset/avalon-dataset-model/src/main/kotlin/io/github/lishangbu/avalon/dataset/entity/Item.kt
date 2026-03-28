package io.github.lishangbu.avalon.dataset.entity

import io.github.lishangbu.avalon.jimmer.id.SnowflakeIdGenerator
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter
import org.babyfish.jimmer.sql.*

@Entity
interface Item {
    /** ID */
    @Id
    @GeneratedValue(generatorType = SnowflakeIdGenerator::class)
    @JsonConverter(LongToStringConverter::class)
    val id: Long

    /** 内部名称 */
    val internalName: String?

    /** 名称 */
    val name: String?

    /** 价格 */
    val cost: Int?

    /** 投掷威力 */
    val flingPower: Int?

    /** 道具投掷效果 */
    @ManyToOne
    val itemFlingEffect: ItemFlingEffect?

    /** 道具属性映射 */
    @ManyToMany
    @JoinTable(
        name = "item_attribute_relation",
        joinColumnName = "item_id",
        inverseJoinColumnName = "item_attribute_id",
    )
    val itemAttributes: List<ItemAttribute>

    /** 简称效果 */
    val shortEffect: String?

    /** 效果 */
    val effect: String?

    /** 文本 */
    val text: String?
}
