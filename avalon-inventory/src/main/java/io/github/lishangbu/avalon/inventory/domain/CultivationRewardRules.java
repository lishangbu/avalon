package io.github.lishangbu.avalon.inventory.domain;

import io.github.lishangbu.avalon.content.battle.model.CultivationRewardDefinition;
import java.util.ArrayList;
import java.util.List;

/** 材料为互斥池，整书为独立池；所有随机点来自持久化事件种子，重试不能重新抽取。 */
public final class CultivationRewardRules {

	private CultivationRewardRules() {
	}

	public static List<CultivationRewardRulesReward> rewards(CultivationRewardDefinition definition,
			CultivationRewardRulesKind kind, String route, CultivationRewardRulesStone stone, int materialRoll,
			int bookRoll) {
		if (kind == null || materialRoll < 0 || materialRoll >= 100 || bookRoll < 0 || bookRoll >= 100)
			throw new IllegalArgumentException("培养奖励种类或随机点无效");
		if (kind == CultivationRewardRulesKind.REGIONAL_ELITE) {
			if (route == null)
				throw new IllegalArgumentException("缺少精英奖励路线");
			var pool = definition.routes().get(route);
			if (pool == null || stone != null)
				throw new IllegalArgumentException("精英奖励路线无效");
			var result = new ArrayList<CultivationRewardRulesReward>();
			if (materialRoll < definition.commonPercent())
				result.add(new CultivationRewardRulesReward(pool.common(), 1, false));
			else if (materialRoll < definition.commonPercent() + definition.specialPercent())
				result.add(new CultivationRewardRulesReward(pool.special(), 1, false));
			if (pool.book() != null && bookRoll < definition.bookPercent())
				result.add(new CultivationRewardRulesReward(pool.book(), 1, false));
			return List.copyOf(result);
		}
		if (route != null)
			throw new IllegalArgumentException("此来源不接受进化路线");
		if (kind == CultivationRewardRulesKind.RESEARCH_COMMISSION) {
			if (stone != null)
				throw new IllegalArgumentException("研究委托不接受强化石选择");
			return List
				.of(new CultivationRewardRulesReward(definition.researchToken(), definition.researchQuantity(), true));
		}
		if (stone == null)
			throw new IllegalArgumentException("请明确本次来源的强化石档位");
		boolean commission = kind == CultivationRewardRulesKind.ENHANCEMENT_COMMISSION;
		return List.of(new CultivationRewardRulesReward(
				stone == CultivationRewardRulesStone.I ? definition.stoneI() : definition.stoneII(),
				commission ? definition.commissionStones() : definition.dungeonStones(),
				commission || stone == CultivationRewardRulesStone.I));
	}

}
