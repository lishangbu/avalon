package io.github.lishangbu.avalon.battle.application.query;

import io.github.lishangbu.avalon.battle.domain.BattleStateCodec;
import org.springframework.data.domain.PageRequest;
import io.github.lishangbu.avalon.battle.domain.DungeonRoundState;
import io.github.lishangbu.avalon.battle.persistence.repository.DungeonBattleRecordRepository;
import io.github.lishangbu.avalon.battle.persistence.repository.DungeonBattleMemberStore;
import io.github.lishangbu.avalon.platform.BusinessException;
import org.springframework.stereotype.Component;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Collection;

/** 内部恢复查询；含服务器随机种子的存档不能直接作为 HTTP 响应，页面另做安全投影。 */
@Component
@RequiredArgsConstructor
public class DungeonBattleQueryService {

	private final DungeonBattleRecordRepository dungeonBattleRecordRepository;

	private final DungeonBattleMemberStore dungeonBattleMemberStore;

	@Transactional
	public DungeonRoundState restore(long id, long character) {
		if (!dungeonBattleMemberStore.member(id, character))
			throw new BusinessException(404, "副本战局不存在");
		var entity = dungeonBattleRecordRepository.get(id);
		return BattleStateCodec.dungeon(entity.getState());
	}

	/** 内部只读查询，由进度服务提供已核验归属的候选；不读取或暴露战局随机种子。 */
	@Transactional
	public Long latestParticipated(Collection<Long> resolved, long character) {
		return dungeonBattleMemberStore.latestParticipated(resolved, character);
	}

	/** 每次只取有限候选，调度器逐场调用命令，单场失败不回滚其他战局。 */
	@Transactional
	public List<Long> due(Instant now) {
		return List.copyOf(dungeonBattleRecordRepository.due(now, PageRequest.of(0, 100)));
	}

}
