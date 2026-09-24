package io.github.lishangbu.avalon.inventory.persistence.repository;

import io.github.lishangbu.avalon.inventory.persistence.entity.CraftingReceipt;
import org.springframework.data.jpa.repository.*;
import io.github.lishangbu.avalon.platform.persistence.EntityRepository;

/** 请求查重必须在角色锁内执行，数据库唯一键防止重复提交。 */
public interface CraftingRepository extends EntityRepository<CraftingReceipt, Long> {

	@Query("select e from CraftingReceipt e where characterId = ?1 and requestId = ?2")
	CraftingReceipt prior(long character, String request);

}
