package io.github.lishangbu.avalon.battle.api;

import io.github.lishangbu.avalon.battle.api.model.DungeonInvitationAccepted;
import io.github.lishangbu.avalon.battle.api.model.DungeonInvitationSend;
import io.github.lishangbu.avalon.battle.api.model.DungeonInvitationSent;
import io.github.lishangbu.avalon.battle.application.command.DungeonInvitationCommandService;
import io.github.lishangbu.avalon.battle.application.model.DungeonInvitationPage;
import io.github.lishangbu.avalon.battle.application.query.DungeonInvitationQueryService;
import io.github.lishangbu.avalon.platform.BusinessException;
import io.github.lishangbu.avalon.platform.CurrentAccount;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

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
	public DungeonInvitationPage inbox(@RequestParam(value = "after", defaultValue = "0") long after) {
		return queries.inbox(owner(), after);
	}

	@PostMapping
	public DungeonInvitationSent send(@RequestBody(required = false) DungeonInvitationSend body) {
		if (body == null || body.lobby() == null || body.recipient() == null)
			throw new BusinessException(400, "请选择队伍和好友");
		return new DungeonInvitationSent(
				Long.toString(commands.send(owner(), body.lobby(), body.recipient(), body.requestId())));
	}

	@PostMapping("/{id}/accept")
	public DungeonInvitationAccepted accept(@PathVariable("id") long id) {
		return new DungeonInvitationAccepted(Long.toString(commands.accept(owner(), id)));
	}

	@PostMapping("/{id}/decline")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void decline(@PathVariable("id") long id) {
		commands.decline(owner(), id);
	}

}
