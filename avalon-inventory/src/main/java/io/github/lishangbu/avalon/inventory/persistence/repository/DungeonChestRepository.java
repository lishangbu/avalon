package io.github.lishangbu.avalon.inventory.persistence.repository;

import io.github.lishangbu.avalon.inventory.persistence.entity.*;
import io.github.lishangbu.avalon.platform.persistence.EntityRepository;
import java.time.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;

/** 类型化持久层入口；写锁及修改均加入调用方事务，不自行拆分业务提交。 */
public interface DungeonChestRepository extends EntityRepository<DungeonChest, Long> {

	@Query(value = "from DungeonChest where runId = ?1")
	DungeonChest byRun(long run);

	@Query(value = "from DungeonChest where characterId = ?1 and claimed = false order by id")
	List<DungeonChest> unclaimed(long character);

}
