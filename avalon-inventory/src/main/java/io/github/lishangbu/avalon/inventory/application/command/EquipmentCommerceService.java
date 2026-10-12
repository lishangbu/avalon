package io.github.lishangbu.avalon.inventory.application.command;

import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.inventory.application.model.EquipmentCommerceRequest;
import io.github.lishangbu.avalon.inventory.application.query.EquipmentShopQueryService;
import io.github.lishangbu.avalon.inventory.persistence.entity.EquipmentReceipt;
import io.github.lishangbu.avalon.inventory.persistence.mapper.InventoryStorageCodec;
import io.github.lishangbu.avalon.inventory.persistence.repository.EquipmentReceiptRepository;
import io.github.lishangbu.avalon.inventory.persistence.repository.InventoryRepository;
import io.github.lishangbu.avalon.platform.BusinessException;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 装备经济命令在角色锁内核验实际资格；库存、金币与防重凭证同事务提交。 */
@Component
@RequiredArgsConstructor
public class EquipmentCommerceService {

	private final CharacterCommandService characters;

	private final InventoryCommandService inventory;

	private final InventoryRepository items;

	private final EquipmentReceiptRepository receipts;

	private final EquipmentShopQueryService shops;

	private final InventoryStorageCodec codec;

	@Transactional
	public void execute(long owner, EquipmentCommerceRequest request) {
		validate(request);
		long character = characters.lock(owner).id();
		String payload = codec.encode(request);
		var prior = receipts.prior(character, request.requestId());
		if (prior != null) {
			if (!prior.getPayload().equals(payload))
				throw new BusinessException(409, "请求号已用于其他装备操作");
			return;
		}
		characters.requireOutsideDungeon(owner);
		if (request.operation().equals("BUY")) {
			var product = shops.view(owner)
				.products()
				.stream()
				.filter(p -> p.code().equals(request.code()))
				.findFirst()
				.orElseThrow(() -> new BusinessException(409, "请前往装备商店购买已解锁商品"));
			characters.changeGold(owner, -product.price());
			String source = "EQUIPMENT_BUY:" + request.requestId();
			inventory.grantStrict(owner, source, product.code(), 1, true, false);
			// 卖价来自实际成交价，不由绑定状态或商品模板推断；穿戴后仍保留。
			var bought = items.source(character, source);
			bought.setSellPrice(product.price() / 5);
		}
		else {
			long itemId;
			try {
				itemId = Long.parseLong(request.expected().id());
			}
			catch (NumberFormatException error) {
				throw new BusinessException(400, "装备编号无效");
			}
			var item = items.get(itemId);
			if (item == null || item.getCharacterId() != character)
				throw new BusinessException(404, "装备不存在");
			if (item.getSlot() == null || item.getQuantity() != 1 || item.isEquipped())
				throw new BusinessException(409, "请先卸下可用装备再操作");
			if (!codec.view(item).equals(request.expected()))
				throw new BusinessException(409, "装备信息已变化，请重新确认");
			if (request.operation().equals("SELL")) {
				if (!shops.view(owner).available() || item.getSellPrice() <= 0)
					throw new BusinessException(409, "请在装备商店出售允许回收的装备");
				characters.changeGold(owner, item.getSellPrice());
			}
			// 数量归零仍保留来源，销毁后不能通过重放旧奖励免费领回。
			item.setQuantity(0);
		}
		var receipt = new EquipmentReceipt();

		receipt.setCharacterId(character);
		receipt.setRequestId(request.requestId());
		receipt.setPayload(payload);
		receipt.setCreatedAt(Instant.now());
		receipts.saveAndFlush(receipt);
	}

	private void validate(EquipmentCommerceRequest request) {
		if (request == null || request.operation() == null || request.requestId() == null)
			throw new BusinessException(400, "请确认装备操作");
		try {
			if (!UUID.fromString(request.requestId()).toString().equals(request.requestId()))
				throw new IllegalArgumentException();
		}
		catch (IllegalArgumentException error) {
			throw new BusinessException(400, "装备请求号无效");
		}
		switch (request.operation()) {
			case "BUY" -> {
				if (request.code() == null || request.expected() != null)
					throw new BusinessException(400, "购买参数无效");
			}
			case "SELL", "DESTROY" -> {
				if (request.expected() == null || request.expected().id() == null || request.code() != null)
					throw new BusinessException(400, "请确认具体装备");
			}
			default -> throw new BusinessException(400, "装备操作无效");
		}
	}

}
