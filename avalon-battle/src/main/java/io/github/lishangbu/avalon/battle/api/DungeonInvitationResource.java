package io.github.lishangbu.avalon.battle.api;

import io.github.lishangbu.avalon.battle.application.command.DungeonInvitationCommandService;
import io.github.lishangbu.avalon.battle.application.query.DungeonInvitationQueryService;
import io.github.lishangbu.avalon.platform.BusinessException;
import io.github.lishangbu.avalon.platform.CurrentAccount;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

/** 发送者和收件人操作都以认证身份授权；客户端只传目标角色和邀请编号。 */
@RestController
@RequestMapping("/api/dungeon/invitations")
@RequiredArgsConstructor
public class DungeonInvitationResource {

	private final DungeonInvitationCommandService commands;

	private final DungeonInvitationQueryService queries;

	private final CurrentAccount identity;

	private long owner() {
		return identity.id();
	}

	@GetMapping
	public DungeonInvitationQueryService.Page inbox(@RequestParam(value = "after", defaultValue = "0") long after) {
		return queries.inbox(owner(), after);
	}

	@PostMapping
	public Sent send(@RequestBody(required = false) Send body) {
		if (body == null || body.lobby() == null || body.recipient() == null)
			throw new BusinessException(400, "请选择队伍和好友");
		return new Sent(Long.toString(commands.send(owner(), body.lobby(), body.recipient(), body.requestId())));
	}

	@PostMapping("/{id}/accept")
	public Accepted accept(@PathVariable("id") long id) {
		return new Accepted(Long.toString(commands.accept(owner(), id)));
	}

	@PostMapping("/{id}/decline")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void decline(@PathVariable("id") long id) {
		commands.decline(owner(), id);
	}

	public record Send(Long lobby, Long recipient, String requestId) {
	}

	public record Sent(String id) {
	}

	public record Accepted(String lobby) {
	}

}
