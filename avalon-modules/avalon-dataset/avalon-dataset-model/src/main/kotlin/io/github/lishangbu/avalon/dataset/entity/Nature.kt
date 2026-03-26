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
    @JoinColumn(name = "decreased_stat_id")
    val decreasedStat: Stat?

    /** 提高能力值 */
    @ManyToOne
    @JoinColumn(name = "increased_stat_id")
    val increasedStat: Stat?

    /** 讨厌风味 */
    @ManyToOne
    @JoinColumn(name = "hates_berry_flavor_id")
    val hatesFlavor: BerryFlavor?

    /** 喜欢风味 */
    @ManyToOne
    @JoinColumn(name = "likes_berry_flavor_id")
    val likesFlavor: BerryFlavor?
}
