package io.github.lishangbu.avalon.content.life;

import io.github.lishangbu.avalon.content.ContentChecks;
import io.github.lishangbu.avalon.content.ContentLoader;
import io.github.lishangbu.avalon.content.item.ItemCatalog;
import io.github.lishangbu.avalon.content.life.model.FarmingCrop;
import io.github.lishangbu.avalon.content.life.model.FarmingDefinition;
import io.github.lishangbu.avalon.content.world.WorldCatalog;
import java.util.HashSet;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** 作物使用现实生长时间；种子与产物引用在启动时校验。 */
@Component
public class FarmingCatalog {

	private final FarmingDefinition definition;

	private final Map<String, FarmingCrop> byId;

	@Autowired
	public FarmingCatalog(ItemCatalog items, WorldCatalog world) {
		this(ContentLoader.read("/content/professions/farming.json", FarmingDefinition.class), items, world);
	}

	public FarmingCatalog(FarmingDefinition definition, ItemCatalog items, WorldCatalog world) {
		this.definition = definition;
		byId = ContentChecks.index("/content/professions/farming.json", "crop.id", definition.crops(), FarmingCrop::id);
		var seeds = new HashSet<String>();
		for (var crop : definition.crops()) {
			items.ordinaryReference("/content/professions/farming.json", crop.id(), "seed", crop.seed());
			items.ordinaryReference("/content/professions/farming.json", crop.id(), "product", crop.product());
			if (!seeds.add(crop.seed()) || crop.level() < 1 || crop.level() > 10 || crop.growthSeconds() < 1
					|| crop.quantity() < 1 || crop.points() < 1)
				throw ContentChecks.invalid("/content/professions/farming.json", crop.id(), "crop", crop, "作物配置无效");
		}
		if (definition.initialPlots() != 6 || definition.maximumPlots() != 24 || definition.expansionGold() < 1)
			throw ContentChecks.invalid("/content/professions/farming.json", "plots", "expansion", definition,
					"农田扩建配置无效");
		for (String location : definition.locations())
			world.referencedPlace("/content/professions/farming.json", "farming", "locations", location);
	}

	public FarmingDefinition definition() {
		return definition;
	}

	public FarmingCrop crop(String id) {
		return byId.get(id);
	}

}
