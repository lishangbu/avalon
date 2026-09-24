package io.github.lishangbu.avalon.inventory.persistence.repository;

import io.github.lishangbu.avalon.inventory.persistence.entity.*;
import io.github.lishangbu.avalon.platform.persistence.EntityRepository;
import java.time.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;

/** 类型化持久层入口；写锁及修改均加入调用方事务，不自行拆分业务提交。 */
public interface InventoryRepository extends EntityRepository<OwnedItem, Long> {

	@Query(value = "select (count(i) > 0) from OwnedItem i where characterId = ?1 and source = ?2")
	boolean existsSource(long character, String source);

	@Query(value = "from OwnedItem where characterId = ?1 and source = ?2")
	OwnedItem source(long character, String source);

	@Query(value = "from OwnedItem where characterId = ?1 and quantity > 0 order by id")
	List<OwnedItem> available(long character);

	@Query(value = "from OwnedItem where characterId = ?1 and pendingQuantity > 0 order by id")
	List<OwnedItem> pending(long character);

	@Query(value = "from OwnedItem where characterId = ?1 and code = ?2 and quantity > 0 order by id")
	List<OwnedItem> matching(long character, String code);

	@Query(value = "from OwnedItem where characterId = ?1 and slot = ?2 and equipped = true")
	List<OwnedItem> equipped(long character, String slot);

}
