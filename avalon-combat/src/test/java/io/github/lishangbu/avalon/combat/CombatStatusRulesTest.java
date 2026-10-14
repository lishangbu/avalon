package io.github.lishangbu.avalon.combat;

import io.github.lishangbu.avalon.combat.model.CombatStatus;
import io.github.lishangbu.avalon.combat.model.CombatStatusStacking;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** 验证公共状态叠加及期限边界，不复制具体技能公式。 */
class CombatStatusRulesTest {

	/** 相同强度允许刷新期限，较弱实例不覆盖来源和期限，过期当轮仍生效。 */
	@Test
	void strongestRefreshesEqualButNeverWeakensAndExpiresAfterLastRound() {
		var rules = new CombatStatusRules();
		var states = new HashMap<String, CombatStatus>();
		rules.install(states, "POISON", status("a", 10, 4), CombatStatusStacking.STRONGEST, 100);
		rules.install(states, "POISON", status("b", 5, 8), CombatStatusStacking.STRONGEST, 100);
		assertEquals("a", states.get("POISON").source());
		rules.install(states, "POISON", status("b", 10, 6), CombatStatusStacking.STRONGEST, 100);
		assertEquals("b", states.get("POISON").source());
		rules.expire(states, 6);
		assertFalse(states.isEmpty());
		rules.expire(states, 7);
		assertTrue(states.isEmpty());
	}

	/** 累加限制与替换独立，永久机制不会按回合自动删除，显式移除不影响其他实例。 */
	@Test
	void additionIsBoundedAndPermanentStatusRequiresExplicitRemoval() {
		var rules = new CombatStatusRules();
		var states = new HashMap<String, CombatStatus>();
		rules.install(states, "POISON", status("a", 8, 0), CombatStatusStacking.ADD, 10);
		rules.install(states, "POISON", status("a", 8, 0), CombatStatusStacking.ADD, 10);
		assertEquals(10, states.get("POISON").strength());
		rules.expire(states, 1000);
		assertFalse(states.isEmpty());
		rules.install(states, "POISON", status("b", 1, 0), CombatStatusStacking.REPLACE, 10);
		assertEquals(1, states.get("POISON").strength());
		rules.remove(states, "POISON");
		assertTrue(states.isEmpty());
	}

	/** 类型冲突和数值溢出均拒绝，失败不会覆盖已经生效的实例。 */
	@Test
	void invalidReplacementAndOverflowLeavePriorStatusIntact() {
		var rules = new CombatStatusRules();
		var states = new HashMap<String, CombatStatus>();
		var prior = status("a", Long.MAX_VALUE, 4);
		rules.install(states, "POISON", prior, CombatStatusStacking.REPLACE, Long.MAX_VALUE);
		assertThrows(ArithmeticException.class,
				() -> rules.install(states, "POISON", status("b", 1, 4), CombatStatusStacking.ADD, Long.MAX_VALUE));
		assertSame(prior, states.get("POISON"));
		var other = new CombatStatus("SLOW", "b", 1, 4, 1, Map.of(), Map.of());
		assertThrows(IllegalArgumentException.class,
				() -> rules.install(states, "POISON", other, CombatStatusStacking.REPLACE, 100));
		assertSame(prior, states.get("POISON"));
	}

	/** 构造可比较实例，来源不同用于核验刷新是否确实采用新输入。 */
	private CombatStatus status(String source, long strength, int until) {
		return new CombatStatus("POISON", source, 1, until, strength, Map.of(), Map.of());
	}

}
