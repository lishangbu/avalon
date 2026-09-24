package io.github.lishangbu.avalon.battle.application.command;

import io.github.lishangbu.avalon.battle.persistence.repository.CombatVitalsRepository;
import io.github.lishangbu.avalon.creature.application.model.CreatureOwnershipChanged;
import org.springframework.stereotype.Component;
import org.springframework.context.event.EventListener;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/** 转手只改资源存档归属，不治疗、不复活；不存在的存档仍沿用原有满资源语义。 */
@Component
@RequiredArgsConstructor
public class CreatureOwnershipVitalsObserver {

	private final CombatVitalsRepository combatVitalsRepository;

	@Transactional(Transactional.TxType.MANDATORY)
	@EventListener
	public void changed(CreatureOwnershipChanged event) {
		var stored = combatVitalsRepository.get(event.creatureId());
		if (stored == null)
			return;
		if (stored.getCharacterId() != event.previousCharacterId())
			throw new IllegalStateException("伙伴资源存档归属不一致");
		stored.setCharacterId(event.characterId());
	}

}
