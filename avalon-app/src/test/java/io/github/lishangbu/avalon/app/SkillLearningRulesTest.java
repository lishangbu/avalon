package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.creature.domain.SkillLearningRules;
import io.github.lishangbu.avalon.platform.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/** 穷举百分位抽签，验证确认过的概率表与数量边界，不依赖随机抽样碰运气。 */
class SkillLearningRulesTest {

	@Test
	void everyProbabilityTicketPreservesLearningAndCapacity() {
		int[][] expected = { { 100, 0, 0, 0 }, { 100, 0, 0, 0 }, { 100, 0, 0, 0 }, { 80, 20, 0, 0 }, { 60, 30, 10, 0 },
				{ 40, 35, 20, 5 }, { 20, 40, 25, 15 }, { 0, 45, 35, 20 } };
		for (int count = 0; count <= 7; count++) {
			var current = IntStream.range(0, count).mapToObj(index -> "旧技能" + index).toList();
			int[] observed = new int[4];
			for (int ticket = 0; ticket < 100; ticket++) {
				var result = SkillLearningRules.learn(current, "新技能", ticket(ticket));
				observed[result.forgotten().size()]++;
				assertTrue(result.remaining().contains("新技能"));
				assertFalse(result.forgotten().contains("新技能"));
				assertTrue(result.remaining().size() <= 7);
				assertEquals(count + 1, result.remaining().size() + result.forgotten().size());
				assertEquals(result.forgotten().size(), new HashSet<>(result.forgotten()).size());
				assertTrue(result.forgotten().stream().noneMatch(result.remaining()::contains));
				assertEquals(count, current.size(), "抽签不能直接改动持久层传来的名单");
			}
			assertArrayEquals(expected[count], observed);
		}
	}

	@Test
	void duplicateLearningIsRejectedBeforeDrawing() {
		var noDraw = new Random() {
			@Override
			public int nextInt(int bound) {
				fail("非法学习不能提前消耗随机结果");
				return 0;
			}
		};
		assertThrows(BusinessException.class, () -> SkillLearningRules.learn(List.of("水弹", "微疗"), "水弹", noDraw));
		assertThrows(IllegalArgumentException.class, () -> SkillLearningRules.probabilities(8));
	}

	/** 首次固定百分位，其后取最后一个候选，覆盖无放回抽样的索引收缩边界。 */
	private Random ticket(int value) {
		return new Random() {
			private boolean first = true;

			@Override
			public int nextInt(int bound) {
				if (first) {
					first = false;
					assertEquals(100, bound);
					return value;
				}
				return bound - 1;
			}
		};
	}

}
