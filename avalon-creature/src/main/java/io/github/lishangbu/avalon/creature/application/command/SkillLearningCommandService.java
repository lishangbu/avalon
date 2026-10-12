package io.github.lishangbu.avalon.creature.application.command;

import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.content.creature.LearningCatalog;
import io.github.lishangbu.avalon.creature.application.model.SkillLearningRequest;
import io.github.lishangbu.avalon.creature.domain.SkillLearningRules;
import io.github.lishangbu.avalon.creature.domain.SkillLearningRulesResult;
import io.github.lishangbu.avalon.creature.persistence.entity.SkillLearningReceipt;
import io.github.lishangbu.avalon.creature.persistence.mapper.CreatureStorageCodec;
import io.github.lishangbu.avalon.creature.persistence.repository.CreatureRepository;
import io.github.lishangbu.avalon.creature.persistence.repository.SkillLearningReceiptRepository;
import io.github.lishangbu.avalon.inventory.application.command.InventoryCommandService;
import io.github.lishangbu.avalon.inventory.application.query.InventoryQueryService;
import io.github.lishangbu.avalon.platform.BusinessException;
import jakarta.transaction.Transactional;
import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 学习写侧拥有事务：角色锁串行化学习、战斗与物品消费，凭证保证响应丢失后重试返回首次结果。 随机数只在所有资格检查通过后抽取；提交失败时扣书、技能与凭证一并回滚。
 */
@Component
@RequiredArgsConstructor
public class SkillLearningCommandService {

	private final CharacterCommandService characters;

	private final CreatureRepository pets;

	private final CreatureStorageCodec codec;

	private final SkillLearningReceiptRepository receipts;

	private final LearningCatalog catalog;

	private final InventoryQueryService inventoryQueries;

	private final InventoryCommandService inventoryCommands;

	private final SecureRandom random = new SecureRandom();

	@Transactional
	public SkillLearningRulesResult learn(long owner, long petId, SkillLearningRequest request) {
		validate(request);
		var context = characters.lock(owner);
		String payload = codec.encode(new Payload(petId, request.bookId(), request.expectedSkills()));
		var prior = receipts.prior(context.id(), request.requestId());
		if (prior != null) {
			if (!prior.getPayload().equals(payload))
				throw new BusinessException(409, "请求号已用于其他学习操作");
			return codec.decode(prior.getResult(), SkillLearningRulesResult.class);
		}
		characters.requireOutsideBattle(owner);
		characters.requireCreatureNotDispatched(owner, petId);
		var pet = pets.get(petId);
		if (pet == null || pet.unavailable() || !pet.getCharacterId().equals(context.id()))
			throw new BusinessException(404, "伙伴不存在");
		var current = codec.view(pet).skills();
		if (!current.equals(request.expectedSkills()))
			throw new BusinessException(409, "伙伴技能已变化，请重新查看并确认");
		var item = inventoryQueries.list(owner)
			.stream()
			.filter(value -> value.id().equals(request.bookId()))
			.findFirst()
			.orElseThrow(() -> new BusinessException(404, "技能书不存在"));
		var book = catalog.book(item.code());
		if (book == null)
			throw new BusinessException(409, "该物种不能学习此技能");
		SkillLearningRules.requireEligible(current, book.skill(), catalog.pool(pet.getSpecies()).contains(book),
				pet.getLevel(), book.level());

		inventoryCommands.consume(owner, Long.parseLong(item.id()), book.code(), 1);
		var result = SkillLearningRules.learn(current, book.skill(), random);
		pet.setSkills(codec.encode(result.remaining()));
		characters.invalidatePreparation(owner);
		var receipt = new SkillLearningReceipt();

		receipt.setCharacterId(context.id());
		receipt.setRequestId(request.requestId());
		receipt.setPayload(payload);
		receipt.setResult(codec.encode(result));
		receipts.saveAndFlush(receipt);
		return result;
	}

	/** 输入在获得写锁前做形状校验，内容资格仍只能由服务端目录和持久化实例决定。 */
	private void validate(SkillLearningRequest request) {
		if (request == null || request.requestId() == null || request.bookId() == null
				|| request.expectedSkills() == null || request.expectedSkills().size() > 7
				|| request.expectedSkills().stream().anyMatch(value -> value == null || value.isBlank()))
			throw new BusinessException(400, "缺少有效的学习确认信息");
		try {
			if (!UUID.fromString(request.requestId()).toString().equals(request.requestId())
					|| Long.parseLong(request.bookId()) <= 0)
				throw new IllegalArgumentException();
		}
		catch (IllegalArgumentException error) {
			throw new BusinessException(400, "学习请求号或物品编号无效");
		}
	}

	private record Payload(long petId, String bookId, List<String> expectedSkills) {
	}

}
