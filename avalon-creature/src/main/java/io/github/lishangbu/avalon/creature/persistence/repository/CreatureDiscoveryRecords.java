package io.github.lishangbu.avalon.creature.persistence.repository;

import io.github.lishangbu.avalon.creature.persistence.entity.*;
import io.github.lishangbu.avalon.platform.persistence.EntityRepository;
import java.time.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;

/** 类型化持久层入口；写锁及修改均加入调用方事务，不自行拆分业务提交。 */
public interface CreatureDiscoveryRecords extends EntityRepository<CreatureDiscovery, Long> {

	@Query(value = "from CreatureDiscovery where characterId = ?1 order by id")
	List<CreatureDiscovery> discovered(long character);

	@Query(value = "select (count(d) > 0) from CreatureDiscovery d where characterId = ?1 and species = ?2 and abilitySlot = ?3 and ability = ?4")
	boolean known(long character, String species, String slot, String ability);

}
