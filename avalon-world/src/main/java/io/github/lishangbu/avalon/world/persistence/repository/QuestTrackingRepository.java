package io.github.lishangbu.avalon.world.persistence.repository;

import io.github.lishangbu.avalon.platform.persistence.EntityRepository;
import io.github.lishangbu.avalon.world.persistence.entity.QuestTracking;
import java.util.Optional;

/** 每个角色只有一份追踪偏好；创建及更新均由角色锁串行化。 */
public interface QuestTrackingRepository extends EntityRepository<QuestTracking, Long> {

	Optional<QuestTracking> findByCharacterId(long characterId);

}
