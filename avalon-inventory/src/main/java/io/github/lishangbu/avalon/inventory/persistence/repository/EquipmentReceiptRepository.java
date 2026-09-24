package io.github.lishangbu.avalon.inventory.persistence.repository;

import io.github.lishangbu.avalon.inventory.persistence.entity.EquipmentReceipt;
import org.springframework.data.jpa.repository.*;
import io.github.lishangbu.avalon.platform.persistence.EntityRepository;

public interface EquipmentReceiptRepository extends EntityRepository<EquipmentReceipt, Long> {

	@Query("select e from EquipmentReceipt e where characterId = ?1 and requestId = ?2")
	EquipmentReceipt prior(long character, String requestId);

}
