package io.github.lishangbu.avalon.character.persistence.repository;

import io.github.lishangbu.avalon.character.persistence.entity.*;
import io.github.lishangbu.avalon.platform.persistence.EntityRepository;
import java.time.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;

/** 类型化持久层入口；写锁及修改均加入调用方事务，不自行拆分业务提交。 */
public interface CharacterFactRepository extends EntityRepository<CharacterFact, Long> {

	@Query(value = "select (count(f) > 0) from CharacterFact f where characterId = ?1 and code = ?2")
	boolean contains(long characterId, String code);

	@Query(value = "select code from CharacterFact where characterId = ?1")
	Set<String> codes(long characterId);

}
