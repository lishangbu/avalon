package io.github.lishangbu.avalon.world.application.query;

import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.content.item.ItemCatalog;
import io.github.lishangbu.avalon.content.item.PotionRecoveryRules;
import io.github.lishangbu.avalon.content.life.GatheringCatalog;
import io.github.lishangbu.avalon.content.life.LifeProductionCatalog;
import io.github.lishangbu.avalon.content.life.LifeSkillRules;
import io.github.lishangbu.avalon.content.life.LifeSkillRulesSkill;
import io.github.lishangbu.avalon.creature.application.query.CreatureQueryService;
import io.github.lishangbu.avalon.inventory.application.query.InventoryQueryService;
import io.github.lishangbu.avalon.world.application.model.GatheringNodeView;
import io.github.lishangbu.avalon.world.application.model.GatheringView;
import io.github.lishangbu.avalon.world.domain.GatheringRules;
import io.github.lishangbu.avalon.world.persistence.repository.GatheringSessionRepository;
import io.github.lishangbu.avalon.world.persistence.repository.LifeSkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 只读当前现场资源和会话，不推进时钟、不补发奖励。 */
@Component
@RequiredArgsConstructor
public class GatheringQueryService {

	private final PotionRecoveryRules potions;

	private final CharacterQueryService characters;

	private final CreatureQueryService creatures;

	private final InventoryQueryService inventory;

	private final GatheringSessionRepository sessions;

	private final LifeSkillRepository skills;

	private final GatheringCatalog catalog;

	private final LifeProductionCatalog tools;

	private final ItemCatalog items;

	public GatheringView view(long owner) {
		long id = characters.requireId(owner);
		var character = characters.find(owner);
		var learned = skills.owned(id);
		var nodes = catalog.nodes().stream().filter(node -> node.location().equals(character.location())).map(node -> {
			var skill = LifeSkillRulesSkill.valueOf(node.skill());
			var cost = GatheringRules.cost(LifeSkillRules.toolTier(node.level()));
			return new GatheringNodeView(node.id(), node.name(), node.skill(), skill.title, skill.tool, node.level(),
					items.item(node.product()).name(), node.quantity(), cost.health(), cost.mana(),
					learned.containsKey(skill) && LifeSkillRules.level(learned.get(skill)) >= node.level());
		}).toList();
		return new GatheringView(sessions.find(id), nodes,
				creatures.list(owner)
					.stream()
					.filter(pet -> pet.partyPosition() < 100 && pet.level() <= character.level())
					.toList(),
				inventory.list(owner).stream().filter(item -> tools.tool(item.code()) != null).toList(), tools.tools(),
				inventory.list(owner).stream().filter(item -> potions.recovery(item.code()) != null).toList());
	}

}
