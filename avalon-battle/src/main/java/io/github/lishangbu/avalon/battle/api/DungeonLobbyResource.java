package io.github.lishangbu.avalon.battle.api;

import io.github.lishangbu.avalon.battle.api.model.DungeonLobbyCreate;
import io.github.lishangbu.avalon.battle.api.model.DungeonLobbyCreated;
import io.github.lishangbu.avalon.battle.api.model.DungeonLobbyCurrent;
import io.github.lishangbu.avalon.battle.api.model.DungeonLobbyStarted;
import io.github.lishangbu.avalon.battle.application.command.DungeonLobbyCommandService;
import io.github.lishangbu.avalon.battle.application.model.DungeonLobbyView;
import io.github.lishangbu.avalon.battle.application.model.DungeonRecruitmentNotice;
import io.github.lishangbu.avalon.battle.application.model.DungeonRecruitmentPage;
import io.github.lishangbu.avalon.battle.application.model.DungeonRecruitmentReplacementPage;
import io.github.lishangbu.avalon.battle.application.query.DungeonRecruitmentQueryService;
import io.github.lishangbu.avalon.platform.BusinessException;
import io.github.lishangbu.avalon.platform.CurrentAccount;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** 公开招募入口。所有成员操作只作用于认证账号，不接受客户端账号或角色名单。 */
@RestController
@RequestMapping("/api/dungeon/lobbies")
@RequiredArgsConstructor
public class DungeonLobbyResource {

	private final DungeonLobbyCommandService commands;

	private final DungeonRecruitmentQueryService queries;

	private final CurrentAccount identity;

	private long owner() {
		return identity.id();
	}

	@GetMapping
	public DungeonRecruitmentPage list(@RequestParam(value = "after", defaultValue = "0") long after) {
		if (after < 0)
			throw new BusinessException(400, "分页位置无效");
		return queries.recruitment(after);
	}

	@GetMapping("/replacements")
	public DungeonRecruitmentReplacementPage replacements(
			@RequestParam(value = "after", defaultValue = "0") long after) {
		if (after < 0)
			throw new BusinessException(400, "分页位置无效");
		return queries.replacements(after);
	}

	@PostMapping("/replacements/{run}")
	public DungeonLobbyCreated replacement(@PathVariable("run") long run,
			@RequestBody(required = false) DungeonLobbyCreate request) {
		if (request == null)
			throw new BusinessException(400, "缺少请求参数");
		return new DungeonLobbyCreated(Long.toString(commands.replacement(run, owner(), request.requestId())));
	}

	@GetMapping("/current")
	public DungeonLobbyCurrent current() {
		return new DungeonLobbyCurrent(queries.current(owner()));
	}

	@GetMapping("/notice")
	public DungeonRecruitmentNotice notice() {
		return queries.notice(owner());
	}

	@PostMapping("/match")
	public DungeonLobbyCreated match() {
		return new DungeonLobbyCreated(Long.toString(commands.match(owner())));
	}

	@GetMapping("/{id}")
	public DungeonLobbyView view(@PathVariable("id") long id) {
		return queries.view(id, owner());
	}

	@PostMapping
	public DungeonLobbyCreated create(@RequestBody(required = false) DungeonLobbyCreate request) {
		if (request == null)
			throw new BusinessException(400, "缺少请求参数");
		return new DungeonLobbyCreated(Long.toString(commands.create(owner(), request.requestId())));
	}

	@PostMapping("/{id}/join")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void join(@PathVariable("id") long id) {
		commands.join(id, owner());
	}

	@PostMapping("/{id}/confirm")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void confirm(@PathVariable("id") long id) {
		commands.confirm(id, owner());
	}

	@PostMapping("/{id}/ready")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void ready(@PathVariable("id") long id) {
		commands.ready(id, owner());
	}

	@PostMapping("/{id}/leave")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void leave(@PathVariable("id") long id) {
		commands.leave(id, owner());
	}

	@PostMapping("/{id}/lead")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void lead(@PathVariable("id") long id) {
		commands.lead(id, owner());
	}

	@PostMapping("/{id}/launch")
	public DungeonLobbyStarted launch(@PathVariable("id") long id) {
		return new DungeonLobbyStarted(Long.toString(commands.launch(id, owner())));
	}

}
