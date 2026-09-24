package io.github.lishangbu.avalon.battle.application.command;

import io.github.lishangbu.avalon.battle.domain.DungeonLobbyState;
import io.github.lishangbu.avalon.battle.persistence.entity.DungeonInvitation;
import tools.jackson.databind.ObjectMapper;
import io.github.lishangbu.avalon.battle.persistence.repository.DungeonInvitationRepository;
import io.github.lishangbu.avalon.battle.persistence.repository.DungeonLobbyRepository;
import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.character.application.query.FriendshipQueryService;
import io.github.lishangbu.avalon.content.battle.DungeonCatalog;
import io.github.lishangbu.avalon.platform.BusinessException;
import org.springframework.stereotype.Component;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import java.time.Instant;

/** 邀请、占位与确认是三个不同动作；收件人主动接受才尝试入队，失败不消耗邀请或副本次数。 */
@Component
@RequiredArgsConstructor
public class DungeonInvitationCommandService {

	private final DungeonInvitationRepository invitations;

	private final DungeonLobbyRepository lobbies;

	private final DungeonLobbyCommandService commands;

	private final CharacterQueryService characters;

	private final FriendshipQueryService friends;

	private final DungeonCatalog catalog;

	private final ObjectMapper codec;

	@Transactional
	public long send(long owner, long lobby, long recipient, String request) {
		if (request == null || !request.matches("[a-zA-Z0-9-]{8,64}"))
			throw new BusinessException(400, "请求号无效");
		long sender = characters.requireId(owner);
		var state = lockLobby(lobby);
		var replay = invitations.request(sender, request);
		if (replay != null) {
			if (replay.getLobbyId() != lobby || replay.getRecipientId() != recipient)
				throw new BusinessException(409, "请求号已用于其他邀请");
			return replay.getId();
		}
		requireInviting(state, owner);
		long receiver = friends.requireFriendOwner(owner, recipient);
		if (state.getMembers().containsKey(receiver))
			throw new BusinessException(409, "好友已在队伍中");
		var pending = invitations.pending(lobby, recipient);
		if (pending != null) {
			if (pending.getSenderId() == sender && Instant.now().isBefore(pending.getExpiresAt()))
				throw new BusinessException(409, "已发送邀请，请等待好友处理");
			pending.setStatus("EXPIRED");
			// 先释放部分唯一索引中的旧待处理记录，再插入本次新邀请。
			invitations.flush();
		}
		var invitation = new DungeonInvitation();

		invitation.setLobbyId(lobby);
		invitation.setSenderId(sender);
		invitation.setRecipientId(recipient);
		invitation.setRequestId(request);
		invitation.setExpiresAt(Instant.now().plusSeconds(catalog.definition().invitationSeconds()));
		invitation.setStatus("PENDING");
		invitations.saveAndFlush(invitation);
		return invitation.getId();
	}

	@Transactional
	public long accept(long owner, long id) {
		long recipient = characters.requireId(owner);
		long lobby = requireLobby(id, recipient);
		var state = lockLobby(lobby);
		var invitation = invitations.lock(id);
		// 成功请求重放只返回原结果，不把已经离队的人重新加入。
		if (invitation.getStatus().equals("ACCEPTED"))
			return lobby;
		requirePending(invitation);
		long sender = friends.requireFriendOwner(owner, invitation.getSenderId());
		requireInviting(state, sender);
		commands.join(lobby, owner);
		invitation.setStatus("ACCEPTED");
		return lobby;
	}

	@Transactional
	public void decline(long owner, long id) {
		long lobby = requireLobby(id, characters.requireId(owner));
		lockLobby(lobby);
		var invitation = invitations.lock(id);
		if (invitation.getStatus().equals("ACCEPTED"))
			throw new BusinessException(409, "邀请已接受，请在队伍中离开");
		if (invitation.getStatus().equals("PENDING"))
			invitation.setStatus("DECLINED");
	}

	private long requireLobby(long id, long recipient) {
		Long lobby = invitations.lobby(id, recipient);
		if (lobby == null)
			throw new BusinessException(404, "邀请不存在");
		return lobby;
	}

	private DungeonLobbyState lockLobby(long id) {
		var row = lobbies.lock(id);
		if (row == null)
			throw new BusinessException(404, "队伍不存在");
		return codec.readValue(row.getState(), DungeonLobbyState.class);
	}

	private void requireInviting(DungeonLobbyState state, long sender) {
		if (state.isClosed() || state.getTargetRun() != null || state.getLeader() != sender
				|| !state.getMembers().containsKey(sender))
			throw new BusinessException(409, "邀请队伍已失效或你不是队长");
		if (state.getMembers().size() >= catalog.definition().players())
			throw new BusinessException(409, "队伍已满");
	}

	private void requirePending(DungeonInvitation invitation) {
		if (!invitation.getStatus().equals("PENDING") || !Instant.now().isBefore(invitation.getExpiresAt()))
			throw new BusinessException(409, "邀请已处理或已过期");
	}

}
