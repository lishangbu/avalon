package io.github.lishangbu.avalon.world.persistence.repository;

import io.github.lishangbu.avalon.platform.persistence.EntityRepository;
import io.github.lishangbu.avalon.world.persistence.entity.QuestObjective;
import java.util.List;

/** 与原行为共用角色锁，唯一约束兜底重复目标；读侧一次加载角色目标，避免每个任务单独查询。 */
public interface QuestObjectiveRepository extends EntityRepository<QuestObjective, Long> {

	List<QuestObjective> findByCharacterId(long characterId);

	boolean existsByCharacterIdAndStepAndFact(long characterId, String step, String fact);

}
