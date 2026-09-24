package io.github.lishangbu.avalon.world.persistence.repository;

import io.github.lishangbu.avalon.platform.persistence.EntityRepository;
import io.github.lishangbu.avalon.world.persistence.entity.QuestAcceptReceipt;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

/** 接取写操作由角色锁串行化；按请求找重放，按步骤找第一次接取时刻。 */
public interface QuestAcceptReceiptRepository extends EntityRepository<QuestAcceptReceipt, Long> {

	@Query("select r from QuestAcceptReceipt r where r.characterId = ?1 and r.requestId = ?2")
	QuestAcceptReceipt prior(long character, String requestId);

	@Query("select r from QuestAcceptReceipt r where r.characterId = ?1 and r.step = ?2 order by r.id")
	List<QuestAcceptReceipt> accepted(long character, String step);

}
