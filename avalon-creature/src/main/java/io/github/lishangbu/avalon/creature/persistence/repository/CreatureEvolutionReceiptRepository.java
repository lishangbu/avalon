package io.github.lishangbu.avalon.creature.persistence.repository;

import io.github.lishangbu.avalon.creature.persistence.entity.CreatureEvolutionReceipt;
import org.springframework.data.jpa.repository.*;
import io.github.lishangbu.avalon.platform.persistence.EntityRepository;

/** 请求号按角色隔离；调用方在角色锁内查询，避免并发重复消耗。 */
public interface CreatureEvolutionReceiptRepository extends EntityRepository<CreatureEvolutionReceipt, Long> {

	@Query("select e from CreatureEvolutionReceipt e where characterId = ?1 and requestId = ?2")
	CreatureEvolutionReceipt prior(long character, String request);

}
