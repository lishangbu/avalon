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

	/** 一次读取当前角色的事实及首次写入时间，供观察记录展示；查询不会刷新或补写历史时间。 */
	List<CharacterFact> findAllByCharacterId(long characterId);

}
