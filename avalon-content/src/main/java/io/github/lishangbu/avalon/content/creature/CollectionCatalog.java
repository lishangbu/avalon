package io.github.lishangbu.avalon.content.creature;

import io.github.lishangbu.avalon.content.item.ItemCatalog;
import io.github.lishangbu.avalon.content.ContentLoader;

import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

/** 图鉴奖励只在启动时读取；奖励不是主线资金前提，且同账号同条件仅结算一次。 */
@Component
public class CollectionCatalog {

	private final Definition definition;

	public CollectionCatalog(ItemCatalog items, CreatureCatalog creatures) {
		definition = ContentLoader.read("/content/collection.json", Definition.class);
		validate(definition.species(), items);
		definition.speciesRewards().forEach((species, reward) -> {
			if (!creatures.contains(species))
				throw new IllegalArgumentException("图鉴奖励物种不存在");
			validate(reward, items);
		});
		var candidates = new HashSet<>(definition.milestoneSpecies());
		var known = creatures.all().stream().map(CreatureCatalog.Pet::id).toList();
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

	private void validate(Reward reward, ItemCatalog items) {
		if (reward == null || reward.gold() < 0 || reward.quantity() < 1 || !items.hasItem(reward.item())
				|| items.item(reward.item()).task())
			throw new IllegalArgumentException("图鉴奖励配置无效");
	}

	public Definition definition() {
		return definition;
	}

	/** 新形态奖励可以覆盖默认值，资格仍沿用账号同物种的一次性凭证。 */
	public Reward reward(String species) {
		return definition.speciesRewards().getOrDefault(species, definition.species());
	}

	public record Reward(int gold, String item, int quantity) {
	}

	public record Milestone(int count, Reward reward) {
	}

	public record Definition(Reward species, List<String> milestoneSpecies, List<Milestone> milestones,
			Map<String, Reward> speciesRewards) {
		public Definition {
			milestoneSpecies = List.copyOf(milestoneSpecies);
			milestones = List.copyOf(milestones);
			speciesRewards = speciesRewards == null ? Map.of() : Map.copyOf(speciesRewards);
		}
	}

}
