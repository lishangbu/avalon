package io.github.lishangbu.avalon.creature.persistence.repository;

import io.github.lishangbu.avalon.creature.persistence.entity.CreatureRenameReceipt;
import org.springframework.data.jpa.repository.*;
import io.github.lishangbu.avalon.platform.persistence.EntityRepository;

/** 凭证按当前角色隔离，调用者不能指定另一个角色的请求作用域。 */
public interface CreatureRenameReceiptRepository extends EntityRepository<CreatureRenameReceipt, Long> {

	@Query("select e from CreatureRenameReceipt e where characterId = ?1 and requestId = ?2")
	CreatureRenameReceipt prior(long character, String request);

}
