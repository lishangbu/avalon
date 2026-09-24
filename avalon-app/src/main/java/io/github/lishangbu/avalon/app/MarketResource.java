package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.market.application.command.MarketAccess;
import io.github.lishangbu.avalon.creature.application.model.CreatureView;
import io.github.lishangbu.avalon.market.application.query.MarketQueryService;
import io.github.lishangbu.avalon.market.application.command.MarketListingCommandService;
import io.github.lishangbu.avalon.market.application.command.MarketPurchaseCommandService;
import io.github.lishangbu.avalon.market.application.command.MarketReturnCommandService;
import io.github.lishangbu.avalon.platform.BusinessException;
import io.github.lishangbu.avalon.platform.CurrentAccount;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import java.util.List;

/** 市场协议入口只取认证账号，不能由请求指定卖方身份；同步数据库事务在虚拟线程执行。 */
@RestController
@RequestMapping("/api/market")
@RequiredArgsConstructor
public class MarketResource {

	private final CurrentAccount identity;

	private final MarketAccess access;

	private final MarketListingCommandService listings;

	private final MarketPurchaseCommandService purchases;

	private final MarketReturnCommandService returns;

	private final MarketQueryService queries;

	@GetMapping("/creatures/eligible")
	public List<CreatureView> eligibleCreatures() {
		return queries.eligibleCreatures(owner());
	}

	@GetMapping("/search")
	public MarketQueryService.Page search(@RequestParam(value = "name", defaultValue = "") String name,
			@RequestParam(value = "category", defaultValue = "ALL") String category,
			@RequestParam(value = "slot", required = false) String slot,
			@RequestParam(value = "quality", required = false) String quality,
			@RequestParam(value = "minimum", required = false) Integer minimum,
			@RequestParam(value = "maximum", required = false) Integer maximum,
			@RequestParam(value = "sort", defaultValue = "NEWEST") String sort,
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "size", defaultValue = "20") int size,
			@RequestParam(value = "species", required = false) String species,
			@RequestParam(value = "hidden", required = false) Boolean hidden,
			@RequestParam(value = "skill", required = false) String skill,
			@RequestParam(value = "enhancementLevel", required = false) Integer enhancementLevel) {
		return queries.search(owner(), name, category, slot, quality, minimum, maximum, sort, page, size,
				new MarketQueryService.CreatureFilter(species, hidden, skill), enhancementLevel);
	}

	@GetMapping("/listings/{id}")
	public MarketQueryService.Detail detail(@PathVariable("id") long id) {
		return queries.detail(owner(), id);
	}

	@GetMapping("/access")
	public MarketQueryService.Access access() {
		return queries.access(owner());
	}

	@GetMapping("/mine")
	public MarketQueryService.Page mine(@RequestParam(value = "state", defaultValue = "ALL") String state,
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "size", defaultValue = "20") int size) {
		return queries.mine(owner(), state, page, size);
	}

	private long owner() {
		return identity.id();
	}

	@PostMapping("/teaching")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void teach(@RequestBody(required = false) Teaching request) {
		if (request == null)
			throw new BusinessException(400, "缺少交易教学确认");
		access.teach(owner(), request.accepted());
	}

	@PostMapping("/items")
	public MarketListingCommandService.Result list(
			@RequestBody(required = false) MarketListingCommandService.Request request) {
		return listings.listItem(owner(), request);
	}

	@PostMapping("/creatures")
	public MarketListingCommandService.Result listCreature(
			@RequestBody(required = false) MarketListingCommandService.CreatureRequest request) {
		return listings.listCreature(owner(), request);
	}

	@PostMapping("/items/batch")
	public MarketListingCommandService.Result listBatch(
			@RequestBody(required = false) MarketListingCommandService.BatchRequest request) {
		return listings.listBatch(owner(), request);
	}

	@PostMapping("/purchase")
	public MarketPurchaseCommandService.Result buy(
			@RequestBody(required = false) MarketPurchaseCommandService.Request request) {
		return purchases.buy(owner(), request);
	}

	@PostMapping("/cancel")
	public MarketReturnCommandService.Result cancel(
			@RequestBody(required = false) MarketReturnCommandService.Request request) {
		return returns.cancel(owner(), request);
	}

	@PostMapping("/returns/claim")
	public MarketReturnCommandService.Result claim(
			@RequestBody(required = false) MarketReturnCommandService.Claim request) {
		return returns.claim(owner(), request);
	}

	public record Teaching(boolean accepted) {
	}

}
