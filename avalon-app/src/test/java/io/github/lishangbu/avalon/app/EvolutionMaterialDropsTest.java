package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.battle.domain.EvolutionMaterialDrops;
import io.github.lishangbu.avalon.content.creature.model.EvolutionDropDrop;
import java.util.Random;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** 穷举双池随机点，验证能同时掉落、边界数量和固定种子重放。 */
class EvolutionMaterialDropsTest {

	private final EvolutionDropDrop drop = new EvolutionDropDrop(2, "WATER_PEARL", 20, "TIDAL_CLAW_CAT_ESSENCE", 10);

	@Test
	void poolsAreIndependentAndHaveExactMarginalProbabilities() {
		int common = 0, family = 0, both = 0;
		for (int a = 0; a < 100; a++) {
			for (int b = 0; b < 100; b++) {
				var rewards = EvolutionMaterialDrops.roll(drop, 2, rolls(a, b));
				if (rewards.stream().anyMatch(r -> r.code().equals("WATER_PEARL")))
					common++;
				if (rewards.stream().anyMatch(r -> r.code().equals("TIDAL_CLAW_CAT_ESSENCE")))
					family++;
				if (rewards.size() == 2)
					both++;
			}
		}
		assertEquals(2000, common);
		assertEquals(1000, family);
		assertEquals(200, both);
	}

	@Test
	void quantityUsesSourceLevelAndCapsAtThreeAndSeedReplayIsStable() {
		for (int level : new int[] { 1, 2, 11 })
			assertEquals(1, EvolutionMaterialDrops.roll(drop, level, rolls(0, 99)).getFirst().quantity());
		assertEquals(2, EvolutionMaterialDrops.roll(drop, 12, rolls(0, 99)).getFirst().quantity());
		assertEquals(3, EvolutionMaterialDrops.roll(drop, 22, rolls(0, 99)).getFirst().quantity());
		assertEquals(3, EvolutionMaterialDrops.roll(drop, 50, rolls(0, 99)).getFirst().quantity());
		assertEquals(EvolutionMaterialDrops.roll(drop, 15, new Random(42)),
				EvolutionMaterialDrops.roll(drop, 15, new Random(42)));
	}

	private Random rolls(int first, int second) {
		return new Random() {
			private int index;

			@Override
			public int nextInt(int bound) {
				return index++ == 0 ? first : second;
			}
		};
	}

}
