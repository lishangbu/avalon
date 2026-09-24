package io.github.lishangbu.avalon.battle.application.query;

import io.github.lishangbu.avalon.battle.domain.BattleRules;
import io.github.lishangbu.avalon.battle.persistence.repository.CombatVitalsRepository;
import io.github.lishangbu.avalon.creature.application.model.CreatureResources;
import org.springframework.stereotype.Component;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import java.util.Map;

/** 真实成长公式与剩余资源的只读投影；查询不补满、不创建资源存档。 */
@Component
@RequiredArgsConstructor
public class CreatureResourceQueryService implements CreatureResources {

	private final BattleRules rules;

	private final CombatVitalsRepository combatVitalsRepository;

	@Override
	@Transactional
	public Resources read(long characterId, long creatureId, String species, int level) {
		var stats = rules.stats(species, level, false, Map.of());
		var stored = combatVitalsRepository.get(creatureId);
		if (stored != null && stored.getCharacterId() != characterId)
			throw new IllegalStateException("伙伴资源存档归属不一致");
		return new Resources(stored == null ? stats.health() : Math.min(stored.getHp(), stats.health()),
				stored == null ? stats.mana() : Math.min(stored.getMp(), stats.mana()), stats.health(), stats.mana(),
				stats.physicalAttack(), stats.magicAttack(), stats.physicalDefense(), stats.magicDefense(),
				stats.speed());
	}

}
