package io.github.lishangbu.avalon.content.life;

import io.github.lishangbu.avalon.content.world.WorldCatalog;
import io.github.lishangbu.avalon.content.item.ItemCatalog;
import io.github.lishangbu.avalon.content.ContentLoader;
import io.github.lishangbu.avalon.content.ContentChecks;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** 作物使用现实生长时间；种子与产物引用在启动时校验。 */
@Component
public class FarmingCatalog {

	private final Definition definition;

	private final Map<String, Crop> byId;

	@Autowired
	public FarmingCatalog(ItemCatalog items, WorldCatalog world) {
		this(ContentLoader.read("/content/farming.json", Definition.class), items, world);
	}

	public FarmingCatalog(Definition definition, ItemCatalog items, WorldCatalog world) {
		this.definition = definition;
		byId = ContentChecks.index("/content/farming.json", "crop.id", definition.crops(), Crop::id);
		var seeds = new HashSet<String>();
		for (var crop : definition.crops()) {
			items.ordinaryReference("/content/farming.json", crop.id(), "seed", crop.seed());
			items.ordinaryReference("/content/farming.json", crop.id(), "product", crop.product());
			if (!seeds.add(crop.seed()) || crop.level() < 1 || crop.level() > 10 || crop.growthSeconds() < 1
					|| crop.quantity() < 1 || crop.points() < 1)
				throw ContentChecks.invalid("/content/farming.json", crop.id(), "crop", crop, "作物配置无效");
		}
		if (definition.initialPlots() != 6 || definition.maximumPlots() != 24 || definition.expansionGold() < 1)
			throw ContentChecks.invalid("/content/farming.json", "plots", "expansion", definition, "农田扩建配置无效");
		for (String location : definition.locations())
			world.referencedPlace("/content/farming.json", "farming", "locations", location);
	}

	public Definition definition() {
		return definition;
	}

	public Crop crop(String id) {
		return byId.get(id);
	}

	public record Crop(String id, String name, String seed, String product, int level, int growthSeconds, int quantity,
			int points) {
	}

	public record Definition(int initialPlots, int maximumPlots, int expansionGold, List<String> locations,
			List<Crop> crops) {
		public Definition {
			locations = List.copyOf(locations);
			crops = List.copyOf(crops);
		}
	}

}
