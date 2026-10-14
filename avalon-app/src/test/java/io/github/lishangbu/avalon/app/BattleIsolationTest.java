package io.github.lishangbu.avalon.app;

import java.util.ArrayList;

import io.github.lishangbu.avalon.battle.domain.BattleResourceSnapshot;
import io.github.lishangbu.avalon.battle.domain.BattleState;
import io.github.lishangbu.avalon.battle.domain.BattleStateOrder;
import io.github.lishangbu.avalon.battle.domain.BattleStateStats;
import io.github.lishangbu.avalon.battle.domain.BattleStateUnit;
import io.github.lishangbu.avalon.platform.BusinessException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;

/** 使用正式技能和战局验证纯引擎边界，防止只在核心替身上成立的状态隔离。 */
class BattleIsolationTest {

	/** 成功计算不改变原战局或库存；相同输入得到逐项相同事件、抽样和状态。 */
	@Test
	void successfulRoundIsPureAndDeterministic() {
		var fixture = new RuleTestFixture();
		var state = fixture.training.createAcademy("BQ020", "MAGE", List.of(), 71L);
		var json = JsonMapper.builder().build();
		String before = json.writeValueAsString(state);
		var orders = List.of(new BattleStateOrder("trainee", "DEFEND", "trainee"),
				new BattleStateOrder("T_CAT", "水弹", "academy-target"),
				new BattleStateOrder("T_STONE", "DEFEND", "T_STONE"));
		var snapshot = new BattleResourceSnapshot(Map.of(), Set.of());
		var first = fixture.engine.advance(state, orders, false, snapshot, Map.of());
		var second = fixture.engine.advance(state, orders, false, snapshot, Map.of());
		assertEquals(before, json.writeValueAsString(state));
		assertNotEquals(before, json.writeValueAsString(first.state()));
		assertEquals(json.writeValueAsString(first.state()), json.writeValueAsString(second.state()));
		assertEquals(first.events(), second.events());
		assertEquals(first.changes(), second.changes());
		assertTrue(first.events().stream().anyMatch(event -> event.kind().equals("RANDOM")));
	}

	/** 预览补位随后指令校验失败，正式状态的位置、入场标记和日志必须保持原样。 */
	@Test
	void rejectedCommandDoesNotApplyPreviewReplacement() {
		var fixture = new RuleTestFixture();
		var state = new BattleState();
		state.setEncounter("WOODS");
		var hero = unit("hero", true, false, 0, 100);
		var fallen = unit("fallen", false, false, 0, 0);
		var reserve = unit("reserve", false, false, 2, 100);
		var enemy = unit("enemy", false, true, 0, 100);
		state.setUnits(new ArrayList<>(List.of(hero, fallen, reserve, enemy)));
		var json = JsonMapper.builder().build();
		String before = json.writeValueAsString(state);
		assertThrows(BusinessException.class,
				() -> fixture.engine.advance(state,
						List.of(new BattleStateOrder("hero", "UNKNOWN_SKILL", "enemy"),
								new BattleStateOrder("reserve", "ATTACK", "enemy")),
						false, new BattleResourceSnapshot(Map.of(), Set.of()), Map.of("reserve", 0)));
		assertEquals(before, json.writeValueAsString(state));
		assertEquals(2, reserve.getPosition());
		assertFalse(reserve.isEntered());
	}

	/** 最小可用单位用于校验失败路径，属性足以排除数据缺失引起的无关异常。 */
	private BattleStateUnit unit(String id, boolean hero, boolean enemy, int position, int hp) {
		var unit = new BattleStateUnit();
		unit.setId(id);
		unit.setName(id);
		unit.setHero(hero);
		unit.setEnemy(enemy);
		unit.setOwnerHero("hero");
		unit.setPosition(position);
		unit.setHp(hp);
		unit.setMp(100);
		unit.setStats(new BattleStateStats(100, 100, 10, 10, 10, 10, 10));
		return unit;
	}

}
