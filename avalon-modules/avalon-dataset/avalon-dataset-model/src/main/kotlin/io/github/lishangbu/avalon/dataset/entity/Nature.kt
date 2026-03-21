package io.github.lishangbu.avalon.dataset.entity

import io.github.lishangbu.avalon.jimmer.id.SnowflakeIdGenerator
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter
import org.babyfish.jimmer.sql.*

@Entity
@Table(name = "nature")
interface Nature {
    @Id
    @GeneratedValue(generatorType = SnowflakeIdGenerator::class)
    @JsonConverter(LongToStringConverter::class)
    val id: Long

    val internalName: String?

    val name: String?

    @ManyToOne
    @JoinColumn(name = "decreased_stat_id")
    val decreasedStat: Stat?

    @ManyToOne
    @JoinColumn(name = "increased_stat_id")
    val increasedStat: Stat?

    @ManyToOne
    @JoinColumn(name = "hates_berry_flavor_id")
    val hatesFlavor: BerryFlavor?

    @ManyToOne
    @JoinColumn(name = "likes_berry_flavor_id")
    val likesFlavor: BerryFlavor?
}
