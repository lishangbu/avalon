package io.github.lishangbu.avalon.creature.application.query;

import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.content.creature.CreatureCatalog;
import io.github.lishangbu.avalon.creature.application.model.CreatureCustodyDetail;
import io.github.lishangbu.avalon.creature.application.model.CreatureResources;
import io.github.lishangbu.avalon.creature.application.model.CreatureView;
import io.github.lishangbu.avalon.creature.persistence.mapper.CreatureStorageCodec;
import io.github.lishangbu.avalon.creature.persistence.repository.CreatureRepository;
import io.github.lishangbu.avalon.platform.BusinessException;
import jakarta.transaction.Transactional;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 仅供市场在核验挂单可见性后调用，公开商品实物资料但不暴露账号或持久化实体。 */
@Component
@RequiredArgsConstructor
public class CreatureCustodyQueryService {

	private final CreatureRepository pets;

	private final CreatureStorageCodec codec;

	private final CreatureCatalog catalog;

	private final CharacterQueryService characters;

	private final CreatureResources resources;

	/** 候选只是当前可交易个体投影，提交仍在角色锁内重新核验完整资料。 */
	@Transactional
	public List<CreatureView> eligible(long owner) {
		var character = characters.find(owner);
		var owned = pets.owned(characters.requireId(owner));
		return owned.stream()
			.filter(p -> p.canEnterCustody())
			.filter(p -> owned.stream()
				.anyMatch(other -> !other.getId().equals(p.getId()) && other.getLevel() <= character.level()))
			.map(codec::view)
			.toList();
	}

	@Transactional
	public CreatureCustodyDetail detail(long seller, long id) {
		long character = characters.requireId(seller);
		var pet = pets.get(id);
		if (pet == null || !pet.isInCustody() || pet.getCharacterId() != character || pet.getReleasedAt() != null)
			throw new BusinessException(409, "托管伙伴已变化，请刷新挂单");
		return new CreatureCustodyDetail(pet.getId().toString(), pet.getSpecies(), catalog.pet(pet.getSpecies()).name(),
				pet.getName(), pet.isCustomName(), pet.getElement(), pet.getLevel(), Long.toString(pet.getExperience()),
				pet.getAbilitySlot(), pet.getAbility(), List.of(codec.decode(pet.getSkills(), String[].class)),
				pet.isBound(), pet.getCaptorCharacterId() == null ? null : pet.getCaptorCharacterId().toString(),
				pet.getCaptorCharacterId() == null ? null : characters.publicName(pet.getCaptorCharacterId()),
				resources.read(character, id, pet.getSpecies(), pet.getLevel()));
	}

}
