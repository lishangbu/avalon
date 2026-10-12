package io.github.lishangbu.avalon.battle.application.query;

import io.github.lishangbu.avalon.battle.application.model.DungeonInvitationInvitation;
import io.github.lishangbu.avalon.battle.application.model.DungeonInvitationPage;
import io.github.lishangbu.avalon.battle.domain.DungeonLobbyState;
import io.github.lishangbu.avalon.battle.persistence.repository.DungeonInvitationRepository;
import io.github.lishangbu.avalon.battle.persistence.repository.DungeonLobbyRepository;
import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.character.application.query.FriendshipQueryService;
import io.github.lishangbu.avalon.content.battle.DungeonCatalog;
import io.github.lishangbu.avalon.platform.BusinessException;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/** 邀请通知只返回收件人的有效邀请，不加载队友背包或编队，也不替用户占位。 */
@Component
@RequiredArgsConstructor
public class DungeonInvitationQueryService {

	private final DungeonInvitationRepository invitations;

	private final DungeonLobbyRepository lobbies;

	private final CharacterQueryService characters;

	private final FriendshipQueryService friends;

	private final DungeonCatalog catalog;

	private final ObjectMapper codec;

	@Transactional
	public DungeonInvitationPage inbox(long owner, long after) {
		if (after < 0)
			throw new BusinessException(400, "分页位置无效");
		var rows = invitations.inbox(characters.requireId(owner), after, Instant.now());
		var entries = rows.stream().map(row -> {
			var lobby = lobbies.get(row.getLobbyId());
			if (lobby == null || !lobby.isOpen())
				return null;
			var state = codec.readValue(lobby.getState(), DungeonLobbyState.class);
			if (state.isClosed() || state.getTargetRun() != null || state.getMembers().containsKey(owner)
					|| state.getMembers().size() >= catalog.definition().players())
				return null;
			Long sender = friends.friendOwner(owner, row.getSenderId());
			if (sender == null)
				return null;
			if (state.getLeader() != sender || !state.getMembers().containsKey(sender))
				return null;
			return new DungeonInvitationInvitation(row.getId().toString(), row.getLobbyId().toString(),
					characters.find(sender).name(), catalog.definition().name(), state.getMembers().size(),
					catalog.definition().players(), row.getExpiresAt());
		}).filter(Objects::nonNull).toList();
		return new DungeonInvitationPage(entries, rows.size() == 50 ? rows.getLast().getId().toString() : null);
	}

}
