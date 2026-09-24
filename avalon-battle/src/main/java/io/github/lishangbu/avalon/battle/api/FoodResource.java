package io.github.lishangbu.avalon.battle.api;

import io.github.lishangbu.avalon.battle.application.command.FoodCommandService;
import io.github.lishangbu.avalon.battle.application.query.FoodQueryService;
import io.github.lishangbu.avalon.platform.CurrentAccount;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/life/food")
@RequiredArgsConstructor
public class FoodResource {

	private final CurrentAccount identity;

	private final FoodQueryService queries;

	private final FoodCommandService commands;

	@GetMapping
	public FoodQueryService.View get() {
		return queries.view(identity.id());
	}

	@PostMapping
	public FoodCommandService.Result eat(@RequestBody(required = false) FoodCommandService.Request request) {
		return commands.eat(identity.id(), request);
	}

}
