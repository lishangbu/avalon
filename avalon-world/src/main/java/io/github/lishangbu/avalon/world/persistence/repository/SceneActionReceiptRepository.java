package io.github.lishangbu.avalon.world.persistence.repository;

import io.github.lishangbu.avalon.platform.persistence.EntityRepository;
import io.github.lishangbu.avalon.world.persistence.entity.SceneActionReceipt;
import org.springframework.data.jpa.repository.Query;

/** 回执按角色和请求号隔离，写并发由上层统一角色锁串行化。 */
public interface SceneActionReceiptRepository extends EntityRepository<SceneActionReceipt, Long> {

	@Query("select r from SceneActionReceipt r where r.characterId = ?1 and r.requestId = ?2")
	SceneActionReceipt prior(long character, String requestId);

}
