package io.github.lishangbu.avalon.identity.persistence.repository;

import io.github.lishangbu.avalon.identity.persistence.entity.*;
import io.github.lishangbu.avalon.platform.persistence.EntityRepository;
import java.time.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;

/** 类型化持久层入口；写锁及修改均加入调用方事务，不自行拆分业务提交。 */
public interface SessionRepository extends EntityRepository<DeviceSession, Long> {

	@Query(value = "select id from DeviceSession where refreshHash = ?1")
	Long idByRefreshHash(String digest);

	@Query(value = "from DeviceSession where accessHash = ?1 and revoked = false")
	DeviceSession byAccessHash(String digest);

	@Query(value = "from DeviceSession where accountId = ?1 and revoked = false and refreshUntil > ?2")
	List<DeviceSession> active(long owner, Instant now);

}
