package io.github.lishangbu.avalon.battle.domain;

import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * 獠牙兽王的确定性脚本。预告消耗整次首领行动，点名保存个体而不是动态生命排序。 本类只选择行动及推进阶段，不计算伤害、不发奖、不访问数据库。
 */
@Component
public class CaveBossPolicy {

	private static final List<String> FIRST = List.of("BOSS_ATTACK", "WARN_HEAVY", "BOSS_HEAVY");

	private static final List<String> SECOND = List.of("BOSS_ATTACK", "WARN_HEAVY", "BOSS_HEAVY", "BOSS_ATTACK",
			"WARN_QUAKE", "BOSS_QUAKE");

	/**
	 * 在玩家提交前固定本轮行动。普通攻击按人物、第一宠、第二宠轮转，重击不改变该游标。
	 */
	public BattleState.Order plan(BattleState state, BattleState.Unit boss) {
		var targets = state.getUnits()
			.stream()
			.filter(unit -> !unit.isEnemy() && unit.alive() && unit.active())
			.sorted(Comparator.comparingInt(this::position))
			.toList();
		String action = action(state);
		String target = boss.getId();
		if (!targets.isEmpty()) {
			if (action.equals("BOSS_ATTACK")) {
				var selected = targets.stream()
					.filter(unit -> position(unit) >= state.getBasicTargetCursor())
					.findFirst()
					.orElse(targets.getFirst());
				target = selected.getId();
				state.setTelegraph("普通攻击：" + selected.getName() + "，无属性物理，威力50。");
			}
			else if (action.equals("BOSS_HEAVY")) {
				var selected = targets.stream()
					.filter(unit -> unit.getId().equals(state.getMarkedTarget()))
					.findFirst()
					.orElse(targets.getFirst());
				target = selected.getId();
				state.setMarkedTarget(target);
				state.setTelegraph("重击即将命中" + selected.getName() + "：无属性物理，威力110。");
			}
		}
		if (action.equals("BOSS_QUAKE"))
			state.setTelegraph("全体震击即将发动：全体出战成员，无属性法术，威力70。");
		if (action.startsWith("WARN_"))
			state.setTelegraph("首领本轮蓄势，不造成伤害。");
		return new BattleState.Order(boss.getId(), action, target);
	}

	/**
	 * 预告在首领实际行动时执行，生命比例按此刻取值；玩家下一轮才面对伤害。
	 */
	public void warn(BattleState state, String action) {
		if (action.equals("WARN_HEAVY")) {
			var target = state.getUnits()
				.stream()
				.filter(unit -> !unit.isEnemy() && unit.alive() && unit.active())
				.min(Comparator
					.comparingDouble((BattleState.Unit unit) -> (double) unit.getHp() / unit.getStats().health())
					.thenComparingInt(this::position));
			target.ifPresent(unit -> {
				state.setMarkedTarget(unit.getId());
				state.getLog().add("獠牙兽王锁定" + unit.getName() + "，下一回合发动重击。");
			});
		}
		else
			state.getLog().add("獠牙兽王积蓄力量，下一回合发动全体震击。");
	}

	/**
	 * 回合末推进；第一阶段必须实际执行重击，且半血后才切换。第二阶段先完成单独的震击二连。
	 */
	public void advance(BattleState state, BattleState.Unit boss, String executed) {
		if (executed.equals("BOSS_ATTACK")) {
			var order = state.getEnemyOrders().stream().filter(value -> value.actor().equals(boss.getId())).findFirst();
			order.flatMap(
					value -> state.getUnits().stream().filter(unit -> unit.getId().equals(value.target())).findFirst())
				.ifPresent(unit -> state.setBasicTargetCursor((position(unit) + 1) % 3));
		}
		if (executed.equals("BOSS_HEAVY"))
			state.setHeavyExecuted(true);
		// 预告回合即使跌破半血也不能切阶段，否则会吞掉下一回合的原定重击。
		if (state.getBossPhase() == 1 && state.isHeavyExecuted() && boss.getHp() * 2 <= boss.getStats().health()
				&& !executed.startsWith("WARN_")) {
			state.setBossPhase(2);
			state.setBossCursor(-2);
			state.getLog().add("獠牙兽王进入第二阶段。");
		}
		else {
			int cursor = state.getBossCursor() + 1;
			state.setBossCursor(cursor < 0 ? cursor : cursor % (state.getBossPhase() == 1 ? 3 : 6));
		}
	}

	private String action(BattleState state) {
		if (state.getBossPhase() == 1)
			return FIRST.get(state.getBossCursor());
		if (state.getBossCursor() == -2)
			return "WARN_QUAKE";
		if (state.getBossCursor() == -1)
			return "BOSS_QUAKE";
		return SECOND.get(state.getBossCursor());
	}

	private int position(BattleState.Unit unit) {
		return unit.isHero() ? 0 : unit.getPosition() + 1;
	}

}
