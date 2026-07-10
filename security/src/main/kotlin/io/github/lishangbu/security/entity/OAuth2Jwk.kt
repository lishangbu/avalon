package io.github.lishangbu.security.entity

import io.github.lishangbu.common.persistence.jimmer.CosIdLongUserIdGenerator
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.GeneratedValue
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Key
import org.babyfish.jimmer.sql.Table

/**
 * 授权服务器用于签发 JWT 的 JWK 持久化记录。
 *
 * 主键由 Jimmer 通过 CosId 生成，`keyId` 对应公开 JWK Set 中的 `kid`，用于 token
 * 验签方定位签名密钥。
 */
@Entity
@Table(name = "oauth2_jwk")
interface OAuth2Jwk {
	@Id
	@GeneratedValue(generatorType = CosIdLongUserIdGenerator::class)
	@JsonConverter(LongToStringConverter::class)
	val id: Long

	@Key
	val keyId: String

	val jwkJson: String
	val active: Boolean
}
