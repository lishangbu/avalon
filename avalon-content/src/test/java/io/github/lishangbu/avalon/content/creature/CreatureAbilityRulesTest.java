package io.github.lishangbu.avalon.content.creature;

import java.util.HashMap;
import java.util.Map;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** 穷举抽签空间验证概率与特性映射，避免用随机抽样制造偶发失败。 */
class CreatureAbilityRulesTest {

	@Test
	void ordinarySlotsCanShareAnAbilityAndHiddenSlotUsesItsOwnAbility() {
		var slots = mock(AbilitySlotsCatalog.class);
		var random = mock(RandomGenerator.class);
		when(slots.ability("CAT", "A")).thenReturn("普通特性");
		when(slots.ability("CAT", "B")).thenReturn("普通特性");
		when(slots.ability("CAT", "H")).thenReturn("隐藏特性");
		var rules = new CreatureAbilityRules(slots);
		var counts = new HashMap<String, Integer>();
		for (int roll = 0; roll < 100; roll++) {
			when(random.nextInt(100)).thenReturn(roll);
			var selected = rules.draw("CAT", random);
			counts.merge(selected.slot(), 1, Integer::sum);
			assertEquals(roll < 45 ? "A" : roll < 90 ? "B" : "H", selected.slot());
			assertEquals(roll < 90 ? "普通特性" : "隐藏特性", selected.ability());
		}
		assertEquals(Map.of("A", 45, "B", 45, "H", 10), counts);
		verify(random, times(100)).nextInt(100);
	}

	@Test
	void secondOrdinarySlotCanDifferWithoutChangingProbability() {
		var slots = mock(AbilitySlotsCatalog.class);
		var random = mock(RandomGenerator.class);
		when(slots.ability("CAT", "B")).thenReturn("第二普通效果");
		when(random.nextInt(100)).thenReturn(45, 89);
		var rules = new CreatureAbilityRules(slots);
		assertEquals(new CreatureAbilityRulesSelection("B", "第二普通效果"), rules.draw("CAT", random));
		assertEquals(new CreatureAbilityRulesSelection("B", "第二普通效果"), rules.draw("CAT", random));
	}

	@Test
	void runtimeSlotsCoverEvolvedFormsAndRejectUnknownSlot() {
		var slots = new AbilitySlotsCatalog(new CreatureCatalog());
		assertEquals("乘风", slots.ability("GALE_FALCON", "H"));
		assertEquals("轻盈", slots.ability("SHADOWTAIL_FOX", "A"));
		assertEquals("灵爪", slots.ability("SHADOWTAIL_FOX", "B"));
		assertEquals("夜袭", slots.ability("SHADOWTAIL_FOX", "H"));
		assertThrows(IllegalArgumentException.class, () -> slots.ability("GALE_FALCON", "C"));
		assertThrows(IllegalArgumentException.class, () -> slots.ability("MISSING", "A"));
	}

}
