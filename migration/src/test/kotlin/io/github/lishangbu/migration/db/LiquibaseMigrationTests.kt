package io.github.lishangbu.migration.db

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import java.nio.file.Files
import java.nio.file.Path
import java.sql.ResultSet
import javax.sql.DataSource

@SpringBootTest(
	classes = [MigrationTestApplication::class],
	properties = [
		"spring.liquibase.change-log=classpath:/db/changelog/db.changelog-master.yaml",
	],
)
@ContextConfiguration(initializers = [MigrationPostgresTestContainer::class])
/**
 * 使用真实 PostgreSQL 验证 Liquibase 主 changelog 可以初始化完整 schema。
 */
class LiquibaseMigrationTests(
	@Autowired private val dataSource: DataSource,
) {
	@Test
	fun `master changelog uses include all`() {
		val resource = javaClass.getResource("/db/changelog/db.changelog-master.yaml")

		assertThat(resource).isNotNull()
		assertThat(resource!!.readText()).contains("includeAll")
	}

	@Test
	fun `liquibase changelog history keeps ordered feature files`() {
		val resource = javaClass.getResource("/db/changelog/changes")

		assertThat(resource).isNotNull()
		val changelogFiles = Files.list(Path.of(resource!!.toURI())).use { paths ->
			paths
				.filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(".yaml") }
				.map { it.fileName.toString() }
				.sorted()
				.toList()
		}
		assertThat(changelogFiles).containsExactly(
			"001-initial-schema.yaml",
			"002-game-data-schema.yaml",
			"003-game-data-seed.yaml",
		)
	}

	@Test
	fun `liquibase does not keep catalog and battle tables`() {
		val tableNames = queryStrings(
			"""
			select table_name
			from information_schema.tables
			where table_schema = 'public'
			""".trimIndent(),
		)

		assertThat(tableNames).doesNotContain(
			"element",
			"stat",
			"personality",
			"compendium",
			"region",
			"creature_species",
			"creature_form",
			"creature_form_element",
			"creature_form_stat",
			"creature_form_egg_group",
			"skill",
			"skill_meta",
			"skill_ailment",
			"skill_category",
			"skill_damage_class",
			"skill_learn_method",
			"skill_target",
			"trait",
			"creature_form_trait",
			"creature_form_skill",
			"item",
			"item_category",
			"item_attribute",
			"item_attribute_binding",
			"item_fling_effect",
			"berry",
			"berry_firmness",
			"berry_flavor",
			"evolution_chain",
			"evolution_node",
			"evolution_trigger",
			"machine",
			"asset_object",
			"creature_form_asset",
			"item_asset",
			"berry_asset",
			"battle_entity_mapping",
			"battle_rule_snapshot",
			"battle_simulation",
			"battle_turn_log",
		)
	}

	@Test
	fun `liquibase creates security and authorization tables`() {
		val tableNames = queryStrings(
			"""
			select table_name
			from information_schema.tables
			where table_schema = 'public'
			""".trimIndent(),
		)

		assertThat(tableNames).contains(
			"security_user",
			"security_role",
			"security_access_node",
			"security_user_role",
			"security_role_access_node",
			"oauth2_client",
			"oauth2_authorization",
			"oauth2_authorization_consent",
			"oauth2_jwk",
		)
		assertThat(tableNames).doesNotContain("security_permission", "security_role_permission")

		val accessNodeCodes = queryStrings(
			"select code from security_access_node order by code",
		)
		val systemAccessNodeCodes = listOf(
			"security:admin",
			"system",
			"system.oauth",
			"system.oauth.clients",
			"system.oauth.jwks",
			"system.rbac",
			"system.rbac.access-nodes",
			"system.rbac.roles",
			"system.rbac.users",
			"system.scheduler",
			"system.scheduler.tasks",
		)
		val gameDataAccessNodeCodes = listOf(
			"game-data",
			"game-data:admin",
			"game-data.abilities",
			"game-data.core",
			"game-data.creature-abilities",
			"game-data.creature-elements",
			"game-data.creature-stats",
			"game-data.creatures",
			"game-data.dictionary",
			"game-data.egg-groups",
			"game-data.elements",
			"game-data.habitats",
			"game-data.item-categories",
			"game-data.items",
			"game-data.relations",
			"game-data.skill-damage-classes",
			"game-data.skills",
			"game-data.species",
			"game-data.species-colors",
			"game-data.species-egg-groups",
			"game-data.species-shapes",
			"game-data.stats",
		)
		assertThat(accessNodeCodes).containsExactlyInAnyOrderElementsOf(gameDataAccessNodeCodes + systemAccessNodeCodes)

		val roleCodes = queryStrings(
			"select code from security_role order by code",
		)
		assertThat(roleCodes).containsExactly("game-data-admin", "system-admin")

		val systemAdminAccessNodeCodes = queryStrings(
			"""
			select n.code
			from security_access_node n
			join security_role_access_node ran on ran.access_node_id = n.id
			join security_role r on r.id = ran.role_id
			where r.code = 'system-admin'
			order by n.code
			""".trimIndent(),
		)
		assertThat(systemAdminAccessNodeCodes).containsExactlyInAnyOrderElementsOf(systemAccessNodeCodes)

		val gameDataAdminAccessNodeCodes = queryStrings(
			"""
			select n.code
			from security_access_node n
			join security_role_access_node ran on ran.access_node_id = n.id
			join security_role r on r.id = ran.role_id
			where r.code = 'game-data-admin'
			order by n.code
			""".trimIndent(),
		)
		assertThat(gameDataAdminAccessNodeCodes).containsExactlyInAnyOrderElementsOf(gameDataAccessNodeCodes)

		val rbacIdTypes = queryMaps(
			"""
			select table_name, data_type
			from information_schema.columns
			where table_schema = 'public'
				and table_name in ('security_user', 'security_role', 'security_access_node')
				and column_name = 'id'
			order by table_name
			""".trimIndent(),
		)
		assertThat(rbacIdTypes.map { it["data_type"] }).containsOnly("bigint")

		val accessNodeColumns = queryStrings(
			"""
			select column_name
			from information_schema.columns
			where table_schema = 'public'
				and table_name = 'security_access_node'
			order by ordinal_position
			""".trimIndent(),
		)
		assertThat(accessNodeColumns).contains(
			"id",
			"code",
			"name",
			"type",
			"parent_id",
			"path",
			"component_key",
			"icon",
			"sort_order",
			"visible",
			"enabled",
			"api_method",
			"api_pattern",
		)

		val menuIcons = queryStrings(
			"""
			select icon
			from security_access_node
			where code in ('system', 'system.rbac.users', 'system.oauth.clients', 'system.scheduler.tasks')
			order by code
			""".trimIndent(),
		)
		assertThat(menuIcons).containsExactly(
			"lucide:settings",
			"lucide:plug",
			"lucide:users",
			"lucide:clock",
		)

		val jwkIdType = queryStrings(
			"""
			select data_type
			from information_schema.columns
			where table_schema = 'public'
				and table_name = 'oauth2_jwk'
				and column_name = 'id'
			""".trimIndent(),
		).single()
		assertThat(jwkIdType).isEqualTo("bigint")

		val jwkActiveUniqueIndex = queryStrings(
			"""
			select indexdef
			from pg_indexes
			where schemaname = 'public'
				and tablename = 'oauth2_jwk'
				and indexname = 'uk_oauth2_jwk__active_true'
			""".trimIndent(),
		).single()
		assertThat(jwkActiveUniqueIndex).contains("UNIQUE", "WHERE (active = true)")

		val oauthClientColumns = queryStrings(
			"""
			select column_name
			from information_schema.columns
			where table_schema = 'public'
				and table_name = 'oauth2_client'
			order by ordinal_position
			""".trimIndent(),
		)
		assertThat(oauthClientColumns).contains(
			"id",
			"client_id",
			"client_secret",
			"client_name",
			"client_authentication_methods",
			"authorization_grant_types",
			"scopes",
			"require_proof_key",
			"require_authorization_consent",
			"access_token_format",
			"access_token_ttl_seconds",
			"refresh_token_ttl_seconds",
			"reuse_refresh_tokens",
		)
		assertThat(oauthClientColumns).doesNotContain("client_settings", "token_settings")

		val clientScopes = queryStrings(
			"select scopes from oauth2_client order by client_id",
		)
		assertThat(clientScopes).containsOnly("game-data:admin security:admin")
	}

	@Test
	fun `liquibase creates game data tables with seed rows`() {
		val tableNames = queryStrings(
			"""
			select table_name
			from information_schema.tables
			where table_schema = 'public'
			""".trimIndent(),
		)

		assertThat(tableNames).contains(
			"game_element",
			"game_stat",
			"game_skill_damage_class",
			"game_item_category",
			"game_species_color",
			"game_species_shape",
			"game_habitat",
			"game_egg_group",
			"game_ability",
			"game_skill",
			"game_item",
			"game_species",
			"game_creature",
			"game_species_egg_group",
			"game_creature_element",
			"game_creature_stat",
			"game_creature_ability",
		)

		val seedCounts = queryMaps(
			"""
			select 'game_element' as table_name, count(*) as row_count from game_element
			union all select 'game_ability', count(*) from game_ability
			union all select 'game_skill', count(*) from game_skill
			union all select 'game_item', count(*) from game_item
			union all select 'game_species', count(*) from game_species
			union all select 'game_creature', count(*) from game_creature
			union all select 'game_creature_stat', count(*) from game_creature_stat
			order by table_name
			""".trimIndent(),
		).associate { it["table_name"] to it["row_count"].toString().toLong() }
		assertThat(seedCounts).containsEntry("game_element", 21L)
		assertThat(seedCounts).containsEntry("game_ability", 373L)
		assertThat(seedCounts).containsEntry("game_skill", 937L)
		assertThat(seedCounts).containsEntry("game_item", 2176L)
		assertThat(seedCounts).containsEntry("game_species", 1025L)
		assertThat(seedCounts).containsEntry("game_creature", 1351L)
		assertThat(seedCounts).containsEntry("game_creature_stat", 8100L)

		val creatureNames = queryStrings(
			"""
			select name
			from game_creature
			where id in (1, 4, 7)
			order by id
			""".trimIndent(),
		)
		assertThat(creatureNames).containsExactly("妙蛙种子", "小火龙", "杰尼龟")
	}

	@Test
	fun `liquibase remarks every application table and column`() {
		val tablesWithoutRemarks = queryStrings(
			"""
			select c.relname
			from pg_class c
			join pg_namespace n on n.oid = c.relnamespace
			where n.nspname = 'public'
				and c.relkind = 'r'
				and c.relname not in ('databasechangelog', 'databasechangeloglock')
				and obj_description(c.oid, 'pg_class') is null
			order by c.relname
			""".trimIndent(),
		)
		assertThat(tablesWithoutRemarks).isEmpty()

		val columnsWithoutRemarks = queryStrings(
			"""
			select cols.table_name || '.' || cols.column_name
			from information_schema.columns cols
			where cols.table_schema = 'public'
				and cols.table_name not in ('databasechangelog', 'databasechangeloglock')
				and col_description(
					(quote_ident(cols.table_schema) || '.' || quote_ident(cols.table_name))::regclass::oid,
					cols.ordinal_position
				) is null
			order by cols.table_name, cols.ordinal_position
			""".trimIndent(),
		)
		assertThat(columnsWithoutRemarks).isEmpty()
	}

	@Test
	fun `liquibase creates quartz job store tables`() {
		val tableNames = queryStrings(
			"""
			select table_name
			from information_schema.tables
			where table_schema = 'public'
			""".trimIndent(),
		)

		assertThat(tableNames).contains(
			"qrtz_job_details",
			"qrtz_triggers",
			"qrtz_simple_triggers",
			"qrtz_cron_triggers",
			"qrtz_simprop_triggers",
			"qrtz_blob_triggers",
			"qrtz_calendars",
			"qrtz_paused_trigger_grps",
			"qrtz_fired_triggers",
			"qrtz_scheduler_state",
			"qrtz_locks",
		)

		val jobDetailColumns = queryStrings(
			"""
			select column_name
			from information_schema.columns
			where table_schema = 'public'
				and table_name = 'qrtz_job_details'
			order by ordinal_position
			""".trimIndent(),
		)
		assertThat(jobDetailColumns).contains(
			"sched_name",
			"job_name",
			"job_group",
			"job_class_name",
			"job_data",
		)

		val quartzIndexes = queryStrings(
			"""
			select indexname
			from pg_indexes
			where schemaname = 'public'
				and tablename in ('qrtz_job_details', 'qrtz_triggers', 'qrtz_fired_triggers')
			order by indexname
			""".trimIndent(),
		)
		assertThat(quartzIndexes).contains(
			"idx_qrtz_j_grp",
			"idx_qrtz_t_next_fire_time",
			"idx_qrtz_ft_trig_inst_name",
		)
	}

	@Test
	fun `liquibase creates scheduled task management tables`() {
		val tableNames = queryStrings(
			"""
			select table_name
			from information_schema.tables
			where table_schema = 'public'
			""".trimIndent(),
		)

		assertThat(tableNames).contains(
			"scheduled_task",
			"scheduled_task_execution",
		)

		val taskColumns = queryStrings(
			"""
			select column_name
			from information_schema.columns
			where table_schema = 'public'
				and table_name = 'scheduled_task'
			order by ordinal_position
			""".trimIndent(),
		)
		assertThat(taskColumns).contains(
			"id",
			"code",
			"handler_code",
			"name",
			"schedule_type",
			"payload_json",
			"enabled",
		)

		val executionColumns = queryStrings(
			"""
			select column_name
			from information_schema.columns
			where table_schema = 'public'
				and table_name = 'scheduled_task_execution'
			order by ordinal_position
			""".trimIndent(),
		)
		assertThat(executionColumns).contains(
			"id",
			"task_id",
			"task_code",
			"handler_code",
			"status",
			"payload_snapshot_json",
			"error_message",
		)

		val taskIndexes = queryStrings(
			"""
			select indexname
			from pg_indexes
			where schemaname = 'public'
				and tablename in ('scheduled_task', 'scheduled_task_execution')
			order by indexname
			""".trimIndent(),
		)
		assertThat(taskIndexes).contains(
			"uk_scheduled_task__code",
			"idx_scheduled_task_execution__task_id_actual_fire_time",
		)
	}

	@Test
	fun `liquibase seeds default security admin user`() {
		val admin = queryMaps(
			"""
			select username, password_hash, enabled, account_non_locked
			from security_user
			where username = 'admin'
			""".trimIndent(),
		).single()

		assertThat(admin["password_hash"]).isEqualTo("{noop}secret")
		assertThat(admin["enabled"]).isEqualTo(true)
		assertThat(admin["account_non_locked"]).isEqualTo(true)

		val roleCodes = queryStrings(
			"""
			select r.code
			from security_role r
			join security_user_role ur on ur.role_id = r.id
			join security_user u on u.id = ur.user_id
			where u.username = 'admin'
			order by r.code
			""".trimIndent(),
		)
		assertThat(roleCodes).containsExactly("game-data-admin", "system-admin")
	}

	private fun queryStrings(sql: String): List<String> =
		query(sql) { resultSet ->
			resultSet.getString(1)
		}

	private fun queryMaps(sql: String): List<Map<String, Any?>> =
		query(sql) { resultSet ->
			val metadata = resultSet.metaData
			(1..metadata.columnCount).associate { index ->
				metadata.getColumnLabel(index) to resultSet.getObject(index)
			}
		}

	private fun <T> query(sql: String, mapper: (ResultSet) -> T): List<T> =
		dataSource.connection.use { connection ->
			connection.prepareStatement(sql).use { statement ->
				statement.executeQuery().use { resultSet ->
					buildList {
						while (resultSet.next()) {
							add(mapper(resultSet))
						}
					}
				}
			}
		}
}
