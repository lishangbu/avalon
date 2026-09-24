package io.github.lishangbu.avalon.inventory.persistence.repository;

import io.github.lishangbu.avalon.inventory.persistence.entity.InventoryClaim;
import org.springframework.data.jpa.repository.*;
import io.github.lishangbu.avalon.platform.persistence.EntityRepository;

/** 与奖励发放共用角色锁，数据库唯一约束保证同一账号请求只结算一次。 */
public interface InventoryClaimRepository extends EntityRepository<InventoryClaim, Long> {

	@Query("select e from InventoryClaim e where characterId = ?1 and requestId = ?2")
	InventoryClaim prior(long character, String request);

}
