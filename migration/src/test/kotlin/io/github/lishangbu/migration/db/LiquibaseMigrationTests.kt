package io.github.lishangbu.migration.db

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.yaml.snakeyaml.Yaml
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
		assertThat(resource!!.readText())
			.contains("includeAll")
			.contains("db/changelog/changes")
	}

	@Test
	fun `liquibase changelog history keeps single initial file and numbered follow ups`() {
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
			"002-battle-rules-schema.yaml",
			"003-battle-effect-rules-schema.yaml",
			"004-battle-rule-coverage-menu.yaml",
			"005-normalize-flinch-status-kind.yaml",
			"006-battle-skill-rule-runtime-fields.yaml",
			"007-battle-skill-weather-modifiers.yaml",
			"008-battle-skill-runtime-seed-corrections.yaml",
			"009-battle-skill-field-effects.yaml",
			"010-battle-skill-speed-field-effects.yaml",
			"011-battle-skill-global-field-effects.yaml",
		)
		assertThat(changelogFiles.count { it.startsWith("001-") }).isEqualTo(1)
	}

	@Test
	fun `liquibase create table changes declare inline remarks`() {
		val missingRemarks = buildList {
			changelogChanges().forEach { change ->
				val createTable = change["createTable"] as? Map<*, *> ?: return@forEach
				val tableName = createTable["tableName"]
				if ((createTable["remarks"] as? String).isNullOrBlank()) {
					add(tableName.toString())
				}
				val columns = createTable["columns"] as? List<*> ?: emptyList<Any>()
				columns.forEach { entry ->
					val column = (entry as? Map<*, *>)?.get("column") as? Map<*, *> ?: return@forEach
					if ((column["remarks"] as? String).isNullOrBlank()) {
						add("$tableName.${column["name"]}")
					}
				}
			}
		}

		assertThat(missingRemarks).isEmpty()
	}

	@Test
	fun `liquibase seed data uses load data files`() {
		val changes = initialChanges()
		val inlineInsertTables = changes.mapNotNull { change ->
			(change["insert"] as? Map<*, *>)?.get("tableName")?.toString()
		}
		val sqlInserts = changes.mapNotNull { change ->
			(change["sql"] as? Map<*, *>)?.get("sql")?.toString()
		}.filter { sql ->
			sql.contains("insert into", ignoreCase = true)
		}
		val systemSeedFiles = changes.mapNotNull { change ->
			(change["loadData"] as? Map<*, *>)?.get("file")?.toString()
		}.filter { file ->
			file.startsWith("db/changelog/data/system/")
		}

		assertThat(inlineInsertTables).isEmpty()
		assertThat(sqlInserts).isEmpty()
		assertThat(systemSeedFiles).containsExactly(
			"db/changelog/data/system/security_role.csv",
			"db/changelog/data/system/security_user.csv",
			"db/changelog/data/system/security_access_node.csv",
			"db/changelog/data/system/security_user_role.csv",
			"db/changelog/data/system/security_role_access_node.csv",
			"db/changelog/data/system/oauth2_client.csv",
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
			"system.oauth.tokens",
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
			"game-data.berry",
			"game-data.berries",
			"game-data.catalog",
			"game-data.core",
			"game-data.creature-abilities",
			"game-data.creature-elements",
			"game-data.creature-stats",
			"game-data.creatures",
			"game-data.dictionary",
			"game-data.egg-groups",
			"game-data.elements",
			"game-data.encounter",
			"game-data.evolution",
			"game-data.ext-relations",
			"game-data.habitats",
			"game-data.item-categories",
			"game-data.item-extra",
			"game-data.items",
			"game-data.relations",
			"game-data.skill-damage-classes",
			"game-data.skill-extra",
			"game-data.skills",
			"game-data.species",
			"game-data.species-colors",
			"game-data.species-egg-groups",
			"game-data.species-shapes",
			"game-data.stats",
			"game-data.world",
		)
		val battleRulesAccessNodeCodes = listOf(
			"battle-rules",
			"battle-rules:admin",
			"battle-rules.ability-rules",
			"battle-rules.battle-formats",
			"battle-rules.coverage",
			"battle-rules.effects",
			"battle-rules.field-rules",
			"battle-rules.format-clause-bindings",
			"battle-rules.format-clauses",
			"battle-rules.format-restrictions",
			"battle-rules.format-special-mechanics",
			"battle-rules.formats",
			"battle-rules.item-rules",
			"battle-rules.special-mechanics",
			"battle-rules.skill-rules",
			"battle-rules.skill-stat-stage-effects",
			"battle-rules.skill-status-effects",
			"battle-rules.status-rules",
			"battle-rules.terrain-rules",
			"battle-rules.weather-rules",
		)
		assertThat(accessNodeCodes).containsAll(battleRulesAccessNodeCodes + gameDataAccessNodeCodes + systemAccessNodeCodes)

		val roleCodes = queryStrings(
			"select code from security_role order by code",
		)
		assertThat(roleCodes).containsExactly("battle-rules-admin", "game-data-admin", "system-admin")

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
		val allGameDataAccessNodeCodes = accessNodeCodes.filter { it.startsWith("game-data") }
		assertThat(gameDataAdminAccessNodeCodes).containsExactlyInAnyOrderElementsOf(allGameDataAccessNodeCodes)

		val battleRulesAdminAccessNodeCodes = queryStrings(
			"""
			select n.code
			from security_access_node n
			join security_role_access_node ran on ran.access_node_id = n.id
			join security_role r on r.id = ran.role_id
			where r.code = 'battle-rules-admin'
			order by n.code
			""".trimIndent(),
		)
		val allBattleRulesAccessNodeCodes = accessNodeCodes.filter { it.startsWith("battle-rules") }
		assertThat(battleRulesAdminAccessNodeCodes).containsExactlyInAnyOrderElementsOf(allBattleRulesAccessNodeCodes)

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
		assertThat(clientScopes).containsOnly("battle-rules:admin game-data:admin security:admin")
	}

	@Test
	fun `liquibase creates battle rules tables with seed rows`() {
		val tableNames = queryStrings(
			"""
			select table_name
			from information_schema.tables
			where table_schema = 'public'
			""".trimIndent(),
		)

		assertThat(tableNames).contains(
			"battle_format",
			"battle_format_clause",
			"battle_format_clause_binding",
			"battle_format_restriction",
			"battle_special_mechanic",
			"battle_format_special_mechanic",
			"battle_status_rule",
			"battle_weather_rule",
			"battle_terrain_rule",
			"battle_field_rule",
			"battle_skill_rule",
			"battle_skill_status_effect",
			"battle_skill_stat_stage_effect",
			"battle_skill_field_effect",
			"battle_skill_global_field_effect",
			"battle_skill_weather_accuracy_override",
			"battle_skill_weather_power_modifier",
			"battle_ability_rule",
			"battle_item_rule",
		)

		val seedCounts = queryMaps(
			"""
			select 'battle_format' as table_name, count(*) as row_count from battle_format
			union all select 'battle_format_clause', count(*) from battle_format_clause
			union all select 'battle_format_clause_binding', count(*) from battle_format_clause_binding
			union all select 'battle_format_restriction', count(*) from battle_format_restriction
			union all select 'battle_special_mechanic', count(*) from battle_special_mechanic
			union all select 'battle_format_special_mechanic', count(*) from battle_format_special_mechanic
			union all select 'battle_status_rule', count(*) from battle_status_rule
			union all select 'battle_weather_rule', count(*) from battle_weather_rule
			union all select 'battle_terrain_rule', count(*) from battle_terrain_rule
			union all select 'battle_field_rule', count(*) from battle_field_rule
			union all select 'battle_skill_rule', count(*) from battle_skill_rule
			union all select 'battle_skill_status_effect', count(*) from battle_skill_status_effect
			union all select 'battle_skill_stat_stage_effect', count(*) from battle_skill_stat_stage_effect
			union all select 'battle_skill_field_effect', count(*) from battle_skill_field_effect
			union all select 'battle_skill_global_field_effect', count(*) from battle_skill_global_field_effect
			union all select 'battle_skill_weather_accuracy_override', count(*) from battle_skill_weather_accuracy_override
			union all select 'battle_skill_weather_power_modifier', count(*) from battle_skill_weather_power_modifier
			union all select 'battle_ability_rule', count(*) from battle_ability_rule
			union all select 'battle_item_rule', count(*) from battle_item_rule
			order by table_name
			""".trimIndent(),
		).associate { it["table_name"] to it["row_count"].toString().toLong() }
		assertThat(seedCounts).containsEntry("battle_ability_rule", 5L)
		assertThat(seedCounts).containsEntry("battle_item_rule", 5L)
		assertThat(seedCounts).containsEntry("battle_format", 4L)
		assertThat(seedCounts).containsEntry("battle_format_clause", 4L)
		assertThat(seedCounts).containsEntry("battle_format_clause_binding", 4L)
		assertThat(seedCounts).containsEntry("battle_format_restriction", 3L)
		assertThat(seedCounts).containsEntry("battle_special_mechanic", 3L)
		assertThat(seedCounts).containsEntry("battle_format_special_mechanic", 6L)
		assertThat(seedCounts).containsEntry("battle_status_rule", 8L)
		assertThat(seedCounts).containsEntry("battle_weather_rule", 5L)
		assertThat(seedCounts).containsEntry("battle_terrain_rule", 4L)
		assertThat(seedCounts).containsEntry("battle_field_rule", 9L)
		assertThat(seedCounts).containsEntry("battle_skill_rule", 18L)
		assertThat(seedCounts).containsEntry("battle_skill_status_effect", 2L)
		assertThat(seedCounts).containsEntry("battle_skill_stat_stage_effect", 2L)
		assertThat(seedCounts).containsEntry("battle_skill_field_effect", 4L)
		assertThat(seedCounts).containsEntry("battle_skill_global_field_effect", 1L)
		assertThat(seedCounts).containsEntry("battle_skill_weather_accuracy_override", 5L)
		assertThat(seedCounts).containsEntry("battle_skill_weather_power_modifier", 7L)

		val formatNames = queryStrings(
			"select name from battle_format order by id",
		)
		assertThat(formatNames).containsExactly("标准单打", "标准双打", "官方双打", "自定义规则")

		val statusNames = queryStrings(
			"select name from battle_status_rule where code in ('burn', 'paralysis', 'sleep') order by code",
		)
		assertThat(statusNames).containsExactly("灼伤", "麻痹", "睡眠")

		val flinchStatusKind = queryStrings(
			"select status_kind from battle_status_rule where code = 'flinch'",
		)
		assertThat(flinchStatusKind).containsExactly("VOLATILE")

		val skillRulePolicies = queryStrings(
			"""
			select effect_policy
			from battle_skill_rule
			where skill_id in (52, 85, 182)
			order by skill_id
			""".trimIndent(),
		)
		assertThat(skillRulePolicies).containsExactly(
			"standard-damage-with-status",
			"standard-damage-with-status",
			"protect-self",
		)

		val runtimeCorrections = queryMaps(
			"""
			select skill_id, min_hits, max_hits, protects_user
			from battle_skill_rule
			where skill_id in (182, 331)
			order by skill_id
			""".trimIndent(),
		)
		assertThat(runtimeCorrections).containsExactly(
			mapOf(
				"skill_id" to 182L,
				"min_hits" to 1,
				"max_hits" to 1,
				"protects_user" to true,
			),
			mapOf(
				"skill_id" to 331L,
				"min_hits" to 2,
				"max_hits" to 5,
				"protects_user" to false,
			),
		)
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
			"game_berry",
			"game_berry_flavor",
			"game_item_attribute",
			"game_skill_ailment",
			"game_growth_rate",
			"game_region",
			"game_location_area_encounter",
			"game_evolution_chain",
			"game_catalog",
			"game_creature_form",
			"game_creature_skill_learn",
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
			union all select 'game_berry', count(*) from game_berry
			union all select 'game_region', count(*) from game_region
			union all select 'game_location_area_encounter', count(*) from game_location_area_encounter
			union all select 'game_creature_skill_learn', count(*) from game_creature_skill_learn
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
		assertThat(seedCounts).containsEntry("game_berry", 64L)
		assertThat(seedCounts).containsEntry("game_region", 11L)
		assertThat(seedCounts).containsEntry("game_location_area_encounter", 22491L)
		assertThat(seedCounts).containsEntry("game_creature_skill_learn", 139841L)

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
		assertThat(roleCodes).containsExactly("battle-rules-admin", "game-data-admin", "system-admin")
	}

	private fun queryStrings(sql: String): List<String> =
		query(sql) { resultSet ->
			resultSet.getString(1)
		}

	@Suppress("UNCHECKED_CAST")
	private fun initialChanges(): List<Map<String, Any?>> {
		val resource = javaClass.getResource("/db/changelog/changes/001-initial-schema.yaml")
		val root = Yaml().load<Map<String, Any?>>(resource!!.readText())
		val databaseChangeLog = root["databaseChangeLog"] as List<Map<String, Any?>>
		val changeSet = databaseChangeLog.single()["changeSet"] as Map<String, Any?>
		return changeSet["changes"] as List<Map<String, Any?>>
	}

	@Suppress("UNCHECKED_CAST")
	private fun changelogChanges(): List<Map<String, Any?>> {
		val resource = javaClass.getResource("/db/changelog/changes")
		return Files.list(Path.of(resource!!.toURI())).use { paths ->
			paths
				.filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(".yaml") }
				.sorted()
				.flatMap { path ->
					val root = Yaml().load<Map<String, Any?>>(Files.readString(path))
					val databaseChangeLog = root["databaseChangeLog"] as List<Map<String, Any?>>
					databaseChangeLog.stream().flatMap { entry ->
						val changeSet = entry["changeSet"] as Map<String, Any?>
						(changeSet["changes"] as List<Map<String, Any?>>).stream()
					}
				}
				.toList()
		}
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
