package io.github.lishangbu.avalon.battle.persistence.repository;

import io.github.lishangbu.avalon.battle.persistence.entity.*;
import io.github.lishangbu.avalon.platform.persistence.EntityRepository;
import java.time.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;

/** 类型化持久层入口；写锁及修改均加入调用方事务，不自行拆分业务提交。 */
public interface DungeonLobbyRepository extends EntityRepository<DungeonLobby, Long> {

	@Query(value = "select 1 from pg_advisory_xact_lock(1096171852, 1)", nativeQuery = true)
	Integer lockMatching();

	@Query(value = "select * from dungeon_lobby l where l.open = true and cast(l.state as jsonb)->>'automatic' = 'true' and (select count(*) from jsonb_each(cast(l.state as jsonb)->'members')) < ?1 order by l.id limit 1 for update",
			nativeQuery = true)
	DungeonLobby firstMatching(int capacity);

	@Query(value = "from DungeonLobby where leaderId = ?1 and requestId = ?2")
	DungeonLobby request(long leader, String request);

	@Query(value = "from DungeonLobby where open = true and id > ?1 order by id")
	List<DungeonLobby> recruitment(long after, Pageable page);

	/** 周期扫描只读取编号，避免加载整份组队 JSON；实际处理时在写事务中重新验证状态。 */
	@Query(value = "select id from DungeonLobby where open = true and deadline <= ?1 order by deadline,id")
	List<Long> due(Instant now, Pageable page);

}
