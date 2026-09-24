package io.github.lishangbu.avalon.battle.application.model;

import io.github.lishangbu.avalon.battle.domain.BattleRules;
import io.github.lishangbu.avalon.battle.domain.BattleState;
import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.content.battle.BattleCatalog;
import io.github.lishangbu.avalon.content.battle.TrainingCatalog;
import io.github.lishangbu.avalon.character.application.query.CharacterFactQueryService;
import io.github.lishangbu.avalon.creature.application.query.CreatureQueryService;
import io.github.lishangbu.avalon.inventory.application.query.InventoryQueryService;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 由各领域公开读模型组成战斗单位，不跨模块访问实体。新建单位初始满资源，存档资源由上层覆盖。
 */
@Component
@RequiredArgsConstructor
public class PartyFactory {

	private final CharacterQueryService characters;

	private final CreatureQueryService creatures;

	private final InventoryQueryService inventory;

	private final BattleCatalog catalog;

	private final BattleRules rules;

	private final TrainingCatalog training;

	private final CharacterFactQueryService facts;

	public List<BattleState.Unit> create(long owner, boolean all) {
		var character = characters.find(owner);
		var equipment = inventory.loadout(owner);
		List<BattleState.Unit> units = new ArrayList<>();
		var hero = new BattleState.Unit();
		hero.setId(character.id());
		hero.setName(character.name());
		hero.setSpecies(character.profession());
		hero.setHero(true);
		hero.setLevel(character.level());
		var skills = new ArrayList<>(catalog.growth(character.profession()).skills());
		if (facts.codes(owner).contains("TRAINING:BQ023"))
			skills.add(training.course(character.profession()).skill());
		hero.setSkills(skills);
		hero.setStats(rules.stats(character.profession(), character.level(), true, equipment.stats()));
		hero.setDefendShield(equipment.defendShield());
		units.add(hero);
		for (var pet : creatures.list(owner))
			if (all || pet.partyPosition() < 4) {
				var u = new BattleState.Unit();
				u.setId(pet.id());
				u.setOwnerHero(hero.getId());
				u.setSpecies(pet.species());
				u.setName(pet.name());
				u.setLevel(pet.level());
				u.setElement(pet.element());
				u.setAbility(pet.ability());
				u.setAbilitySlot(pet.abilitySlot());
				u.setSkills(pet.skills());
				u.setPosition(pet.partyPosition());
				u.setStats(rules.stats(pet.species(), pet.level(), false, Map.of()));
				units.add(u);
			}
		units.forEach(u -> {
			u.setHp(u.getStats().health());
			u.setMp(u.getStats().mana());
		});
		return units;
	}

}
