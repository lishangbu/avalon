package io.github.lishangbu.avalon.battle.persistence.repository;

import io.github.lishangbu.avalon.battle.persistence.entity.WorkshopCourseReceipt;
import io.github.lishangbu.avalon.platform.persistence.EntityRepository;
import org.springframework.data.jpa.repository.Query;

/** 请求在角色范围内去重，不能拿另一个会话的请求号执行新动作。 */
public interface WorkshopCourseReceiptRepository extends EntityRepository<WorkshopCourseReceipt, Long> {

	@Query("select r from WorkshopCourseReceipt r where r.characterId = ?1 and r.requestId = ?2")
	WorkshopCourseReceipt prior(long character, String request);

}
