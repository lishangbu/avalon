package io.github.lishangbu.avalon.dataset.entity

import io.github.lishangbu.avalon.jimmer.id.SnowflakeIdGenerator
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter
import org.babyfish.jimmer.sql.*

@Entity
interface Nature {
    /** ID */
    @Id
    @GeneratedValue(generatorType = SnowflakeIdGenerator::class)
    @JsonConverter(LongToStringConverter::class)
    val id: Long

    /** 内部名称 */
    val internalName: String?

    /** 名称 */
    val name: String?

    /** 降低能力值 */
    @ManyToOne
    val decreasedStat: Stat?

    /** 提高能力值 */
    @ManyToOne
    val increasedStat: Stat?

    /** 讨厌风味 */
    @ManyToOne
    val hatesBerryFlavor: BerryFlavor?

    /** 喜欢风味 */
    @ManyToOne
    val likesBerryFlavor: BerryFlavor?
}
