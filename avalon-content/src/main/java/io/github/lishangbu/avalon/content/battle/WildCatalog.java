package io.github.lishangbu.avalon.content.battle;

import io.github.lishangbu.avalon.content.ContentLoader;
import io.github.lishangbu.avalon.content.battle.model.WildDefinition;
import io.github.lishangbu.avalon.content.battle.model.WildProfile;
import io.github.lishangbu.avalon.content.creature.CreatureCatalog;
import io.github.lishangbu.avalon.content.item.ItemCatalog;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 区域物种池与精英增幅独立于物种成长。同一区域的单怪、双怪入口共用一份池， 每个敌人独立抽取；玩家等级只检查入口资格，不改变野生幻兽等级分布。
 */
@Component
public class WildCatalog {

	private final Map<String, WildProfile> profiles;

	@Autowired
	public WildCatalog(CreatureCatalog creatures, ItemCatalog items) {
		this(ContentLoader.read("/content/battle/wilds.json", WildDefinition.class), creatures, items);
	}

	/** 显式内容输入与生产资源共用同一校验，离线重放不读取当前配置。 */
	public WildCatalog(WildDefinition raw, CreatureCatalog creatures, ItemCatalog items) {
		profiles = Map.copyOf(raw.profiles());
		profiles.forEach((id, profile) -> {
			if (profile.species().isEmpty() || profile.minPlayerLevel() < 1 || profile.healthMultiplier() < 1
					|| !Double.isFinite(profile.attackMultiplier()) || profile.attackMultiplier() < 1
					|| profile.experienceMultiplier() < 1 || profile.goldMultiplier() < 1)
				throw new IllegalArgumentException("野外模板参数无效：" + id);
			int total = 0;
			for (var entry : profile.species().entrySet()) {
				var spawn = entry.getValue();
				if (!creatures.obtainableDirectly(entry.getKey()) || spawn.weight() <= 0 || spawn.minLevel() < 1
						|| spawn.maxLevel() > GrowthRules.LEVEL_CAP || spawn.maxLevel() < spawn.minLevel())
					throw new IllegalArgumentException("野外物种或等级无效：" + id);
				total = Math.addExact(total, spawn.weight());
			}
			if (profile.elite() != !profile.bookDrops().isEmpty())
				throw new IllegalArgumentException("精英必须独立配置书池：" + id);
			if (profile.elite() != !profile.supplyDrops().isEmpty())
				throw new IllegalArgumentException("精英必须独立配置补给池：" + id);
			int supplyTotal = 0;
			for (var entry : profile.supplyDrops().entrySet()) {
				if (entry.getValue() <= 0 || (!entry.getKey().equals("NONE") && !items.hasItem(entry.getKey())))
					throw new IllegalArgumentException("精英补给池条目无效：" + id);
				supplyTotal = Math.addExact(supplyTotal, entry.getValue());
			}
			int bookTotal = 0;
			for (var entry : profile.bookDrops().entrySet()) {
				if (entry.getValue() <= 0 || (!entry.getKey().equals("NONE") && !items.hasItem(entry.getKey())))
					throw new IllegalArgumentException("精英书池条目无效：" + id);
				bookTotal = Math.addExact(bookTotal, entry.getValue());
			}
		});
	}

	public WildProfile profile(String id) {
		var profile = profiles.get(id);
		if (profile == null)
			throw new IllegalArgumentException("野外模板不存在：" + id);
		return profile;
	}

}
