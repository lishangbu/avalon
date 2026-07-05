package io.github.lishangbu.battlerules.service

import io.github.lishangbu.common.web.invalidValue
import java.util.Locale

private const val DEFAULT_INDIVIDUAL_VALUE = 31
private const val MIN_INDIVIDUAL_VALUE = 0
private const val MAX_INDIVIDUAL_VALUE = 31
private const val DEFAULT_EFFORT_VALUE = 0
private const val MIN_EFFORT_VALUE = 0
private const val MAX_EFFORT_VALUE = 252
private const val MAX_TOTAL_EFFORT_VALUE = 510

private val battleParticipantStatCodes = listOf(
	"hp",
	"attack",
	"defense",
	"special-attack",
	"special-defense",
	"speed",
)
private val battleParticipantStatCodeSet = battleParticipantStatCodes.toSet()
private val natureStatCodes = battleParticipantStatCodeSet - "hp"

/**
 * 战斗参与方能力配置。
 *
 * 该值对象把 HTTP 请求中松散的 map 字段规范化成战斗公式可以直接消费的六项能力配置：
 * - 个体值缺省为 31，合法范围为 0..31。
 * - 努力值缺省为 0，单项合法范围为 0..252，总和不能超过 510。
 * - 性格只允许影响五项非 HP 能力；中性性格使用两个空字段表达。
 *
 * 这里刻意不把个体值、努力值和性格塞进 battle-engine。纯引擎只需要已经计算好的最终能力快照；这些输入字段属于
 * battle-rules 的应用层装配责任。这样未来如果管理端换一种队伍编辑 DTO，或者数据库增加预设队伍表，战斗引擎的
 * 回合结算模型仍然不用改。
 */
