package io.github.lishangbu.avalon.battle.persistence.repository;

import io.github.lishangbu.avalon.battle.persistence.entity.*;
import io.github.lishangbu.avalon.platform.persistence.EntityRepository;
import java.time.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;

/** 类型化持久层入口；写锁及修改均加入调用方事务，不自行拆分业务提交。 */
public interface SupplyPurchaseRepository extends EntityRepository<SupplyPurchase, Long> {

	@Query(value = "from SupplyPurchase where characterId = ?1 and requestId = ?2")
	SupplyPurchase purchase(long character, String key);

}
