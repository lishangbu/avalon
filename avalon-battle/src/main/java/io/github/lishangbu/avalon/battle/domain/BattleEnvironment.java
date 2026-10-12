package io.github.lishangbu.avalon.battle.domain;

import io.github.lishangbu.avalon.content.battle.model.BattleSkill;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/** 环境属于整场战斗，随服务器战局持久化；重连不制造入场，不刷新期限。 */
public final class BattleEnvironment {

	private BattleEnvironment() {
	}

	public static void enter(BattleState state) {
		List<BattleStateUnit> entered = state.getUnits()
			.stream()
			.filter(unit -> unit.active() && unit.alive() && !unit.isEntered())
			.sorted(Comparator.comparingDouble((BattleStateUnit unit) -> entrySpeed(state, unit))
				.reversed()
				.thenComparing(BattleStateUnit::isEnemy)
				.thenComparing(unit -> unit.getOwnerHero() == null ? "" : unit.getOwnerHero())
				.thenComparingInt(BattleStateUnit::getPosition)
				.thenComparing(BattleStateUnit::getId))
			.toList();
		for (var unit : entered) {
			unit.setEntered(true);
			if (!"复制".equals(unit.getAbility()))
				applyEntry(state, unit);
		}
		// 先保存所有复制结果，再执行入场效果，避免抽选受遍历顺序影响。
		for (var unit : entered) {
			if (!"复制".equals(unit.getAbility()))
				continue;
			var candidates = state.getUnits()
				.stream()
				.filter(target -> target.isEnemy() != unit.isEnemy() && target.alive() && target.active()
						&& !target.isHero() && !target.isMechanism() && target.getAbility() != null
						&& !"复制".equals(target.getAbility()) && !"化学变化气体".equals(target.getAbility()))
				.sorted(Comparator.comparing(BattleStateUnit::getId))
				.toList();
			if (!candidates.isEmpty()) {
				var random = new Random(state.getSeed() ^ ((long) state.getRound() << 32) ^ unit.getId().hashCode());
				unit.setCopiedAbility(candidates.get(random.nextInt(candidates.size())).effect());
				state.getLog().add(unit.getName() + "复制了" + unit.effect() + "。");
			}
		}
		for (var unit : entered.stream()
			.filter(unit -> unit.getCopiedAbility() != null)
			.sorted(Comparator.comparing(BattleStateUnit::isEnemy)
				.thenComparing(unit -> unit.getOwnerHero() == null ? "" : unit.getOwnerHero())
				.thenComparingInt(BattleStateUnit::getPosition)
				.thenComparing(BattleStateUnit::getId))
			.toList())
			applyEntry(state, unit);
	}

	private static void applyEntry(BattleState state, BattleStateUnit unit) {
		String weather = switch (unit.effect() == null ? "" : unit.effect()) {
			case "日照" -> "SUN";
			case "降雨" -> "RAIN";
			case "扬沙" -> "SAND";
			default -> null;
		};
		if (weather != null) {
			state.setWeather(weather);
			state.setWeatherUntil(state.getRound() + 4);
			state.setWeatherSourceLevel(unit.getLevel());
			state.getLog().add(unit.getName() + "发动" + unit.effect() + "，天气持续至第" + state.getWeatherUntil() + "回合末。");
		}
		if ("电气制造".equals(unit.effect())) {
			state.setElectricUntil(state.getRound() + 4);
			state.getLog().add(unit.getName() + "展开电气场地，持续至第" + state.getElectricUntil() + "回合末。");
		}
	}

	private static double entrySpeed(BattleState state, BattleStateUnit unit) {
		int base = unit.getStats().speed();
		if ("轻盈".equals(unit.effect()))
			base = (int) Math.floor(base * 1.1);
		return base * speedMultiplier(state, unit) * (unit.getSlowUntil() >= state.getRound() ? .8 : 1);
	}

	public static double speedMultiplier(BattleState state, BattleStateUnit unit) {
		if (state.getWeatherUntil() < state.getRound())
			return 1;
		return ("SUN".equals(state.getWeather()) && "叶绿素".equals(unit.effect()))
				|| ("RAIN".equals(state.getWeather()) && "悠游自如".equals(unit.effect())) ? 1.3 : 1;
	}

	public static double damageMultiplier(BattleState state, BattleSkill skill) {
		double multiplier = state.getElectricUntil() >= state.getRound() && "雷".equals(skill.element()) ? 1.15 : 1;
		if (state.getWeatherUntil() >= state.getRound()) {
			if ("SUN".equals(state.getWeather()))
				multiplier *= "火".equals(skill.element()) ? 1.2 : "水".equals(skill.element()) ? .8 : 1;
			if ("RAIN".equals(state.getWeather()))
				multiplier *= "水".equals(skill.element()) ? 1.2 : "火".equals(skill.element()) ? .8 : 1;
		}
		return multiplier;
	}

	public static void finish(BattleState state) {
		if (!state.getStatus().equals("ACTIVE"))
			return;
		if (state.getWeatherUntil() >= state.getRound() && "SAND".equals(state.getWeather())) {
			// 同一跳同时确定所有受害者，来源倒下或离场不会提前停止天气。
			for (var unit : state.getUnits()) {
				if (!unit.alive() || !unit.active() || "地".equals(unit.getElement()) || unit.isMechanism())
					continue;
				int loss = Math.min(unit.getStats().health() / 32, state.getWeatherSourceLevel() * 2);
				unit.setHp(Math.max(0, unit.getHp() - loss));
				state.getLog().add("沙暴使" + unit.getName() + "损失" + loss + "生命。");
				if (!unit.alive())
					DefensiveAbilityPolicy.leave(unit);
			}
			boolean friends = state.getUnits().stream().anyMatch(unit -> !unit.isEnemy() && unit.alive());
			boolean enemies = state.getUnits()
				.stream()
				.anyMatch(unit -> unit.isEnemy() && !unit.isMechanism() && unit.alive());
			if (!friends)
				state.setStatus("LOST");
			else if (!enemies)
				state.setStatus("WON");
		}
	}

}
