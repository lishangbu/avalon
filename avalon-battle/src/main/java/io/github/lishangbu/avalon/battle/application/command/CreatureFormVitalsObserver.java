package io.github.lishangbu.avalon.battle.application.command;

import io.github.lishangbu.avalon.battle.domain.BattleRules;
import io.github.lishangbu.avalon.battle.persistence.repository.CombatVitalsStore;
import io.github.lishangbu.avalon.battle.persistence.repository.CombatVitalsRepository;
import io.github.lishangbu.avalon.creature.application.model.CreatureFormChanged;
import org.springframework.stereotype.Component;
import org.springframework.context.event.EventListener;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import java.util.Map;

/** 形态成长与资源存档同步提交；无论伙伴在队伍还是仓库，都不通过进化补充资源。 */
@Component
@RequiredArgsConstructor
public class CreatureFormVitalsObserver {

	private final BattleRules rules;

	private final CombatVitalsStore combatVitalsStore;

	private final CombatVitalsRepository combatVitalsRepository;

	@Transactional(Transactional.TxType.MANDATORY)
	@EventListener
	public void changed(CreatureFormChanged event) {
		var before = rules.stats(event.previousSpecies(), event.level(), false, Map.of());
		var after = rules.stats(event.species(), event.level(), false, Map.of());
		var stored = combatVitalsRepository.get(event.creatureId());
		if (stored != null && stored.getCharacterId() != event.characterId())
			throw new IllegalStateException("伙伴资源存档归属不一致");
		// 无存档表示此前按原形态满资源展示，必须在上限增加前将这个旧值实体化。
		int hp = stored == null ? before.health() : stored.getHp();
		int mp = stored == null ? before.mana() : stored.getMp();
		combatVitalsStore.saveVitals(event.characterId(), event.creatureId(), Math.min(hp, after.health()),
				Math.min(mp, after.mana()));
	}

}
