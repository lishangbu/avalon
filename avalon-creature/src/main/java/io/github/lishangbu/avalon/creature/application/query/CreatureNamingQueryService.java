package io.github.lishangbu.avalon.creature.application.query;

import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.content.creature.CreatureNamingCatalog;
import io.github.lishangbu.avalon.creature.application.model.CreatureNamingPreview;
import io.github.lishangbu.avalon.creature.persistence.repository.CreatureRepository;
import io.github.lishangbu.avalon.platform.BusinessException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 只读报价含更名次数，用于确认时识别另一页面的修改；不占用免费次数。 */
@Component
@RequiredArgsConstructor
public class CreatureNamingQueryService {

	private final CharacterQueryService characters;

	private final CreatureRepository pets;

	private final CreatureNamingCatalog catalog;

	@Transactional
	public CreatureNamingPreview preview(long owner, long id) {
		long character = Long.parseLong(characters.find(owner).id());
		var pet = pets.get(id);
		if (pet == null || pet.getCharacterId() != character || pet.unavailable())
			throw new BusinessException(404, "伙伴不存在");
		return new CreatureNamingPreview(pet.getName(), pet.getRenameCount(), catalog.price(pet.getRenameCount()),
				catalog.definition().maxLength());
	}

}
