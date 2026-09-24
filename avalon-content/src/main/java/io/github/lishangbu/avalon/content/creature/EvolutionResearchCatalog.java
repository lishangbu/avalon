package io.github.lishangbu.avalon.content.creature;

import io.github.lishangbu.avalon.content.ContentChecks;

import io.github.lishangbu.avalon.content.item.ItemCatalog;
import io.github.lishangbu.avalon.content.ContentLoader;

import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;

/** 研究凭证兑换的启动配置；只允许对应进化路线的材料和新增技能书，不充当通用兑换商城。 */
@Component
public class EvolutionResearchCatalog {

	private final Definition definition;

	public EvolutionResearchCatalog(ItemCatalog items, EvolutionCatalog evolution, LearningCatalog learning) {
		definition = ContentLoader.read("/content/evolution-research.json", Definition.class);
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

	public Definition definition() {
		return definition;
	}

	public Offer offer(String code) {
		return definition.offers().stream().filter(offer -> offer.code().equals(code)).findFirst().orElse(null);
	}

	public record Offer(String code, int price) {
	}

	public record Definition(String token, List<Offer> offers) {
		public Definition {
			offers = List.copyOf(offers);
		}
	}

}
