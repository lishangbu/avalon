package io.github.lishangbu.avalon.creature.persistence.repository;

import io.github.lishangbu.avalon.creature.persistence.entity.ResearchExchangeReceipt;
import org.springframework.data.jpa.repository.*;
import io.github.lishangbu.avalon.platform.persistence.EntityRepository;

/** 研究兑换凭证只按认证角色查询，调用者需先持有角色锁。 */
public interface ResearchExchangeReceiptRepository extends EntityRepository<ResearchExchangeReceipt, Long> {

	@Query("select e from ResearchExchangeReceipt e where characterId = ?1 and requestId = ?2")
	ResearchExchangeReceipt prior(long character, String request);

}
