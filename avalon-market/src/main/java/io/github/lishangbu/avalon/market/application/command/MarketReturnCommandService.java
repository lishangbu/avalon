package io.github.lishangbu.avalon.market.application.command;

import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.creature.application.command.CreatureCustodyService;
import io.github.lishangbu.avalon.market.persistence.entity.MarketListing;
import io.github.lishangbu.avalon.market.persistence.repository.MarketListingRepository;
import io.github.lishangbu.avalon.platform.BusinessException;
import org.springframework.stereotype.Component;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import java.time.Instant;

/** 取消只转待领不直接入包；领取与实物交付同事务，满包不会丢失退货。 */
@Component
@RequiredArgsConstructor
public class MarketReturnCommandService {

	private final CharacterCommandService characters;

	private final MarketAccess access;

	private final MarketListingRepository listings;

	private final MarketReceipts receipts;

	private final MarketItemDelivery custody;

	private final CreatureCustodyService creatureCustody;

	@Transactional
	public Result cancel(long owner, Request request) {
		validate(request);
		characters.lock(owner);
		var prior = receipts.replay(owner, request.requestId(), "CANCEL", request, Result.class);
		if (prior != null)
			return prior;
		access.require(owner);
		var listing = owned(owner, request.listingId());
		if (!"ACTIVE".equals(listing.getState()) && !"RETURN_PENDING".equals(listing.getState()))
			throw new BusinessException(409, "挂单已结束");
		listing.setState("RETURN_PENDING");
		var result = new Result(listing.getId().toString(), listing.getRemaining(), null);
		receipts.save(owner, request.requestId(), "CANCEL", request, result);
		return result;
	}

	@Transactional
	public Result claim(long owner, Claim request) {
		if (request == null || request.quantity() <= 0)
			throw new BusinessException(400, "退货领取数量无效");
		receipts.validate(request.requestId());
		characters.lock(owner);
		var prior = receipts.replay(owner, request.requestId(), "CLAIM_RETURN", request, Result.class);
		if (prior != null)
			return prior;
		access.require(owner);
		var listing = owned(owner, request.listingId());
		// 主动领取不等待后台扫描；到期检查与领取在同一锁内进行。
		if ("ACTIVE".equals(listing.getState()) && !Instant.now().isBefore(listing.getExpiresAt()))
			listing.setState("RETURN_PENDING");
		if (!"RETURN_PENDING".equals(listing.getState()) || request.quantity() > listing.getRemaining())
			throw new BusinessException(409, "退货状态或剩余数量已变化");
		String item = switch (listing.getKind()) {
			case "ITEM" -> custody.receive(listing, owner, request.quantity(),
					"MARKET:RETURN:" + owner + ":" + request.requestId());
			case "CREATURE" -> creatureCustody.receive(owner, owner, listing.getAssetId());
			default -> throw new BusinessException(409, "暂不支持该退货类型");
		};
		listing.setRemaining(listing.getRemaining() - request.quantity());
		if (listing.getRemaining() == 0)
			listing.setState("RETURNED");
		var result = new Result(listing.getId().toString(), listing.getRemaining(), item);
		receipts.save(owner, request.requestId(), "CLAIM_RETURN", request, result);
		return result;
	}

	/** 后台恢复入口：先取得卖方角色锁再锁挂单，不能以扫描结果直接覆盖已经成交的状态。 */
	@Transactional
	public void expire(long id) {
		Long seller = listings.seller(id);
		if (seller == null)
			return;
		characters.lock(seller);
		var listing = listings.lock(id);
		if ("ACTIVE".equals(listing.getState()) && !Instant.now().isBefore(listing.getExpiresAt()))
			listing.setState("RETURN_PENDING");
	}

	private MarketListing owned(long owner, long id) {
		// 归属先用标量检查，禁止拿着自己的角色锁再锁其他卖家的挂单。
		Long seller = listings.seller(id);
		if (seller == null || seller != owner)
			throw new BusinessException(404, "挂单不存在");
		return listings.lock(id);
	}

	private void validate(Request request) {
		if (request == null)
			throw new BusinessException(400, "缺少取消请求");
		receipts.validate(request.requestId());
	}

	public record Request(String requestId, long listingId) {
	}

	public record Claim(String requestId, long listingId, int quantity) {
	}

	public record Result(String listingId, int remaining, String itemId) {
	}

}
