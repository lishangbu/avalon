package io.github.lishangbu.avalon.content.battle;

import io.github.lishangbu.avalon.content.ContentLoader;
import io.github.lishangbu.avalon.content.battle.model.DungeonRewardDefinition;
import io.github.lishangbu.avalon.content.battle.model.DungeonRewardDrop;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.Map;

/** 副本基础奖励的只读目录；启动时校验引用，避免通关时才发现不存在的物品。 */
@Component
public class DungeonRewardCatalog {

	private final DungeonRewardDefinition definition;

	private final Map<String, DungeonRewardDefinition> definitions;

	@Autowired
	public DungeonRewardCatalog(DungeonCatalog dungeons) {
		this(dungeons, production(dungeons));
	}

	private static Map<String, DungeonRewardDefinition> production(DungeonCatalog dungeons) {
		var values = new java.util.LinkedHashMap<String, DungeonRewardDefinition>();
		for (var bundle : DungeonDocuments.current())
			if (dungeons.contains(bundle.dungeon().id()))
				values.put(bundle.dungeon().id(), bundle.rewards());
		return values;
	}

	public DungeonRewardCatalog(DungeonCatalog dungeons, Map<String, DungeonRewardDefinition> values) {
		values.forEach((id, raw) -> validate(dungeons, id, raw));
		definitions = Map.copyOf(values);
		definition = definition(dungeons.definition().id());
	}

	private static void validate(DungeonCatalog dungeons, String id, DungeonRewardDefinition raw) {
		var dungeon = dungeons.definition(id);
		if (raw.completionExperience() < 0 || raw.firstCompletionExperience() < 0 || raw.dailyGold() < 0
				|| raw.supplyPool().isEmpty()
				|| raw.supplyPool().stream().mapToLong(DungeonRewardDrop::weight).sum() != 100)
			throw new IllegalArgumentException("副本奖励数值或掉落总权重无效");
		raw.supplyStages().forEach(stage -> {
			if (!dungeons.owningStage(stage).id().equals(id))
				throw new IllegalArgumentException("副本掉落阶段必须属于本副本");
		});
		raw.dailySupplies().forEach((code, amount) -> {
			if (amount < 1 || !dungeon.supplies().contains(code))
				throw new IllegalArgumentException("每日副本补给配置无效");
		});
		for (var drop : raw.supplyPool())
			if (drop.weight() < 1 || (!"NONE".equals(drop.code()) && !dungeon.supplies().contains(drop.code())))
				throw new IllegalArgumentException("副本掉落必须引用入本可用补给");
	}

	public DungeonRewardDefinition definition(String dungeon) {
		var value = definitions.get(dungeon);
		if (value == null)
			throw new IllegalArgumentException("副本奖励配置不存在");
		return value;
	}

	public DungeonRewardDefinition definition() {
		return definition;
	}

}
