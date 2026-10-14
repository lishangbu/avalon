package io.github.lishangbu.avalon.battle.domain;

import io.github.lishangbu.avalon.content.battle.model.BattleSkillEffect;
import io.github.lishangbu.avalon.content.battle.AcademyTrainingCatalog;
import io.github.lishangbu.avalon.content.battle.TrainingCatalog;
import io.github.lishangbu.avalon.content.battle.model.BattleSkill;
import java.util.List;
import java.util.Random;
import java.util.function.LongFunction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 学院训练敌方策略；仅修改隔离的训练战局，不操作真实角色和幻兽资产。 */
@Component
@RequiredArgsConstructor
public class TrainingEnemyPolicy implements BattleEnemyPolicy {

	/** 课程资格与意图规则。 */
	private final TrainingPolicy training;

	/** 职业课程的固定攻击参数。 */
	private final TrainingCatalog catalog;

	/** 学院课程的固定攻击参数。 */
	private final AcademyTrainingCatalog academy;

	/** 所有直接攻击共用的数值及保护链。 */
	private final DirectAttackResolver attacks;

	public String kind() {
		return "TRAINING";
	}

	public boolean supports(BattleState state) {
		return state.getTraining() != null;
	}

	public void plan(BattleState state, LongFunction<Random> randoms) {
		training.plan(state, randoms.apply(state.getSeed() ^ (state.getRound() * 7919L)));
	}

	/** 训练脚本仅接管TRAINING_HIT，捕获训练中的普通攻击仍由通用技能规则执行。 */
	public boolean execute(BattleEnemyAction action) {
		if (!action.order().action().equals("TRAINING_HIT"))
			return false;
		var state = action.state();
		var course = state.getTraining().getAcademyCourse();
		int power = course != null && !course.equals("BQ023") ? academy.course(course).power()
				: catalog.course(state.getTraining().getProfession()).opponent().power();
		var attack = new BattleSkill("训练攻击", List.of(new BattleSkillEffect("DAMAGE", power)), "无", "PA", "PD", 0, 0, 0,
				"ENEMY", null, null);
		var target = action.target();
		if (target != null && target.alive() && target.active()) {
			int before = target.getHp();
			attacks.apply(state, action.actor(), List.of(target), attack, false,
					.9375 + action.random().nextDouble() * .125, action.defending(), action.covers());
			if ("BQ018".equals(course) && target.isHero() && action.defending().contains(target.getId())
					&& target.getHp() < before)
				state.getTraining().setDefendedHit(true);
		}
		return true;
	}

	/** 活动训练回合末核验课程进度。 */
	public void finish(BattleState state) {
		training.finishRound(state);
	}

	/** 提前击倒对手也要验证目标；认输不能误判为完成课程。 */
	public void close(BattleState state, boolean surrender) {
		if (!state.getStatus().equals("ACTIVE") && !surrender)
			training.finishRound(state);
	}

}
