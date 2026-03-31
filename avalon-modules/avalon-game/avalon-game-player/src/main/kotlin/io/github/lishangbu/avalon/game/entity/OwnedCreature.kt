package io.github.lishangbu.avalon.game.entity

import io.github.lishangbu.avalon.jimmer.id.SnowflakeIdGenerator
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.GeneratedValue
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Table
import java.time.Instant

@Entity
@Table(name = "owned_creature")
interface OwnedCreature {
    @Id
    @GeneratedValue(generatorType = SnowflakeIdGenerator::class)
    @JsonConverter(LongToStringConverter::class)
    val id: Long

    val playerId: Long

    val creatureId: Long

    val creatureSpeciesId: Long

    val nickname: String?

    val level: Int

    val experience: Int

    val natureId: Long?

    val abilityInternalName: String?

    val currentHp: Int

    val maxHp: Int

    val statusId: String?

    val storageType: String

    val storageBoxId: Long?

    val storageSlot: Int?

    val partySlot: Int?

    val capturedAt: Instant?

    val captureItemId: Long?

    val captureSessionId: String?

    val sourceType: String

    val createdAt: Instant

    val updatedAt: Instant
}
