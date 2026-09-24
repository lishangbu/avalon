package io.github.lishangbu.avalon.market.persistence.repository;

import io.github.lishangbu.avalon.market.persistence.entity.MarketReceipt;
import org.springframework.data.jpa.repository.*;
import io.github.lishangbu.avalon.platform.persistence.EntityRepository;

/** 请求号隔离至认证账号，读取与保存均在参与者锁内。 */
public interface MarketReceiptRepository extends EntityRepository<MarketReceipt, Long> {

	@Query("select e from MarketReceipt e where accountId = ?1 and requestId = ?2")
	MarketReceipt prior(long account, String request);

}
