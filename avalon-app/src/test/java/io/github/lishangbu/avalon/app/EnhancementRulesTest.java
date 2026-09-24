package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.content.item.EnhancementCatalog;
import io.github.lishangbu.avalon.inventory.domain.EnhancementRules;
import io.github.lishangbu.avalon.platform.BusinessException;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import java.util.Map;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/** 使用真实JSON验证每个概率边界与装备档位，非蒙特卡洛抽样，不将期望当作必达保证。 */
@Tag("rules")
class EnhancementRulesTest {

	private final RuleTestFixture fixture = new RuleTestFixture();

	private final EnhancementCatalog catalog = fixture.enhancement;

	@Test
	void allSevenTargetsRespectProbabilityLossAndFullMaterialCost() {
		for (int current = 0; current < 7; current++) {
			var quote = EnhancementRules.quote(catalog.definition(), 25, "精良", current);
			assertEquals(90 - current * 10, quote.successPercent());
			assertEquals(current + 1, quote.quantity());
			assertEquals(60L * (current + 1), quote.gold());
			int successes = 0;
			for (int roll = 0; roll < 100; roll++) {
				int level = EnhancementRules.outcome(quote, roll);
				if (level == current + 1)
					successes++;
				else
					assertEquals(Math.max(0, current - 1), level);
			}
			assertEquals(quote.successPercent(), successes);
		}
		assertThrows(BusinessException.class, () -> EnhancementRules.quote(catalog.definition(), 25, "精良", 7));
		assertThrows(IllegalArgumentException.class,
				() -> EnhancementRules.outcome(EnhancementRules.quote(catalog.definition(), 1, "普通", 0), 100));
	}

	@Test
	void equipmentLevelSelectsStoneAndFeeIndependentlyOfEnhancementAndQuality() {
		var rules = catalog.definition();
		assertEquals(10, EnhancementRules.quote(rules, 10, "普通", 0).gold());
		assertEquals(20, EnhancementRules.quote(rules, 11, "普通", 0).gold());
		assertEquals("ENHANCEMENT_STONE_I", EnhancementRules.quote(rules, 20, "传说", 6).stone());
		assertEquals("ENHANCEMENT_STONE_II", EnhancementRules.quote(rules, 21, "普通", 0).stone());
		assertEquals(840, EnhancementRules.quote(rules, 30, "传说", 6).gold());
		assertThrows(BusinessException.class, () -> EnhancementRules.quote(rules, 31, "普通", 0));
		assertThrows(BusinessException.class, () -> EnhancementRules.quote(rules, 0, "普通", 0));
		assertThrows(BusinessException.class, () -> EnhancementRules.quote(rules, 10, "稀有", 0));
	}

	@Test
	void statsUseOriginalValuesOnceAndDoNotAmplifyEffectsOrIntroduceMissingStats() {
		var original = Map.of("health", 19, "physicalAttack", 3, "speed", 0, "shield", 25);
		var rules = catalog.definition();
		assertEquals(original, EnhancementRules.stats(original, 0, rules));
		var maximum = EnhancementRules.stats(original, 7, rules);
		assertEquals(25, maximum.get("health"));
		assertEquals(4, maximum.get("physicalAttack"));
		assertEquals(0, maximum.get("speed"));
		assertEquals(25, maximum.get("shield"));
		assertFalse(maximum.containsKey("magicAttack"));
		assertEquals(24, EnhancementRules.stats(original, 6, rules).get("health"));
		assertEquals(original, Map.of("health", 19, "physicalAttack", 3, "speed", 0, "shield", 25));
		assertThrows(ArithmeticException.class,
				() -> EnhancementRules.stats(Map.of("health", Integer.MAX_VALUE), 7, rules));
	}

	@Test
	void feeRoundingAndOverflowCannotCreateFreeOrNegativeAttempts() {
		var original = catalog.definition();
		var oddFee = withFees(List.of(new EnhancementCatalog.Fee(1, 30, 11)));
		EnhancementCatalog.validate(oddFee);
		assertEquals(17, EnhancementRules.quote(oddFee, 1, "优秀", 0).gold());
		assertEquals(33, EnhancementRules.quote(oddFee, 1, "优秀", 1).gold());
		var overflowing = withFees(List.of(new EnhancementCatalog.Fee(1, 30, Long.MAX_VALUE)));
		assertThrows(BusinessException.class, () -> EnhancementRules.quote(overflowing, 1, "传说", 0));
		assertThrows(IllegalArgumentException.class, () -> EnhancementCatalog.validate(
				withFees(List.of(new EnhancementCatalog.Fee(1, 10, 10), new EnhancementCatalog.Fee(10, 30, 20)))));
		assertThrows(IllegalArgumentException.class, () -> EnhancementCatalog.validate(
				withFees(List.of(new EnhancementCatalog.Fee(1, 10, 10), new EnhancementCatalog.Fee(12, 30, 20)))));
		assertThrows(IllegalArgumentException.class,
				() -> EnhancementCatalog.validate(withFees(List.of(new EnhancementCatalog.Fee(1, 30, 0)))));
		assertEquals(30, original.fees().getLast().maximum());
	}

	private EnhancementCatalog.Definition withFees(List<EnhancementCatalog.Fee> fees) {
		var value = catalog.definition();
		return new EnhancementCatalog.Definition(value.playerLevel(), value.locations(), value.maximumLevel(),
				value.successPercent(), value.statPercentPerLevel(), fees, value.qualityMultipliers(), value.stones());
	}

}
