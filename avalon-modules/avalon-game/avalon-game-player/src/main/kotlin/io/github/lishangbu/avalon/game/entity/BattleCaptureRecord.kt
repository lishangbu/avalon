package io.github.lishangbu.avalon.game.entity

import io.github.lishangbu.avalon.jimmer.id.SnowflakeIdGenerator
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.GeneratedValue
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Table
import java.math.BigDecimal
import java.time.Instant

@Entity
@Table(name = "battle_capture_record")
interface BattleCaptureRecord {
    @Id
    @GeneratedValue(generatorType = SnowflakeIdGenerator::class)
    @JsonConverter(LongToStringConverter::class)
    val id: Long

    val sessionId: String

    val playerId: Long

    val targetUnitId: String

    val ballItemId: Long

    val creatureId: Long

    val creatureSpeciesId: Long

    val captureRate: Int

    val currentHp: Int

    val maxHp: Int

    val statusId: String?

    val shakes: Int

    val success: Boolean

    val reason: String

    val finalRate: BigDecimal

    val ownedCreatureId: Long?

    val createdAt: Instant
}
