package io.github.lishangbu.battlerules.dto

import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

/**
 * 战斗沙盒复盘列表响应。
 */
@Schema(description = "战斗沙盒复盘列表响应。")
@Immutable
interface BattleSandboxReplaySummaryResponse {
	@get:Schema(type = "string", description = "复盘记录 ID。", example = "865732440461672401")
	@JsonConverter(LongToStringConverter::class)
	val id: Long
	@get:Schema(description = "复盘标题。")
	val title: String
	@get:Schema(description = "赛制稳定 code。", example = "standard-single")
	val formatCode: String
	@get:Schema(description = "保存时的最新回合序号。", example = "3")
	val turnNumber: Int
	@get:Schema(description = "保存时该回合是否完成结算。", example = "true")
	val resolved: Boolean
	@get:Schema(description = "战斗结果摘要；未结束时为空。", nullable = true)
	val resultSummary: String?
	@get:Schema(description = "保存时间。")
	val savedAt: Instant
}
