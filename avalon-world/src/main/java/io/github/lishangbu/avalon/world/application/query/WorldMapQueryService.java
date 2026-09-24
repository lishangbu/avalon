package io.github.lishangbu.avalon.world.application.query;

import io.github.lishangbu.avalon.character.application.query.CharacterFactQueryService;
import io.github.lishangbu.avalon.content.world.WorldCatalog;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 当前角色可见地图投影。未解锁地点和通往它的出口一起隐藏，移动命令仍独立核验资格。
 */
@Component
@RequiredArgsConstructor
public class WorldMapQueryService {

	private final CharacterFactQueryService factQueries;

	private final WorldCatalog catalog;

	public WorldCatalog.Definition visible(long owner) {
		var facts = factQueries.codes(owner);
		var unlocked = catalog.definition()
			.places()
			.stream()
			.filter(place -> catalog.accessible(place, facts::contains))
			.map(WorldCatalog.Place::id)
			.collect(Collectors.toSet());
		var places = catalog.definition()
			.places()
			.stream()
			.filter(place -> unlocked.contains(place.id()))
			.map(place -> new WorldCatalog.Place(place.id(), place.name(), place.description(), place.x(), place.y(),
					place.exits().stream().filter(unlocked::contains).toList(), place.requiredFact(), place.services(),
					place.closedByFact(),
					place.npcs().stream().filter(binding -> WorldCatalog.present(binding, facts::contains)).toList(),
					place.requiredFacts()))
			.toList();
		var present = places.stream()
			.flatMap(place -> place.npcs().stream())
			.map(WorldCatalog.NpcBinding::npcId)
			.collect(Collectors.toSet());
		return new WorldCatalog.Definition(catalog.definition().start(), catalog.definition().professions(), places,
				List.of(), catalog.definition().npcs().stream().filter(npc -> present.contains(npc.id())).toList());
	}

}
