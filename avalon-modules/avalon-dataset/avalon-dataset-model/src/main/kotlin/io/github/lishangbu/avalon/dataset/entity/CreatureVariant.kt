package io.github.lishangbu.avalon.dataset.entity

import io.github.lishangbu.avalon.jimmer.id.SnowflakeIdGenerator
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.GeneratedValue
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.JoinColumn
import org.babyfish.jimmer.sql.ManyToOne
import org.babyfish.jimmer.sql.Table

@Entity
@Table(name = "creature_variant")
interface CreatureVariant {
    /** ID */
    @Id
    @GeneratedValue(generatorType = SnowflakeIdGenerator::class)
    @JsonConverter(LongToStringConverter::class)
    val id: Long

    /** 背面默认图片 */
    val backDefault: String?

    /** 背面雌性图片 */
    val backFemale: String?

    /** 背面闪光图片 */
    val backShiny: String?

    /** 背面闪光雌性图片 */
    val backShinyFemale: String?

    /** 是否仅战斗形态 */
    val battleOnly: Boolean?

    /** 是否默认形态 */
    val defaultForm: Boolean?

    /** 形态名称 */
    val formName: String?

    /** 形态顺序 */
    val formOrder: Int?

    /** 正面默认图片 */
    val frontDefault: String?

    /** 正面雌性图片 */
    val frontFemale: String?

    /** 正面闪光图片 */
    val frontShiny: String?

    /** 正面闪光雌性图片 */
    val frontShinyFemale: String?

    /** 内部名称 */
    val internalName: String?

    /** 是否超级进化形态 */
    val mega: Boolean?

    /** 名称 */
    val name: String?

    /** 生物 */
    @ManyToOne
    @JoinColumn(name = "creature_id")
    val creature: Creature?

    /** 排序顺序 */
    val sortingOrder: Int?
}
