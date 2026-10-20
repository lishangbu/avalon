package io.github.lishangbu.avalon.app;

import tools.jackson.databind.ObjectMapper;
import io.github.lishangbu.avalon.battle.domain.DungeonProgressPolicy;
import io.github.lishangbu.avalon.battle.domain.DungeonProgressState;
import io.github.lishangbu.avalon.platform.BusinessException;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** 副本战间进度与战局分开保存，验证重试、退出和断线不会跳关或自动准备。 */
@Tag("rules")
class DungeonProgressTest {

	private final RuleTestFixture fixture = new RuleTestFixture();

	private final DungeonProgressPolicy progress = fixture.progress;

	private final ObjectMapper json = new ObjectMapper();

	private DungeonProgressState create() {
		return progress.create("a", List.of("a", "b", "c"));
	}

	/** 单人沿用正式进度状态机，失败保留清理进度，重读后不能跳关或重复结算。 */
	@Test
	void soloDungeonRetainsSequentialProgressAcrossDefeatAndReload() {
		var source = fixture.dungeon.definition();
		var stages = source.stages()
			.subList(0, 3)
			.stream()
			.map(stage -> new io.github.lishangbu.avalon.content.battle.model.DungeonStage("NOM_" + stage.id(),
					stage.name(), stage.wildSpecies(), stage.healthMultiplier(), stage.health(), stage.attack(),
					stage.defense(), stage.speed(), stage.guardHealth(), stage.guardDefense(), stage.experience(),
					stage.gold(), stage.actions()))
			.toList();
		var definition = new io.github.lishangbu.avalon.content.battle.model.DungeonDefinition("NOM_SEAL", "诺姆封印室", 20,
				"N020", "NOM_SEAL_FORECOURT", 1, source.dailyStarts(), source.resetHour(), source.zone(),
				source.roundSeconds(), source.absenceRounds(), source.confirmSeconds(), source.holdSeconds(),
				source.invitationSeconds(), List.of(0, 1, 2), source.supplies(), stages);
		var catalog = new io.github.lishangbu.avalon.content.battle.DungeonCatalog(List.of(source, definition),
				fixture.creatures, fixture.quests, fixture.items, fixture.world);
		var solo = new DungeonProgressPolicy(catalog);
		assertEquals(source, catalog.definition());
		assertEquals(definition, catalog.owningStage(stages.getFirst().id()));
		assertEquals(source, catalog.owningStage(source.stages().getFirst().id()));
		var battlePolicy = new io.github.lishangbu.avalon.battle.domain.DungeonBattlePolicy(catalog, fixture.creatures,
				fixture.rules, fixture.attacks);
		assertTrue(battlePolicy.create(stages.getFirst().id(), List.of(), 1)
			.getUnits()
			.stream()
			.allMatch(unit -> unit.getLevel() == 20));
		assertTrue(battlePolicy.create(source.stages().getFirst().id(), List.of(), 1)
			.getUnits()
			.stream()
			.allMatch(unit -> unit.getLevel() == source.level()));
		assertThrows(IllegalArgumentException.class,
				() -> new io.github.lishangbu.avalon.content.battle.DungeonCatalog(List.of(source, source),
						fixture.creatures, fixture.quests, fixture.items, fixture.world));
		assertEquals("D01", solo.create("a", List.of("a", "b", "c")).getDungeon());
		var state = solo.create("NOM_SEAL", "a", List.of("a"));
		assertThrows(BusinessException.class, () -> solo.create("NOM_SEAL", "a", List.of("a", "b", "c")));
		assertThrows(BusinessException.class, () -> solo.join(state, "b"));
		for (int stage = 0; stage < 3; stage++) {
			assertEquals(stage, state.getCleared());
			assertThrows(BusinessException.class, () -> solo.nextStage(state));
			solo.ready(state, "a");
			solo.begin(state, 100 + stage * 2);
			solo.resolve(state, 100 + stage * 2, "LOST");
			assertEquals(stage, state.getCleared());
			solo.rest(state, "a");
			solo.ready(state, "a");
			assertEquals(definition.stages().get(stage).id(), solo.nextStage(state));
			solo.begin(state, 101 + stage * 2);
			var restored = json.readValue(json.writeValueAsString(state), DungeonProgressState.class);
			assertTrue(solo.resolve(restored, 101 + stage * 2, "WON"));
			assertFalse(solo.resolve(restored, 101 + stage * 2, "WON"));
			state.setCleared(restored.getCleared());
			state.setActiveBattle(restored.getActiveBattle());
			state.setMembers(restored.getMembers());
			state.setResolvedBattles(restored.getResolvedBattles());
		}
		assertThrows(BusinessException.class, () -> solo.ready(state, "a"));
	}

	private void readyAll(DungeonProgressState state) {
		state.getMembers().forEach((id, member) -> {
			if (!member.isExited())
				progress.ready(state, id);
		});
	}

	private void win(DungeonProgressState state, long battle) {
		readyAll(state);
		progress.begin(state, battle);
		assertTrue(progress.resolve(state, battle, "WON"));
	}

