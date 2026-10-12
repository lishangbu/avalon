package io.github.lishangbu.avalon.inventory.application.command;

import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.content.item.ItemCatalog;
import io.github.lishangbu.avalon.inventory.application.model.InventoryClaimRequest;
import io.github.lishangbu.avalon.inventory.domain.BagCapacityRules;
import io.github.lishangbu.avalon.inventory.domain.BagCapacityRulesEntry;
import io.github.lishangbu.avalon.inventory.persistence.entity.InventoryClaim;
import io.github.lishangbu.avalon.inventory.persistence.repository.InventoryClaimRepository;
import io.github.lishangbu.avalon.inventory.persistence.repository.InventoryRepository;
import io.github.lishangbu.avalon.platform.BusinessException;
import jakarta.transaction.Transactional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 只把已固定奖励移入库存，不重新抽取或重新计算装备属性；战中不能用待领补给绕过入场限制。 */
@Component
@RequiredArgsConstructor
public class InventoryClaimService {

	private final CharacterCommandService characters;

	private final InventoryRepository items;

	private final InventoryClaimRepository receipts;

	private final ItemCatalog catalog;

	@Transactional
	public void claim(long owner, long itemId, InventoryClaimRequest request) {
		if (request == null || request.requestId() == null || request.quantity() <= 0)
			throw new BusinessException(400, "请确认领取数量和请求号");
		try {
			if (!UUID.fromString(request.requestId()).toString().equals(request.requestId()))
				throw new IllegalArgumentException();
		}
		catch (IllegalArgumentException error) {
			throw new BusinessException(400, "领取请求号无效");
		}
		long character = characters.lock(owner).id();
		var prior = receipts.prior(character, request.requestId());
		if (prior != null) {
			if (prior.getItemId() != itemId || prior.getQuantity() != request.quantity())
				throw new BusinessException(409, "请求号已用于其他领取操作");
			return;
		}
		characters.requireOutsideBattle(owner);
		var item = items.get(itemId);
		if (item == null || item.getCharacterId() != character)
			throw new BusinessException(404, "待领奖励不存在");
		if (item.getPendingQuantity() < request.quantity())
			throw new BusinessException(409, "待领数量已变化，请刷新");
		var current = items.available(character)
			.stream()
			.map(value -> new BagCapacityRulesEntry(value.getCode(), value.isBound(), value.getQuantity(),
					value.getSlot() != null, value.getSlot() == null && catalog.item(value.getCode()).task()))
			.toList();
		var addition = new BagCapacityRulesEntry(item.getCode(), item.isBound(), request.quantity(),
				item.getSlot() != null, item.getSlot() == null && catalog.item(item.getCode()).task());
		if (BagCapacityRules.fits(current, addition) != request.quantity())
			throw new BusinessException(409, "背包空间不足，请减少领取数量或整理背包");
		item.setQuantity(Math.addExact(item.getQuantity(), request.quantity()));
		item.setPendingQuantity(item.getPendingQuantity() - request.quantity());
		characters.invalidatePreparation(owner);
		var receipt = new InventoryClaim();

		receipt.setCharacterId(character);
		receipt.setItemId(itemId);
		receipt.setQuantity(request.quantity());
		receipt.setRequestId(request.requestId());
		receipts.saveAndFlush(receipt);
	}

}
