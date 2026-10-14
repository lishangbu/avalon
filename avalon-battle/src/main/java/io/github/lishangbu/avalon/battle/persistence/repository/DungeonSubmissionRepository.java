package io.github.lishangbu.avalon.battle.persistence.repository;

import io.github.lishangbu.avalon.battle.persistence.entity.*;
import io.github.lishangbu.avalon.platform.persistence.EntityRepository;
import java.time.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;

/** 类型化持久层入口；写锁及修改均加入调用方事务，不自行拆分业务提交。 */
public interface DungeonSubmissionRepository extends EntityRepository<DungeonSubmission, Long> {

	@Query(value = "from DungeonSubmission where battleId = ?1 and characterId = ?2 and round = ?3")
	DungeonSubmission submission(long battle, long character, int round);

	/** 成员独立请求UUID，重试后即使回合已推进仍可定位原载荷。 */
	@Query("from DungeonSubmission where battleId = ?1 and characterId = ?2 and requestId = ?3")
	DungeonSubmission request(long battle, long character, String request);

}
