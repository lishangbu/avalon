package io.github.lishangbu.avalon.battle.api;

import io.github.lishangbu.avalon.battle.application.command.BattleCommandService;
import io.github.lishangbu.avalon.battle.application.model.BattleLobby;
import io.github.lishangbu.avalon.battle.application.model.CharacterCombatProfile;
import io.github.lishangbu.avalon.battle.application.model.BattleView;
import io.github.lishangbu.avalon.battle.application.query.BattleQueryService;
import io.github.lishangbu.avalon.battle.domain.BattleState.Order;
import io.github.lishangbu.avalon.platform.BusinessException;
import io.github.lishangbu.avalon.platform.CurrentAccount;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

/**
 * 认证身份决定所有权，客户端只提供命令参数。同步 JDBC 在虚拟线程运行。
 */
@RestController
@RequestMapping("/api/battle")
@RequiredArgsConstructor
public class BattleResource {

	private final BattleCommandService commands;

	private final BattleQueryService queries;

	private final CurrentAccount identity;

	private long owner() {
		return identity.id();
	}

	@GetMapping
	public Current current() {
		return new Current(queries.current(owner()));
	}

	@GetMapping("/character-profile")
	public CharacterCombatProfile characterProfile() {
		return queries.characterProfile(owner());
	}

	@GetMapping("/lobby")
	public BattleLobby lobby() {
		return queries.lobby(owner());
	}

	@PostMapping
	public BattleView start(@RequestBody(required = false) Start request) {
		require(request);
		return commands.start(owner(), request.encounter(), request.requestId());
	}

	@PostMapping("/{id}/turn")
	public BattleView turn(@PathVariable("id") long id, @RequestBody(required = false) Turn request) {
		require(request);
		return commands.turn(owner(), id, request.round(), request.orders(), request.surrender(),
				request.replacements());
	}

	@PostMapping("/recover")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void recover() {
		commands.recover(owner());
	}

	@PostMapping("/purchase")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void purchase(@RequestBody(required = false) Purchase request) {
		require(request);
		commands.purchase(owner(), request.code(), request.requestId());
	}

	private void require(Object request) {
		if (request == null)
			throw new BusinessException(400, "缺少请求参数");
	}

	public record Start(String encounter, String requestId) {
	}

	public record Turn(int round, List<Order> orders, boolean surrender, Map<String, Integer> replacements) {
	}

	public record Purchase(String code, String requestId) {
	}

	/**
	 * 无历史战局也返回稳定 JSON 外壳，避免 204 被浏览器查询缓存误判为缺失响应。
	 */
	public record Current(BattleView battle) {
	}

}
