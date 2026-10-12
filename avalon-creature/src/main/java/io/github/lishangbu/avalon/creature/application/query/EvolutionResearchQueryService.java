package io.github.lishangbu.avalon.creature.application.query;

import io.github.lishangbu.avalon.character.application.query.CharacterFactQueryService;
import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.content.creature.EvolutionCatalog;
import io.github.lishangbu.avalon.content.creature.EvolutionResearchCatalog;
import io.github.lishangbu.avalon.content.creature.model.EvolutionRoute;
import io.github.lishangbu.avalon.content.item.ItemCatalog;
import io.github.lishangbu.avalon.content.world.WorldCatalog;
import io.github.lishangbu.avalon.creature.application.model.EvolutionResearchMaterial;
import io.github.lishangbu.avalon.creature.application.model.EvolutionResearchOffer;
import io.github.lishangbu.avalon.creature.application.model.EvolutionResearchRoute;
import io.github.lishangbu.avalon.creature.application.model.EvolutionResearchView;
import io.github.lishangbu.avalon.inventory.application.query.InventoryQueryService;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 研究所入口的只读投影；低等级、不在设施或正在冒险时不公开可操作入口。 */
@Component
@RequiredArgsConstructor
public class EvolutionResearchQueryService {

	private final CharacterQueryService characters;

	private final CharacterFactQueryService facts;

	private final EvolutionCatalog catalog;

	private final WorldCatalog world;

	private final ItemCatalog items;

	private final EvolutionResearchCatalog exchange;

	private final InventoryQueryService inventory;

	@Transactional
	public EvolutionResearchView view(long owner) {
		var character = characters.find(owner);
		if (character == null)
			return unavailable();
		var adventure = characters.adventure(owner);
		var recorded = facts.codes(owner);
		boolean researchLocation = catalog.definition().locations().contains(character.location());
		boolean available = character.level() >= catalog.definition().playerLevel() && (researchLocation
				|| (recorded.contains("EVOLUTION:TUTORIAL") && world.provides(character.location(), "RECOVERY")))
				&& adventure.battle() == null && adventure.dungeon() == null;
		if (!available)
			return unavailable();
		String selected = catalog.definition()
			.routes()
			.stream()
			.map(EvolutionRoute::source)
			.filter(source -> recorded.contains("EVOLUTION:ROUTE:" + source))
			.findFirst()
			.orElse(null);
		var routes = catalog.definition()
			.routes()
			.stream()
			.map(route -> new EvolutionResearchRoute(route.source(), route.target(), route.name(), route.level(),
					route.materials()
						.entrySet()
						.stream()
						.sorted(Map.Entry.comparingByKey())
						.map(entry -> new EvolutionResearchMaterial(entry.getKey(), items.item(entry.getKey()).name(),
								entry.getValue()))
						.toList()))
			.toList();
		long tokens = inventory.list(owner)
			.stream()
			.filter(item -> item.code().equals(exchange.definition().token()))
			.mapToLong(item -> item.quantity())
			.sum();
		var offers = exchange.definition()
			.offers()
			.stream()
			.filter(offer -> researchLocation)
			.map(offer -> new EvolutionResearchOffer(offer.code(), items.item(offer.code()).name(), offer.price()))
			.toList();
		return new EvolutionResearchView(true, recorded.contains("EVOLUTION:TUTORIAL"), selected, routes, tokens,
				offers, catalog.definition().playerLevel());
	}

	private EvolutionResearchView unavailable() {
		return new EvolutionResearchView(false, false, null, List.of(), 0, List.of(),
				catalog.definition().playerLevel());
	}

}
