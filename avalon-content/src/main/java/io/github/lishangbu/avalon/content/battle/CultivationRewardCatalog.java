package io.github.lishangbu.avalon.content.battle;

import io.github.lishangbu.avalon.content.ContentLoader;
import io.github.lishangbu.avalon.content.battle.model.CultivationRewardDefinition;
import io.github.lishangbu.avalon.content.creature.EvolutionCatalog;
import io.github.lishangbu.avalon.content.creature.EvolutionResearchCatalog;
import io.github.lishangbu.avalon.content.creature.LearningCatalog;
import io.github.lishangbu.avalon.content.item.EnhancementCatalog;
import io.github.lishangbu.avalon.content.item.ItemCatalog;
import java.util.HashSet;
import java.util.Set;
import org.springframework.stereotype.Component;

/** 培养奖励结算目录。只定义已确认产出，不把目录存在等同于具体区域已经开放。 */
@Component
public class CultivationRewardCatalog {

	private final CultivationRewardDefinition definition;

	public CultivationRewardCatalog(ItemCatalog items, EvolutionCatalog evolution, LearningCatalog learning,
			EvolutionResearchCatalog research, EnhancementCatalog enhancement) {
		definition = ContentLoader.read("/content/battle/cultivation-rewards.json", CultivationRewardDefinition.class);
		validate(definition);
		if (!definition.researchToken().equals(research.definition().token())
				|| !definition.stoneI().equals(enhancement.definition().stones().get(0).item())
				|| !definition.stoneII().equals(enhancement.definition().stones().get(1).item()))
			throw new IllegalArgumentException("培养奖励物品与培养目录不一致");
		var seen = new HashSet<String>();
		for (var route : evolution.definition().routes()) {
			var pool = definition.routes().get(route.source());
			if (pool == null || !route.materials().keySet().equals(Set.of(pool.common(), pool.special()))
					|| (route.skill() != null && learning.pool(route.target())
						.stream()
						.noneMatch(book -> book.code().equals(pool.book()) && book.skill().equals(route.skill())))
					|| (route.skill() == null && pool.book() != null))
				throw new IllegalArgumentException("培养奖励路线不完整：" + route.source());
			var codes = new HashSet<>(Set.of(pool.common(), pool.special()));
			if (pool.book() != null)
				codes.add(pool.book());
			for (String code : codes) {
				if (items.item(code).task())
					throw new IllegalArgumentException("培养奖励不能是任务物品");
			}
			seen.add(route.source());
		}
		if (!seen.equals(definition.routes().keySet()))
			throw new IllegalArgumentException("培养奖励包含未知进化路线");
	}

	public static void validate(CultivationRewardDefinition value) {
		if (value.researchQuantity() <= 0 || value.commissionStones() <= 0 || value.dungeonStones() <= 0
				|| value.commonPercent() < 0 || value.specialPercent() < 0
				|| value.commonPercent() > 100 - value.specialPercent() || value.bookPercent() < 0
				|| value.bookPercent() > 100 || value.routes().isEmpty())
			throw new IllegalArgumentException("培养奖励数量或概率无效");
	}

	public CultivationRewardDefinition definition() {
		return definition;
	}

}
