package io.github.lishangbu.avalon.world;

import io.github.lishangbu.avalon.world.domain.GatheringRareRoll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GatheringRareRollTest {

	@Test
	void proficiencyBonusIsSmallAndCapped() {
		assertEquals(5, GatheringRareRoll.chance(5, 5));
		assertEquals(6, GatheringRareRoll.chance(6, 5));
		assertEquals(8, GatheringRareRoll.chance(10, 5));
		assertEquals(5, GatheringRareRoll.chance(10, 10));
		assertThrows(IllegalArgumentException.class, () -> GatheringRareRoll.chance(4, 5));
	}

}
