package io.github.lishangbu.avalon.character.persistence.repository;

import io.github.lishangbu.avalon.character.persistence.entity.*;
import io.github.lishangbu.avalon.platform.persistence.EntityRepository;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.*;
import jakarta.persistence.LockModeType;
import java.util.*;
import java.time.*;

/** 类型化持久层入口；写锁及修改均加入调用方事务，不自行拆分业务提交。 */
public interface CharacterRepository extends EntityRepository<PlayerCharacter, Long> {

	@Query(value = "from PlayerCharacter where accountId = ?1")
	PlayerCharacter owned(long accountId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query(value = "from PlayerCharacter where accountId = ?1")
	PlayerCharacter lockOwned(long accountId);

	@Query(value = "from PlayerCharacter where name = ?1")
	PlayerCharacter byName(String name);

}
