package io.github.lishangbu.avalon.dataset.entity

import io.github.lishangbu.avalon.jimmer.id.SnowflakeIdGenerator
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter
import org.babyfish.jimmer.sql.*

@Entity
@Table(name = "berry")
interface Berry {
    /** ID */
    @Id
    @GeneratedValue(generatorType = SnowflakeIdGenerator::class)
    @JsonConverter(LongToStringConverter::class)
    val id: Long

    /** 内部名称 */
    val internalName: String?

    /** 名称 */
    val name: String?

    /** 成长时间 */
    val growthTime: Int?

    /** 最大收获量 */
    val maxHarvest: Int?

    /** 体积 */
    val bulk: Int?

    /** 顺滑度 */
    val smoothness: Int?

    /** 土壤干燥度 */
    val soilDryness: Int?

    /** 树果硬度 */
    @ManyToOne
    @JoinColumn(name = "berry_firmness_id")
    val berryFirmness: BerryFirmness?

    /** 树果硬度 ID */
    @IdView("berryFirmness")
    @JsonConverter(LongToStringConverter::class)
    val berryFirmnessId: Long?

    /** 自然礼物属性 */
    @ManyToOne
    @JoinColumn(name = "natural_gift_type_id")
    val naturalGiftType: Type?

    /** 自然礼物属性 ID */
    @IdView("naturalGiftType")
    @JsonConverter(LongToStringConverter::class)
    val naturalGiftTypeId: Long?

    /** 自然礼物威力 */
    val naturalGiftPower: Int?
}
