package io.github.lishangbu.avalon.world.application.query;

import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.content.life.FishingCatalog;
import io.github.lishangbu.avalon.content.life.LifeProductionCatalog;
import io.github.lishangbu.avalon.inventory.application.model.InventoryItemView;
import io.github.lishangbu.avalon.inventory.application.query.InventoryQueryService;
import io.github.lishangbu.avalon.world.application.model.FishingViews;
import io.github.lishangbu.avalon.content.life.LifeSkillRules;
import io.github.lishangbu.avalon.content.life.LifeSkillRules.Skill;
import io.github.lishangbu.avalon.world.persistence.repository.FishingSessionRepository;
import io.github.lishangbu.avalon.world.persistence.repository.LifeSkillRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FishingQueryService {

	private final CharacterQueryService characters;

	private final FishingCatalog catalog;

	private final LifeProductionCatalog tools;

	private final InventoryQueryService inventory;

	private final LifeSkillRepository skills;

	private final FishingSessionRepository sessions;

	private final FishingViews views;

	public View view(long owner) {
		long id = characters.requireId(owner);
		var hero = characters.find(owner);
		var points = skills.owned(id).get(Skill.FISHING);
		return new View(views.view(sessions.find(id)), points == null ? 0 : LifeSkillRules.level(points),
				catalog.definition()
					.fish()
					.stream()
					.filter(fish -> fish.locations().contains(hero.location()))
					.toList(),
				inventory.list(owner)
					.stream()
					.filter(item -> catalog.definition().bait().equals(item.code()) || tools.tool(item.code()) != null
							&& tools.tool(item.code()).kind().equals(Skill.FISHING.tool))
					.toList(),
				tools.tools().stream().filter(tool -> tool.kind().equals(Skill.FISHING.tool)).toList(),
				catalog.definition().bait());
	}

	public record View(FishingViews.Snapshot session, int level, List<FishingCatalog.Fish> fish,
			List<InventoryItemView> inventory, List<LifeProductionCatalog.Tool> tools, String bait) {
	}

}
