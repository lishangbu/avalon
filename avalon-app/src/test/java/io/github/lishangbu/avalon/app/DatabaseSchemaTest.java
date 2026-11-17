package io.github.lishangbu.avalon.app;

import javax.sql.DataSource;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import static org.junit.jupiter.api.Assertions.*;

/** 真实空库初始化必须提供完整当前结构，不留下兼容基础设施。 */
class DatabaseSchemaTest extends SpringIntegrationTest {

	@Autowired
	DataSource dataSource;

	@Autowired
	EntityManagerFactory entityManagerFactory;

	/** 独立保存迁移前的结构契约，避免映射遗漏使业务约束悄然失效。 */
	@Test
	void hibernateSchemaPreservesTheDatabaseContract() throws Exception {
		try (var resource = getClass().getResourceAsStream("/schema-contract.json");
				var connection = dataSource.getConnection()) {
			var contract = jsonMapper.readTree(resource);
			for (var table : contract.get("tables")) {
				for (var column : table.get("columns")) {
					try (var query = connection.prepareStatement("""
							SELECT format_type(a.atttypid, a.atttypmod), NOT a.attnotnull,
							obj_description(c.oid), col_description(c.oid, a.attnum)
							FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
							JOIN pg_attribute a ON a.attrelid = c.oid
							WHERE n.nspname = 'public' AND c.relname = ? AND a.attname = ?
							""")) {
						String name = table.get("name").asString();
						String field = column.get("name").asString();
						query.setString(1, name);
						query.setString(2, field);
						try (var rows = query.executeQuery()) {
							assertTrue(rows.next(), name + "." + field);
							assertEquals(column.get("type").asString(),
									rows.getString(1).replace("timestamp(6)", "timestamp"), name + "." + field);
							assertEquals(column.get("nullable").asBoolean(), rows.getBoolean(2), name + "." + field);
							assertEquals(table.get("comment").asString(), rows.getString(3), name);
							assertEquals(column.get("comment").asString(), rows.getString(4), name + "." + field);
						}
					}
				}
			}
			for (var constraint : contract.get("constraints")) {
				try (var query = connection.prepareStatement(
						"SELECT contype::text FROM pg_constraint WHERE conrelid = ?::regclass AND conname = ?")) {
					query.setString(1, constraint.get("table").asString());
					query.setString(2, constraint.get("name").asString());
					try (var rows = query.executeQuery()) {
						assertTrue(rows.next(), constraint.toString());
						assertEquals(constraint.get("type").asString(), rows.getString(1));
					}
				}
			}
			for (var index : contract.get("indexes")) {
				try (var query = connection.prepareStatement(
						"SELECT i.indisunique, pg_get_expr(i.indpred, i.indrelid) FROM pg_index i JOIN pg_class c ON c.oid = i.indexrelid WHERE c.relname = ?")) {
					query.setString(1, index.get("name").asString());
					try (var rows = query.executeQuery()) {
						assertTrue(rows.next(), index.toString());
						assertEquals(index.get("unique").asBoolean(), rows.getBoolean(1));
						if (index.has("where")) {
							assertNotNull(rows.getString(2), index.toString());
						}
					}
				}
			}
			try (var query = connection.createStatement();
					var rows = query.executeQuery(
							"SELECT condeferrable, condeferred FROM pg_constraint WHERE conname = 'owned_item_origin_grant_fkey'")) {
				assertTrue(rows.next());
				assertTrue(rows.getBoolean(1));
				assertTrue(rows.getBoolean(2));
			}
		}
	}

	@Test
	void schemaUpdatePreservesRowsAndRestoresComments() throws Exception {
		long id = -91701L;
		try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
			statement.executeUpdate("INSERT INTO account(id, username, password_hash, roles) VALUES (" + id
					+ ", 'schema-restart', 'hash', 'USER')");
			statement.execute("COMMENT ON TABLE account IS 'outdated'");
			statement.execute("COMMENT ON COLUMN account.username IS 'outdated'");
		}
		try {
			Map<String, Object> settings = Map.of("jakarta.persistence.nonJtaDataSource", dataSource,
					"hibernate.dialect", "io.github.lishangbu.avalon.platform.persistence.AvalonPostgreSQLDialect",
					"hibernate.physical_naming_strategy",
					"org.hibernate.boot.model.naming.PhysicalNamingStrategySnakeCaseImpl", "hibernate.hbm2ddl.auto",
					"update", "hibernate.hbm2ddl.halt_on_error", true,
					"hibernate.schema_update.unique_constraint_strategy", "RECREATE_QUIETLY");
			try (var registry = new StandardServiceRegistryBuilder().applySettings(settings).build()) {
				var sources = new MetadataSources(registry);
				entityManagerFactory.getMetamodel()
					.getEntities()
					.forEach(entity -> sources.addAnnotatedClass(entity.getJavaType()));
				var metadata = sources.buildMetadata();
				SchemaManagementToolCoordinator.process(metadata, registry, settings, action -> {
				});
			}
			try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
				try (var rows = statement.executeQuery("SELECT username FROM account WHERE id = " + id)) {
					assertTrue(rows.next());
					assertEquals("schema-restart", rows.getString(1));
				}
				try (var rows = statement.executeQuery("""
						SELECT obj_description('account'::regclass),
						col_description('account'::regclass, (SELECT attnum FROM pg_attribute
						WHERE attrelid = 'account'::regclass AND attname = 'username'))
						""")) {
					assertTrue(rows.next());
					for (int i = 1; i <= 2; i++) {
						assertNotNull(rows.getString(i));
						assertNotEquals("outdated", rows.getString(i));
					}
				}
			}
		}
		finally {
			try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
				statement.executeUpdate("DELETE FROM account WHERE id = " + id);
			}
		}
	}

	@Test
	void currentSchemaHasDocumentedObjectsAndNoCompatibilityTables() throws Exception {
		try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
			try (var rows = statement.executeQuery("SELECT to_regclass('public.account_credentials')")) {
				assertTrue(rows.next());
				assertNull(rows.getString(1), "空库不应创建认证视图");
			}
			try (var rows = statement.executeQuery("""
					SELECT count(*) FROM pg_tables WHERE schemaname = 'public'
					AND (tablename IN ('quest_definition_guard', 'inventory_reconciliation') OR tablename LIKE 'qrtz_%')
					""")) {
				assertTrue(rows.next());
				assertEquals(0, rows.getInt(1));
			}
			try (var rows = statement.executeQuery(
					"SELECT count(*) FROM pg_tables WHERE schemaname = 'public' AND tablename IN ('databasechangelog', 'databasechangeloglock')")) {
				assertTrue(rows.next());
				assertEquals(0, rows.getInt(1));
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
