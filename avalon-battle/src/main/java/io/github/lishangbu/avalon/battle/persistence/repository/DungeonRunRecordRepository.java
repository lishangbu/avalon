package io.github.lishangbu.avalon.battle.persistence.repository;

import io.github.lishangbu.avalon.battle.persistence.entity.*;
import io.github.lishangbu.avalon.platform.persistence.EntityRepository;
import java.time.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;

/** 类型化持久层入口；写锁及修改均加入调用方事务，不自行拆分业务提交。 */
public interface DungeonRunRecordRepository extends EntityRepository<DungeonRun, Long> {

	@Query(value = "from DungeonRun where leaderId = ?1 and requestId = ?2")
	DungeonRun request(long leader, String key);

	@Query(value = "from DungeonRun where active = true and activeBattleId is null and id > ?1 order by id")
	List<DungeonRun> recruitment(long after, Pageable page);

	@Query(value = "select r.id, b.id from DungeonRun r, DungeonBattle b where r.active = true and r.activeBattleId = b.id and b.active = true and b.deadline <= ?1 order by b.deadline, r.id")
	List<Object[]> due(Instant now, Pageable page);

}
