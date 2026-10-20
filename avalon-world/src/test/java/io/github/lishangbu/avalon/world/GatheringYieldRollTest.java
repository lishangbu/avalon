package io.github.lishangbu.avalon.world;

import io.github.lishangbu.avalon.world.domain.GatheringYieldRoll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** 成功率保留空产出机会，专业优势提高产出而不绕过资源等级资格。 */
class GatheringYieldRollTest {

	@Test
	void skillAdvantageImprovesYieldButNeverGuaranteesIt() {
		assertEquals(80, GatheringYieldRoll.chance(1, 1));
		assertEquals(88, GatheringYieldRoll.chance(5, 1));
		assertEquals(95, GatheringYieldRoll.chance(10, 1));
		assertEquals(80, GatheringYieldRoll.chance(10, 10));
		assertThrows(IllegalArgumentException.class, () -> GatheringYieldRoll.chance(1, 2));
		assertThrows(IllegalArgumentException.class, () -> GatheringYieldRoll.chance(11, 1));
		assertThrows(IllegalArgumentException.class, () -> GatheringYieldRoll.chance(1, 0));
	}

}
