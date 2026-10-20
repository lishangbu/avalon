package io.github.lishangbu.avalon.quest;

import io.github.lishangbu.avalon.quest.model.QuestBranch;
import io.github.lishangbu.avalon.quest.model.QuestCondition;
import io.github.lishangbu.avalon.quest.model.QuestDelivery;
import io.github.lishangbu.avalon.quest.model.QuestEffect;
import io.github.lishangbu.avalon.quest.model.QuestEvent;
import io.github.lishangbu.avalon.quest.model.QuestMeasure;
import io.github.lishangbu.avalon.quest.model.QuestObjective;
import io.github.lishangbu.avalon.quest.model.QuestRepeat;
import io.github.lishangbu.avalon.quest.model.QuestStep;
import io.github.lishangbu.avalon.quest.model.QuestTimeout;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static io.github.lishangbu.avalon.quest.model.QuestStatus.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 通过正式引擎接口验证任务规则，使用固定时间及简单宿主替身，无须数据库或游戏服务。 测试重点是跨轮、跨周期、失败和重复请求，而非复述内部集合操作。
 */
class QuestEngineTest {

	private final QuestEngine engine = new QuestEngine();

	private final Instant now = Instant.parse("2026-10-12T15:59:00Z");

	private final QuestHost host = new QuestHost() {
		public long value(QuestMeasure measure, String code) {
			return 0;
		}

		public boolean test(QuestCondition condition) {
			return true;
		}
	};

	/** 连续击杀累计到阈值，重复事件不推进，自动领奖只有首次产生效果。 */
	@Test
	void countsRealActionsAndDeduplicatesRewards() {
		var definition = definition(QuestRepeat.ONCE, 1, step("kill", QuestDelivery.AUTO, 3));
		var state = engine.accept(definition, List.of(), "i1", now, host).instance();
		var first = event(definition, state, "e1", 1);
		state = first.instance();
		assertEquals(1L, state.progress().get("kill").get("count"));
		assertEquals(state, event(definition, state, "e1", 1).instance());
		var finished = event(definition, state, "e2", 2);
		assertEquals(COMPLETED, finished.instance().status());
		assertEquals("i1:1:kill:0", finished.grants().getFirst().source());
		assertTrue(engine.refresh(definition, finished.instance(), now, host).grants().isEmpty());
		assertThrows(IllegalStateException.class,
				() -> engine.accept(definition, List.of(finished.instance()), "i2", now, host));
	}

	/** 同一个行为可以推进并行目标，但不能偷跑前置尚未完成的后续步骤。 */
	@Test
	void doesNotApplyAnActionToAJustUnlockedStep() {
		var first = step("first", QuestDelivery.AUTO, 1);
		var second = new QuestStep("second", List.of("first"), first.objectives(), List.of(), first.effects(),
				QuestDelivery.AUTO, null, null);
		var definition = definition(QuestRepeat.ONCE, 1, first, second);
		var state = engine.accept(definition, List.of(), "i1", now, host).instance();
		var next = event(definition, state, "e1", 1);
		assertEquals(Set.of("first"), next.instance().completed());
		assertFalse(next.instance().progress().containsKey("second"));
		assertEquals(COMPLETED, event(definition, next.instance(), "e2", 1).instance().status());
	}

	/** 放弃期间没有进度，普通支线重接恢复原实例；恢复前发生的事件即使迟到也不计入。 */
	@Test
	void resumesOrdinarySideQuestWithoutRecountingOldActions() {
		var definition = definition(QuestRepeat.ONCE, 1, step("kill", QuestDelivery.MANUAL, 3));
		var state = engine.accept(definition, List.of(), "i1", now, host).instance();
		state = event(definition, state, "e1", 1).instance();
		state = engine.abandon(definition, state, now).instance();
		assertEquals(PAUSED, state.status());
		state = engine.accept(definition, List.of(state), "unused", now.plusSeconds(1), host).instance();
		assertEquals("i1", state.id());
		var late = engine.record(definition, state, new QuestEvent("late", "KILL:WOLF", 2, now), now.plusSeconds(1),
				host);
		assertEquals(1L, late.instance().progress().get("kill").get("count"));
	}

