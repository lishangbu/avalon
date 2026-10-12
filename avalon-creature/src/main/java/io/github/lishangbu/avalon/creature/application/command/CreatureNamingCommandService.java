package io.github.lishangbu.avalon.creature.application.command;

import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.content.creature.CreatureNamingCatalog;
import io.github.lishangbu.avalon.content.text.ForbiddenWordsCatalog;
import io.github.lishangbu.avalon.creature.application.model.CreatureNamingRequest;
import io.github.lishangbu.avalon.creature.persistence.entity.CreatureRenameReceipt;
import io.github.lishangbu.avalon.creature.persistence.mapper.CreatureStorageCodec;
import io.github.lishangbu.avalon.creature.persistence.repository.CreatureRenameReceiptRepository;
import io.github.lishangbu.avalon.creature.persistence.repository.CreatureRepository;
import io.github.lishangbu.avalon.platform.BusinessException;
import jakarta.transaction.Transactional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 角色锁内核验归属、报价与冒险状态，名称、次数、扣款及凭证原子提交。 */
@Component
@RequiredArgsConstructor
public class CreatureNamingCommandService {

	private final CharacterCommandService characters;

	private final CreatureRepository pets;

	private final CreatureRenameReceiptRepository receipts;

	private final CreatureStorageCodec codec;

	private final CreatureNamingCatalog catalog;

	/** 只检查新名称，expectedName是旧状态确认值，必须允许将旧违规名字改为合规名字。 */
	private final ForbiddenWordsCatalog forbiddenWords;

	/**
	 * 格式、请求号与旧状态先按既有契约核对。成功回执重放不产生新文字，仍可返回； 真正的新名称在扣款、次数递增及回执保存前检测，拒绝不会占用首次免费资格。
	 */
	@Transactional
	public void rename(long owner, long id, CreatureNamingRequest request) {
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
		// 检测不修改输入，不替换星号，也不检查旧名字；失败直接结束本次事务。
		// 后续金币、改名计数、编队准备版本和幂等回执必须保持拒绝前状态。
		if (forbiddenWords.contains(request.name()))
			throw new BusinessException(400, "名称包含不允许使用的内容，请修改后重试");
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

	private record Payload(long petId, CreatureNamingRequest request) {
	}

}
