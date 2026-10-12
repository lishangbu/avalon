package io.github.lishangbu.avalon.battle.application.query;

import io.github.lishangbu.avalon.battle.application.model.DungeonDay;
import io.github.lishangbu.avalon.battle.application.model.DungeonRunDueBattle;
import io.github.lishangbu.avalon.battle.domain.DungeonProgressState;
import io.github.lishangbu.avalon.battle.persistence.repository.DungeonDailyStartStore;
import io.github.lishangbu.avalon.battle.persistence.repository.DungeonRunRecordRepository;
import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.content.battle.DungeonCatalog;
import io.github.lishangbu.avalon.platform.BusinessException;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/** 内部进度恢复查询；账号映射和编队编号不能直接作为公开页面 DTO 返回。 */
@Component
@RequiredArgsConstructor
public class DungeonRunQueryService {

	private final DungeonRunRecordRepository dungeonRunRecordRepository;

	private final DungeonDailyStartStore dungeonDailyStartStore;

	private final CharacterQueryService characters;

	private final ObjectMapper codec;

	private final DungeonCatalog catalog;

	@Transactional
	public DungeonProgressState restore(long run, long owner) {
		var entity = dungeonRunRecordRepository.get(run);
		if (entity == null)
			throw new BusinessException(404, "副本进度不存在");
		var state = codec.readValue(entity.getState(), DungeonProgressState.class);
		var member = state.getMembers().get(characters.requireId(owner).toString());
		if (member == null || member.getOwner() != owner)
			throw new BusinessException(404, "副本进度不存在");
		// 只修正读模型，不续期或保存；开战持锁后再次核验修订与在线截止时间。
		var now = Instant.now();
		state.getMembers().values().stream().filter(value -> !value.isExited()).forEach(value -> {
			if ((state.getActiveBattle() == null && !value.presentAt(now))
					|| value.getPreparedRevision() != characters.adventure(value.getOwner()).preparationRevision())
				value.setReady(false);
		});
		return state;
	}

	@Transactional
	public int remainingStarts(long owner) {
		var day = DungeonDay.at(Instant.now(), catalog.definition().zone(), catalog.definition().resetHour());
		return Math.max(0,
				catalog.definition().dailyStarts() - dungeonDailyStartStore.starts(characters.requireId(owner), day));
	}

	/** 只扫描已到期的正式副本；命令持锁后仍重新检查关联战局与截止时间。 */
	@Transactional
	public List<DungeonRunDueBattle> due(Instant now) {
		return dungeonRunRecordRepository.due(now, PageRequest.of(0, 100))
			.stream()
			.map(row -> new DungeonRunDueBattle((Long) row[0], (Long) row[1]))
			.toList();
	}

}
