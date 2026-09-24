package io.github.lishangbu.avalon.inventory.persistence.repository;

import io.github.lishangbu.avalon.inventory.persistence.entity.EnhancementReceipt;
import org.springframework.data.jpa.repository.*;
import io.github.lishangbu.avalon.platform.persistence.EntityRepository;

/** 读取前调用方必须持有角色写锁，避免并发请求重复执行随机尝试。 */
public interface EnhancementReceiptRepository extends EntityRepository<EnhancementReceipt, Long> {

	@Query("select e from EnhancementReceipt e where characterId = ?1 and requestId = ?2")
	EnhancementReceipt prior(long character, String request);

}
