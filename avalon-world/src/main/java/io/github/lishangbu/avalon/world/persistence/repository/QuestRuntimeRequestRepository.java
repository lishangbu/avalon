package io.github.lishangbu.avalon.world.persistence.repository;

import io.github.lishangbu.avalon.platform.persistence.EntityRepository;
import io.github.lishangbu.avalon.world.persistence.entity.QuestRuntimeRequest;
import java.util.Optional;

/** 请求回执和奖励共用调用方事务，数据库唯一约束作为角色锁之外的最终防重。 */
public interface QuestRuntimeRequestRepository extends EntityRepository<QuestRuntimeRequest, Long> {

	Optional<QuestRuntimeRequest> findByCharacterIdAndRequestId(long characterId, String requestId);

}
