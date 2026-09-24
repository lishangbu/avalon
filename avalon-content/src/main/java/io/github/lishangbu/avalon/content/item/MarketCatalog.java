package io.github.lishangbu.avalon.content.item;

import io.github.lishangbu.avalon.content.world.WorldCatalog;
import io.github.lishangbu.avalon.content.ContentLoader;

import org.springframework.stereotype.Component;
import java.util.List;

/** 交易所由城镇设施承载；目录本身不提高生产等级上限。 */
@Component
public class MarketCatalog {

	private final Definition definition;

	public MarketCatalog(WorldCatalog world) {
		definition = ContentLoader.read("/content/market.json", Definition.class);
		if (definition.playerLevel() < 1 || definition.locations().isEmpty())
			throw new IllegalArgumentException("交易所开放条件无效");
		definition.locations().forEach(world::place);
	}

	public Definition definition() {
		return definition;
	}

	public record Definition(int playerLevel, List<String> locations) {
		public Definition {
			locations = List.copyOf(locations);
		}
	}

}
