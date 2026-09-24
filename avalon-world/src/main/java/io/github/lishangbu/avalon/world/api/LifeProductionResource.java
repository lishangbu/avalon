package io.github.lishangbu.avalon.world.api;

import io.github.lishangbu.avalon.platform.CurrentAccount;
import io.github.lishangbu.avalon.world.application.command.LifeCraftingService;
import io.github.lishangbu.avalon.world.application.command.LifeShopService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import io.github.lishangbu.avalon.world.application.query.LifeProductionQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 生产请求只描述意图，材料成本、资格与工具报废均由服务端结算。 */
@RestController
@RequestMapping("/api/life/production")
@RequiredArgsConstructor
public class LifeProductionResource {

	private final CurrentAccount identity;

	private final LifeProductionQueryService queries;

	private final LifeCraftingService crafting;

	private final LifeShopService shop;

	@PostMapping("/sell")
	public int sell(@RequestBody(required = false) LifeShopService.Request request) {
		return shop.sell(identity.id(), request);
	}

	@PostMapping("/buy")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void buy(@RequestBody(required = false) LifeShopService.Request request) {
		shop.buy(identity.id(), request);
	}

	@GetMapping
	public LifeProductionQueryService.View get() {
		return queries.view(identity.id());
	}

	@PostMapping
	public LifeCraftingService.Result craft(@RequestBody(required = false) LifeCraftingService.Request request) {
		return crafting.craft(identity.id(), request);
	}

}
