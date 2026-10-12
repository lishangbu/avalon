package io.github.lishangbu.avalon.content.creature;

import io.github.lishangbu.avalon.content.ContentLoader;
import io.github.lishangbu.avalon.content.creature.model.CollectionDefinition;
import io.github.lishangbu.avalon.content.creature.model.CollectionReward;
import io.github.lishangbu.avalon.content.creature.model.CreaturePet;
import io.github.lishangbu.avalon.content.item.ItemCatalog;
import java.util.HashSet;
import org.springframework.stereotype.Component;

/** 图鉴奖励只在启动时读取；奖励不是主线资金前提，且同账号同条件仅结算一次。 */
@Component
public class CollectionCatalog {

	private final CollectionDefinition definition;

	public CollectionCatalog(ItemCatalog items, CreatureCatalog creatures) {
		definition = ContentLoader.read("/content/collection.json", CollectionDefinition.class);
		validate(definition.species(), items);
		definition.speciesRewards().forEach((species, reward) -> {
			if (!creatures.contains(species))
				throw new IllegalArgumentException("图鉴奖励物种不存在");
			validate(reward, items);
		});
		var candidates = new HashSet<>(definition.milestoneSpecies());
		var known = creatures.all().stream().map(CreaturePet::id).toList();
		if (candidates.isEmpty() || candidates.size() != definition.milestoneSpecies().size()
				|| !known.containsAll(candidates))
			throw new IllegalArgumentException("图鉴阶段奖励物种清单无效");
		var thresholds = new HashSet<Integer>();
		for (var milestone : definition.milestones()) {
			if (milestone.count() < 1 || milestone.count() > candidates.size() || !thresholds.add(milestone.count()))
				throw new IllegalArgumentException("图鉴里程碑数量无效");
			validate(milestone.reward(), items);
		}
	}

	private void validate(CollectionReward reward, ItemCatalog items) {
		if (reward == null || reward.gold() < 0 || reward.quantity() < 1 || !items.hasItem(reward.item())
				|| items.item(reward.item()).task())
			throw new IllegalArgumentException("图鉴奖励配置无效");
	}

	public CollectionDefinition definition() {
		return definition;
	}

	/** 新形态奖励可以覆盖默认值，资格仍沿用账号同物种的一次性凭证。 */
	public CollectionReward reward(String species) {
		return definition.speciesRewards().getOrDefault(species, definition.species());
	}

}
