package io.github.lishangbu.avalon.inventory.persistence.repository;

import io.github.lishangbu.avalon.inventory.persistence.entity.CultivationRewardReceipt;
import org.springframework.data.jpa.repository.*;
import io.github.lishangbu.avalon.platform.persistence.EntityRepository;

/** 事件结算入口先锁角色；空奖励凭证与物品发放凭证同事务保存。 */
public interface CultivationRewardReceiptRepository extends EntityRepository<CultivationRewardReceipt, Long> {

	@Query("select e from CultivationRewardReceipt e where characterId = ?1 and eventKey = ?2")
	CultivationRewardReceipt prior(long character, String event);

}
