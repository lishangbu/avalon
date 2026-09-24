package io.github.lishangbu.avalon.app;

import java.sql.DriverManager;
import java.util.UUID;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** 在独立旧版数据库结构插入挂单，再执行正式迁移，覆盖有存量数据的升级。 */
class MarketMigrationTest extends SpringIntegrationTest {

	@Test
	void upgradePreservesOnlyOutstandingItemCustodyAndCanRunAgain() throws Exception {
		String schema = "market_migration_" + UUID.randomUUID().toString().replace("-", "");
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
				int beforeBatch = 0;
				for (var change : liquibase.getDatabaseChangeLog().getChangeSets()) {
					if (change.getId().equals("057-market-item-batches"))
						break;
					beforeBatch++;
				}
				assertTrue(beforeBatch > 0);
				assertTrue(beforeBatch < liquibase.getDatabaseChangeLog().getChangeSets().size());
				liquibase.update(beforeBatch, new Contexts(), new LabelExpression());
				statements.execute("""
						INSERT INTO account(id,username,password_hash,roles) VALUES(1,'legacy_market','hash','PLAYER');
						INSERT INTO market_listing(id,seller_account_id,asset_id,kind,state,unit_price,
						  original_quantity,remaining,created_at,expires_at) VALUES
						  (10,1,100,'ITEM','ACTIVE',2,99,40,now(),now()+interval '2 days'),
						  (11,1,101,'ITEM','RETURN_PENDING',2,50,17,now(),now()+interval '2 days'),
						  (12,1,102,'ITEM','SOLD',2,99,0,now(),now()+interval '2 days'),
						  (13,1,103,'ITEM','RETURNED',2,50,0,now(),now()+interval '2 days'),
						  (14,1,104,'CREATURE','ACTIVE',2,1,1,now(),now()+interval '2 days'),
						  (15,1,105,'ITEM','ACTIVE',2,20,8,now()-interval '3 days',now()-interval '1 day');
						""");
				liquibase.update(new Contexts(), new LabelExpression());
				liquibase.update(new Contexts(), new LabelExpression());
				try (var rows = statements.executeQuery(
						"SELECT listing_id,asset_id,remaining FROM market_listing_item ORDER BY listing_id")) {
					for (long[] expected : new long[][] { { 10, 100, 40 }, { 11, 101, 17 }, { 15, 105, 8 } }) {
						assertTrue(rows.next());
						assertEquals(expected[0], rows.getLong(1));
						assertEquals(expected[1], rows.getLong(2));
						assertEquals(expected[2], rows.getLong(3));
					}
					assertFalse(rows.next(), "已售罄、已退回或伙伴挂单不能产生物品托管余量");
				}
				try (var rows = statements.executeQuery(
						"SELECT original_quantity,remaining,state,expires_at<now() FROM market_listing WHERE id=15")) {
					assertTrue(rows.next());
					assertEquals(20, rows.getInt(1));
					assertEquals(8, rows.getInt(2));
					assertEquals("ACTIVE", rows.getString(3));
					assertTrue(rows.getBoolean(4), "升级不能延长旧挂单期限");
				}
			}
			finally {
				connection.rollback();
				connection.setAutoCommit(true);
				connection.setSchema("public");
				statements.execute("DROP SCHEMA " + schema + " CASCADE");
			}
		}
	}

}
