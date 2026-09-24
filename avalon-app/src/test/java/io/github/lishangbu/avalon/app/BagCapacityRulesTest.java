package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.inventory.domain.BagCapacityRules;
import io.github.lishangbu.avalon.inventory.domain.BagCapacityRules.Entry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** 多个奖励来源并不多占格，但不同绑定状态永远不合并。 */
class BagCapacityRulesTest {

	@Test
	void stacksAcrossSourcesRespectBindingAndExcludeTaskItems() {
		assertEquals(5,
				BagCapacityRules.slots(List.of(new Entry("BOOK", false, 98, false, false),
						new Entry("BOOK", false, 2, false, false), new Entry("BOOK", true, 1, false, false),
						new Entry("SWORD", true, 2, true, false), new Entry("QUEST", true, 100, false, true))));
		assertThrows(IllegalArgumentException.class,
				() -> BagCapacityRules.slots(List.of(new Entry("BOOK", false, -1, false, false))));
	}

}
