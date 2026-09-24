package io.github.lishangbu.avalon.market.application.command;

import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.content.creature.CreatureCatalog;
import io.github.lishangbu.avalon.creature.application.command.CreatureCustodyService;
import io.github.lishangbu.avalon.creature.application.model.CreatureView;
import io.github.lishangbu.avalon.inventory.application.command.ItemCustodyService;
import io.github.lishangbu.avalon.inventory.application.model.InventoryItemView;
import io.github.lishangbu.avalon.market.domain.MarketRules;
import io.github.lishangbu.avalon.market.persistence.entity.MarketListing;
import io.github.lishangbu.avalon.market.persistence.repository.MarketListingRepository;
import io.github.lishangbu.avalon.platform.BusinessException;
import org.springframework.stereotype.Component;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.HashSet;
import io.github.lishangbu.avalon.inventory.domain.BagCapacityRules;

/** 上架扣实物、建挂单与请求回执同事务完成；失败不会留下无挂单的托管物品。 */
@Component
@RequiredArgsConstructor
public class MarketListingCommandService {

	private final CharacterCommandService characters;

	private final MarketAccess access;

	private final MarketListingRepository listings;

	private final MarketReceipts receipts;

	private final ItemCustodyService custody;

	private final CreatureCustodyService creatureCustody;

	private final CreatureCatalog creatures;

	@Transactional
	public Result listItem(long owner, Request request) {
		if (request == null || request.expected() == null)
			throw new BusinessException(400, "缺少上架确认信息");
		return listItems(owner, request.requestId(), request.unitPrice(), request.quantity(),
				List.of(new Selection(request.itemId(), request.quantity(), request.expected())), request, "LIST_ITEM");
	}

	/** 同种非装备材料合为一单，逐批校验确认快照，并保留原始奖励出处。 */
	@Transactional
	public Result listBatch(long owner, BatchRequest request) {
		if (request == null || request.items() == null || request.items().isEmpty()
				|| request.items().size() > BagCapacityRules.CAPACITY * BagCapacityRules.STACK_LIMIT)
			throw new BusinessException(400, "缺少有效的材料上架确认信息");
		var ids = new HashSet<Long>();
		String code = null;
		long total = 0;
		for (var selected : request.items()) {
			if (selected == null || selected.expected() == null || selected.expected().slot() != null
					|| selected.quantity() <= 0 || !ids.add(selected.itemId()))
				throw new BusinessException(400, "合并上架仅支持不重复的同种材料");
			if (code != null && !code.equals(selected.expected().code()))
				throw new BusinessException(400, "合并上架仅支持同种材料");
			code = selected.expected().code();
			total += selected.quantity();
		}
		if (total > BagCapacityRules.CAPACITY * BagCapacityRules.STACK_LIMIT)
			throw new BusinessException(400, "上架数量超过背包容量");
		return listItems(owner, request.requestId(), request.unitPrice(), (int) total, request.items(), request,
				"LIST_ITEM_BATCH");
	}

	private Result listItems(long owner, String requestId, long unitPrice, int quantity, List<Selection> selections,
			Object request, String operation) {
		receipts.validate(requestId);
		MarketRules.price(unitPrice, quantity);
		characters.lock(owner);
		var prior = receipts.replay(owner, requestId, operation, request, Result.class);
		if (prior != null)
			return prior;
		access.require(owner);
		Instant now = Instant.now();
		// 扫描尚未运行时，到期挂单也按待领处理并阻止继续上架；不通过抛错事务内改状态。
		if (listings.hasReturns(owner) || listings.overdue(owner, now) > 0)
			throw new BusinessException(409, "请先领取市场退货再上架");
		if (listings.active(owner) >= MarketRules.MAX_LISTINGS)
			throw new BusinessException(409, "最多同时上架20单");
		var listing = new MarketListing();
		for (var selected : selections) {
			long asset = custody.deposit(owner, selected.itemId(), selected.quantity(), selected.expected());
			listing.getItemAssets().put(asset, selected.quantity());
		}

		listing.setSellerAccountId(owner);
		listing.setAssetId(listing.getItemAssets().keySet().iterator().next());
		listing.setKind("ITEM");
		var item = selections.getFirst().expected(); // deposit 已在角色锁内对照真实库存逐字段核验。
		listing.setItemCode(item.code());
		listing.setItemName(item.name());
		listing.setItemSlot(item.slot());
		listing.setItemQuality(item.quality());
		listing.setItemLevel(item.requiredLevel());
		listing.setEnhancementLevel(item.slot() == null ? null : item.enhancementLevel());
		listing.setItemCategory(item.slot() != null ? "EQUIPMENT" : item.code().startsWith("BOOK_") ? "BOOK"
				: item.code().startsWith("FRAGMENT_") ? "FRAGMENT" : "MATERIAL");
		listing.setState("ACTIVE");
		listing.setUnitPrice(unitPrice);
		listing.setOriginalQuantity(quantity);
		listing.setRemaining(quantity);
		listing.setCreatedAt(now);
		listing.setExpiresAt(MarketRules.expires(now));
		listings.saveAndFlush(listing);
		var result = new Result(listing.getId().toString(), listing.getExpiresAt());
		receipts.save(owner, requestId, operation, request, result);
		return result;
	}

	/** 幻兽逐只上架；搜索名称使用物种名，玩家昵称只作为个体详情展示。 */
	@Transactional
	public Result listCreature(long owner, CreatureRequest request) {
		if (request == null || request.expected() == null)
			throw new BusinessException(400, "缺少伙伴上架确认信息");
		receipts.validate(request.requestId());
		MarketRules.price(request.unitPrice(), 1);
		characters.lock(owner);
		var prior = receipts.replay(owner, request.requestId(), "LIST_CREATURE", request, Result.class);
		if (prior != null)
			return prior;
		access.require(owner);
		Instant now = Instant.now();
		if (listings.hasReturns(owner) || listings.overdue(owner, now) > 0)
			throw new BusinessException(409, "请先领取市场退货再上架");
		if (listings.active(owner) >= MarketRules.MAX_LISTINGS)
			throw new BusinessException(409, "最多同时上架20单");
		long asset = creatureCustody.deposit(owner, request.creatureId(), request.expected());
		var listing = new MarketListing();

		listing.setSellerAccountId(owner);
		listing.setAssetId(asset);
		listing.setKind("CREATURE");
		listing.setItemCode(request.expected().species());
		listing.setItemName(creatures.pet(request.expected().species()).name());
		listing.setItemLevel(request.expected().level());
		listing.setItemCategory("CREATURE");
		listing.setCreatureHidden("H".equals(request.expected().abilitySlot()));
		listing.getCreatureSkills().addAll(request.expected().skills());
		listing.setState("ACTIVE");
		listing.setUnitPrice(request.unitPrice());
		listing.setOriginalQuantity(1);
		listing.setRemaining(1);
		listing.setCreatedAt(now);
		listing.setExpiresAt(MarketRules.expires(now));
		listings.saveAndFlush(listing);
		var result = new Result(listing.getId().toString(), listing.getExpiresAt());
		receipts.save(owner, request.requestId(), "LIST_CREATURE", request, result);
		return result;
	}

	public record Request(String requestId, long itemId, int quantity, long unitPrice, InventoryItemView expected) {
	}

	public record Selection(long itemId, int quantity, InventoryItemView expected) {
	}

	public record BatchRequest(String requestId, List<Selection> items, long unitPrice) {
	}

	public record CreatureRequest(String requestId, long creatureId, long unitPrice, CreatureView expected) {
	}

	public record Result(String listingId, Instant expiresAt) {
	}

}
