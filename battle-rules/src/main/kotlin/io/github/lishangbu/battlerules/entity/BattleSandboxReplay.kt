package io.github.lishangbu.battlerules.entity

import io.github.lishangbu.common.persistence.jimmer.CosIdLongUserIdGenerator
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.GeneratedValue
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Table
import java.time.Instant

/**
 * 战斗沙盒复盘记录。
 *
 * 这张表保存管理端沙盒请求和响应的最小 JSON 对。响应用于页面导入和排障查看，请求用于后续确定性重放校验；
 * 二者都不是规则资料源，也不会被拆进战斗规则三范式表，避免把临时排障材料误当成生产规则配置。
 */
@Entity
@Table(name = "battle_sandbox_replay")
interface BattleSandboxReplay {
	@Id
	@GeneratedValue(generatorType = CosIdLongUserIdGenerator::class)
	@JsonConverter(LongToStringConverter::class)
	val id: Long

	val title: String
	val formatCode: String
	val turnNumber: Int
	val resolved: Boolean
	val resultSummary: String?
	val requestJson: String?
	val responseJson: String
	val savedAt: Instant
}
