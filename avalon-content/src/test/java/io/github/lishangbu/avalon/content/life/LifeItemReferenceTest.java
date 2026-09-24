package io.github.lishangbu.avalon.content.life;

import io.github.lishangbu.avalon.content.item.ItemCatalog;
import io.github.lishangbu.avalon.content.world.WorldCatalog;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LifeItemReferenceTest {

	private final WorldCatalog world = new WorldCatalog();

	private final ItemCatalog items = new ItemCatalog(new ItemCatalog.Definition(List.of(
			new ItemCatalog.Item("NORMAL", "普通物品", "资源", ItemCatalog.Category.MATERIAL, null, ItemCatalog.Scope.GENERAL,
					false, null, null, null, null, null, null),
			new ItemCatalog.Item("QUEST", "任务物品", "任务", ItemCatalog.Category.MATERIAL, null, ItemCatalog.Scope.GENERAL,
					true, null, null, null, null, null, null))),
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
		assertTrue(error.getMessage().contains("/content/" + file + ".json"));
		assertTrue(error.getMessage().contains(field + "=" + code));
	}

	private void gathering(String product, String rare) {
		new GatheringCatalog(new GatheringCatalog.Definition(List
			.of(new GatheringCatalog.Node("NODE", "资源", "MINING", 5, world.definition().start(), product, 1, rare))),
				items, world);
	}

	private void farming(String seed, String product) {
		new FarmingCatalog(new FarmingCatalog.Definition(6, 24, 1, List.of(world.definition().start()),
				List.of(new FarmingCatalog.Crop("CROP", "作物", seed, product, 1, 1, 1, 1))), items, world);
	}

	private void fishing(String bait, String fish) {
		new FishingCatalog(
				new FishingCatalog.Definition(bait,
						List.of(new FishingCatalog.Fish(fish, "鱼", 1, 1, 1, List.of(world.definition().start())))),
				items, world);
	}

}
