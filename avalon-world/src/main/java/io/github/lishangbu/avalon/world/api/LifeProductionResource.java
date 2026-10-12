package io.github.lishangbu.avalon.world.api;

import io.github.lishangbu.avalon.platform.CurrentAccount;
import io.github.lishangbu.avalon.world.application.command.LifeCraftingService;
import io.github.lishangbu.avalon.world.application.command.LifeShopService;
import io.github.lishangbu.avalon.world.application.model.LifeCraftingRequest;
import io.github.lishangbu.avalon.world.application.model.LifeCraftingResult;
import io.github.lishangbu.avalon.world.application.model.LifeProductionView;
import io.github.lishangbu.avalon.world.application.model.LifeShopRequest;
import io.github.lishangbu.avalon.world.application.query.LifeProductionQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
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
	public int sell(@RequestBody(required = false) LifeShopRequest request) {
		return shop.sell(identity.id(), request);
	}

	@PostMapping("/buy")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void buy(@RequestBody(required = false) LifeShopRequest request) {
		shop.buy(identity.id(), request);
	}

	@GetMapping
	public LifeProductionView get() {
		return queries.view(identity.id());
	}

	@PostMapping
	public LifeCraftingResult craft(@RequestBody(required = false) LifeCraftingRequest request) {
		return crafting.craft(identity.id(), request);
	}

}
