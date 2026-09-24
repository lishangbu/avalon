package io.github.lishangbu.avalon.inventory.domain;

import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;
import io.github.lishangbu.avalon.content.item.EnhancementCatalog;
import io.github.lishangbu.avalon.platform.BusinessException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** 纯计算规则：调用方提供原始属性和一次随机点数，规则不访问数据库、不重复抽签。 */
public final class EnhancementRules {

	private EnhancementRules() {
	}

	private static final Set<String> STATS = Set.of("health", "mana", "physicalAttack", "magicAttack",
			"physicalDefense", "magicDefense", "speed");

	public static Quote quote(EnhancementCatalog.Definition rules, int equipmentLevel, String quality, int current) {
		if (current < 0 || current >= rules.maximumLevel())
			throw new BusinessException(409, "装备已达强化上限或等级无效");
		var fee = rules.fees()
			.stream()
			.filter(band -> band.minimum() <= equipmentLevel && equipmentLevel <= band.maximum())
			.findFirst()
			.orElseThrow(() -> new BusinessException(409, "该装备等级尚未配置强化费用"));
		var stone = rules.stones()
			.stream()
			.filter(band -> band.minimum() <= equipmentLevel && equipmentLevel <= band.maximum())
			.findFirst()
			.orElseThrow(() -> new BusinessException(409, "该装备等级尚未配置强化材料"));
		var multiplier = rules.qualityMultipliers().get(quality);
		if (multiplier == null)
			throw new BusinessException(409, "装备品质尚未配置强化费用");
		int target = current + 1;
		try {
			long gold = BigDecimal.valueOf(fee.gold())
				.multiply(multiplier)
				.multiply(BigDecimal.valueOf(target))
				.setScale(0, RoundingMode.CEILING)
				.longValueExact();
			return new Quote(current, target, Math.max(0, current - 1), rules.successPercent().get(current),
					stone.item(), target, gold);
		}
		catch (ArithmeticException error) {
			throw new BusinessException(409, "强化费用超出支持范围");
		}
	}

	/** 点数严格为0～99，概率边界不使用浮点比较；没有保底或历史次数输入。 */
	public static int outcome(Quote quote, int roll) {
		if (roll < 0 || roll >= 100)
			throw new IllegalArgumentException("强化随机点数无效");
		return roll < quote.successPercent() ? quote.target() : quote.failureLevel();
	}

	/** 永远从原始属性计算一次；非基础战斗字段原样保留，不放大护盾、技能或触发效果。 */
	public static Map<String, Integer> stats(Map<String, Integer> original, int level,
			EnhancementCatalog.Definition rules) {
		if (level < 0 || level > rules.maximumLevel())
			throw new IllegalArgumentException("强化等级无效");
		var result = new LinkedHashMap<String, Integer>();
		long percent = Math.addExact(100L, Math.multiplyExact((long) level, rules.statPercentPerLevel()));
		original.forEach((key, value) -> {
			if (value < 0)
				throw new IllegalArgumentException("装备基础属性不能为负数");
			result.put(key,
					STATS.contains(key) ? Math.toIntExact(Math.multiplyExact((long) value, percent) / 100) : value);
		});
		return Map.copyOf(result);
	}

	public record Quote(int current, int target, int failureLevel, int successPercent, String stone, int quantity,
			@JsonSerialize(using = ToStringSerializer.class) long gold) {
	}

}