	/** 每日凌晨按服务器时区过期，下一周期生成新实例；同周期达成待交付的任务保留资格。 */
	@Test
	void expiresUnfinishedDailyButPreservesReadyClaims() {
		var definition = definition(QuestRepeat.DAILY, 1, step("kill", QuestDelivery.MANUAL, 2));
		var state = engine.accept(definition, List.of(), "i1", now, host).instance();
		var tomorrow = now.plusSeconds(60);
		var expired = engine.refresh(definition, state, tomorrow, host).instance();
		assertEquals(EXPIRED, expired.status());
		assertEquals("i2", engine.accept(definition, List.of(expired), "i2", tomorrow, host).instance().id());
		var ready = event(definition, state, "e1", 2).instance();
		assertEquals(READY, ready.status());
		assertEquals(READY, engine.refresh(definition, ready, tomorrow, host).instance().status());
		assertEquals(COMPLETED, engine.deliver(definition, ready, "kill", tomorrow, host).instance().status());
	}

	/** 每周周一当地零时重置，完成次数在同一周期内约束，跨周恢复资格。 */
	@Test
	void weeklyQuotaUsesLocalMonday() {
		var definition = definition(QuestRepeat.WEEKLY, 1, step("kill", QuestDelivery.AUTO, 1));
		var state = event(definition, engine.accept(definition, List.of(), "i1", now, host).instance(), "e1", 1)
			.instance();
		assertEquals("2026-10-12", state.period());
		assertThrows(IllegalStateException.class, () -> engine.accept(definition, List.of(state), "i2", now, host));
		var sunday = Instant.parse("2026-10-18T15:59:59Z");
		assertEquals("2026-10-12", engine.period(definition, sunday));
		assertEquals("2026-10-19",
				engine.accept(definition, List.of(state), "i2", sunday.plusSeconds(1), host).instance().period());
	}

	/** 十次委托各发一次奖励，最后一次另发整环奖励，奖励来源按轮次区分。 */
	@Test
	void settlesTenCommissionsAndRingBonusExactlyOnce() {
		var definition = new QuestDefinition("q", false, true, QuestRepeat.RING, "Asia/Hong_Kong", 2, 10, null, true,
				List.of(step("kill", QuestDelivery.AUTO, 1)), List.of(new QuestEffect("GOLD", null, 100)));
		var state = engine.accept(definition, List.of(), "ring1", now, host).instance();
		for (int i = 1; i <= 10; i++) {
			var result = event(definition, state, "e" + i, 1);
			assertEquals(i == 10 ? 2 : 1, result.grants().size());
			assertEquals("ring1:" + i + ":kill:0", result.grants().getFirst().source());
			state = result.instance();
		}
		assertEquals(COMPLETED, state.status());
		assertEquals(11, state.receipts().size());
		assertTrue(event(definition, state, "e10", 1).grants().isEmpty());
	}

	/** 放弃环任务不返还日开启次数，重新接取清空本环并用新编号，不撤销已发奖励。 */
	@Test
	void abandonedRingConsumesOpeningQuota() {
		var definition = definition(QuestRepeat.RING, 1, step("kill", QuestDelivery.AUTO, 1));
		var state = event(definition, engine.accept(definition, List.of(), "r1", now, host).instance(), "e1", 1)
			.instance();
		assertEquals(2, state.round());
		var abandoned = engine.abandon(definition, state, now).instance();
		assertEquals(ABANDONED, abandoned.status());
		assertEquals(1, abandoned.receipts().size());
		assertThrows(IllegalStateException.class, () -> engine.accept(definition, List.of(abandoned), "r2", now, host));
		assertEquals(1,
				engine.accept(definition, List.of(abandoned), "r2", now.plusSeconds(86400), host).instance().round());
	}

	/** 接取限时恰好到截止点失败，下线没有特殊待遇；允许重试时创建全新实例。 */
	@Test
	void timeoutFailsAndRetryStartsClean() {
		var definition = new QuestDefinition("q", false, true, QuestRepeat.ONCE, "Asia/Hong_Kong", 1, 10,
				new QuestTimeout(60L, null), true, List.of(step("kill", QuestDelivery.MANUAL, 2)), List.of());
		var state = event(definition, engine.accept(definition, List.of(), "i1", now, host).instance(), "e1", 1)
			.instance();
		assertEquals(ACTIVE, engine.refresh(definition, state, now.plusSeconds(59), host).instance().status());
		var failed = engine.refresh(definition, state, now.plusSeconds(60), host).instance();
		assertEquals(FAILED, failed.status());
		var retry = engine.accept(definition, List.of(failed), "i2", now.plusSeconds(60), host).instance();
		assertTrue(retry.progress().isEmpty());
		assertEquals(now.plusSeconds(60), retry.acceptedAt());
	}

