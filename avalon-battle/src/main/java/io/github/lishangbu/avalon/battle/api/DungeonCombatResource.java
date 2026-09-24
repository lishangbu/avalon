package io.github.lishangbu.avalon.battle.api;

import io.github.lishangbu.avalon.battle.application.model.DungeonCombatView;
import io.github.lishangbu.avalon.battle.application.command.DungeonRunCommandService;
import io.github.lishangbu.avalon.battle.application.query.DungeonCombatQueryService;
import io.github.lishangbu.avalon.battle.domain.BattleState;
import io.github.lishangbu.avalon.battle.domain.DungeonRoundState;
import io.github.lishangbu.avalon.platform.BusinessException;
import io.github.lishangbu.avalon.platform.CurrentAccount;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

/** 进度和战局双重归属验证，禁止通过猜测其他副本编号读取敌方或队友内部存档。 */
@RestController
@RequestMapping("/api/dungeon/runs/{run}/battles/{battle}")
@RequiredArgsConstructor
public class DungeonCombatResource {

	private final DungeonCombatQueryService queries;

	private final DungeonRunCommandService commands;

	private final CurrentAccount identity;

	@GetMapping
	public DungeonCombatView view(@PathVariable("run") long run, @PathVariable("battle") long battle) {
		return queries.view(run, battle, identity.id());
	}

	/** 客户端只提交本人本轮选招；角色归属由认证账号和进度名单共同确定。 */
	@PostMapping("/submit")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void submit(@PathVariable("run") long run, @PathVariable("battle") long battle,
			@RequestBody(required = false) Submit request) {
		if (request == null || request.round() < 1 || request.orders() == null || request.replacements() == null
				|| request.orders().size() > 3 || request.orders().stream().anyMatch(order -> order == null)
				|| request.replacements().size() > 2
				|| request.replacements()
					.entrySet()
					.stream()
					.anyMatch(entry -> entry.getKey() == null || entry.getValue() == null))
			throw new BusinessException(400, "回合指令格式无效");
		commands.submit(run, identity.id(), battle, request.round(),
				new DungeonRoundState.Submission(request.orders(), request.replacements()));
	}

	public record Submit(int round, List<BattleState.Order> orders, Map<String, Integer> replacements) {
	}

}
