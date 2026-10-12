package io.github.lishangbu.avalon.content.creature;

import io.github.lishangbu.avalon.content.creature.model.CreatureDefinition;
import io.github.lishangbu.avalon.content.creature.model.CreaturePet;
import io.github.lishangbu.avalon.content.creature.model.EvolutionDefinition;
import io.github.lishangbu.avalon.content.creature.model.EvolutionRoute;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CreatureOrderingTest {

	@Test
	void listingIncludesEvolvedFormsInStableIdOrderWithoutExposingMutability() {
		var source = new CreaturePet("Z", "原形态", "水", 1, "A", "特性", List.of(), null);
		var route = new EvolutionRoute("Z", "A", "进化形态", 2, Map.of("M", 1), null, "进化说明");
		var catalog = new CreatureCatalog(new CreatureDefinition(List.of(source)),
				new EvolutionDefinition(6, List.of(route)));
		assertEquals(List.of("A", "Z"), catalog.all().stream().map(CreaturePet::id).toList());
		assertSame(catalog.pet("A"), catalog.all().getFirst());
		assertEquals("进化说明", catalog.all().getFirst().description());
		assertFalse(catalog.obtainableDirectly("A"));
		assertThrows(UnsupportedOperationException.class, () -> catalog.all().clear());
		assertEquals(2, catalog.all().size());
	}

}
