package io.github.lishangbu.avalon.battle.domain;

import io.github.lishangbu.avalon.content.battle.model.BattleSkillEffect;
import io.github.lishangbu.avalon.content.battle.BattleCatalog;
import io.github.lishangbu.avalon.content.battle.model.BattleSkill;
import java.util.List;
import java.util.Random;
import java.util.function.LongFunction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 单人洞穴首领策略；预告、目标轮转和阶段游标均存放在首领私有机制数据中。 */
@Component
@RequiredArgsConstructor
public class CaveEnemyPolicy implements BattleEnemyPolicy {

	/** 只读遭遇配置确认该首领使用洞穴策略。 */
	private final BattleCatalog catalog;

	/** 预告及阶段游标规则。 */
	private final CaveBossPolicy boss;

	/** 共用直接攻击规则，不复制伤害和保护计算。 */
	private final DirectAttackResolver attacks;

	public String kind() {
		return "CAVE";
	}

	public boolean supports(BattleState state) {
		return state.getTraining() == null && state.getDungeonStage() == null
				&& catalog.encounter(state.getEncounter()).boss() != null;
	}

	public void plan(BattleState state, LongFunction<Random> randoms) {
		var unit = state.getUnits().stream().filter(BattleStateUnit::isEnemy).findFirst().orElseThrow();
		state.setEnemyOrders(List.of(boss.plan(state, unit)));
	}

	/** 已预告攻击使用原有数值；警告动作只更新公开预告，不产生直接伤害。 */
	public boolean execute(BattleEnemyAction action) {
		var state = action.state();
		var code = action.order().action();
		if (code.startsWith("WARN_")) {
			boss.warn(state, code);
			return true;
		}
		boolean area = code.equals("BOSS_QUAKE");
		var target = action.target();
		var targets = area
				? state.getUnits().stream().filter(unit -> !unit.isEnemy() && unit.active() && unit.alive()).toList()
				: target != null && target.alive() && target.active() ? List.of(target) : List.<BattleStateUnit>of();
		var attack = new BattleSkill(area ? "全体震击" : code.equals("BOSS_HEAVY") ? "重击" : "普通攻击",
				List.of(new BattleSkillEffect("DAMAGE", area ? 70 : code.equals("BOSS_HEAVY") ? 110 : 50)), "无",
				area ? "MA" : "PA", area ? "MD" : "PD", 0, 0, 0, "ENEMY", null, null);
		attacks.apply(state, action.actor(), targets, attack, area, .9375 + action.random().nextDouble() * .125,
				action.defending(), action.covers());
		return true;
	}

	/** 当前预告执行完毕且战局仍活动时，才推进首领游标。 */
	public void finish(BattleState state) {
		if (state.getStatus().equals("ACTIVE")) {
			var unit = state.getUnits().stream().filter(BattleStateUnit::isEnemy).findFirst().orElseThrow();
			boss.advance(state, unit, state.getEnemyOrders().getFirst().action());
		}
	}

	/** 单人首领没有额外终局资格或附属机制。 */
	public void close(BattleState state, boolean surrender) {
	}

}
