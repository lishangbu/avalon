package io.github.lishangbu.avalon.inventory.persistence.repository;

import io.github.lishangbu.avalon.inventory.persistence.entity.BookRecyclingReceipt;
import org.springframework.data.jpa.repository.*;
import io.github.lishangbu.avalon.platform.persistence.EntityRepository;

public interface BookRecyclingReceiptRepository extends EntityRepository<BookRecyclingReceipt, Long> {

	@Query("select e from BookRecyclingReceipt e where characterId = ?1 and requestId = ?2")
	BookRecyclingReceipt prior(long character, String requestId);

}