	/** 步骤计时从前置交付后开放时开始，而非整项任务接取时开始。 */
	@Test
	void stepDeadlineBeginsWhenStepOpens() {
		var first = step("first", QuestDelivery.MANUAL, 1);
		var second = new QuestStep("second", List.of("first"), first.objectives(), List.of(), List.of(),
				QuestDelivery.MANUAL, null, new QuestTimeout(30L, null));
		var definition = definition(QuestRepeat.ONCE, 1, first, second);
		var state = event(definition, engine.accept(definition, List.of(), "i1", now, host).instance(), "e1", 1)
			.instance();
		state = engine.deliver(definition, state, "first", now.plusSeconds(100), host).instance();
		assertEquals(now.plusSeconds(100), state.openedAt().get("second"));
		assertEquals(ACTIVE, engine.refresh(definition, state, now.plusSeconds(129), host).instance().status());
		assertEquals(FAILED, engine.refresh(definition, state, now.plusSeconds(130), host).instance().status());
	}

	/** 未选择路线不能交付，选后不可反悔；公共汇合步骤只等待所选路线。 */
	@Test
	void exclusiveBranchesJoinWithoutCompletingUnselectedRoute() {
		var a = new QuestStep("a", List.of(), List.of(), List.of(), List.of(), QuestDelivery.MANUAL,
				new QuestBranch("route", "A"), null);
		var b = new QuestStep("b", List.of(), List.of(), List.of(), List.of(), QuestDelivery.MANUAL,
				new QuestBranch("route", "B"), null);
		var join = new QuestStep("join", List.of("a", "b"), List.of(), List.of(),
				List.of(new QuestEffect("GOLD", null, 1)), QuestDelivery.AUTO, null, null);
		var definition = definition(QuestRepeat.ONCE, 1, a, b, join);
		var initial = engine.accept(definition, List.of(), "i1", now, host).instance();
		assertThrows(IllegalStateException.class, () -> engine.deliver(definition, initial, "a", now, host));
		var chosen = engine.choose(definition, initial, "route", "A", now, host).instance();
		assertThrows(IllegalStateException.class, () -> engine.choose(definition, chosen, "route", "B", now, host));
		var complete = engine.deliver(definition, chosen, "a", now, host);
		assertEquals(COMPLETED, complete.instance().status());
		assertEquals(Set.of("a", "join"), complete.instance().completed());
	}

	/** 即时持有量可以减少，累计获得目标不会减少；物品消耗由宿主效果执行而非读条件执行。 */
	@Test
	void holdingAndAcquisitionAreDistinct() {
		var step = new QuestStep("collect", List.of(),
				List.of(new QuestObjective("held", QuestMeasure.HOLDING, "ITEM:POTION", 2),
						new QuestObjective("gained", QuestMeasure.EVENT, "ITEM:POTION", 3)),
				List.of(), List.of(new QuestEffect("TAKE_ITEM", "POTION", 2)), QuestDelivery.MANUAL, null, null);
		var definition = definition(QuestRepeat.ONCE, 1, step);
		var state = engine.accept(definition, List.of(), "i1", now, host).instance();
		state = engine.record(definition, state, new QuestEvent("pick", "ITEM:POTION", 3, now), now, host).instance();
		var enough = new QuestHost() {
			public long value(QuestMeasure measure, String code) {
				return 2;
			}

			public boolean test(QuestCondition condition) {
				return true;
			}
		};
		assertEquals(READY, engine.refresh(definition, state, now, enough).instance().status());
		var finalState = state;
		assertThrows(IllegalStateException.class, () -> engine.deliver(definition, finalState, "collect", now, host));
		assertEquals("TAKE_ITEM",
				engine.deliver(definition, state, "collect", now, enough).grants().getFirst().effect().kind());
	}

	/** 错误配置在构造阶段拒绝，包括环、未知前置、互斥依赖和非法限时。 */
	@Test
	void rejectsInvalidDefinitions() {
		var a = new QuestStep("a", List.of("b"), List.of(), List.of(), List.of(), QuestDelivery.MANUAL, null, null);
		var b = new QuestStep("b", List.of("a"), List.of(), List.of(), List.of(), QuestDelivery.MANUAL, null, null);
		assertThrows(IllegalArgumentException.class, () -> definition(QuestRepeat.ONCE, 1, a, b));
		assertThrows(IllegalArgumentException.class, () -> definition(QuestRepeat.ONCE, 1, a));
		assertThrows(IllegalArgumentException.class, () -> new QuestTimeout(1L, now));
		assertThrows(IllegalArgumentException.class, () -> new QuestTimeout(0L, null));
	}

