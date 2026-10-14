package io.github.lishangbu.avalon.combat;

import io.github.lishangbu.avalon.combat.model.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** 在无Spring、数据库与游戏目录的环境下验证真实核心边界，而不是复制宿主规则实现。 */
class CombatRoundEngineTest {

	/** 可变测试状态用来检查复制与异常隔离，单位游戏公式不属于此测试的职责。 */
	private static final class State {

		int value;

		boolean terminal;

	}

	/** 最小纯规则宿主：各阶段记录顺序，并允许指定阶段产生终局。 */
	private CombatRoundRules<State, String> rules(List<CombatPhase> visited, CombatPhase stop) {
		return new CombatRoundRules<>() {
			public State copy(State source) {
				var copy = new State();
				copy.value = source.value;
				copy.terminal = source.terminal;
				return copy;
			}

			public int round(State state) {
				return 1;
			}

			public boolean terminal(State state) {
				return state.terminal;
			}

			public void apply(CombatPhase phase, CombatFrame<State, String> frame) {
				visited.add(phase);
				frame.state().value++;
				if (phase == stop)
					frame.state().terminal = true;
			}
		};
	}

	/** 核心固定阶段只执行一次，成功输出不复用调用方状态。 */
	@Test
	void stagesAreFixedAndInputIsIsolated() {
		var visited = new ArrayList<CombatPhase>();
		var input = new State();
		var result = new CombatRoundEngine().resolve(input, "orders", rules(visited, null), CombatLimits.defaults());
		assertEquals(List.of(CombatPhase.values()), visited);
		assertEquals(0, input.value);
		assertEquals(9, result.state().value);
		assertEquals(9, result.events().size());
		assertThrows(UnsupportedOperationException.class, () -> result.events().clear());
	}

	/** 行动阶段出现终局时不得追加中毒、环境或治疗跳点，但仍需执行一次收尾。 */
	@Test
	void terminalSkipsRoundEndButClosesOnce() {
		var visited = new ArrayList<CombatPhase>();
		new CombatRoundEngine().resolve(new State(), "orders", rules(visited, CombatPhase.ACTIONS),
				CombatLimits.defaults());
		assertFalse(visited.contains(CombatPhase.END));
		assertEquals(CombatPhase.CLOSE, visited.getLast());
		assertEquals(1, Collections.frequency(visited, CombatPhase.CLOSE));
	}

	/** 事件预算耗尽后不返回半轮结果，调用方原状态保持完整。 */
	@Test
	void eventBudgetFailureDoesNotModifyOriginal() {
		var input = new State();
		var visited = new ArrayList<CombatPhase>();
		assertThrows(IllegalStateException.class,
				() -> new CombatRoundEngine().resolve(input, "orders", rules(visited, null), new CombatLimits(2, 8)));
		assertEquals(0, input.value);
		assertEquals(2, visited.size());
	}

	/** 底层抽样记录包含shuffle等JDK内部抽样，同种子重试既保持结果也保持事件序列。 */
	@Test
	void trackedRandomMatchesJdkAndReplaysEveryDraw() {
		List<CombatEvent> first = null;
		for (int replay = 0; replay < 2; replay++) {
			var frame = new CombatFrame<>(new State(), "orders", CombatLimits.defaults());
			frame.phase(CombatPhase.OPEN);
			var actual = frame.random("round", 581L);
			var expected = new Random(581L);
			for (int i = 0; i < 100; i++) {
				assertEquals(expected.nextInt(17), actual.nextInt(17));
				assertEquals(expected.nextDouble(), actual.nextDouble());
			}
			var left = new ArrayList<>(List.of(1, 2, 3, 4, 5));
			var right = new ArrayList<>(left);
			Collections.shuffle(left, actual);
			Collections.shuffle(right, expected);
			assertEquals(right, left);
			if (first == null)
				first = frame.events();
			else
				assertEquals(first, frame.events());
			assertThrows(IllegalStateException.class, () -> frame.random("round", 582L));
		}
	}

	/** 反应递归无法绕过深度预算；展开失败后依然丢弃本回合而不继续结算。 */
	@Test
	void recursiveTriggersHaveFiniteDepth() {
		var frame = new CombatFrame<>(new State(), "orders", new CombatLimits(100, 2));
		for (var phase : CombatPhase.values()) {
			frame.phase(phase);
			if (phase == CombatPhase.ACTIONS)
				break;
		}
		assertThrows(IllegalStateException.class,
				() -> frame.trigger("one", () -> frame.trigger("two", () -> frame.trigger("three", () -> {
				}))));
		assertEquals(2, frame.events().size());
		assertDoesNotThrow(() -> frame.trigger("after", () -> {
		}));
	}

	/** 资源数量取自快照，重复使用合并消耗；不需要或允许连接真实资产服务。 */
	@Test
	void resourcesReserveAgainstCopiedSnapshot() {
		var items = new HashMap<>(Map.of("POTION", 3L));
		var resources = new CombatResources(Map.of("hero", items));
		items.put("POTION", 100L);
		resources.consume("hero", "POTION", 2);
		resources.consume("hero", "POTION", 1);
		assertEquals(0, resources.count("hero", "POTION"));
		assertEquals(2, resources.changes().size());
		assertThrows(IllegalStateException.class, () -> resources.consume("hero", "POTION", 1));
		assertEquals(100, items.get("POTION"));
	}

	/** 注册顺序不影响技能配置顺序，未知效果与重复类型均明确拒绝而不降级。 */
	@Test
	void effectRegistryUsesDeclaredOrderAndRejectsUnknownOrDuplicate() {
		var output = new ArrayList<String>();
		var one = handler("ONE");
		var two = handler("TWO");
		var registry = new CombatEffects<>(List.of(two, one), String::toString);
		var frame = new CombatFrame<>(new State(), "orders", CombatLimits.defaults());
		for (var phase : CombatPhase.values()) {
			frame.phase(phase);
			if (phase == CombatPhase.ACTIONS)
				break;
		}
		registry.apply(List.of("ONE", "TWO", "ONE"), output, frame);
		assertEquals(List.of("ONE", "TWO", "ONE"), output);
		assertThrows(IllegalArgumentException.class, () -> registry.validate(List.of("UNKNOWN")));
		assertThrows(IllegalArgumentException.class, () -> new CombatEffects<>(List.of(one, one), String::toString));
	}

	/** 无共享状态的效果替身，验证扩展契约而不引入特定游戏技能或数值。 */
	private CombatEffectHandler<String, List<String>> handler(String type) {
		return new CombatEffectHandler<>() {
			public String kind() {
				return type;
			}

			public void validate(String effect) {
				assertEquals(type, effect);
			}

			public void apply(String effect, List<String> context) {
				context.add(effect);
			}
		};
	}

}
