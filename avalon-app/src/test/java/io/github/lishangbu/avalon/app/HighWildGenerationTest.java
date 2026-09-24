package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.battle.domain.WildGeneration;
import io.github.lishangbu.avalon.content.battle.BattleCatalog;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import static org.junit.jupiter.api.Assertions.*;

@Tag("rules")
class HighWildGenerationTest {

	private final RuleTestFixture fixture = new RuleTestFixture();

	private final WildGeneration generation = fixture.generation;

	private final BattleCatalog battles = fixture.battles;

	@Test
	void allRegionsGenerateRealCombatUnitsInFiveLevelBandsThroughLevelFifty() {
		String[] regions = { "COPPER", "IRON", "IRON_DEEP", "SILVER", "SILVER_DEEP", "GOLD", "GOLD_DEEP" };
		int[] minimum = { 16, 21, 26, 31, 36, 41, 46 }, maximum = { 20, 25, 30, 35, 40, 45, 50 };
		for (int index = 0; index < regions.length; index++) {
			boolean upperSeen = false;
			for (int seed = 0; seed < 200; seed++) {
				var units = generation.create(battles.encounter("HIGH_" + regions[index] + "_WILD"), new Random(seed));
				assertEquals(1, units.size());
				var enemy = units.getFirst();
				assertTrue(enemy.getLevel() >= minimum[index] && enemy.getLevel() <= maximum[index]);
				assertTrue(enemy.getHp() > 0);
				assertFalse(enemy.getSkills().isEmpty());
				upperSeen |= enemy.getLevel() == maximum[index];
			}
			assertTrue(upperSeen, regions[index]);
		}
	}

}
