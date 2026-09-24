package io.github.lishangbu.avalon.inventory.domain;

import io.github.lishangbu.avalon.content.battle.CultivationRewardCatalog.Definition;
import java.util.ArrayList;
import java.util.List;

/** 材料为互斥池，整书为独立池；所有随机点来自持久化事件种子，重试不能重新抽取。 */
public final class CultivationRewardRules {

	private CultivationRewardRules() {
	}

	public enum Kind {

		RESEARCH_COMMISSION, REGIONAL_ELITE, ENHANCEMENT_COMMISSION, ENHANCEMENT_DUNGEON

	}

	public enum Stone {

		I, II

	}

	public record Reward(String code, int quantity, boolean bound) {
	}

	public static List<Reward> rewards(Definition definition, Kind kind, String route, Stone stone, int materialRoll,
			int bookRoll) {
		if (kind == null || materialRoll < 0 || materialRoll >= 100 || bookRoll < 0 || bookRoll >= 100)
			throw new IllegalArgumentException("培养奖励种类或随机点无效");
		if (kind == Kind.REGIONAL_ELITE) {
			if (route == null)
				throw new IllegalArgumentException("缺少精英奖励路线");
			var pool = definition.routes().get(route);
			if (pool == null || stone != null)
				throw new IllegalArgumentException("精英奖励路线无效");
			var result = new ArrayList<Reward>();
			if (materialRoll < definition.commonPercent())
				result.add(new Reward(pool.common(), 1, false));
			else if (materialRoll < definition.commonPercent() + definition.specialPercent())
				result.add(new Reward(pool.special(), 1, false));
			if (pool.book() != null && bookRoll < definition.bookPercent())
				result.add(new Reward(pool.book(), 1, false));
			return List.copyOf(result);
		}
		if (route != null)
			throw new IllegalArgumentException("此来源不接受进化路线");
		if (kind == Kind.RESEARCH_COMMISSION) {
			if (stone != null)
				throw new IllegalArgumentException("研究委托不接受强化石选择");
			return List.of(new Reward(definition.researchToken(), definition.researchQuantity(), true));
		}
		if (stone == null)
			throw new IllegalArgumentException("请明确本次来源的强化石档位");
		boolean commission = kind == Kind.ENHANCEMENT_COMMISSION;
		return List.of(new Reward(stone == Stone.I ? definition.stoneI() : definition.stoneII(),
				commission ? definition.commissionStones() : definition.dungeonStones(),
				commission || stone == Stone.I));
	}

}
