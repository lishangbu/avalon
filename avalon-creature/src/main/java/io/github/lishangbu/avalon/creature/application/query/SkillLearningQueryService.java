package io.github.lishangbu.avalon.creature.application.query;

import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.content.creature.LearningCatalog;
import io.github.lishangbu.avalon.creature.application.model.LearningView;
import io.github.lishangbu.avalon.creature.domain.SkillLearningRules;
import io.github.lishangbu.avalon.creature.persistence.mapper.CreatureStorageCodec;
import io.github.lishangbu.avalon.creature.persistence.repository.CreatureRepository;
import io.github.lishangbu.avalon.inventory.application.query.InventoryQueryService;
import io.github.lishangbu.avalon.platform.BusinessException;
import org.springframework.stereotype.Component;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/** 学习预览不锁角色、不抽随机数、不消耗库存；提交时命令侧重新核验全部前提。 */
@Component
@RequiredArgsConstructor
public class SkillLearningQueryService {

	private final CharacterQueryService characters;

	private final CreatureRepository pets;

	private final CreatureStorageCodec codec;

	private final InventoryQueryService inventory;

	private final LearningCatalog catalog;

	@Transactional
	public LearningView preview(long owner, long petId) {
		long character = characters.requireId(owner);
		var pet = pets.get(petId);
		if (pet == null || pet.unavailable() || pet.getCharacterId() != character)
			throw new BusinessException(404, "伙伴不存在");
		var current = codec.view(pet).skills();
		var items = inventory.list(owner);
		var options = catalog.pool(pet.getSpecies()).stream().map(book -> {
			var books = items.stream()
				.filter(item -> item.code().equals(book.code()))
				.map(item -> new LearningView.Book(item.id(), item.name(), item.quantity(), item.bound()))
				.toList();
			String reason = current.contains(book.skill()) ? "已学会"
					: pet.getLevel() < book.level() ? "伙伴等级不足" : books.isEmpty() ? "缺少技能书" : null;
			return new LearningView.Option(book.skill(), book.level(), reason, books);
		}).toList();
		return new LearningView(Long.toString(petId), current, SkillLearningRules.probabilities(current.size()),
				options);
	}

}
