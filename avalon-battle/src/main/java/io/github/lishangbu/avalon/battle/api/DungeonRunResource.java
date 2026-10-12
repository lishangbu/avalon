package io.github.lishangbu.avalon.battle.api;

import io.github.lishangbu.avalon.battle.api.model.DungeonRunMove;
import io.github.lishangbu.avalon.battle.api.model.DungeonRunRecruit;
import io.github.lishangbu.avalon.battle.api.model.DungeonRunStarted;
import io.github.lishangbu.avalon.battle.application.command.DungeonRunCommandService;
import io.github.lishangbu.avalon.battle.application.model.DungeonProgressCurrent;
import io.github.lishangbu.avalon.battle.application.model.DungeonRunView;
import io.github.lishangbu.avalon.battle.application.query.DungeonProgressQueryService;
import io.github.lishangbu.avalon.platform.BusinessException;
import io.github.lishangbu.avalon.platform.CurrentAccount;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** 战间命令只操作认证成员本人；不接受客户端编队或库存，也不提供跳过确认的开本入口。 */
@RestController
@RequestMapping("/api/dungeon/runs")
@RequiredArgsConstructor
public class DungeonRunResource {

	private final DungeonRunCommandService commands;

	private final DungeonProgressQueryService queries;

	private final CurrentAccount identity;

	private long owner() {
		return identity.id();
	}

	@GetMapping("/current")
	public DungeonProgressCurrent current() {
		return queries.current(owner());
	}

	@GetMapping("/{id}")
	public DungeonRunView view(@PathVariable("id") long id) {
		return queries.view(id, owner());
	}

	@PostMapping("/{id}/recruit")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void recruit(@PathVariable("id") long id, @RequestBody(required = false) DungeonRunRecruit request) {
		if (request == null || request.enabled() == null)
			throw new BusinessException(400, "请选择招募状态");
		commands.recruit(id, owner(), request.enabled());
	}

	@PostMapping("/{id}/presence")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void presence(@PathVariable("id") long id) {
		commands.presence(id, owner());
	}

	@PostMapping("/{id}/ready")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void ready(@PathVariable("id") long id) {
		commands.ready(id, owner());
	}

	@PostMapping("/{id}/rest")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void rest(@PathVariable("id") long id) {
		commands.rest(id, owner());
	}

	@PostMapping("/{id}/move")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void move(@PathVariable("id") long id, @RequestBody(required = false) DungeonRunMove request) {
		if (request == null || request.location() == null)
			throw new BusinessException(400, "请选择路线位置");
		commands.move(id, owner(), request.location());
	}

	@PostMapping("/{id}/begin")
	public DungeonRunStarted begin(@PathVariable("id") long id) {
		return new DungeonRunStarted(Long.toString(commands.begin(id, owner())));
	}

	@PostMapping("/{id}/leave")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void leave(@PathVariable("id") long id) {
		commands.leave(id, owner());
	}

}
