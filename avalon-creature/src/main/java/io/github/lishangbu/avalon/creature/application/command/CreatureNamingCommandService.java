package io.github.lishangbu.avalon.creature.application.command;

import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.content.creature.CreatureNamingCatalog;
import io.github.lishangbu.avalon.creature.persistence.entity.CreatureRenameReceipt;
import io.github.lishangbu.avalon.creature.persistence.mapper.CreatureStorageCodec;
import io.github.lishangbu.avalon.creature.persistence.repository.CreatureRepository;
import io.github.lishangbu.avalon.creature.persistence.repository.CreatureRenameReceiptRepository;
import io.github.lishangbu.avalon.platform.BusinessException;
import org.springframework.stereotype.Component;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import java.util.UUID;

/** 角色锁内核验归属、报价与冒险状态，名称、次数、扣款及凭证原子提交。 */
@Component
@RequiredArgsConstructor
public class CreatureNamingCommandService {

	private final CharacterCommandService characters;

	private final CreatureRepository pets;

	private final CreatureRenameReceiptRepository receipts;

	private final CreatureStorageCodec codec;

	private final CreatureNamingCatalog catalog;

	@Transactional
	public void rename(long owner, long id, Request request) {
		if (request == null || request.name() == null || request.expectedName() == null || request.renamed() < 0
				|| !request.name().matches("[\\p{L}\\p{N}]+")
				|| request.name().codePointCount(0, request.name().length()) > catalog.definition().maxLength())
			throw new BusinessException(400, "伙伴名称仅支持规定长度内的文字和数字");
		try {
			if (!UUID.fromString(request.requestId()).toString().equals(request.requestId()))
				throw new IllegalArgumentException();
		}
		catch (IllegalArgumentException | NullPointerException error) {
			throw new BusinessException(400, "改名请求号无效");
		}
		var context = characters.lock(owner);
		String payload = codec.encode(new Payload(id, request));
		var prior = receipts.prior(context.id(), request.requestId());
		if (prior != null) {
			if (!prior.getPayload().equals(payload))
				throw new BusinessException(409, "请求号已用于其他改名操作");
			return;
		}
		characters.requireOutsideDungeon(owner);
		var pet = pets.get(id);
		if (pet == null || !pet.getCharacterId().equals(context.id()) || pet.unavailable())
			throw new BusinessException(404, "伙伴不存在");
		if (!pet.getName().equals(request.expectedName()) || pet.getRenameCount() != request.renamed()
				|| catalog.price(pet.getRenameCount()) != request.expectedPrice())
			throw new BusinessException(409, "改名信息已变化，请重新查看费用");
		if (pet.getName().equals(request.name()))
			throw new BusinessException(409, "新名称与当前名称相同");
		characters.changeGold(owner, -catalog.price(pet.getRenameCount()));
		pet.setName(request.name());
		pet.setCustomName(true);
		pet.setRenameCount(Math.incrementExact(pet.getRenameCount()));
		characters.invalidatePreparation(owner);
		var receipt = new CreatureRenameReceipt();

		receipt.setCharacterId(context.id());
		receipt.setRequestId(request.requestId());
		receipt.setPayload(payload);
		receipts.saveAndFlush(receipt);
	}

	public record Request(String requestId, String name, String expectedName, int renamed, int expectedPrice) {
	}

	private record Payload(long petId, Request request) {
	}

}
