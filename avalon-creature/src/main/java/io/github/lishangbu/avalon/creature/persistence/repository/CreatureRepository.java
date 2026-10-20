package io.github.lishangbu.avalon.creature.persistence.repository;

import io.github.lishangbu.avalon.creature.persistence.entity.*;
import io.github.lishangbu.avalon.platform.persistence.EntityRepository;
import java.time.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;

/** 类型化持久层入口；写锁及修改均加入调用方事务，不自行拆分业务提交。 */
public interface CreatureRepository extends EntityRepository<OwnedCreature, Long> {

	@Query(value = "select (count(p) > 0) from OwnedCreature p where originCharacterId = ?1 and source = ?2")
	boolean hasSource(long character, String source);

	@Query(value = "from OwnedCreature where originCharacterId = ?1 order by id")
	List<OwnedCreature> originated(long character);

	@Query(value = "from OwnedCreature where characterId = ?1 and releasedAt is null and inCustody = false order by id")
	List<OwnedCreature> owned(long character);

	@Query(value = "from OwnedCreature where characterId = ?1 order by id")
	List<OwnedCreature> history(long character);

	@Query(value = "select partyPosition from OwnedCreature where characterId = ?1 and releasedAt is null and inCustody = false")
	Set<Integer> positions(long character);

}
