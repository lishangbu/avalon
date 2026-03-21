package io.github.lishangbu.avalon.authorization.entity

import io.github.lishangbu.avalon.jimmer.id.SnowflakeIdGenerator
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.GeneratedValue
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Table
import java.time.Instant

@Entity
@Table(name = "authentication_log")
interface AuthenticationLog {
    @Id
    @GeneratedValue(generatorType = SnowflakeIdGenerator::class)
    @JsonConverter(LongToStringConverter::class)
    val id: Long

    val username: String?

    val clientId: String?

    val grantType: String?

    val ip: String?

    val userAgent: String?

    val success: Boolean?

    val errorMessage: String?

    val occurredAt: Instant?
}
