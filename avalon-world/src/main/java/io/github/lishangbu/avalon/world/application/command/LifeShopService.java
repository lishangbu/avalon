package io.github.lishangbu.avalon.world.application.command;

import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.content.life.LifeProductionCatalog;
import io.github.lishangbu.avalon.inventory.application.command.InventoryCommandService;
import io.github.lishangbu.avalon.inventory.application.query.InventoryQueryService;
import io.github.lishangbu.avalon.platform.BusinessException;
import io.github.lishangbu.avalon.world.application.model.LifeShopRequest;
import io.github.lishangbu.avalon.world.persistence.repository.LifeOperationRepository;
import jakarta.transaction.Transactional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 木工具补购不要求制造专业，允许专业之间从商店重新启动生产。 */
@Component
@RequiredArgsConstructor
public class LifeShopService {

	private final CharacterCommandService characters;

	private final LifeProductionCatalog catalog;

	private final InventoryCommandService inventory;

	private final LifeOperationRepository receipts;

	private final InventoryQueryService queries;

	@Transactional
	public int sell(long owner, LifeShopRequest request) {
		try {
			if (request == null || !UUID.fromString(request.requestId()).toString().equals(request.requestId())
					|| Long.parseLong(request.item()) <= 0 || request.quantity() < 1 || request.quantity() > 99)
				throw new IllegalArgumentException();
		}
		catch (IllegalArgumentException | NullPointerException error) {
			throw new BusinessException(400, "出售请求无效");
		}
		var context = characters.lock(owner);
		String payload = "LIFE_SELL:" + request.item() + ":" + request.quantity();
		var prior = receipts.prior(context.id(), request.requestId());
		if (prior != null) {
			if (!prior.payload().equals(payload))
				throw new BusinessException(409, "请求号已用于其他操作");
			return Integer.parseInt(prior.result());
		}
		characters.requireOutsideDungeon(owner);
		characters.requireNoLifeActivity(owner);
		if (!Set.of("SUPPLY_SHOP", "CITY_SHOP").contains(context.location()))
			throw new BusinessException(409, "请到生活用品商店出售");
		var item = queries.list(owner)
			.stream()
			.filter(value -> value.id().equals(request.item()))
			.findFirst()
			.orElseThrow(() -> new BusinessException(409, "所选物品不存在"));
		Integer price = catalog.sellPrices().get(item.code());
		if (price == null)
			throw new BusinessException(409, "该物品不在生活回收清单内");
		int gold = Math.multiplyExact(price, request.quantity());
		inventory.consumeForSale(owner, Long.parseLong(item.id()), item.code(), request.quantity());
		characters.changeGold(owner, gold);
		receipts.record(context.id(), request.requestId(), payload, Integer.toString(gold));
		return gold;
	}

	@Transactional
	public void buy(long owner, LifeShopRequest request) {
		if (request == null || request.item() == null || request.quantity() < 1 || request.quantity() > 99)
			throw new BusinessException(400, "请选择商品与购买数量");
		try {
			if (!UUID.fromString(request.requestId()).toString().equals(request.requestId()))
				throw new IllegalArgumentException();
		}
		catch (IllegalArgumentException | NullPointerException error) {
			throw new BusinessException(400, "购买请求号无效");
		}
		var context = characters.lock(owner);
		String payload = "BUY:" + request.item() + ":" + request.quantity();
		var prior = receipts.prior(context.id(), request.requestId());
		if (prior != null) {
			if (!prior.payload().equals(payload))
				throw new BusinessException(409, "请求号已用于其他生活操作");
			return;
		}
		characters.requireOutsideDungeon(owner);
		var offer = catalog.offers()
			.stream()
			.filter(value -> value.item().equals(request.item()))
			.findFirst()
			.orElseThrow(() -> new BusinessException(404, "商品不存在"));
		if (!offer.locations().contains(context.location()))
			throw new BusinessException(409, "请到生活用品商店购买");
		characters.changeGold(owner, -Math.multiplyExact(offer.price(), request.quantity()));
		inventory.grantStrict(owner, "LIFE_SHOP:" + request.requestId(), offer.item(), request.quantity(), false,
				false);
		receipts.record(context.id(), request.requestId(), payload, "{}");
	}

}