	@Test
	void fourStagesNeedFreshReadinessAndResolveOnlyOnceEvenAfterReload() throws Exception {
		var state = create();
		var initial = state;
		assertThrows(BusinessException.class, () -> progress.nextStage(initial));
		readyAll(state);
		assertEquals("D01_1", progress.nextStage(state));
		progress.begin(state, 101);
		state = json.readValue(json.writeValueAsString(state), DungeonProgressState.class);
		assertTrue(progress.resolve(state, 101, "WON"));
		assertFalse(progress.resolve(state, 101, "WON"));
		assertEquals(1, state.getCleared());
		var restored = state;
		assertThrows(BusinessException.class, () -> progress.resolve(restored, 101, "LOST"));
		assertThrows(BusinessException.class, () -> progress.begin(restored, 102));
		readyAll(state);
		assertEquals("D01_2", progress.nextStage(state));
		progress.begin(state, 102);
		assertFalse(progress.resolve(state, 101, "WON"));
		assertEquals(102L, state.getActiveBattle());
		progress.resolve(state, 102, "WON");
		readyAll(state);
		assertEquals("D01_3", progress.nextStage(state));
		progress.begin(state, 103);
		progress.resolve(state, 103, "WON");
		assertEquals(3, state.getCleared());
		readyAll(state);
		assertEquals("D01_4", progress.nextStage(state));
		progress.begin(state, 104);
		assertTrue(progress.resolve(state, 104, "WON"));
		assertEquals(4, state.getCleared());
		assertThrows(BusinessException.class, () -> progress.ready(restored, "a"));
		assertThrows(BusinessException.class, () -> progress.join(restored, "d"));
	}

	@Test
	void wipeKeepsClearedStagesAndReturnsToNearestUnlockedRestWithoutAutomaticReadiness() {
		var state = create();
		win(state, 101);
		readyAll(state);
		progress.begin(state, 102);
		progress.resolve(state, 102, "LOST");
		assertEquals(1, state.getCleared());
		assertEquals(0, state.getMembers().get("a").getLocation());
		assertThrows(BusinessException.class, () -> progress.ready(state, "a"));
		for (String id : List.of("a", "b", "c")) {
			progress.rest(state, id);
			progress.move(state, id, 1);
		}
		win(state, 103);
		readyAll(state);
		progress.begin(state, 104);
		progress.resolve(state, 104, "LOST");
		assertEquals(2, state.getCleared());
		assertEquals(2, state.getMembers().get("a").getLocation());
		assertFalse(state.getMembers().get("a").isReady());
		assertThrows(BusinessException.class, () -> progress.begin(state, 105));
		readyAll(state);
		assertEquals("D01_3", progress.nextStage(state));
		assertThrows(BusinessException.class, () -> progress.begin(state, 104));
	}

	@Test
	void restRequiresWalkingClearedAdjacentNodesAndCancelsOnlyOwnReadiness() {
		var state = create();
		assertThrows(BusinessException.class, () -> progress.move(state, "a", 1));
		win(state, 101);
		assertThrows(BusinessException.class, () -> progress.rest(state, "a"));
		readyAll(state);
		progress.move(state, "a", 0);
		progress.rest(state, "a");
		assertFalse(state.getMembers().get("a").isReady());
		assertTrue(state.getMembers().get("b").isReady());
		assertThrows(BusinessException.class, () -> progress.nextStage(state));
		progress.move(state, "a", 1);
		progress.ready(state, "a");
		progress.begin(state, 102);
		progress.resolve(state, 102, "WON");
		assertThrows(BusinessException.class, () -> progress.move(state, "a", 0));
		progress.move(state, "a", 1);
		progress.move(state, "a", 0);
		progress.rest(state, "a");
		assertThrows(BusinessException.class, () -> progress.move(state, "a", -1));
	}

	@Test
	void voluntaryExitIsPermanentButMissingMemberCanManuallyReadyOnRetry() {
		var state = create();
		assertThrows(BusinessException.class, () -> progress.join(state, "d"));
		win(state, 101);
		progress.leave(state, "b");
		progress.leave(state, "b");
		assertThrows(BusinessException.class, () -> progress.join(state, "b"));
		assertThrows(BusinessException.class, () -> progress.ready(state, "b"));
		progress.join(state, "d");
		assertEquals(0, state.getMembers().get("d").getLocation());
		progress.move(state, "d", 1);
		readyAll(state);
		progress.begin(state, 102);
		// 超时移出仅属于单场战局，不应在副本进度中标记为主动退出。
		progress.resolve(state, 102, "LOST");
		for (String id : List.of("a", "c", "d"))
			progress.move(state, id, 1);
		readyAll(state);
		progress.begin(state, 103);
		assertEquals("a", state.getLeader());
		assertTrue(state.getMembers().get("b").isExited());
	}

	@Test
	void battleBlocksRestMovementAndExitWhileDisconnectDoesNotCancelBattle() {
		var state = create();
		readyAll(state);
		progress.invalidateReady(state, "a");
		assertThrows(BusinessException.class, () -> progress.begin(state, 101));
		progress.ready(state, "a");
		progress.begin(state, 101);
		progress.invalidateReady(state, "a");
		assertEquals(101L, state.getActiveBattle());
		assertThrows(BusinessException.class, () -> progress.rest(state, "a"));
		assertThrows(BusinessException.class, () -> progress.move(state, "a", 0));
		assertThrows(BusinessException.class, () -> progress.leave(state, "a"));
		assertThrows(BusinessException.class, () -> progress.join(state, "d"));
		assertThrows(BusinessException.class, () -> progress.ready(state, "a"));
		assertThrows(BusinessException.class, () -> progress.resolve(state, 102, "WON"));
		assertThrows(BusinessException.class, () -> progress.resolve(state, 101, "ACTIVE"));
		assertEquals(0, state.getCleared());
	}

	@Test
	void invalidMembershipNeverCreatesAProgress() {
		assertThrows(BusinessException.class, () -> progress.create("a", List.of("a", "a", "b")));
		assertThrows(BusinessException.class, () -> progress.create("a", List.of("b", "c", "d")));
		assertThrows(BusinessException.class, () -> progress.create("a", List.of("a", "b")));
		assertThrows(BusinessException.class, () -> progress.ready(create(), "intruder"));
	}

}
