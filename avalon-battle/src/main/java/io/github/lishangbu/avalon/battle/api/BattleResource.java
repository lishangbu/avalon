package io.github.lishangbu.avalon.battle.api;

import io.github.lishangbu.avalon.battle.api.model.BattleCurrent;
import io.github.lishangbu.avalon.battle.api.model.BattlePurchase;
import io.github.lishangbu.avalon.battle.api.model.BattleTurnRequest;
import io.github.lishangbu.avalon.battle.api.model.BattleStart;
import io.github.lishangbu.avalon.battle.application.command.BattleCommandService;
import io.github.lishangbu.avalon.battle.application.model.BattleLobby;
import io.github.lishangbu.avalon.battle.application.model.BattleView;
import io.github.lishangbu.avalon.battle.application.model.CharacterCombatProfile;
import io.github.lishangbu.avalon.battle.application.query.BattleQueryService;
import io.github.lishangbu.avalon.platform.BusinessException;
import io.github.lishangbu.avalon.platform.CurrentAccount;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

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
	public BattleCurrent current() {
		return new BattleCurrent(queries.current(owner()));
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
	public BattleView start(@RequestBody(required = false) BattleStart request) {
		require(request);
		return commands.start(owner(), request.encounter(), request.requestId());
	}

	@PostMapping("/{id}/turn")
	public BattleView turn(@PathVariable("id") long id, @RequestBody(required = false) BattleTurnRequest request) {
		require(request);
		return commands.turn(owner(), id, request.requestId(), request.revision(), request.round(), request.orders(),
				request.surrender(), request.replacements());
	}

	@PostMapping("/recover")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void recover() {
		commands.recover(owner());
	}

	@PostMapping("/purchase")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void purchase(@RequestBody(required = false) BattlePurchase request) {
		require(request);
		commands.purchase(owner(), request.code(), request.requestId());
	}

	private void require(Object request) {
		if (request == null)
			throw new BusinessException(400, "缺少请求参数");
	}

}
