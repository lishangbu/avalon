package io.github.lishangbu.avalon.authorization.entity

import io.github.lishangbu.avalon.jimmer.id.SnowflakeIdGenerator
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.GeneratedValue
import org.babyfish.jimmer.sql.Id
import java.time.Instant

@Entity
interface AuthenticationLog {
    /** ID */
    @Id
    @GeneratedValue(generatorType = SnowflakeIdGenerator::class)
    @JsonConverter(LongToStringConverter::class)
    val id: Long

    /** 用户名 */
    val username: String?

    /** 客户端 ID */
    val clientId: String?

    /** 授权属性 */
    val grantType: String?

    /** IP */
    val ip: String?

    /** 用户代理 */
    val userAgent: String?

    /** 成功 */
    val success: Boolean?

    /** 错误信息 */
    val errorMessage: String?

    /** 发生时间 */
    val occurredAt: Instant?
}
