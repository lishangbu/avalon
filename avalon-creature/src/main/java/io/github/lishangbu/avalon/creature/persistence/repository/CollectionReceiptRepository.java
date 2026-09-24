package io.github.lishangbu.avalon.creature.persistence.repository;

import io.github.lishangbu.avalon.creature.persistence.entity.CollectionReceipt;
import org.springframework.data.jpa.repository.*;
import io.github.lishangbu.avalon.platform.persistence.EntityRepository;

import java.util.List;

/** 账号目前只有一个不可删除角色，角色锁串行化该账号的图鉴结算。 */
public interface CollectionReceiptRepository extends EntityRepository<CollectionReceipt, Long> {

	@Query("select e from CollectionReceipt e where accountId = ?1 order by id")
	List<CollectionReceipt> owned(long account);

}
