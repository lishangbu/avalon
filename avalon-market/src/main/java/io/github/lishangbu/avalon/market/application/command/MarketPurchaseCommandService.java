package io.github.lishangbu.avalon.market.application.command;

import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.creature.application.command.CreatureCustodyService;
import io.github.lishangbu.avalon.market.application.model.MarketPurchaseRequest;
import io.github.lishangbu.avalon.market.application.model.MarketPurchaseResult;
import io.github.lishangbu.avalon.market.domain.MarketRules;
import io.github.lishangbu.avalon.market.persistence.repository.MarketListingRepository;
import io.github.lishangbu.avalon.platform.BusinessException;
import jakarta.transaction.Transactional;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 买方扣款、卖方税后到账、实物交付、挂单余量和回执共享一个数据库事务。 */
@Component
@RequiredArgsConstructor
public class MarketPurchaseCommandService {

	private final CharacterCommandService characters;

	private final MarketParticipantLocks locks;

	private final MarketListingRepository listings;

	private final MarketAccess access;

	private final MarketReceipts receipts;

	private final MarketItemDelivery custody;

	private final CreatureCustodyService creatureCustody;

	@Transactional
	public MarketPurchaseResult buy(long buyer, MarketPurchaseRequest request) {
		if (request == null)
			throw new BusinessException(400, "缺少购买确认信息");
		receipts.validate(request.requestId());
		var price = MarketRules.price(request.unitPrice(), request.quantity());
		Long seller = listings.seller(request.listingId());
		if (seller == null)
			throw new BusinessException(404, "挂单不存在");
		if (seller == buyer)
			throw new BusinessException(409, "不能购买自己的挂单");
		locks.acquire(seller, buyer);
		var prior = receipts.replay(buyer, request.requestId(), "BUY", request, MarketPurchaseResult.class);
		if (prior != null)
			return prior;
		access.require(buyer);
		var listing = listings.lock(request.listingId());
		MarketRules.requirePurchase(seller, buyer, listing.getState(), listing.getExpiresAt(), Instant.now(),
				listing.getRemaining(), request.quantity());
		if (listing.getUnitPrice() != request.unitPrice())
			throw new BusinessException(409, "成交单价已变化");
		try {
			characters.changeGold(buyer, -price.gross());
			characters.changeGold(seller, price.sellerNet());
		}
		catch (ArithmeticException error) {
			throw new BusinessException(409, "成交后金币余额超出支持范围");
		}
		String delivered = switch (listing.getKind()) {
			case "ITEM" ->
				custody.receive(listing, buyer, request.quantity(), "MARKET:BUY:" + buyer + ":" + request.requestId());
			case "CREATURE" -> creatureCustody.receive(seller, buyer, listing.getAssetId());
			default -> throw new BusinessException(409, "该商品类型尚不支持购买");
		};
		listing.setRemaining(listing.getRemaining() - request.quantity());
		if (listing.getRemaining() == 0)
			listing.setState("SOLD");
		var result = new MarketPurchaseResult(listing.getId().toString(), delivered, request.quantity(),
				Long.toString(price.gross()), Long.toString(price.tax()), Long.toString(price.sellerNet()));
		receipts.save(buyer, request.requestId(), "BUY", request, result);
		return result;
	}

}
