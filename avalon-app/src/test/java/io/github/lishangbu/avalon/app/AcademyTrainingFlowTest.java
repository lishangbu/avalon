package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.battle.application.command.BattleCommandService;
import io.github.lishangbu.avalon.battle.application.command.TrainingCommandService;
import io.github.lishangbu.avalon.battle.domain.BattleState;
import io.github.lishangbu.avalon.battle.domain.BattleState.Order;
import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.character.application.command.CharacterFactCommandService;
import io.github.lishangbu.avalon.character.application.query.CharacterFactQueryService;
import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.identity.application.command.RegistrationService;
import io.github.lishangbu.avalon.identity.application.query.AccountQueryService;
import io.github.lishangbu.avalon.platform.BusinessException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.*;

/** 真实事务验证新事实门禁、同请求重放、隔离资源及课程与任务奖励分离。 */
class AcademyTrainingFlowTest extends SpringIntegrationTest {

	@Autowired
	RegistrationService registrations;

	@Autowired
	AccountQueryService accounts;

	@Autowired
	CharacterCommandService characters;

	@Autowired
	CharacterFactCommandService facts;

	@Autowired
	CharacterQueryService characterQueries;

	@Autowired
	CharacterFactQueryService factQueries;

	@Autowired
	TrainingCommandService training;

	@Autowired
	BattleCommandService battles;

	@Autowired
	JdbcTemplate jdbc;

