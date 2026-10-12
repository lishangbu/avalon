package io.github.lishangbu.avalon.inventory.application.command;

import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.content.item.CraftingCatalog;
import io.github.lishangbu.avalon.content.item.ItemCatalog;
import io.github.lishangbu.avalon.inventory.application.model.CraftingRequest;
import io.github.lishangbu.avalon.inventory.application.model.CraftingResult;
import io.github.lishangbu.avalon.inventory.domain.BagCapacityRules;
import io.github.lishangbu.avalon.inventory.domain.BagCapacityRulesEntry;
import io.github.lishangbu.avalon.inventory.persistence.entity.CraftingReceipt;
import io.github.lishangbu.avalon.inventory.persistence.mapper.InventoryStorageCodec;
import io.github.lishangbu.avalon.inventory.persistence.repository.CraftingRepository;
import io.github.lishangbu.avalon.inventory.persistence.repository.InventoryRepository;
import io.github.lishangbu.avalon.platform.BusinessException;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 合成拥有完整写事务。只消耗玩家逐项选择的碎片，不自动混用绑定库存。 先验证净空间，再扣金币和材料，成品与请求凭证一并提交，任何失败全部回滚。
 */
@Component
@RequiredArgsConstructor
public class CraftingCommandService {

	private final CharacterCommandService characters;

	private final InventoryRepository inventory;

	private final InventoryCommandService commands;

	private final CraftingRepository receipts;

	private final InventoryStorageCodec codec;

	private final CraftingCatalog crafting;

	private final ItemCatalog items;

	@Transactional
	public CraftingResult craft(long owner, CraftingRequest request) {
		validate(request);
		var context = characters.lock(owner);
		var selected = new TreeMap<>(request.fragments());
		String payload = codec.encode(new Payload(request.recipe(), selected));
		var prior = receipts.prior(context.id(), request.requestId());
		if (prior != null) {
			if (!prior.getPayload().equals(payload))
				throw new BusinessException(409, "请求号已用于其他合成");
			return result(prior);
		}
		characters.requireOutsideBattle(owner);
		var recipe = crafting.recipe(request.recipe());
		if (recipe == null)
			throw new BusinessException(404, "配方不存在");
		if (selected.values().stream().mapToLong(Integer::longValue).sum() != recipe.quantity())
			throw new BusinessException(400, "请选择配方要求数量的同名碎片");
		var owned = inventory.available(context.id());
		boolean bound = false;
		for (var entry : selected.entrySet()) {
			var item = owned.stream()
				.filter(value -> value.getId().toString().equals(entry.getKey()))
				.findFirst()
				.orElseThrow(() -> new BusinessException(404, "所选碎片不存在"));
			if (!item.getCode().equals(recipe.fragment()) || item.getQuantity() < entry.getValue())
				throw new BusinessException(409, "碎片类型或数量已变化，请重新选择");
			bound |= item.isBound();
		}
		var projection = new ArrayList<BagCapacityRulesEntry>();
		for (var item : owned) {
			int quantity = item.getQuantity() - selected.getOrDefault(item.getId().toString(), 0);
			projection.add(new BagCapacityRulesEntry(item.getCode(), item.isBound(), quantity, item.getSlot() != null,
					item.getSlot() == null && items.item(item.getCode()).task()));
		}
		projection.add(new BagCapacityRulesEntry(recipe.product(), bound, 1, false, false));
		int slots = BagCapacityRules.slots(projection);
		if (slots > BagCapacityRules.CAPACITY)
			throw new BusinessException(409, "背包空间不足，还需腾出 " + (slots - BagCapacityRules.CAPACITY) + " 格");
		characters.changeGold(owner, -recipe.gold());
		selected.forEach((id, quantity) -> commands.consume(owner, Long.parseLong(id), recipe.fragment(), quantity));
		commands.grantStrict(owner, "CRAFT:" + request.requestId(), recipe.product(), 1, false, bound);
		var receipt = new CraftingReceipt();

		receipt.setCharacterId(context.id());
		receipt.setRequestId(request.requestId());
		receipt.setPayload(payload);
		receipt.setProduct(recipe.product());
		receipt.setBound(bound);
		receipts.saveAndFlush(receipt);
		return result(receipt);
	}

	private CraftingResult result(CraftingReceipt receipt) {
		return new CraftingResult(receipt.getProduct(), items.item(receipt.getProduct()).name(), receipt.isBound());
	}

	private void validate(CraftingRequest request) {
		if (request == null || request.recipe() == null || request.requestId() == null || request.fragments() == null
				|| request.fragments().isEmpty() || request.fragments().size() > 99)
			throw new BusinessException(400, "缺少合成确认信息");
		try {
			if (!UUID.fromString(request.requestId()).toString().equals(request.requestId()))
				throw new IllegalArgumentException();
			for (var entry : request.fragments().entrySet())
				if (entry.getKey() == null || Long.parseLong(entry.getKey()) <= 0 || entry.getValue() == null
						|| entry.getValue() <= 0)
					throw new IllegalArgumentException();
		}
		catch (IllegalArgumentException error) {
			throw new BusinessException(400, "合成请求号或碎片选择无效");
		}
	}

	private record Payload(String recipe, Map<String, Integer> fragments) {
	}

}
