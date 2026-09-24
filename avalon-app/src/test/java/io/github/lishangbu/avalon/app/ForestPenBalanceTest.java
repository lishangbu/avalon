package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.battle.domain.BattleEngine;
import io.github.lishangbu.avalon.battle.domain.BattleRules;
import io.github.lishangbu.avalon.battle.domain.DungeonBattlePolicy;
import io.github.lishangbu.avalon.content.battle.BattleCatalog;
import io.github.lishangbu.avalon.content.creature.CreatureCatalog;
import io.github.lishangbu.avalon.content.battle.DungeonCatalog;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Tag;
import static org.junit.jupiter.api.Assertions.*;

/** 推荐等级专项：正式七维与首领参数，基础装备，无药品；战间休整按副本允许的恢复窗口计。 */
@Tag("rules")
class ForestPenBalanceTest {

	private final RuleTestFixture fixture = new RuleTestFixture();

	private final BattleRules rules = fixture.rules;

	private final BattleEngine engine = fixture.engine;

	private final BattleCatalog battles = fixture.battles;

	private final CreatureCatalog creatures = fixture.creatures;

	private final DungeonCatalog dungeon = fixture.dungeon;

	private final DungeonBattlePolicy policy = fixture.dungeonPolicy;

	@ParameterizedTest
	@ValueSource(strings = { "MAGE", "RANGER", "WARRIOR", "PRIEST" })
	void recommendedLevelSupportsDifferentProfessionsWithoutHiddenAbilities(String middle) {
		var samples = new DungeonCombatSamples(rules, engine, battles, creatures, dungeon, policy);
		double rounds = samples.meanRounds(middle, 12, 8);
		assertTrue(rounds > 0 && rounds <= 60);
	}

}
