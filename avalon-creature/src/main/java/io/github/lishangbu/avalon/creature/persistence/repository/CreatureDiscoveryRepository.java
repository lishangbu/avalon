package io.github.lishangbu.avalon.creature.persistence.repository;

import io.github.lishangbu.avalon.creature.persistence.entity.CreatureDiscovery;
import io.github.lishangbu.avalon.creature.persistence.entity.OwnedCreature;

import org.springframework.stereotype.Component;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/** 调用者须持有角色锁；先登记后结算奖励，进化修改物种之前也必须保留旧形态事实。 */
@Component
@RequiredArgsConstructor
public class CreatureDiscoveryRepository {

	private final CreatureDiscoveryRecords records;

	@Transactional(Transactional.TxType.MANDATORY)
	public void remember(OwnedCreature pet) {
		if (records.known(pet.getCharacterId(), pet.getSpecies(), pet.getAbilitySlot(), pet.getAbility()))
			return;
		var discovery = new CreatureDiscovery();

		discovery.setCharacterId(pet.getCharacterId());
		discovery.setSpecies(pet.getSpecies());
		discovery.setAbilitySlot(pet.getAbilitySlot());
		discovery.setAbility(pet.getAbility());
		records.save(discovery);
	}

}
