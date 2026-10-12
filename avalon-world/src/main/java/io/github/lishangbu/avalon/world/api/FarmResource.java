package io.github.lishangbu.avalon.world.api;

import io.github.lishangbu.avalon.platform.CurrentAccount;
import io.github.lishangbu.avalon.world.application.command.FarmCommandService;
import io.github.lishangbu.avalon.world.application.command.FarmProductionService;
import io.github.lishangbu.avalon.world.application.model.FarmProductionRequest;
import io.github.lishangbu.avalon.world.application.model.FarmProductionResult;
import io.github.lishangbu.avalon.world.application.model.FarmRequest;
import io.github.lishangbu.avalon.world.application.model.FarmState;
import io.github.lishangbu.avalon.world.application.model.FarmView;
import io.github.lishangbu.avalon.world.application.query.FarmQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/life/farm")
@RequiredArgsConstructor
public class FarmResource {

	private final CurrentAccount identity;

	private final FarmQueryService queries;

	private final FarmCommandService management;

	private final FarmProductionService production;

	@GetMapping
	public FarmView get() {
		return queries.view(identity.id());
	}

	@PostMapping("/manage")
	public FarmState manage(@RequestBody(required = false) FarmRequest request) {
		return management.manage(identity.id(), request);
	}

	@PostMapping("/produce")
	public FarmProductionResult produce(@RequestBody(required = false) FarmProductionRequest request) {
		return production.perform(identity.id(), request);
	}

}
