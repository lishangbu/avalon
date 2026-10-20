package io.github.lishangbu.avalon.app;

import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import static org.junit.jupiter.api.Assertions.*;

/** 真实空库初始化必须提供完整当前结构，不留下兼容基础设施。 */
class DatabaseSchemaTest extends SpringIntegrationTest {

	@Autowired
	DataSource dataSource;

	@Test
	void currentSchemaHasDocumentedObjectsAndNoCompatibilityTables() throws Exception {
		try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
			try (var rows = statement.executeQuery("""
					SELECT count(*) FROM pg_tables WHERE schemaname = 'public'
					AND (tablename IN ('quest_definition_guard', 'inventory_reconciliation') OR tablename LIKE 'qrtz_%')
					""")) {
				assertTrue(rows.next());
				assertEquals(0, rows.getInt(1));
			}
			try (var rows = statement.executeQuery("SELECT count(*) FROM databasechangelog")) {
				assertTrue(rows.next());
				assertEquals(1, rows.getInt(1));
			}
			try (var rows = statement.executeQuery("""
					SELECT c.relname, a.attname, obj_description(c.oid, 'pg_class'), col_description(c.oid, a.attnum)
					FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
					JOIN pg_attribute a ON a.attrelid = c.oid
					WHERE n.nspname = 'public' AND c.relkind IN ('r', 'v')
					AND c.relname NOT IN ('databasechangelog', 'databasechangeloglock')
					AND a.attnum > 0 AND NOT a.attisdropped
					""")) {
				int columns = 0;
				while (rows.next()) {
					String field = rows.getString(1) + "." + rows.getString(2);
					assertNotNull(rows.getString(3), field);
					assertFalse(rows.getString(3).isBlank(), field);
					assertNotNull(rows.getString(4), field);
					assertFalse(rows.getString(4).isBlank(), field);
					columns++;
				}
				assertTrue(columns > 0);
			}
		}
	}

}
