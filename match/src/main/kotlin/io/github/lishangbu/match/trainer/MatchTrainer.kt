package io.github.lishangbu.match.trainer

import io.github.lishangbu.common.persistence.jimmer.CosIdLongUserIdGenerator
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.GeneratedValue
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Table
import org.babyfish.jimmer.sql.Version
import java.time.Instant

/**
 * Trainer 聚合根的持久化实体，对应 `match_trainer`。
 *
 * accountId 是 OAuth 账户所有权边界；displayNameKey 用于全局唯一比较；revision 参与条件写入，
 * archivedAt 非空表示逻辑归档，名称、Team 和历史仍永久保留。
 */
@Entity
@Table(name = "match_trainer")
interface MatchTrainer {
	@Id
	@GeneratedValue(generatorType = CosIdLongUserIdGenerator::class)
	@JsonConverter(LongToStringConverter::class)
	val id: Long
	/** 所属安全账户 ID。 */
	val accountId: Long
	/** 经过 NFKC 规范化后向玩家展示的不可变名称。 */
	val displayName: String
	/** 规范化并小写后的全局唯一名称键。 */
	val displayNameKey: String
	/** 创建命令的 UUID 字符串，用于账户内幂等。 */
	val commandId: String
	@Version
	/** 条件写入的乐观锁版本，同时作为 REST expectedRevision。 */
	val revision: Long
	/** 逻辑归档时间；为空表示有效 Trainer。 */
	val archivedAt: Instant?
	val createdAt: Instant
	val updatedAt: Instant
}
