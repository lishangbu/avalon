package io.github.lishangbu.security.entity

import io.github.lishangbu.common.persistence.jimmer.CosIdLongUserIdGenerator
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.GeneratedValue
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Key
import org.babyfish.jimmer.sql.Table
import java.time.Instant

/** Sa-Token 运行状态，保存 token 映射、登录会话和框架内部索引。 */
@Entity
@Table(name = "security_token_state")
interface SecurityTokenState {
	@Id
	@GeneratedValue(generatorType = CosIdLongUserIdGenerator::class)
	@JsonConverter(LongToStringConverter::class)
	val id: Long

	@Key
	val stateKey: String

	val stateValue: String

	val expiresAt: Instant?
}