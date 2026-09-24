package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.battle.application.command.DungeonRunCommandService;
import io.github.lishangbu.avalon.battle.application.command.DungeonLobbyCommandService;
import io.github.lishangbu.avalon.battle.application.query.DungeonLobbyQueryService;
import io.github.lishangbu.avalon.battle.application.query.DungeonRunQueryService;

import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

/** 内存调度器 仅唤醒到期回合；回合推进和下一截止时间由数据库中的正式副本事务决定。 */
@Component
@RequiredArgsConstructor
public class DungeonRoundMaintenance {

	private static final Logger LOG = LoggerFactory.getLogger(DungeonRoundMaintenance.class);

	private final DungeonRunQueryService queries;

	private final DungeonRunCommandService commands;

	private final DungeonLobbyQueryService lobbies;

	private final DungeonLobbyCommandService lobbyCommands;

	/** 每场使用独立事务；单场失败不阻塞其他副本，也不在重启后追算离线期间的全部回合。 */
	void expireLobbies() {
		for (long lobby : lobbies.due(Instant.now())) {
			try {
				lobbyCommands.expire(lobby);
			}
			catch (RuntimeException error) {
				LOG.error("组队到期清理失败，等待重试：lobby={}", lobby, error);
			}
		}
	}

	/** 回合扫描与组队扫描独立调度，组队清理不会占用回合扫描的执行线程。 */
	void expire() {
		for (var due : queries.due(Instant.now())) {
			try {
				commands.expire(due.run(), due.battle());
			}
			catch (RuntimeException error) {
				LOG.error("副本回合结算失败，保留原截止时间等待重试：run={} battle={}", due.run(), due.battle(), error);
			}
		}
	}

}
