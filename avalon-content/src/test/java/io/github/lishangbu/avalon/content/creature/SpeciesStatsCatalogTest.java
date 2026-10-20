package io.github.lishangbu.avalon.content.creature;

import io.github.lishangbu.avalon.content.ContentLoader;
import io.github.lishangbu.avalon.content.creature.model.SpeciesStatsStats;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/** 使用已定形态的独立预期值检查取整与进化差异，不通过旧倍率反推测试答案。 */
class SpeciesStatsCatalogTest {

	private final SpeciesStatsCatalog catalog = new SpeciesStatsCatalog();

	@Test
	void levelTwelveUsesIntegerStatsIncludingMana() {
		assertEquals(new SpeciesStatsStats(433, 114, 64, 80, 68, 80, 48), catalog.atLevel("CLAW_CAT", 12));
		assertEquals(new SpeciesStatsStats(555, 127, 64, 98, 76, 90, 49), catalog.atLevel("STREAM_CLAW_CAT", 12));
		assertEquals(85, catalog.base("CLAW_CAT").health());
	}

	@Test
	void evolutionReadsTargetWithoutCompoundingPreviousStats() {
		assertEquals(2194, catalog.atLevel("TIDAL_CLAW_CAT", 50).health());
		assertEquals(384, catalog.atLevel("TIDAL_CLAW_CAT", 50).mana());
		catalog.atLevel("STREAM_CLAW_CAT", 12);
		assertEquals(2194, catalog.atLevel("TIDAL_CLAW_CAT", 50).health());
		assertThrows(IllegalArgumentException.class, () -> catalog.atLevel("CLAW_CAT", 0));
		assertThrows(IllegalArgumentException.class, () -> catalog.atLevel("CLAW_CAT", 51));
		assertThrows(IllegalArgumentException.class, () -> catalog.base("MISSING"));
	}

	@Test
	void schemaRejectsFractionalAndMissingBaseStats() throws Exception {
		try (var input = getClass().getResourceAsStream("/content/creatures/species-stats.json")) {
			var document = JsonMapper.builder().build().readTree(input);
			var cat = (ObjectNode) document.path("species").path("CLAW_CAT");
			cat.put("health", 85.5);
			assertThrows(IllegalArgumentException.class,
					() -> ContentLoader.validate("/content/creatures/species-stats.json", document));
			cat.put("health", 85);
			cat.remove("mana");
			assertThrows(IllegalArgumentException.class,
					() -> ContentLoader.validate("/content/creatures/species-stats.json", document));
		}
	}

}
