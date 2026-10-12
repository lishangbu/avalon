package io.github.lishangbu.avalon.content.life;

import io.github.lishangbu.avalon.content.ContentChecks;
import io.github.lishangbu.avalon.content.ContentLoader;
import io.github.lishangbu.avalon.content.item.ItemCatalog;
import io.github.lishangbu.avalon.content.life.model.FishingDefinition;
import io.github.lishangbu.avalon.content.life.model.FishingFish;
import io.github.lishangbu.avalon.content.world.WorldCatalog;
import java.util.HashSet;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class FishingCatalog {

	private final FishingDefinition definition;

	private final Map<String, FishingFish> byId;

	@Autowired
	public FishingCatalog(ItemCatalog items, WorldCatalog world) {
		this(ContentLoader.read("/content/fishing.json", FishingDefinition.class), items, world);
	}

	public FishingCatalog(FishingDefinition definition, ItemCatalog items, WorldCatalog world) {
		this.definition = definition;
		byId = ContentChecks.index("/content/fishing.json", "fish.id", definition.fish(), FishingFish::id);
		items.ordinaryReference("/content/fishing.json", "fishing", "bait", definition.bait());
		for (var fish : definition.fish()) {
			if (fish.name() == null || fish.name().isBlank())
				throw ContentChecks.invalid("/content/fishing.json", fish.id(), "name", fish.name(), "鱼种名称不能为空");
			if (fish.level() < 1 || fish.level() > 10)
				throw ContentChecks.invalid("/content/fishing.json", fish.id(), "level", fish.level(), "等级必须为1至10");
			if (fish.difficulty() < 1 || fish.difficulty() > 10)
				throw ContentChecks.invalid("/content/fishing.json", fish.id(), "difficulty", fish.difficulty(),
						"难度必须为1至10");
			if (fish.points() < 1)
				throw ContentChecks.invalid("/content/fishing.json", fish.id(), "points", fish.points(), "熟练度必须为正数");
			if (fish.locations().isEmpty() || new HashSet<>(fish.locations()).size() != fish.locations().size())
				throw ContentChecks.invalid("/content/fishing.json", fish.id(), "locations", fish.locations(),
						"地点不能为空或重复");
			items.ordinaryReference("/content/fishing.json", fish.id(), "fish.id", fish.id());
			fish.locations()
				.forEach(location -> world.referencedPlace("/content/fishing.json", fish.id(), "locations", location));
		}
	}

	public FishingDefinition definition() {
		return definition;
	}

	public FishingFish fish(String id) {
		return byId.get(id);
	}

}
