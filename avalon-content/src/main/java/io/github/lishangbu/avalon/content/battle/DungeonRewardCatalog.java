package io.github.lishangbu.avalon.content.battle;

import io.github.lishangbu.avalon.content.ContentLoader;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/** 副本基础奖励的只读目录；启动时校验引用，避免通关时才发现不存在的物品。 */
@Component
public class DungeonRewardCatalog {

	private final Definition definition;

	public DungeonRewardCatalog(DungeonCatalog dungeons) {
		var raw = ContentLoader.read("/content/dungeon-rewards.json", Definition.class);
		if (raw.completionExperience() < 0 || raw.firstCompletionExperience() < 0 || raw.dailyGold() < 0
				|| raw.supplyPool().isEmpty() || raw.supplyPool().stream().mapToLong(Drop::weight).sum() != 100)
			throw new IllegalArgumentException("副本奖励数值或掉落总权重无效");
		raw.supplyStages().forEach(dungeons::stage);
		raw.dailySupplies().forEach((code, amount) -> {
			if (amount < 1 || !dungeons.definition().supplies().contains(code))
				throw new IllegalArgumentException("每日副本补给配置无效");
		});
		for (var drop : raw.supplyPool())
			if (drop.weight() < 1
					|| (!"NONE".equals(drop.code()) && !dungeons.definition().supplies().contains(drop.code())))
				throw new IllegalArgumentException("副本掉落必须引用入本可用补给");
		definition = raw;
	}

	public Definition definition() {
		return definition;
	}

	public record Definition(int completionExperience, int firstCompletionExperience, int dailyGold,
			Map<String, Integer> dailySupplies, List<String> supplyStages, List<Drop> supplyPool) {
		public Definition {
			dailySupplies = dailySupplies == null ? Map.of() : Map.copyOf(dailySupplies);
			supplyStages = supplyStages == null ? List.of() : List.copyOf(supplyStages);
			supplyPool = supplyPool == null ? List.of() : List.copyOf(supplyPool);
		}
	}

	public record Drop(String code, int weight) {
	}

}