	@Test
	void captureProjectionUsesTemporaryItemsAndNeverCreatesAssets() {
		String name = "cap_" + UUID.randomUUID().toString().replace("-", "").substring(0, 9);
		registrations.register(name, "Academy-test-1234", "测试");
		long owner = accounts.accountId(name);
		characters.create(owner, name, "MAGE");
		transactions.run(() -> {
			long id = characters.lock(owner).id();
			jdbc.update("UPDATE player_character SET location = 'ACADEMY' WHERE id = ?", id);
			facts.record(owner, "QUEST:BQ018");
		});
		var before = characterQueries.find(owner);
		boolean captured = false;
		for (int seed = 0; seed < 40 && !captured; seed++) {
			String key = UUID.randomUUID().toString();
			var view = training.startAcademy(owner, "BQ020", List.of(), key);
			assertNull(view.academyCapture().capturedAbilitySlot());
			assertNull(view.units().stream().filter(u -> u.enemy()).findFirst().orElseThrow().ability());
			long id = Long.parseLong(view.id());
			// 仅固定测试战局随机种子；捕捉仍走概率判定，不改目标生命或成功结果。
			var state = jsonMapper.readValue(
					jdbc.queryForObject("SELECT state FROM battle WHERE id = ?", String.class, id), BattleState.class);
			state.setSeed(seed);
			jdbc.update("UPDATE battle SET state = ? WHERE id = ?", jsonMapper.writeValueAsString(state), id);
			view = training.captureInfo(owner, id, "SEAL_I");
			assertTrue(view.academyCapture().viewed());
			view = battles.turn(
					owner, id, view.round(), List.of(new Order("trainee", "DEFEND", "trainee"),
							new Order("T_CAT", "水弹", "academy-target"), new Order("T_STONE", "DEFEND", "T_STONE")),
					false, Map.of());
			int attemptRound = view.round();
			var orders = List.of(new Order("trainee", "SEAL_I", "academy-target"),
					new Order("T_CAT", "DEFEND", "T_CAT"), new Order("T_STONE", "DEFEND", "T_STONE"));
			view = battles.turn(owner, id, attemptRound, orders, false, Map.of());
			assertEquals(7, view.supplies().get("SEAL_I"));
			assertEquals(view, battles.turn(owner, id, attemptRound, orders, false, Map.of()));
			captured = view.status().equals("WON");
			if (captured) {
				assertNotNull(view.academyCapture().capturedAbilitySlot());
				assertNotNull(view.academyCapture().capturedAbility());
				assertEquals(view, training.startAcademy(owner, "BQ020", List.of(), key));
			}
			else if (view.status().equals("ACTIVE"))
				battles.turn(owner, id, view.round(), List.of(), true, Map.of());
		}
		assertTrue(captured);
		assertTrue(factQueries.codes(owner).contains("TRAINING:BQ020"));
		assertFalse(factQueries.codes(owner).contains("QUEST:BQ020"));
		assertEquals(before.experience(), characterQueries.find(owner).experience());
		assertEquals(before.gold(), characterQueries.find(owner).gold());
		long characterId = Long.parseLong(before.id());
		assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM owned_creature WHERE character_id = ?", Integer.class,
				characterId));
		assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM owned_item WHERE character_id = ?", Integer.class,
				characterId));
	}

	@Test
	void malformedCourseAndSelectionFailBeforeLookingUpCharacter() {
		assertThrows(BusinessException.class,
				() -> training.startAcademy(-1, null, List.of(), UUID.randomUUID().toString()));
		assertThrows(BusinessException.class,
				() -> training.startAcademy(-1, " ", List.of(), UUID.randomUUID().toString()));
		assertThrows(BusinessException.class,
				() -> training.startAcademy(-1, "BQ019", Arrays.asList("T_CAT", null), UUID.randomUUID().toString()));
	}

	@Test
	void borrowedCourseUsesItsOwnFactsAndNeverCreatesRealPetsOrRewards() {
		String name = "aca_" + UUID.randomUUID().toString().replace("-", "").substring(0, 9);
		registrations.register(name, "Academy-test-1234", "测试");
		long owner = accounts.accountId(name);
		characters.create(owner, name, "MAGE");
		transactions.run(() -> {
			long id = characters.lock(owner).id();
			jdbc.update("UPDATE player_character SET location = 'ACADEMY' WHERE id = ?", id);
			facts.record(owner, "QUEST:BQ014");
		});
		assertThrows(BusinessException.class,
				() -> training.startAcademy(owner, "BQ018", List.of(), UUID.randomUUID().toString()));
		// 远行资格不允许绕过学院入学，也不存在直接从野外遭遇接口领取职业训练奖励的路径。
		for (String removed : List.of("BQ023_TRAINING", "BQ019_SWAP"))
			assertThrows(BusinessException.class, () -> battles.start(owner, removed, UUID.randomUUID().toString()));
		transactions.run(() -> facts.record(owner, "TRAINING:BQ018"));
		assertThrows(
				BusinessException.class, () -> training.startAcademy(owner, "BQ019",
						List.of("T_CAT", "T_STONE", "T_SPROUT", "T_GALE"), UUID.randomUUID().toString()),
				"第一课胜利尚未交付时，不允许提前练习后续任务目标");
		transactions.run(() -> facts.record(owner, "QUEST:BQ018"));
		var before = characterQueries.find(owner);
		var selected = List.of("T_CAT", "T_STONE", "T_SPROUT", "T_GALE");
		String key = UUID.randomUUID().toString();
		var battle = training.startAcademy(owner, "BQ019", selected, key);
		assertEquals(battle.id(), training.startAcademy(owner, "BQ019", selected, key).id());
		assertThrows(BusinessException.class,
				() -> training.startAcademy(owner, "BQ019", List.of("T_STONE", "T_CAT", "T_SPROUT", "T_GALE"), key));
		assertThrows(BusinessException.class,
				() -> training.startAcademy(owner, "BQ018", List.of(), UUID.randomUUID().toString()));
		var done = battles.turn(
				owner, Long.parseLong(battle.id()), battle.round(), List.of(new Order("trainee", "DEFEND", "trainee"),
						new Order("T_CAT", "SWAP", "T_SPROUT"), new Order("T_STONE", "DEFEND", "T_STONE")),
				false, Map.of());
		assertEquals("WON", done.status());
		assertTrue(factQueries.codes(owner).contains("TRAINING:BQ019"));
		assertFalse(factQueries.codes(owner).contains("QUEST:BQ019"));
		assertEquals(before.experience(), characterQueries.find(owner).experience());
		assertEquals(before.gold(), characterQueries.find(owner).gold());
		long characterId = Long.parseLong(before.id());
		assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM owned_creature WHERE character_id = ?", Integer.class,
				characterId));
		assertEquals(done.id(), training.startAcademy(owner, "BQ019", selected, key).id());
	}

}
