package io.github.lishangbu.avalon.world.persistence.repository;

import io.github.lishangbu.avalon.platform.persistence.EntityRepository;
import io.github.lishangbu.avalon.world.persistence.entity.QuestRuntimeInstance;
import java.util.List;
import java.util.Optional;

/** 查询均按角色归属过滤；调用者持角色锁后才能接取、统计次数或修改实例。 */
public interface QuestRuntimeInstanceRepository extends EntityRepository<QuestRuntimeInstance, String> {

	List<QuestRuntimeInstance> findByCharacterIdAndQuestCodeOrderByAttemptAscIdAsc(long characterId, String questCode);

	List<QuestRuntimeInstance> findByCharacterIdOrderByAcceptedAtAscAttemptAscIdAsc(long characterId);

	Optional<QuestRuntimeInstance> findByIdAndCharacterId(String id, long characterId);

}
