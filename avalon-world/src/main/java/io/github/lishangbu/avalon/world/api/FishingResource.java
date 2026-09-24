package io.github.lishangbu.avalon.world.api;

import io.github.lishangbu.avalon.platform.CurrentAccount;
import io.github.lishangbu.avalon.world.application.command.FishingCommandService;
import io.github.lishangbu.avalon.world.application.model.FishingViews;
import io.github.lishangbu.avalon.world.application.query.FishingQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/life/fishing")
@RequiredArgsConstructor
public class FishingResource {

	private final CurrentAccount identity;

	private final FishingCommandService commands;

	private final FishingQueryService queries;

	private final FishingViews views;

	@GetMapping
	public FishingQueryService.View get() {
		return queries.view(identity.id());
	}

	@PostMapping("/start")
	public FishingViews.Snapshot start(@RequestBody(required = false) FishingCommandService.Start request) {
		return views.view(commands.start(identity.id(), request));
	}

	@PostMapping("/input")
	public FishingViews.Snapshot input(@RequestBody(required = false) FishingCommandService.Input request) {
		return views.view(commands.input(identity.id(), request));
	}

}
