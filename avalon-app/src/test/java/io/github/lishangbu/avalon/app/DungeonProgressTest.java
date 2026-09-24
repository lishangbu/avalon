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