	/** 固定截止时间到点即失败；周期领奖保留规则不能绕过显式限时交付约束。 */
	@Test
	void fixedDeadlineAndReadyClaimHaveExplicitSemantics() {
		var definition = new QuestDefinition("q", false, true, QuestRepeat.ONCE, "Asia/Hong_Kong", 1, 10,
				new QuestTimeout(null, now.plusSeconds(30)), true, List.of(step("kill", QuestDelivery.MANUAL, 1)),
				List.of());
		var state = engine.accept(definition, List.of(), "i1", now, host).instance();
		assertEquals(FAILED, engine.refresh(definition, state, now.plusSeconds(30), host).instance().status());
		var ready = event(definition, state, "e1", 1).instance();
		assertEquals(FAILED, engine.refresh(definition, ready, now.plusSeconds(30), host).instance().status());
		assertThrows(IllegalStateException.class,
				() -> engine.deliver(definition, ready, "kill", now.plusSeconds(300), host));
	}

	/** 主线不可放弃，任务定义与动作入口都执行约束，不能依赖浏览器隐藏按钮。 */
	@Test
	void mainQuestCannotBeAbandoned() {
		var definition = new QuestDefinition("q", true, false, QuestRepeat.ONCE, "Asia/Hong_Kong", 1, 10, null, false,
				List.of(step("kill", QuestDelivery.MANUAL, 1)), List.of());
		var state = engine.accept(definition, List.of(), "i1", now, host).instance();
		assertThrows(IllegalStateException.class, () -> engine.abandon(definition, state, now));
		assertThrows(IllegalArgumentException.class, () -> new QuestDefinition("q", true, true, QuestRepeat.ONCE,
				"Asia/Hong_Kong", 1, 10, null, false, definition.steps(), List.of()));
	}

	/** 目标数量调整让旧READY不再达成时，跨周期资格随之失效，而非延迟到第二次命令才过期。 */
	@Test
	void changedGoalImmediatelyExpiresInvalidReadyClaim() {
		var before = definition(QuestRepeat.DAILY, 1, step("kill", QuestDelivery.MANUAL, 1));
		var after = definition(QuestRepeat.DAILY, 1, step("kill", QuestDelivery.MANUAL, 2));
		var ready = event(before, engine.accept(before, List.of(), "i1", now, host).instance(), "e1", 1).instance();
		assertEquals(EXPIRED, engine.inspect(after, ready, now.plusSeconds(60), host).status());
		assertEquals(EXPIRED, engine.refresh(after, ready, now.plusSeconds(60), host).instance().status());
	}

	/** 宿主开放条件未满足时，不开放秒表也不累计行为；满足后只计算新的真实事件。 */
	@Test
	void activationConditionsGateCountingAndStepTimers() {
		var base = step("kill", QuestDelivery.MANUAL, 1);
		var guarded = new QuestStep(base.id(), base.requires(), base.objectives(), List.of(), base.effects(),
				base.delivery(), null, new QuestTimeout(30L, null),
				List.of(new QuestCondition("FACT", "OTHER_QUEST", 0)));
		var definition = definition(QuestRepeat.ONCE, 1, guarded);
		var closed = new QuestHost() {
			public long value(QuestMeasure measure, String code) {
				return 0;
			}

			public boolean test(QuestCondition condition) {
				return false;
			}
		};
		var state = engine.accept(definition, List.of(), "i1", now, closed).instance();
		state = engine.record(definition, state, new QuestEvent("old", "KILL:WOLF", 1, now), now, closed).instance();
		assertTrue(state.progress().isEmpty());
		assertTrue(state.openedAt().isEmpty());
		state = engine.refresh(definition, state, now.plusSeconds(100), host).instance();
		assertEquals(now.plusSeconds(100), state.openedAt().get("kill"));
		var finished = engine
			.record(definition, state, new QuestEvent("new", "KILL:WOLF", 1, now.plusSeconds(101)),
					now.plusSeconds(101), host)
			.instance();
		assertEquals(READY, finished.status());
	}

	/** 构造测试任务时显式给出所有生命周期参数，避免隐藏默认值影响时间相关断言。 */
	private QuestDefinition definition(QuestRepeat repeat, int limit, QuestStep... steps) {
		return new QuestDefinition("q", false, true, repeat, "Asia/Hong_Kong", limit, 10, null, true, List.of(steps),
				List.of());
	}

	private QuestStep step(String id, QuestDelivery delivery, int count) {
		return new QuestStep(id, List.of(),
				List.of(new QuestObjective("count", QuestMeasure.EVENT, "KILL:WOLF", count)), List.of(),
				List.of(new QuestEffect("GOLD", null, 10)), delivery, null, null);
	}

	private QuestTransition event(QuestDefinition definition, QuestInstance instance, String id, long amount) {
		return engine.record(definition, instance, new QuestEvent(id, "KILL:WOLF", amount, now), now, host);
	}

}
