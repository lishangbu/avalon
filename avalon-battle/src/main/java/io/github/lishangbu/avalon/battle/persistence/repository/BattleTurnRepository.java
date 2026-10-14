package io.github.lishangbu.avalon.battle.persistence.repository;

import io.github.lishangbu.avalon.battle.persistence.entity.*;
import io.github.lishangbu.avalon.platform.persistence.EntityRepository;
import java.time.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;

/** 类型化持久层入口；写锁及修改均加入调用方事务，不自行拆分业务提交。 */
public interface BattleTurnRepository extends EntityRepository<BattleTurn, Long> {

	@Query(value = "from BattleTurn where battleId = ?1 and round = ?2")
	BattleTurn turn(long battle, int round);

	/** 按请求UUID查询完成回执，必须先验证战局归属。 */
	@Query("from BattleTurn where battleId = ?1 and requestId = ?2")
	BattleTurn request(long battle, String request);

}
