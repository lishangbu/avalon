package io.github.lishangbu.avalon.content.creature;

import io.github.lishangbu.avalon.content.ContentChecks;
import io.github.lishangbu.avalon.content.ContentLoader;
import io.github.lishangbu.avalon.content.creature.model.EvolutionResearchDefinition;
import io.github.lishangbu.avalon.content.creature.model.EvolutionResearchOfferConfig;
import io.github.lishangbu.avalon.content.item.ItemCatalog;
import java.util.HashSet;
import org.springframework.stereotype.Component;

/** 研究凭证兑换的启动配置；只允许对应进化路线的材料和新增技能书，不充当通用兑换商城。 */
@Component
public class EvolutionResearchCatalog {

	private final EvolutionResearchDefinition definition;

	public EvolutionResearchCatalog(ItemCatalog items, EvolutionCatalog evolution, LearningCatalog learning) {
		definition = ContentLoader.read("/content/creatures/evolution-research.json",
				EvolutionResearchDefinition.class);
		if (!items.hasItem(definition.token()) || items.item(definition.token()).task()
				|| definition.offers().isEmpty())
			throw new IllegalArgumentException("研究凭证或兑换目录无效");
		var allowed = new HashSet<String>();
		for (var route : evolution.definition().routes()) {
			allowed.addAll(route.materials().keySet());
			learning.pool(route.target())
				.stream()
				.filter(book -> book.skill().equals(route.skill()))
				.forEach(book -> allowed.add(book.code()));
		}
		var seen = new HashSet<String>();
		for (var offer : definition.offers()) {
			ContentChecks.checkId(seen, offer.code());
			if (!allowed.contains(offer.code()) || !items.hasItem(offer.code()) || items.item(offer.code()).task()
					|| offer.code().equals(definition.token()) || offer.price() < 1)
				throw new IllegalArgumentException("研究兑换条目无效：" + offer.code());
		}
		if (!seen.equals(allowed))
			throw new IllegalArgumentException("进化材料或新增技能书缺少研究兑换来源");
	}

	public EvolutionResearchDefinition definition() {
		return definition;
	}

	public EvolutionResearchOfferConfig offer(String code) {
		return definition.offers().stream().filter(offer -> offer.code().equals(code)).findFirst().orElse(null);
	}

}
