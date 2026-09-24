package io.github.lishangbu.avalon.content.battle;

import io.github.lishangbu.avalon.content.ContentLoader;
import io.github.lishangbu.avalon.content.creature.CreatureCatalog;
import io.github.lishangbu.avalon.content.creature.SpeciesStatsCatalog;
import io.github.lishangbu.avalon.content.item.ItemCatalog;
import io.github.lishangbu.avalon.content.quest.QuestCatalog;
import io.github.lishangbu.avalon.content.world.WorldCatalog;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BattleDiagnosticsTest {

	private final WorldCatalog world = new WorldCatalog();

	private final ItemCatalog items = new ItemCatalog(world);

	private final CreatureCatalog creatures = new CreatureCatalog();

	private final QuestCatalog quests = new QuestCatalog(world, items, creatures);

	private final WildCatalog wilds = new WildCatalog(creatures, items);

	private final SpeciesStatsCatalog stats = new SpeciesStatsCatalog();

	private final BattleCatalog.Definition raw = ContentLoader.read("/content/battle.json",
			BattleCatalog.Definition.class);

	@Test
	void encounterReferenceErrorsIdentifySourceAndField() {
		var base = raw.encounters().getFirst();
		for (String field : List.of("location", "requires", "captureRequires", "wild")) {
			var encounter = new BattleCatalog.Encounter("BROKEN", base.name(),
					field.equals("location") ? "MISSING" : base.location(),
					field.equals("requires") ? "MISSING" : base.requires(),
					field.equals("captureRequires") ? "MISSING" : base.captureRequires(), false, base.species(), null,
					field.equals("wild") ? "MISSING" : base.wild(), base.count());
			var error = assertThrows(IllegalArgumentException.class,
					() -> load(new BattleCatalog.Definition(raw.growth(), raw.skills(), List.of(encounter),
							raw.products(), raw.elements(), raw.supplyDrops(), raw.bookDrops())));
			assertTrue(error.getMessage().contains("/content/battle.json [BROKEN] " + field + "=MISSING"),
					error.getMessage());
		}
	}

	@Test
	void duplicateSkillsAndOverflowingDropWeightsHaveContentContext() {
		var skill = raw.skills().getFirst();
		var duplicate = assertThrows(IllegalArgumentException.class,
				() -> load(new BattleCatalog.Definition(raw.growth(), List.of(skill, skill), raw.encounters(),
						raw.products(), raw.elements(), raw.supplyDrops(), raw.bookDrops())));
		assertTrue(duplicate.getMessage().contains("skills.id=" + skill.id()));
		var overflow = assertThrows(IllegalArgumentException.class,
				() -> load(new BattleCatalog.Definition(raw.growth(), raw.skills(), raw.encounters(), raw.products(),
						raw.elements(), Map.of("NONE", Integer.MAX_VALUE, "HEALTH_POTION", 1), raw.bookDrops())));
		assertTrue(overflow.getMessage().contains("/content/battle.json"));
		assertTrue(overflow.getMessage().contains("supplyDrops."));
		assertTrue(overflow.getMessage().contains("总权重"));
		var catalog = load(raw);
		var missing = assertThrows(IllegalArgumentException.class, () -> catalog.growth("MISSING"));
		assertTrue(missing.getMessage().contains("growth=MISSING"));
	}

	private BattleCatalog load(BattleCatalog.Definition definition) {
		return new BattleCatalog(definition, creatures, items, world, quests, wilds, stats);
	}

}
