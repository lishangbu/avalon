package io.github.lishangbu.avalon.battle.persistence.repository;

import io.github.lishangbu.avalon.battle.persistence.entity.PvpCommandReceipt;
import io.github.lishangbu.avalon.platform.persistence.EntityRepository;
import org.springframework.data.jpa.repository.Query;

public interface PvpCommandReceiptRepository extends EntityRepository<PvpCommandReceipt, Long> {

	@Query("from PvpCommandReceipt where battleId = ?1 and characterId = ?2 and requestId = ?3")
	PvpCommandReceipt request(long battle, long character, String request);

}
