package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.battle.domain.BattleEnvironment;
import io.github.lishangbu.avalon.battle.domain.DefensiveAbilityPolicy;
import io.github.lishangbu.avalon.battle.domain.BattleState;
import io.github.lishangbu.avalon.battle.domain.BattleState.Stats;
import io.github.lishangbu.avalon.battle.domain.BattleState.Unit;
import io.github.lishangbu.avalon.content.battle.BattleCatalog;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BattleEnvironmentTest {

	@Test
	void slowerSetterOverridesAndReadingAgainCannotRefreshWeather() {
		var state = new BattleState();
		var sun = unit("sun", "日照", 100);
		var rain = unit("rain", "降雨", 50);
		state.setUnits(List.of(sun, rain));
		BattleEnvironment.enter(state);
		assertEquals("RAIN", state.getWeather());
		assertEquals(5, state.getWeatherUntil());
		state.setRound(3);
		BattleEnvironment.enter(state);
		assertEquals(5, state.getWeatherUntil());
		var swimmer = unit("swimmer", "悠游自如", 100);
		assertEquals(1.3, BattleEnvironment.speedMultiplier(state, swimmer));
		state.setRound(6);
		assertEquals(1, BattleEnvironment.speedMultiplier(state, swimmer));
	}

	@Test
	void terrainAndWeatherCoexistAndExpireAfterFiveRounds() {
		var state = new BattleState();
		state.setUnits(List.of(unit("sun", "日照", 100), unit("electric", "电气制造", 50)));
		BattleEnvironment.enter(state);
		var thunder = new BattleCatalog.Skill("雷牙", "DAMAGE", "雷", "PA", "PD", 100, 0, 0, 0, "ENEMY");
		var water = new BattleCatalog.Skill("水弹", "DAMAGE", "水", "MA", "MD", 100, 0, 0, 0, "ENEMY");
		state.setRound(5);
		assertEquals(1.15, BattleEnvironment.damageMultiplier(state, thunder));
		assertEquals(.8, BattleEnvironment.damageMultiplier(state, water));
		state.setRound(6);
		assertEquals(1, BattleEnvironment.damageMultiplier(state, thunder));
		assertEquals(1, BattleEnvironment.damageMultiplier(state, water));
	}

	@Test
	void sandUsesPersistedSourceLevelAndDoesNotHitReservesOrEarth() {
		var state = new BattleState();
		var sand = unit("sand", "扬沙", 100);
		sand.setElement("地");
		sand.setLevel(2);
		var enemy = unit("enemy", null, 50);
		enemy.setEnemy(true);
		var reserve = unit("reserve", null, 30);
		reserve.setPosition(2);
		state.setUnits(List.of(sand, enemy, reserve));
		BattleEnvironment.enter(state);
		sand.setPosition(3);
		BattleEnvironment.finish(state);
		assertEquals(316, enemy.getHp());
		assertEquals(320, reserve.getHp());
		assertEquals(320, sand.getHp());
	}

	@Test
	void traceKeepsNativeIdentityAndDoesNotRerollOnRefresh() {
		var state = new BattleState();
		var tracer = unit("trace", "复制", 100);
		var rain = unit("rain", "降雨", 50);
		rain.setEnemy(true);
		state.setUnits(List.of(tracer, rain));
		BattleEnvironment.enter(state);
		assertEquals("复制", tracer.getAbility());
		assertEquals("降雨", tracer.effect());
		state.setRound(3);
		BattleEnvironment.enter(state);
		assertEquals(5, state.getWeatherUntil());
		DefensiveAbilityPolicy.leave(tracer);
		assertEquals("复制", tracer.effect());
		assertEquals("RAIN", state.getWeather(), "离场不撤销已有天气");
	}

	private Unit unit(String id, String ability, int speed) {
		var unit = new Unit();
		unit.setId(id);
		unit.setName(id);
		unit.setAbility(ability);
		unit.setStats(new Stats(320, 20, 10, 10, 10, 10, speed));
		unit.setHp(320);
		return unit;
	}

}
