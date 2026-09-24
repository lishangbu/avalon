package io.github.lishangbu.avalon.world.persistence.repository;

import io.github.lishangbu.avalon.platform.persistence.EntityRepository;
import io.github.lishangbu.avalon.world.persistence.entity.*;
import java.time.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;

/** 类型化持久层入口；写锁及修改均加入调用方事务，不自行拆分业务提交。 */
public interface QuestRepository extends EntityRepository<QuestReceipt, Long> {

	@Query(value = "select step from QuestReceipt where characterId = ?1")
	Set<String> done(long character);

	@Query(value = "from QuestReceipt where characterId = ?1 and step = ?2")
	QuestReceipt prior(long character, String step);

	@Query(value = "from QuestReceipt where characterId = ?1 order by createdAt,id")
	List<QuestReceipt> ordered(long character);

	default List<Progress> progress(long character) {
		return ordered(character).stream()
			.map(receipt -> new Progress(receipt.getStep(), receipt.getQuestCode(), receipt.isCompleted()))
			.toList();
	}

	record Progress(String step, String questCode, boolean completed) {
	}

}
