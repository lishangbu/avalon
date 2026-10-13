package io.github.lishangbu.avalon.world.persistence.repository;

import io.github.lishangbu.avalon.platform.persistence.EntityRepository;
import io.github.lishangbu.avalon.world.persistence.entity.QuestJournalState;
import java.util.List;
import java.util.Optional;

/** 写操作由上层角色锁串行化，唯一约束防止同角色同任务出现两份参与状态。 */
public interface QuestJournalStateRepository extends EntityRepository<QuestJournalState, Long> {

	Optional<QuestJournalState> findByCharacterIdAndQuestCode(long characterId, String questCode);

	List<QuestJournalState> findByCharacterId(long characterId);

}
