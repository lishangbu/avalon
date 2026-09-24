package io.github.lishangbu.avalon.world.application.query;

import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.content.life.FarmingCatalog;
import io.github.lishangbu.avalon.content.life.LifeProductionCatalog;
import io.github.lishangbu.avalon.inventory.application.model.InventoryItemView;
import io.github.lishangbu.avalon.inventory.application.query.InventoryQueryService;
import io.github.lishangbu.avalon.world.domain.FarmingRules;
import io.github.lishangbu.avalon.world.domain.LifeClock;
import io.github.lishangbu.avalon.content.life.LifeSkillRules;
import io.github.lishangbu.avalon.content.life.LifeSkillRules.Skill;
import io.github.lishangbu.avalon.world.persistence.repository.FarmRepository;
import io.github.lishangbu.avalon.world.persistence.repository.LifeSkillRepository;
import java.util.List;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FarmQueryService {

	private final CharacterQueryService characters;

	private final FarmRepository farms;

	private final LifeSkillRepository skills;

	private final FarmingCatalog catalog;

	private final LifeProductionCatalog tools;

	private final InventoryQueryService inventory;

	private final LifeClock clock;

	public View view(long owner) {
		long id = characters.requireId(owner);
		var character = characters.find(owner);
		var state = farms.find(id);
		var now = clock.now();
		var points = skills.owned(id).get(Skill.FARMING);
		var plots = IntStream.range(0, state.plots()).mapToObj(index -> {
			var planted = state.crops().get(index);
			if (planted == null)
				return new Plot(index, null, null, false, false, false, 0);
			var crop = catalog.crop(planted.crop());
			return new Plot(index, crop.id(), crop.name(), planted.bound(), planted.wateredAt() != null,
					FarmingRules.mature(planted.wateredAt(), crop.growthSeconds(), now),
					FarmingRules.remainingSeconds(planted.wateredAt(), crop.growthSeconds(), now));
		}).toList();
		var held = inventory.list(owner)
			.stream()
			.filter(item -> catalog.definition().crops().stream().anyMatch(crop -> crop.seed().equals(item.code()))
					|| (tools.tool(item.code()) != null && tools.tool(item.code()).kind().equals(Skill.FARMING.tool)))
			.toList();
		return new View(catalog.definition().locations().contains(character.location()), points != null,
				points == null ? 0 : LifeSkillRules.level(points), catalog.definition().maximumPlots(),
				catalog.definition().expansionGold(), plots, catalog.definition().crops(), held,
				tools.tools().stream().filter(tool -> tool.kind().equals(Skill.FARMING.tool)).toList());
	}

	public record View(boolean atFarm, boolean learned, int level, int maximumPlots, int expansionGold,
			List<Plot> plots, List<FarmingCatalog.Crop> crops, List<InventoryItemView> inventory,
			List<LifeProductionCatalog.Tool> tools) {
	}

	public record Plot(int index, String crop, String name, boolean bound, boolean watered, boolean mature,
			long remainingSeconds) {
	}

}
