package io.github.lishangbu.avalon.inventory.persistence.repository;

import io.github.lishangbu.avalon.inventory.persistence.entity.*;
import io.github.lishangbu.avalon.platform.persistence.EntityRepository;
import java.time.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;

/** 类型化持久层入口；写锁及修改均加入调用方事务，不自行拆分业务提交。 */
public interface InventoryGrantRepository extends EntityRepository<InventoryGrant, Long> {

	@Query(value = "select (count(g) > 0) from InventoryGrant g where characterId = ?1 and source = ?2")
	boolean existsSource(long character, String source);

	@Query(value = "select id from InventoryGrant where characterId = ?1 and source = ?2")
	Long grantId(long character, String source);

}
