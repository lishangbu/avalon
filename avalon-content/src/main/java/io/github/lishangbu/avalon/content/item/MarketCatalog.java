package io.github.lishangbu.avalon.content.item;

import io.github.lishangbu.avalon.content.ContentLoader;
import io.github.lishangbu.avalon.content.item.model.MarketDefinition;
import io.github.lishangbu.avalon.content.world.WorldCatalog;
import org.springframework.stereotype.Component;

/** 交易所由城镇设施承载；目录本身不提高生产等级上限。 */
@Component
public class MarketCatalog {

	private final MarketDefinition definition;

	public MarketCatalog(WorldCatalog world) {
		definition = ContentLoader.read("/content/rules/market.json", MarketDefinition.class);
		if (definition.playerLevel() < 1 || definition.locations().isEmpty())
			throw new IllegalArgumentException("交易所开放条件无效");
		definition.locations().forEach(world::place);
	}

	public MarketDefinition definition() {
		return definition;
	}

}
