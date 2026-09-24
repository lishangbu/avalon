package io.github.lishangbu.avalon.battle.domain;

import io.github.lishangbu.avalon.content.battle.BattleCatalog;
import io.github.lishangbu.avalon.content.creature.CreatureCatalog;
import io.github.lishangbu.avalon.content.creature.CreatureAbilityRules;
import io.github.lishangbu.avalon.content.battle.WildCatalog;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

/** 只根据入场种子生成单位，不读写存档；生成后的个体作为战局的一部分一次性保存。 */
@Component
@RequiredArgsConstructor
public class WildGeneration {

	private final BattleCatalog battles;

	private final CreatureCatalog creatures;

	private final CreatureAbilityRules abilities;

	private final WildCatalog wilds;

	private final BattleRules rules;

	public List<BattleState.Unit> create(BattleCatalog.Encounter encounter, Random random) {
		var profile = encounter.wild() == null ? null : wilds.profile(encounter.wild());
		var weights = new TreeMap<>(encounter.species());
		if (profile != null)
			profile.species().forEach((code, spawn) -> weights.put(code, spawn.weight()));
		var result = new ArrayList<BattleState.Unit>();
		int count = profile == null ? 1 : encounter.count();
		for (int index = 0; index < count; index++) {
			int roll = random.nextInt(weights.values().stream().mapToInt(Integer::intValue).sum());
			String chosen = null;
			for (var entry : weights.entrySet()) {
				roll -= entry.getValue();
				if (roll < 0) {
					chosen = entry.getKey();
					break;
				}
			}
			var species = creatures.pet(chosen);
			var growth = battles.growth(chosen);
			var spawn = profile == null ? null : profile.species().get(chosen);
			var enemy = new BattleState.Unit();
			enemy.setId("enemy-" + (index + 1));
			enemy.setEnemy(true);
			enemy.setSpecies(chosen);
			enemy.setElite(profile != null && profile.elite());
			enemy.setName((enemy.isElite() ? "精英·" : "") + species.name());
			enemy.setElement(species.element());
			enemy.setSkills(growth.skills());
			enemy.setLevel(encounter.tutorial() ? species.level()
					: random.nextInt(spawn == null ? growth.minLevel() : spawn.minLevel(),
							(spawn == null ? growth.maxLevel() : spawn.maxLevel()) + 1));
			var ability = abilities.draw(chosen, random);
			enemy.setAbilitySlot(ability.slot());
			enemy.setAbility(ability.ability());
			var stats = rules.stats(chosen, enemy.getLevel(), false, Map.of());
			enemy.setStats(profile == null ? stats
					: new BattleState.Stats(stats.health() * profile.healthMultiplier(), stats.mana(),
							(int) Math.floor(stats.physicalAttack() * profile.attackMultiplier()),
							(int) Math.floor(stats.magicAttack() * profile.attackMultiplier()), stats.physicalDefense(),
							stats.magicDefense(), stats.speed()));
			enemy.setHp(enemy.getStats().health());
			enemy.setMp(enemy.getStats().mana());
			result.add(enemy);
		}
		return result;
	}

}
