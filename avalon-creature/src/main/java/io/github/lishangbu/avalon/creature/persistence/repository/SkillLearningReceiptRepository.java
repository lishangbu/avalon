package io.github.lishangbu.avalon.creature.persistence.repository;

import io.github.lishangbu.avalon.creature.persistence.entity.SkillLearningReceipt;
import org.springframework.data.jpa.repository.*;
import io.github.lishangbu.avalon.platform.persistence.EntityRepository;

/** 在角色写锁保护下检查请求凭证，数据库唯一约束作为并发最终防线。 */
public interface SkillLearningReceiptRepository extends EntityRepository<SkillLearningReceipt, Long> {

	@Query("select e from SkillLearningReceipt e where characterId = ?1 and requestId = ?2")
	SkillLearningReceipt prior(long character, String request);

}
