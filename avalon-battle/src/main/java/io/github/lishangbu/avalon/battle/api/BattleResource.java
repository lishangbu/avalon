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
import io.github.lishangbu.avalon.battle.application.command.PvpCommandService;
import io.github.lishangbu.avalon.battle.api.model.PvpStartRequest;
import io.github.lishangbu.avalon.battle.api.model.PvpTurnRequest;
import io.github.lishangbu.avalon.battle.application.model.PvpView;
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

	private final PvpCommandService pvp;

	private final io.github.lishangbu.avalon.battle.application.command.PvpProtectionService protection;

	@GetMapping("/pvp/protection")
	public PvpProtectionView protection() {
		return new PvpProtectionView(protection.current(owner()));
	}

	@GetMapping("/pvp/players")
	public java.util.List<PvpPlayer> pvpPlayers() {
		return queries.pvpCandidates(owner())
			.stream()
			.map(player -> new PvpPlayer(Long.toString(player.id()), player.name()))
			.toList();
	}

	/** 人物编号超出 JavaScript 安全整数范围，公开接口统一传字符串。 */
	public record PvpPlayer(String id, String name) {
	}

	@GetMapping("/pvp/current")
	public PvpPresence currentPvp() {
		return new PvpPresence(queries.activePvp(owner()));
	}

	public record PvpPresence(String id) {
	}

	@PostMapping("/pvp/protection")
	public java.time.Instant protect(@RequestBody(required = false) PvpProtectionRequest request) {
		require(request);
		return protection.use(owner(), request.requestId());
	}

	public record PvpProtectionRequest(java.util.UUID requestId) {
	}

	public record PvpProtectionView(java.time.Instant expiresAt) {
	}

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

	@PostMapping("/pvp")
	public PvpView startPvp(@RequestBody(required = false) PvpStartRequest request) {
		require(request);
		if (request.opponentCharacter() == null)
			throw new BusinessException(400, "缺少目标角色");
		return pvp.start(owner(), request.opponentCharacter(), request.requestId());
	}

	@GetMapping("/pvp/{id}")
	public PvpView pvp(@PathVariable("id") long id) {
		return pvp.refresh(owner(), id);
	}

	@PostMapping("/pvp/{id}/turn")
	public PvpView pvpTurn(@PathVariable("id") long id, @RequestBody(required = false) PvpTurnRequest request) {
		require(request);
		return pvp.turn(owner(), id, request.requestId(), request.round(), request.revision(), request.orders(),
				request.replacements(), request.surrender());
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
