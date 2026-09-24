package io.github.lishangbu.avalon.app;

import java.sql.DriverManager;
import java.util.UUID;
import javax.sql.DataSource;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

/** 在独立 schema 执行真实 025→027 升级，不用启动后的实体插入代替历史迁移验证。 */
class CreatureMigrationTest extends SpringIntegrationTest {

	@Autowired
	DataSource dataSource;

	@Test
	void upgradeRetainsReleasedDiscoveriesAndOnlyRecognizesProvenCaptures() throws Exception {
		String schema = "migration_" + UUID.randomUUID().toString().replace("-", "");
		try (var connection = DriverManager.getConnection(DATABASE.getJdbcUrl(), DATABASE.getUsername(),
				DATABASE.getPassword()); var statements = connection.createStatement()) {
			statements.execute("CREATE SCHEMA " + schema);
			try {
				connection.setSchema(schema);
				var database = DatabaseFactory.getInstance()
					.findCorrectDatabaseImplementation(new JdbcConnection(connection));
				database.setDefaultSchemaName(schema);
				database.setLiquibaseSchemaName(schema);
				var liquibase = new Liquibase("db/changelog.yaml", new ClassLoaderResourceAccessor(), database);
				liquibase.update(25, new Contexts(), new LabelExpression());
				statements.execute("""
						INSERT INTO account(id,username,password_hash,roles) VALUES(1,'legacy','hash','PLAYER');
						INSERT INTO player_character(id,account_id,name,profession,location,version)
						  VALUES(2,1,'旧档','WARRIOR','VILLAGE_HOME',0);
						INSERT INTO battle(id,character_id,request_id,active,state)
						  VALUES(3,2,'legacy',false,'{"encounter":"WOODS"}');
						INSERT INTO owned_creature(id,character_id,source,species,name,element,level,experience,
						  ability_slot,ability,skills,party_position,rename_count,released_at) VALUES
						  (4,2,'WOODS:3:enemy1','STONE_GOLEM','小岩怪','地',1,0,'H','守护','[]',NULL,0,now()),
						  (5,2,'UNKNOWN_SOURCE','SPROUT','旧昵称','木',1,0,'A','活力','[]',0,1,NULL),
						  (6,2,'WOODS:999:enemy2','GALE_SPARROW','青岚雀','风',8,2800,'A','轻盈','[]',100,0,NULL);
						""");
				liquibase.update(new Contexts(), new LabelExpression());
				try (var rows = statements.executeQuery("SELECT ability_slot,ability FROM owned_creature WHERE id=5")) {
					assertTrue(rows.next());
					assertEquals("A", rows.getString(1));
					assertEquals("完好防护", rows.getString(2), "旧存档沿用原槽位，并同步正式特性");
				}
				try (var rows = statements.executeQuery(
						"SELECT id,origin_character_id,captor_character_id,origin_kind,bound,custom_name FROM owned_creature ORDER BY id")) {
					assertTrue(rows.next());
					assertEquals(4, rows.getLong(1));
					assertEquals(2, rows.getLong(2));
					assertEquals(2, rows.getLong(3));
					assertEquals("CAPTURE", rows.getString(4));
					assertFalse(rows.getBoolean(5));
					assertFalse(rows.getBoolean(6));
					assertTrue(rows.next());
					assertNull(rows.getObject(3));
					assertEquals("UNKNOWN", rows.getString(4));
					assertTrue(rows.getBoolean(5));
					assertTrue(rows.getBoolean(6));
					assertTrue(rows.next());
					assertNull(rows.getObject(3));
					assertTrue(rows.getBoolean(5));
				}
				liquibase.update(new Contexts(), new LabelExpression());
				try (var rows = statements.executeQuery("SELECT count(*) FROM creature_discovery")) {
					assertTrue(rows.next());
					assertEquals(3, rows.getInt(1));
				}
				try (var rows = statements.executeQuery("SELECT count(*) FROM databasechangelog")) {
					assertTrue(rows.next());
					// 核验整个正式迁移链均已执行，新增任务表无需维护另一份变更集数量。
					assertEquals(liquibase.getDatabaseChangeLog().getChangeSets().size(), rows.getInt(1));
				}
				try (var rows = statements.executeQuery(
						"SELECT count(*) FROM pg_tables WHERE schemaname = current_schema() AND tablename LIKE 'qrtz_%'")) {
					assertTrue(rows.next());
					assertEquals(0, rows.getInt(1), "内存调度不保留框架任务表");
				}
				// 验证实际数据库元数据，防止 YAML 可以解析但 remarks 没有生成 COMMENT SQL。
				// 同时覆盖后续 addColumn、认证只读视图及调度表清理。
				try (var rows = statements.executeQuery("""
						SELECT c.relname, a.attname,
						       obj_description(c.oid, 'pg_class') AS table_comment,
						       col_description(c.oid, a.attnum) AS column_comment
						FROM pg_class c
						JOIN pg_namespace n ON n.oid = c.relnamespace
						JOIN pg_attribute a ON a.attrelid = c.oid
						WHERE n.nspname = current_schema() AND c.relkind IN ('r', 'v')
						  AND c.relname NOT IN ('databasechangelog', 'databasechangeloglock')
						  AND a.attnum > 0 AND NOT a.attisdropped
						""")) {
					int columns = 0;
					while (rows.next()) {
						String field = rows.getString(1) + "." + rows.getString(2);
						assertNotNull(rows.getString(3), "缺少表或视图注释：" + field);
						assertFalse(rows.getString(3).isBlank(), "表或视图注释为空：" + field);
						assertNotNull(rows.getString(4), "缺少字段注释：" + field);
						assertFalse(rows.getString(4).isBlank(), "字段注释为空：" + field);
						columns++;
					}
					assertTrue(columns > 0, "必须实际检查数据库字段，不能空集通过");
				}
			}
			finally {
				// 独立测试 schema 才允许清理，恢复连接默认路径，避免影响其他应用测试。
				connection.rollback();
				connection.setAutoCommit(true);
				connection.setSchema("public");
				statements.execute("DROP SCHEMA " + schema + " CASCADE");
			}
		}
	}

}
