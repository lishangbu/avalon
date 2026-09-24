package io.github.lishangbu.avalon.creature.persistence.repository;

import io.github.lishangbu.avalon.creature.persistence.entity.CreatureReleaseReceipt;
import org.springframework.data.jpa.repository.*;
import io.github.lishangbu.avalon.platform.persistence.EntityRepository;

/** 放生凭证的归属由角色锁内的应用服务确定。 */
public interface CreatureReleaseReceiptRepository extends EntityRepository<CreatureReleaseReceipt, Long> {

	@Query("select e from CreatureReleaseReceipt e where characterId = ?1 and requestId = ?2")
	CreatureReleaseReceipt prior(long character, String request);

}
