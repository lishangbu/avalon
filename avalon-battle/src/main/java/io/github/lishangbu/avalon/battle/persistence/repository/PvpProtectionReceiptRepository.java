package io.github.lishangbu.avalon.battle.persistence.repository;

import io.github.lishangbu.avalon.battle.persistence.entity.PvpProtectionReceipt;
import io.github.lishangbu.avalon.platform.persistence.EntityRepository;
import org.springframework.data.jpa.repository.Query;

public interface PvpProtectionReceiptRepository extends EntityRepository<PvpProtectionReceipt, Long> {

	@Query("from PvpProtectionReceipt where characterId = ?1 and requestId = ?2")
	PvpProtectionReceipt request(long character, String request);

}
