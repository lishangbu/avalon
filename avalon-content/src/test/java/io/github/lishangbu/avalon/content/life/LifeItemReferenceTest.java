package io.github.lishangbu.avalon.content.life;

import io.github.lishangbu.avalon.content.item.ItemCatalog;
import io.github.lishangbu.avalon.content.item.model.ItemCategory;
import io.github.lishangbu.avalon.content.item.model.ItemDefinition;
import io.github.lishangbu.avalon.content.item.model.ItemItem;
import io.github.lishangbu.avalon.content.item.model.ItemScope;
import io.github.lishangbu.avalon.content.life.model.FarmingCrop;
import io.github.lishangbu.avalon.content.life.model.FarmingDefinition;
import io.github.lishangbu.avalon.content.life.model.FishingDefinition;
import io.github.lishangbu.avalon.content.life.model.FishingFish;
import io.github.lishangbu.avalon.content.life.model.GatheringDefinition;
import io.github.lishangbu.avalon.content.life.model.GatheringNode;
import io.github.lishangbu.avalon.content.world.WorldCatalog;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LifeItemReferenceTest {

	private final WorldCatalog world = new WorldCatalog();

	private final ItemCatalog items = new ItemCatalog(new ItemDefinition(List.of(
			new ItemItem("NORMAL", "普通物品", "资源", ItemCategory.MATERIAL, null, ItemScope.GENERAL, false, null, null,
					null, null, null, null),
			new ItemItem("QUEST", "任务物品", "任务", ItemCategory.MATERIAL, null, ItemScope.GENERAL, true, null, null, null,
					null, null, null))),
			world);

	@Test
	void allLifeResourceReferencesRejectQuestAndMissingItemsAtTheirSource() {
		for (String invalid : List.of("QUEST", "MISSING")) {
			check("gathering", "product", invalid, () -> gathering(invalid, null));
			check("gathering", "rare", invalid, () -> gathering("NORMAL", invalid));
			check("farming", "seed", invalid, () -> farming(invalid, "NORMAL"));
			check("farming", "product", invalid, () -> farming("NORMAL", invalid));
			check("fishing", "bait", invalid, () -> fishing(invalid, "NORMAL"));
			check("fishing", "fish.id", invalid, () -> fishing("NORMAL", invalid));
		}
		assertDoesNotThrow(() -> gathering("NORMAL", "NORMAL"));
		assertDoesNotThrow(() -> farming("NORMAL", "NORMAL"));
		assertDoesNotThrow(() -> fishing("NORMAL", "NORMAL"));
	}

	private void check(String file, String field, String code, Runnable construct) {
		var error = assertThrows(IllegalArgumentException.class, construct::run);
		assertTrue(error.getMessage().contains(io.github.lishangbu.avalon.content.ContentTestResources.path(file)));
		assertTrue(error.getMessage().contains(field + "=" + code));
	}

	private void gathering(String product, String rare) {
		new GatheringCatalog(
				new GatheringDefinition(List
					.of(new GatheringNode("NODE", "资源", "MINING", 5, world.definition().start(), product, 1, rare))),
				items, world);
	}

	private void farming(String seed, String product) {
		new FarmingCatalog(new FarmingDefinition(6, 24, 1, List.of(world.definition().start()),
				List.of(new FarmingCrop("CROP", "作物", seed, product, 1, 1, 1, 1))), items, world);
	}

	private void fishing(String bait, String fish) {
		new FishingCatalog(
				new FishingDefinition(bait,
						List.of(new FishingFish(fish, "鱼", 1, 1, 1, List.of(world.definition().start())))),
				items, world);
	}

}
