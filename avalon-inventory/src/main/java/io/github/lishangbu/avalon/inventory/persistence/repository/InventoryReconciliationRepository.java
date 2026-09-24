package io.github.lishangbu.avalon.inventory.persistence.repository;

import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.simple.JdbcClient;
import lombok.RequiredArgsConstructor;
import java.util.List;

/** 旧档整理队列属于迁移基础设施，不为临时队列引入额外领域实体。 */
@Repository
@RequiredArgsConstructor
public class InventoryReconciliationRepository {

	private final JdbcClient jdbc;

	public List<Long> nextOwners() {
		return jdbc.sql(
				"SELECT c.account_id FROM inventory_reconciliation r JOIN player_character c ON c.id = r.character_id ORDER BY r.character_id LIMIT 100")
			.query(Long.class)
			.list();
	}

	public boolean pending(long character) {
		return jdbc.sql("SELECT count(*) FROM inventory_reconciliation WHERE character_id = :id")
			.param("id", character)
			.query(Long.class)
			.single() > 0;
	}

	public void complete(long character) {
		jdbc.sql("DELETE FROM inventory_reconciliation WHERE character_id = :id").param("id", character).update();
	}

}