data class BattleParticipantStatConfig(
	val individualValues: Map<String, Int>,
	val effortValues: Map<String, Int>,
	val natureIncreasedStat: String?,
	val natureDecreasedStat: String?,
) {
	/**
	 * 读取指定能力的有效个体值。
	 *
	 * [from] 会把所有缺省项都补齐，因此这里使用 `getValue`。如果未来有调用方绕过工厂方法直接构造非法对象，
	 * `getValue` 会立即暴露问题，避免悄悄按 0 继续计算出生产环境难以追踪的战斗数值。
	 */
	fun individualValue(statCode: String): Int =
		individualValues.getValue(statCode)

	/**
	 * 读取指定能力的有效努力值。
	 *
	 * 努力值在进入公式前已经按现代规则完成单项和总和校验。这里不再重复判断，是为了让能力公式保持纯计算逻辑，
	 * 也让所有 API 错误都稳定发生在 DTO 装配边界。
	 */
	fun effortValue(statCode: String): Int =
		effortValues.getValue(statCode)

	/**
	 * 将性格修正应用到非 HP 能力上。
	 *
	 * 现代主系列能力公式是在基础能力、个体值、努力值和等级折算出中性能力后，再对非 HP 能力做 1.1 或 0.9 的
	 * 向下取整修正。这里使用整数乘除而不是浮点数，避免 1.1 这类十进制在二进制浮点中的精度误差影响边界值。
	 */
	fun applyNature(statCode: String, neutralValue: Int): Int =
		when (statCode) {
			natureIncreasedStat -> neutralValue * 11 / 10
			natureDecreasedStat -> neutralValue * 9 / 10
			else -> neutralValue
		}

	companion object {
		val DEFAULT: BattleParticipantStatConfig =
			BattleParticipantStatConfig(
				individualValues = normalizeStatValues(
					fieldName = "individualValues",
					values = emptyMap(),
					min = MIN_INDIVIDUAL_VALUE,
					max = MAX_INDIVIDUAL_VALUE,
					defaultValue = DEFAULT_INDIVIDUAL_VALUE,
				),
				effortValues = normalizeEffortValues(emptyMap()),
				natureIncreasedStat = null,
				natureDecreasedStat = null,
			)

		/**
		 * 从请求字段构造规范化能力配置。
		 *
		 * 请求 map 允许只传真正有差异的能力；工厂方法会补齐缺省项，并把 code 统一成小写稳定形式。重复、空白、
		 * 未支持的能力 code 会在这里转成结构化 `ApiException`，保证准备校验、行动校验和沙盒结算得到一致响应。
		 */
		fun from(
			individualValues: Map<String, Int>,
			effortValues: Map<String, Int>,
			natureIncreasedStat: String?,
			natureDecreasedStat: String?,
		): BattleParticipantStatConfig {
			val normalizedNature = normalizeNature(natureIncreasedStat, natureDecreasedStat)
			return BattleParticipantStatConfig(
				individualValues = normalizeStatValues(
					fieldName = "individualValues",
					values = individualValues,
					min = MIN_INDIVIDUAL_VALUE,
					max = MAX_INDIVIDUAL_VALUE,
					defaultValue = DEFAULT_INDIVIDUAL_VALUE,
				),
				effortValues = normalizeEffortValues(effortValues),
				natureIncreasedStat = normalizedNature.first,
				natureDecreasedStat = normalizedNature.second,
			)
		}

		private fun normalizeEffortValues(values: Map<String, Int>): Map<String, Int> {
			val normalized = normalizeStatValues(
				fieldName = "effortValues",
				values = values,
				min = MIN_EFFORT_VALUE,
				max = MAX_EFFORT_VALUE,
				defaultValue = DEFAULT_EFFORT_VALUE,
			)
			val totalEffortValue = normalized.values.sum()
			if (totalEffortValue > MAX_TOTAL_EFFORT_VALUE) {
				invalidValue("effortValues", "effortValues 总和不能超过 $MAX_TOTAL_EFFORT_VALUE")
			}
			return normalized
		}

		private fun normalizeStatValues(
			fieldName: String,
			values: Map<String, Int>,
			min: Int,
			max: Int,
			defaultValue: Int,
		): Map<String, Int> {
			val suppliedValues = mutableMapOf<String, Int>()
			values.forEach { (rawCode, value) ->
				val code = rawCode.trim().lowercase(Locale.ROOT)
				if (code.isBlank()) {
					invalidValue(fieldName, "$fieldName 不能包含空能力 code")
				}
				if (code !in battleParticipantStatCodeSet) {
					invalidValue(fieldName, "$fieldName 包含不支持的能力 code: $rawCode")
				}
				if (suppliedValues.put(code, value) != null) {
					invalidValue(fieldName, "$fieldName 包含重复能力 code: $code")
				}
				if (value !in min..max) {
					invalidValue(fieldName, "$fieldName.$code 必须在 $min 到 $max 之间")
				}
			}
			return battleParticipantStatCodes.associateWith { suppliedValues[it] ?: defaultValue }
		}

		private fun normalizeNature(
			rawIncreasedStat: String?,
			rawDecreasedStat: String?,
		): Pair<String?, String?> {
			val increasedStat = rawIncreasedStat.normalizedNullableStatCode()
			val decreasedStat = rawDecreasedStat.normalizedNullableStatCode()
			if (increasedStat == null && decreasedStat == null) {
				return null to null
			}
			if (increasedStat == null || decreasedStat == null) {
				invalidValue(
					"natureIncreasedStat",
					"natureIncreasedStat 和 natureDecreasedStat 必须同时填写或同时留空",
				)
			}
			if (increasedStat !in natureStatCodes) {
				invalidValue("natureIncreasedStat", "natureIncreasedStat 只支持 attack、defense、special-attack、special-defense、speed")
			}
			if (decreasedStat !in natureStatCodes) {
				invalidValue("natureDecreasedStat", "natureDecreasedStat 只支持 attack、defense、special-attack、special-defense、speed")
			}
			if (increasedStat == decreasedStat) {
				invalidValue("natureIncreasedStat", "natureIncreasedStat 和 natureDecreasedStat 不能相同")
			}
			return increasedStat to decreasedStat
		}

		private fun String?.normalizedNullableStatCode(): String? =
			this?.trim()
				?.takeIf { it.isNotBlank() }
				?.lowercase(Locale.ROOT)
	}
}
