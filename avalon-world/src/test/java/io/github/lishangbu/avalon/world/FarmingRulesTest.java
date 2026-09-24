package io.github.lishangbu.avalon.world;

import io.github.lishangbu.avalon.world.domain.FarmingRules;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FarmingRulesTest {

	private final Instant now = Instant.parse("2026-09-22T00:00:00Z");

	@Test
	void unwateredCropsNeverGrowAndWateringAgainDoesNotResetTime() {
		assertEquals(900, FarmingRules.remainingSeconds(null, 900, now.plusSeconds(999999)));
		assertFalse(FarmingRules.mature(null, 900, now.plusSeconds(999999)));
		var first = FarmingRules.water(null, now);
		assertEquals(first, FarmingRules.water(first, now.plusSeconds(300)));
		assertEquals(600, FarmingRules.remainingSeconds(first, 900, now.plusSeconds(300)));
	}

	@Test
	void harvestBoundaryIsExactAndOfflineMaturityNeverExpires() {
		assertFalse(FarmingRules.mature(now, 900, now.plusMillis(899999)));
		assertEquals(1, FarmingRules.remainingSeconds(now, 900, now.plusMillis(899999)));
		assertTrue(FarmingRules.mature(now, 900, now.plusSeconds(900)));
		assertTrue(FarmingRules.mature(now, 900, now.plusSeconds(900000)));
		assertEquals(900, FarmingRules.remainingSeconds(now, 900, now.minusSeconds(20)));
	}

}
