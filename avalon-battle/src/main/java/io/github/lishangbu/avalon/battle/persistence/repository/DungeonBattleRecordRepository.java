package io.github.lishangbu.avalon.battle.persistence.repository;

import io.github.lishangbu.avalon.battle.persistence.entity.*;
import io.github.lishangbu.avalon.platform.persistence.EntityRepository;
import java.time.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;

/** 类型化持久层入口；写锁及修改均加入调用方事务，不自行拆分业务提交。 */
public interface DungeonBattleRecordRepository extends EntityRepository<DungeonBattle, Long> {

	@Query(value = "select id from DungeonBattle where active = true and deadline <= ?1 order by deadline,id")
	List<Long> due(Instant now, Pageable page);

}
