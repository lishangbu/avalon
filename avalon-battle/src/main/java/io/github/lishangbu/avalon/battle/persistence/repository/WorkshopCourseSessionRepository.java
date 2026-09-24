package io.github.lishangbu.avalon.battle.persistence.repository;

import io.github.lishangbu.avalon.battle.persistence.entity.WorkshopCourseSession;
import io.github.lishangbu.avalon.platform.persistence.EntityRepository;
import org.springframework.data.jpa.repository.Query;

/** 会话只按角色与请求查找；所有写操作由角色锁串行化，角色上的课程占用阻止重复开课。 */
public interface WorkshopCourseSessionRepository extends EntityRepository<WorkshopCourseSession, Long> {

	@Query("select s from WorkshopCourseSession s where s.characterId = ?1 and s.requestId = ?2")
	WorkshopCourseSession prior(long character, String request);

	@Query("select s from WorkshopCourseSession s where s.characterId = ?1 and s.status = 'ACTIVE'")
	WorkshopCourseSession active(long character);

}
