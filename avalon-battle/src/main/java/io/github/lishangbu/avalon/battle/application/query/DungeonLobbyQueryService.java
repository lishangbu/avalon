package io.github.lishangbu.avalon.battle.application.query;

import org.springframework.data.domain.PageRequest;

import io.github.lishangbu.avalon.battle.domain.DungeonLobbyState;
import tools.jackson.databind.ObjectMapper;
import io.github.lishangbu.avalon.battle.persistence.repository.DungeonLobbyRepository;
import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.platform.BusinessException;
import org.springframework.stereotype.Component;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import java.time.Instant;
import java.util.List;

/** 内部恢复查询保持只读；公开页面必须另行投影，不能暴露账号名单。 */
@Component
@RequiredArgsConstructor
public class DungeonLobbyQueryService {

	private final DungeonLobbyRepository lobbies;

	private final ObjectMapper codec;

	private final CharacterQueryService characters;

	@Transactional
	public DungeonLobbyState restore(long lobby, long owner) {
		var row = lobbies.get(lobby);
		if (row == null)
			throw new BusinessException(404, "队伍不存在");
		var state = codec.readValue(row.getState(), DungeonLobbyState.class);
		if (!state.getMembers().containsKey(owner))
			throw new BusinessException(404, "队伍不存在");
		state.getMembers().forEach((candidate, member) -> {
			if (member.getPreparedRevision() != characters.adventure(candidate).preparationRevision()
					|| member.getDeadline() == null || !Instant.now().isBefore(member.getDeadline()))
				member.setReady(false);
		});
		return state;
	}

	@Transactional
	public List<Long> due(Instant now) {
		return List.copyOf(lobbies.due(now, PageRequest.of(0, 100)));
	}

}
