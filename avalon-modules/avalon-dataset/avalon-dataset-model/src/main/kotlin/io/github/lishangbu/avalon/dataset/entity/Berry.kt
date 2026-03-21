package io.github.lishangbu.avalon.dataset.entity

import io.github.lishangbu.avalon.jimmer.id.SnowflakeIdGenerator
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter
import org.babyfish.jimmer.sql.Column
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.GeneratedValue
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.IdView
import org.babyfish.jimmer.sql.JoinColumn
import org.babyfish.jimmer.sql.ManyToOne
import org.babyfish.jimmer.sql.Table

@Entity
@Table(name = "berry")
interface Berry {
    @Id
    @GeneratedValue(generatorType = SnowflakeIdGenerator::class)
    @JsonConverter(LongToStringConverter::class)
    val id: Long

    @Column(name = "internal_name")
    val internalName: String?

    @Column(name = "name")
    val name: String?

    @Column(name = "growth_time")
    val growthTime: Int?

    @Column(name = "max_harvest")
    val maxHarvest: Int?

    @Column(name = "bulk")
    val bulk: Int?

    @Column(name = "smoothness")
    val smoothness: Int?

    @Column(name = "soil_dryness")
    val soilDryness: Int?

    @ManyToOne
    @JoinColumn(name = "berry_firmness_id")
    val berryFirmness: BerryFirmness?

    @IdView("berryFirmness")
    @JsonConverter(LongToStringConverter::class)
    val berryFirmnessId: Long?

    @ManyToOne
    @JoinColumn(name = "natural_gift_type_id")
    val naturalGiftType: Type?

    @IdView("naturalGiftType")
    @JsonConverter(LongToStringConverter::class)
    val naturalGiftTypeId: Long?

    @Column(name = "natural_gift_power")
    val naturalGiftPower: Int?
}
