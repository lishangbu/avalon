package io.github.lishangbu.avalon.inventory.persistence.repository;

import io.github.lishangbu.avalon.inventory.persistence.entity.*;
import io.github.lishangbu.avalon.platform.persistence.EntityRepository;
import java.time.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;

/** 类型化持久层入口；写锁及修改均加入调用方事务，不自行拆分业务提交。 */
public interface DungeonSupplyScopeRepository extends EntityRepository<DungeonSupplyScope, Long> {

	@Query(value = "from DungeonSupplyScope where runId = ?1 and characterId = ?2")
	DungeonSupplyScope scope(long run, long character);

	@Query(value = "select (count(s) > 0) from DungeonSupplyScope s where characterId = ?1 and active = true")
	boolean hasActive(long character);

}
