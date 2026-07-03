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
	fun `liquibase changelog history keeps initial file and merged battle file`() {
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
			"002-battle-rules.yaml",
		)
		assertThat(changelogFiles.count { it.startsWith("001-") }).isEqualTo(1)
		assertThat(changelogFiles.count { it.startsWith("002-") }).isEqualTo(1)
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
			"battle-rules.action-validation",
			"battle-rules.ability-rules",
			"battle-rules.battle-formats",
			"battle-rules.effects",
			"battle-rules.field-rules",
			"battle-rules.format-clause-bindings",
			"battle-rules.format-clauses",
			"battle-rules.format-restrictions",
			"battle-rules.format-special-mechanics",
			"battle-rules.formats",
			"battle-rules.item-rules",
			"battle-rules.preparation-validation",
			"battle-rules.special-mechanics",
			"battle-rules.skill-field-effects",
			"battle-rules.skill-global-field-effects",
			"battle-rules.skill-rules",
			"battle-rules.skill-charge-skip-weathers",
			"battle-rules.skill-stat-stage-effects",
			"battle-rules.skill-stat-stage-operations",
			"battle-rules.skill-status-effects",
			"battle-rules.skill-terrain-element-overrides",
			"battle-rules.skill-terrain-power-modifiers",
			"battle-rules.skill-weather-accuracy-overrides",
			"battle-rules.skill-weather-element-overrides",
			"battle-rules.skill-weather-power-modifiers",
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
			"battle_skill_stat_stage_operation",
			"battle_skill_field_effect",
			"battle_skill_global_field_effect",
			"battle_skill_weather_accuracy_override",
			"battle_skill_weather_element_override",
			"battle_skill_weather_power_modifier",
			"battle_skill_terrain_element_override",
			"battle_skill_terrain_power_modifier",
			"battle_skill_charge_skip_weather",
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
			union all select 'battle_skill_stat_stage_operation', count(*) from battle_skill_stat_stage_operation
			union all select 'battle_skill_field_effect', count(*) from battle_skill_field_effect
			union all select 'battle_skill_global_field_effect', count(*) from battle_skill_global_field_effect
			union all select 'battle_skill_weather_accuracy_override', count(*) from battle_skill_weather_accuracy_override
			union all select 'battle_skill_weather_element_override', count(*) from battle_skill_weather_element_override
			union all select 'battle_skill_weather_power_modifier', count(*) from battle_skill_weather_power_modifier
			union all select 'battle_skill_terrain_element_override', count(*) from battle_skill_terrain_element_override
			union all select 'battle_skill_terrain_power_modifier', count(*) from battle_skill_terrain_power_modifier
			union all select 'battle_skill_charge_skip_weather', count(*) from battle_skill_charge_skip_weather
			union all select 'battle_ability_rule', count(*) from battle_ability_rule
			union all select 'battle_item_rule', count(*) from battle_item_rule
			order by table_name
			""".trimIndent(),
		).associate { it["table_name"] to it["row_count"].toString().toLong() }
		assertThat(seedCounts).containsEntry("battle_ability_rule", 68L)
		assertThat(seedCounts).containsEntry("battle_item_rule", 61L)
		assertThat(seedCounts).containsEntry("battle_format", 4L)
		assertThat(seedCounts).containsEntry("battle_format_clause", 4L)
		assertThat(seedCounts).containsEntry("battle_format_clause_binding", 4L)
		assertThat(seedCounts).containsEntry("battle_format_restriction", 3L)
		assertThat(seedCounts).containsEntry("battle_special_mechanic", 3L)
		assertThat(seedCounts).containsEntry("battle_format_special_mechanic", 6L)
		assertThat(seedCounts).containsEntry("battle_status_rule", 13L)
		assertThat(seedCounts).containsEntry("battle_weather_rule", 5L)
		assertThat(seedCounts).containsEntry("battle_terrain_rule", 4L)
		assertThat(seedCounts).containsEntry("battle_field_rule", 9L)
		assertThat(seedCounts).containsEntry("battle_skill_rule", 937L)
		assertThat(seedCounts).containsEntry("battle_skill_status_effect", 133L)
		assertThat(seedCounts).containsEntry("battle_skill_stat_stage_effect", 234L)
		assertThat(seedCounts).containsEntry("battle_skill_stat_stage_operation", 39L)
		assertThat(seedCounts).containsEntry("battle_skill_field_effect", 8L)
		assertThat(seedCounts).containsEntry("battle_skill_global_field_effect", 1L)
		assertThat(seedCounts).containsEntry("battle_skill_weather_accuracy_override", 5L)
		assertThat(seedCounts).containsEntry("battle_skill_weather_element_override", 4L)
		assertThat(seedCounts).containsEntry("battle_skill_weather_power_modifier", 7L)
		assertThat(seedCounts).containsEntry("battle_skill_terrain_element_override", 4L)
		assertThat(seedCounts).containsEntry("battle_skill_terrain_power_modifier", 4L)
		assertThat(seedCounts).containsEntry("battle_skill_charge_skip_weather", 1L)

		val enabledSkillsWithoutRules = queryMaps(
			"""
			select s.id, s.code
			from game_skill s
			where s.enabled = true
				and not exists (
					select 1
					from battle_skill_rule r
					where r.skill_id = s.id
						and r.enabled = true
				)
			order by s.id
			""".trimIndent(),
		)
		assertThat(enabledSkillsWithoutRules)
			.describedAs("所有启用技能都必须有启用中的基础战斗规则，运行时不再保留无规则 fallback")
			.isEmpty()

		val derivedBasicSkillRules = queryMaps(
			"""
			select skill_id, target_policy, hit_policy, min_hits, max_hits, critical_hit_stage
			from battle_skill_rule
			where skill_id in (2, 3, 37, 57, 74, 129)
			order by skill_id
			""".trimIndent(),
		)
		assertThat(derivedBasicSkillRules).containsExactly(
			mapOf(
				"skill_id" to 2L,
				"target_policy" to "selected-target",
				"hit_policy" to "standard-hit",
				"min_hits" to 1,
				"max_hits" to 1,
				"critical_hit_stage" to 1,
			),
			mapOf(
				"skill_id" to 3L,
				"target_policy" to "selected-target",
				"hit_policy" to "multi-hit",
				"min_hits" to 2,
				"max_hits" to 5,
				"critical_hit_stage" to 0,
			),
			mapOf(
				"skill_id" to 37L,
				"target_policy" to "random-opponent",
				"hit_policy" to "standard-hit",
				"min_hits" to 1,
				"max_hits" to 1,
				"critical_hit_stage" to 0,
			),
			mapOf(
				"skill_id" to 57L,
				"target_policy" to "all-adjacent-participants",
				"hit_policy" to "standard-hit",
				"min_hits" to 1,
				"max_hits" to 1,
				"critical_hit_stage" to 0,
			),
			mapOf(
				"skill_id" to 74L,
				"target_policy" to "self",
				"hit_policy" to "always-hit",
				"min_hits" to 1,
				"max_hits" to 1,
				"critical_hit_stage" to 0,
			),
			mapOf(
				"skill_id" to 129L,
				"target_policy" to "all-opponents",
				"hit_policy" to "always-hit",
				"min_hits" to 1,
				"max_hits" to 1,
				"critical_hit_stage" to 0,
			),
		)

		val derivedHpSkillRules = queryMaps(
			"""
			select skill_id, effect_policy, target_policy, damage_policy
			from battle_skill_rule
			where skill_id in (36, 138, 344, 456, 457, 733)
			order by skill_id
			""".trimIndent(),
		)
		assertThat(derivedHpSkillRules).containsExactly(
			mapOf(
				"skill_id" to 36L,
				"effect_policy" to "recoil-quarter-damage",
				"target_policy" to "selected-target",
				"damage_policy" to "standard-damage",
			),
			mapOf(
				"skill_id" to 138L,
				"effect_policy" to "drain-half-damage",
				"target_policy" to "selected-target",
				"damage_policy" to "standard-damage",
			),
			mapOf(
				"skill_id" to 344L,
				"effect_policy" to "recoil-third-damage",
				"target_policy" to "selected-target",
				"damage_policy" to "standard-damage",
			),
			mapOf(
				"skill_id" to 456L,
				"effect_policy" to "self-heal-half-max-hp",
				"target_policy" to "self",
				"damage_policy" to "no-damage",
			),
			mapOf(
				"skill_id" to 457L,
				"effect_policy" to "recoil-half-damage",
				"target_policy" to "selected-target",
				"damage_policy" to "standard-damage",
			),
			mapOf(
				"skill_id" to 733L,
				"effect_policy" to "drain-full-damage",
				"target_policy" to "selected-target",
				"damage_policy" to "standard-damage",
			),
		)

		val derivedRampageSkillRules = queryMaps(
			"""
			select skill_id, target_policy, lock_move_turns_min, lock_move_turns_max, confuses_user_after_lock
			from battle_skill_rule
			where skill_id in (37, 80, 200)
			order by skill_id
			""".trimIndent(),
		)
		assertThat(derivedRampageSkillRules).containsExactly(
			mapOf(
				"skill_id" to 37L,
				"target_policy" to "random-opponent",
				"lock_move_turns_min" to 2,
				"lock_move_turns_max" to 3,
				"confuses_user_after_lock" to true,
			),
			mapOf(
				"skill_id" to 80L,
				"target_policy" to "random-opponent",
				"lock_move_turns_min" to 2,
				"lock_move_turns_max" to 3,
				"confuses_user_after_lock" to true,
			),
			mapOf(
				"skill_id" to 200L,
				"target_policy" to "random-opponent",
				"lock_move_turns_min" to 2,
				"lock_move_turns_max" to 3,
				"confuses_user_after_lock" to true,
			),
		)

		val derivedStatusEffects = queryMaps(
			"""
			select sr.skill_id, br.code as status_code, se.chance_percent
			from battle_skill_status_effect se
			join battle_skill_rule sr on sr.id = se.skill_rule_id
			join battle_status_rule br on br.id = se.status_rule_id
			where sr.skill_id in (7, 47, 344, 435, 464)
			order by sr.skill_id
			""".trimIndent(),
		)
		assertThat(derivedStatusEffects).containsExactly(
			mapOf(
				"skill_id" to 7L,
				"status_code" to "burn",
				"chance_percent" to 10,
			),
			mapOf(
				"skill_id" to 47L,
				"status_code" to "sleep",
				"chance_percent" to 100,
			),
			mapOf(
				"skill_id" to 344L,
				"status_code" to "paralysis",
				"chance_percent" to 10,
			),
			mapOf(
				"skill_id" to 435L,
				"status_code" to "paralysis",
				"chance_percent" to 30,
			),
			mapOf(
				"skill_id" to 464L,
				"status_code" to "sleep",
				"chance_percent" to 100,
			),
		)

		val derivedPoisonEffects = queryMaps(
			"""
			select sr.skill_id, br.code as status_code, se.chance_percent
			from battle_skill_status_effect se
			join battle_skill_rule sr on sr.id = se.skill_rule_id
			join battle_status_rule br on br.id = se.status_rule_id
			where sr.skill_id in (40, 92, 305)
			order by sr.skill_id
			""".trimIndent(),
		)
		assertThat(derivedPoisonEffects).containsExactly(
			mapOf(
				"skill_id" to 40L,
				"status_code" to "poison",
				"chance_percent" to 30,
			),
			mapOf(
				"skill_id" to 92L,
				"status_code" to "bad-poison",
				"chance_percent" to 100,
			),
			mapOf(
				"skill_id" to 305L,
				"status_code" to "bad-poison",
				"chance_percent" to 50,
			),
		)

		val derivedFlinchEffects = queryMaps(
			"""
			select sr.skill_id, br.code as status_code, se.chance_percent
			from battle_skill_status_effect se
			join battle_skill_rule sr on sr.id = se.skill_rule_id
			join battle_status_rule br on br.id = se.status_rule_id
			where sr.skill_id in (23, 157, 252)
			order by sr.skill_id
			""".trimIndent(),
		)
		assertThat(derivedFlinchEffects).containsExactly(
			mapOf(
				"skill_id" to 23L,
				"status_code" to "flinch",
				"chance_percent" to 30,
			),
			mapOf(
				"skill_id" to 157L,
				"status_code" to "flinch",
				"chance_percent" to 30,
			),
			mapOf(
				"skill_id" to 252L,
				"status_code" to "flinch",
				"chance_percent" to 100,
			),
		)

		val derivedBindingEffects = queryMaps(
			"""
			select sr.skill_id, br.code as status_code, se.chance_percent
			from battle_skill_status_effect se
			join battle_skill_rule sr on sr.id = se.skill_rule_id
			join battle_status_rule br on br.id = se.status_rule_id
			where sr.skill_id in (35, 83, 611, 819)
			order by sr.skill_id
			""".trimIndent(),
		)
		assertThat(derivedBindingEffects).containsExactly(
			mapOf(
				"skill_id" to 35L,
				"status_code" to "binding",
				"chance_percent" to 100,
			),
			mapOf(
				"skill_id" to 83L,
				"status_code" to "binding",
				"chance_percent" to 100,
			),
			mapOf(
				"skill_id" to 611L,
				"status_code" to "binding",
				"chance_percent" to 100,
			),
			mapOf(
				"skill_id" to 819L,
				"status_code" to "binding",
				"chance_percent" to 100,
			),
		)

		val derivedStatStageEffects = queryMaps(
			"""
			select sr.skill_id, st.code as stat_code, se.target_scope, se.stage_delta, se.chance_percent
			from battle_skill_stat_stage_effect se
			join battle_skill_rule sr on sr.id = se.skill_rule_id
			join game_stat st on st.id = se.stat_id
			where sr.skill_id in (28, 74, 81, 189)
			order by sr.skill_id, st.code
			""".trimIndent(),
		)
		assertThat(derivedStatStageEffects).containsExactly(
			mapOf(
				"skill_id" to 28L,
				"stat_code" to "accuracy",
				"target_scope" to "TARGET",
				"stage_delta" to -1,
				"chance_percent" to 100,
			),
			mapOf(
				"skill_id" to 74L,
				"stat_code" to "attack",
				"target_scope" to "USER",
				"stage_delta" to 1,
				"chance_percent" to 100,
			),
			mapOf(
				"skill_id" to 74L,
				"stat_code" to "special-attack",
				"target_scope" to "USER",
				"stage_delta" to 1,
				"chance_percent" to 100,
			),
			mapOf(
				"skill_id" to 81L,
				"stat_code" to "speed",
				"target_scope" to "ALL_OPPONENTS",
				"stage_delta" to -2,
				"chance_percent" to 100,
			),
			mapOf(
				"skill_id" to 189L,
				"stat_code" to "accuracy",
				"target_scope" to "TARGET",
				"stage_delta" to -1,
				"chance_percent" to 100,
			),
		)

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

		val tormentStatus = queryMaps(
			"""
			select code, status_kind, effect_policy, min_turns, max_turns
			from battle_status_rule
			where code = 'torment'
			""".trimIndent(),
		)
		assertThat(tormentStatus).containsExactly(
			mapOf(
				"code" to "torment",
				"status_kind" to "VOLATILE",
				"effect_policy" to "volatile-torment",
				"min_turns" to null,
				"max_turns" to null,
			),
		)

		val bindingStatus = queryMaps(
			"""
			select code, status_kind, effect_policy, min_turns, max_turns
			from battle_status_rule
			where code = 'binding'
			""".trimIndent(),
		)
		assertThat(bindingStatus).containsExactly(
			mapOf(
				"code" to "binding",
				"status_kind" to "VOLATILE",
				"effect_policy" to "volatile-binding",
				"min_turns" to 4,
				"max_turns" to 5,
			),
		)

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

		val tormentSkillRule = queryMaps(
			"""
			select skill_id, effect_policy, damage_policy
			from battle_skill_rule
			where skill_id = 259
			""".trimIndent(),
		)
		assertThat(tormentSkillRule).containsExactly(
			mapOf(
				"skill_id" to 259L,
				"effect_policy" to "apply-torment",
				"damage_policy" to "status-effect",
			),
		)

		val bindingSkillRule = queryMaps(
			"""
			select skill_id, effect_policy, damage_policy
			from battle_skill_rule
			where skill_id = 20
			""".trimIndent(),
		)
		assertThat(bindingSkillRule).containsExactly(
			mapOf(
				"skill_id" to 20L,
				"effect_policy" to "standard-damage-with-status",
				"damage_policy" to "standard-damage",
			),
		)

		val fixedDamageSkillRules = queryMaps(
			"""
			select skill_id, effect_policy, damage_policy
			from battle_skill_rule
			where skill_id in (49, 69, 82, 101)
			order by skill_id
			""".trimIndent(),
		)
		assertThat(fixedDamageSkillRules).containsExactly(
			mapOf(
				"skill_id" to 49L,
				"effect_policy" to "fixed-damage-20",
				"damage_policy" to "fixed-damage",
			),
			mapOf(
				"skill_id" to 69L,
				"effect_policy" to "user-level-fixed-damage",
				"damage_policy" to "fixed-damage",
			),
			mapOf(
				"skill_id" to 82L,
				"effect_policy" to "fixed-damage-40",
				"damage_policy" to "fixed-damage",
			),
			mapOf(
				"skill_id" to 101L,
				"effect_policy" to "user-level-fixed-damage",
				"damage_policy" to "fixed-damage",
			),
		)

		val proportionalDamageSkillRules = queryMaps(
			"""
			select skill_id, effect_policy, damage_policy
			from battle_skill_rule
			where skill_id in (162, 717, 877)
			order by skill_id
			""".trimIndent(),
		)
		assertThat(proportionalDamageSkillRules).containsExactly(
			mapOf(
				"skill_id" to 162L,
				"effect_policy" to "target-current-hp-half-damage",
				"damage_policy" to "proportional-damage",
			),
			mapOf(
				"skill_id" to 717L,
				"effect_policy" to "target-current-hp-half-damage",
				"damage_policy" to "proportional-damage",
			),
			mapOf(
				"skill_id" to 877L,
				"effect_policy" to "target-current-hp-half-damage",
				"damage_policy" to "proportional-damage",
			),
		)

		val hpDerivedDamageSkillRules = queryMaps(
			"""
			select skill_id, effect_policy, damage_policy
			from battle_skill_rule
			where skill_id in (283, 515)
			order by skill_id
			""".trimIndent(),
		)
		assertThat(hpDerivedDamageSkillRules).containsExactly(
			mapOf(
				"skill_id" to 283L,
				"effect_policy" to "target-hp-minus-user-hp-damage",
				"damage_policy" to "hp-derived-damage",
			),
			mapOf(
				"skill_id" to 515L,
				"effect_policy" to "user-current-hp-sacrifice-damage",
				"damage_policy" to "hp-derived-damage",
			),
		)

		val oneHitKnockOutSkillRules = queryMaps(
			"""
			select skill_id, effect_policy, damage_policy
			from battle_skill_rule
			where skill_id in (12, 32, 90, 329)
			order by skill_id
			""".trimIndent(),
		)
		assertThat(oneHitKnockOutSkillRules).containsExactly(
			mapOf(
				"skill_id" to 12L,
				"effect_policy" to "one-hit-knockout-damage",
				"damage_policy" to "one-hit-knockout-damage",
			),
			mapOf(
				"skill_id" to 32L,
				"effect_policy" to "one-hit-knockout-damage",
				"damage_policy" to "one-hit-knockout-damage",
			),
			mapOf(
				"skill_id" to 90L,
				"effect_policy" to "one-hit-knockout-damage",
				"damage_policy" to "one-hit-knockout-damage",
			),
			mapOf(
				"skill_id" to 329L,
				"effect_policy" to "same-element-sensitive-one-hit-knockout-damage",
				"damage_policy" to "one-hit-knockout-damage",
			),
		)

		val targetHealingSkillRules = queryMaps(
			"""
			select skill_id, effect_policy, target_policy, damage_policy
			from battle_skill_rule
			where skill_id in (505, 666)
			order by skill_id
			""".trimIndent(),
		)
		assertThat(targetHealingSkillRules).containsExactly(
			mapOf(
				"skill_id" to 505L,
				"effect_policy" to "target-heal-half-max-hp",
				"target_policy" to "selected-target",
				"damage_policy" to "no-damage",
			),
			mapOf(
				"skill_id" to 666L,
				"effect_policy" to "terrain-target-heal-max-hp",
				"target_policy" to "selected-target",
				"damage_policy" to "no-damage",
			),
		)

		val sandstormHealingSkillRules = queryMaps(
			"""
			select skill_id, effect_policy, target_policy, damage_policy
			from battle_skill_rule
			where skill_id = 659
			""".trimIndent(),
		)
		assertThat(sandstormHealingSkillRules).containsExactly(
			mapOf(
				"skill_id" to 659L,
				"effect_policy" to "sandstorm-self-heal-max-hp",
				"target_policy" to "self",
				"damage_policy" to "no-damage",
			),
		)

		val strengthSapSkillRules = queryMaps(
			"""
			select skill_id, effect_policy, target_policy, damage_policy
			from battle_skill_rule
			where skill_id = 668
			""".trimIndent(),
		)
		assertThat(strengthSapSkillRules).containsExactly(
			mapOf(
				"skill_id" to 668L,
				"effect_policy" to "self-heal-by-target-current-attack",
				"target_policy" to "selected-target",
				"damage_policy" to "no-damage",
			),
		)

		val strengthSapStatStageEffects = queryMaps(
			"""
			select sr.skill_id, st.code as stat_code, se.target_scope, se.stage_delta, se.chance_percent
			from battle_skill_stat_stage_effect se
			join battle_skill_rule sr on sr.id = se.skill_rule_id
			join game_stat st on st.id = se.stat_id
			where sr.skill_id = 668
			""".trimIndent(),
		)
		assertThat(strengthSapStatStageEffects).containsExactly(
			mapOf(
				"skill_id" to 668L,
				"stat_code" to "attack",
				"target_scope" to "TARGET",
				"stage_delta" to -1,
				"chance_percent" to 100,
			),
		)

		val purifySkillRules = queryMaps(
			"""
			select skill_id, effect_policy, target_policy, damage_policy
			from battle_skill_rule
			where skill_id = 685
			""".trimIndent(),
		)
		assertThat(purifySkillRules).containsExactly(
			mapOf(
				"skill_id" to 685L,
				"effect_policy" to "target-major-status-cure-self-heal-half-max-hp",
				"target_policy" to "selected-target",
				"damage_policy" to "no-damage",
			),
		)

		val conditionalPowerSkillRules = queryMaps(
			"""
			select skill_id, effect_policy, damage_policy
			from battle_skill_rule
			where skill_id in (263, 362, 474, 506, 512, 804, 875)
			order by skill_id
			""".trimIndent(),
		)
		assertThat(conditionalPowerSkillRules).containsExactly(
			mapOf(
				"skill_id" to 263L,
				"effect_policy" to "power-double-if-user-burn-poison-paralysis",
				"damage_policy" to "standard-damage",
			),
			mapOf(
				"skill_id" to 362L,
				"effect_policy" to "power-double-if-target-half-hp-or-less",
				"damage_policy" to "standard-damage",
			),
			mapOf(
				"skill_id" to 474L,
				"effect_policy" to "power-double-if-target-poisoned",
				"damage_policy" to "standard-damage",
			),
			mapOf(
				"skill_id" to 506L,
				"effect_policy" to "power-double-if-target-major-status",
				"damage_policy" to "standard-damage",
			),
			mapOf(
				"skill_id" to 512L,
				"effect_policy" to "power-double-if-user-has-no-held-item",
				"damage_policy" to "standard-damage",
			),
			mapOf(
				"skill_id" to 804L,
				"effect_policy" to "power-double-if-target-grounded-electric-terrain",
				"damage_policy" to "standard-damage",
			),
			mapOf(
				"skill_id" to 875L,
				"effect_policy" to "power-one-and-half-if-electric-terrain",
				"damage_policy" to "standard-damage",
			),
		)

		val terrainPrioritySkillRules = queryMaps(
			"""
			select skill_id, effect_policy, damage_policy
			from battle_skill_rule
			where skill_id = 803
			""".trimIndent(),
		)
		assertThat(terrainPrioritySkillRules).containsExactly(
			mapOf(
				"skill_id" to 803L,
				"effect_policy" to "priority-plus-one-if-user-grounded-grassy-terrain",
				"damage_policy" to "standard-damage",
			),
		)

		val postDamageStatusCureSkillRules = queryMaps(
			"""
			select skill_id, effect_policy, damage_policy
			from battle_skill_rule
			where skill_id in (265, 358, 664)
			order by skill_id
			""".trimIndent(),
		)
		assertThat(postDamageStatusCureSkillRules).containsExactly(
			mapOf(
				"skill_id" to 265L,
				"effect_policy" to "power-double-if-target-paralysis-cure-target-paralysis-after-damage",
				"damage_policy" to "standard-damage",
			),
			mapOf(
				"skill_id" to 358L,
				"effect_policy" to "power-double-if-target-sleep-cure-target-sleep-after-damage",
				"damage_policy" to "standard-damage",
			),
			mapOf(
				"skill_id" to 664L,
				"effect_policy" to "cure-target-burn-after-damage",
				"damage_policy" to "standard-damage",
			),
		)

		val dynamicPowerSkillRules = queryMaps(
			"""
			select skill_id, effect_policy, damage_policy
			from battle_skill_rule
			where skill_id in (386, 500, 681)
			order by skill_id
			""".trimIndent(),
		)
		assertThat(dynamicPowerSkillRules).containsExactly(
			mapOf(
				"skill_id" to 386L,
				"effect_policy" to "power-by-target-positive-stat-stage-sum-max-200",
				"damage_policy" to "standard-damage",
			),
			mapOf(
				"skill_id" to 500L,
				"effect_policy" to "power-by-user-positive-stat-stage-sum",
				"damage_policy" to "standard-damage",
			),
			mapOf(
				"skill_id" to 681L,
				"effect_policy" to "power-by-user-positive-stat-stage-sum",
				"damage_policy" to "standard-damage",
			),
		)

		val userElementRemovalSkillRules = queryMaps(
			"""
			select skill_id, effect_policy, damage_policy
			from battle_skill_rule
			where skill_id in (682, 892)
			order by skill_id
			""".trimIndent(),
		)
		assertThat(userElementRemovalSkillRules).containsExactly(
			mapOf(
				"skill_id" to 682L,
				"effect_policy" to "remove-user-element-after-damage",
				"damage_policy" to "standard-damage",
			),
			mapOf(
				"skill_id" to 892L,
				"effect_policy" to "remove-user-element-after-damage",
				"damage_policy" to "standard-damage",
			),
		)

		val speedRatioPowerSkillRules = queryMaps(
			"""
			select skill_id, effect_policy, damage_policy
			from battle_skill_rule
			where skill_id in (360, 486)
			order by skill_id
			""".trimIndent(),
		)
		assertThat(speedRatioPowerSkillRules).containsExactly(
			mapOf(
				"skill_id" to 360L,
				"effect_policy" to "power-by-target-user-speed-ratio-max-150",
				"damage_policy" to "standard-damage",
			),
			mapOf(
				"skill_id" to 486L,
				"effect_policy" to "power-by-user-target-speed-ratio",
				"damage_policy" to "standard-damage",
			),
		)

		val weightPowerSkillRules = queryMaps(
			"""
			select skill_id, effect_policy, damage_policy
			from battle_skill_rule
			where skill_id in (67, 447, 484, 535)
			order by skill_id
			""".trimIndent(),
		)
		assertThat(weightPowerSkillRules).containsExactly(
			mapOf(
				"skill_id" to 67L,
				"effect_policy" to "power-by-target-weight-threshold",
				"damage_policy" to "standard-damage",
			),
			mapOf(
				"skill_id" to 447L,
				"effect_policy" to "power-by-target-weight-threshold",
				"damage_policy" to "standard-damage",
			),
			mapOf(
				"skill_id" to 484L,
				"effect_policy" to "power-by-user-target-weight-ratio",
				"damage_policy" to "standard-damage",
			),
			mapOf(
				"skill_id" to 535L,
				"effect_policy" to "power-by-user-target-weight-ratio",
				"damage_policy" to "standard-damage",
			),
		)

		val weightReductionSkillRules = queryMaps(
			"""
			select skill_id, effect_policy, target_policy, damage_policy
			from battle_skill_rule
			where skill_id = 475
			""".trimIndent(),
		)
		assertThat(weightReductionSkillRules).containsExactly(
			mapOf(
				"skill_id" to 475L,
				"effect_policy" to "self-weight-reduction-100kg-after-speed-change",
				"target_policy" to "self",
				"damage_policy" to "no-damage",
			),
		)

		val taggedSkillRules = queryMaps(
			"""
			select skill_id, makes_contact, punch_based, slicing_based, critical_hit_stage
			from battle_skill_rule
			where skill_id in (5, 15, 163, 400, 427, 895)
			order by skill_id
			""".trimIndent(),
		)
		assertThat(taggedSkillRules).containsExactly(
			mapOf(
				"skill_id" to 5L,
				"makes_contact" to true,
				"punch_based" to true,
				"slicing_based" to false,
				"critical_hit_stage" to 0,
			),
			mapOf(
				"skill_id" to 15L,
				"makes_contact" to true,
				"punch_based" to false,
				"slicing_based" to true,
				"critical_hit_stage" to 0,
			),
			mapOf(
				"skill_id" to 163L,
				"makes_contact" to true,
				"punch_based" to false,
				"slicing_based" to true,
				"critical_hit_stage" to 1,
			),
			mapOf(
				"skill_id" to 400L,
				"makes_contact" to true,
				"punch_based" to false,
				"slicing_based" to true,
				"critical_hit_stage" to 1,
			),
			mapOf(
				"skill_id" to 427L,
				"makes_contact" to false,
				"punch_based" to false,
				"slicing_based" to true,
				"critical_hit_stage" to 1,
			),
			mapOf(
				"skill_id" to 895L,
				"makes_contact" to false,
				"punch_based" to false,
				"slicing_based" to true,
				"critical_hit_stage" to 1,
			),
		)

		val rechargeSkillRules = queryMaps(
			"""
			select skill_id, recharges_after_use
			from battle_skill_rule
			where skill_id = 63
			""".trimIndent(),
		)
		assertThat(rechargeSkillRules).containsExactly(
			mapOf(
				"skill_id" to 63L,
				"recharges_after_use" to true,
			),
		)

		val chargeSkillRules = queryMaps(
			"""
			select skill_id, charges_before_use
			from battle_skill_rule
			where skill_id = 76
			""".trimIndent(),
		)
		assertThat(chargeSkillRules).containsExactly(
			mapOf(
				"skill_id" to 76L,
				"charges_before_use" to true,
			),
		)

		val substituteSkillRules = queryMaps(
			"""
			select skill_id, effect_policy, target_policy
			from battle_skill_rule
			where skill_id = 164
			""".trimIndent(),
		)
		assertThat(substituteSkillRules).containsExactly(
			mapOf(
				"skill_id" to 164L,
				"effect_policy" to "create-substitute-quarter-max-hp",
				"target_policy" to "self",
			),
		)

		val chargeSkipWeathers = queryMaps(
			"""
			select sr.skill_id, wr.code as weather_code
			from battle_skill_charge_skip_weather cs
			join battle_skill_rule sr on sr.id = cs.skill_rule_id
			join battle_weather_rule wr on wr.id = cs.weather_rule_id
			where sr.skill_id = 76
			""".trimIndent(),
		)
		assertThat(chargeSkipWeathers).containsExactly(
			mapOf(
				"skill_id" to 76L,
				"weather_code" to "harsh-sunlight",
			),
		)

		val weatherElementOverrides = queryMaps(
			"""
			select wr.code as weather_code, ge.code as element_code
			from battle_skill_weather_element_override eo
			join battle_skill_rule sr on sr.id = eo.skill_rule_id
			join battle_weather_rule wr on wr.id = eo.weather_rule_id
			join game_element ge on ge.id = eo.target_element_id
			where sr.skill_id = 311
			order by eo.sort_order
			""".trimIndent(),
		)
		assertThat(weatherElementOverrides).containsExactly(
			mapOf("weather_code" to "harsh-sunlight", "element_code" to "fire"),
			mapOf("weather_code" to "rain", "element_code" to "water"),
			mapOf("weather_code" to "sandstorm", "element_code" to "rock"),
			mapOf("weather_code" to "snow", "element_code" to "ice"),
		)

		val terrainPowerModifiers = queryMaps(
			"""
			select tr.code as terrain_code, pm.power_multiplier
			from battle_skill_terrain_power_modifier pm
			join battle_skill_rule sr on sr.id = pm.skill_rule_id
			join battle_terrain_rule tr on tr.id = pm.terrain_rule_id
			where sr.skill_id = 805
			order by pm.sort_order
			""".trimIndent(),
		)
		assertThat(terrainPowerModifiers).containsExactly(
			mapOf("terrain_code" to "electric-terrain", "power_multiplier" to 2.0),
			mapOf("terrain_code" to "grassy-terrain", "power_multiplier" to 2.0),
			mapOf("terrain_code" to "misty-terrain", "power_multiplier" to 2.0),
			mapOf("terrain_code" to "psychic-terrain", "power_multiplier" to 2.0),
		)

		val terrainElementOverrides = queryMaps(
			"""
			select tr.code as terrain_code, ge.code as element_code
			from battle_skill_terrain_element_override eo
			join battle_skill_rule sr on sr.id = eo.skill_rule_id
			join battle_terrain_rule tr on tr.id = eo.terrain_rule_id
			join game_element ge on ge.id = eo.target_element_id
			where sr.skill_id = 805
			order by eo.sort_order
			""".trimIndent(),
		)
		assertThat(terrainElementOverrides).containsExactly(
			mapOf("terrain_code" to "electric-terrain", "element_code" to "electric"),
			mapOf("terrain_code" to "grassy-terrain", "element_code" to "grass"),
			mapOf("terrain_code" to "misty-terrain", "element_code" to "fairy"),
			mapOf("terrain_code" to "psychic-terrain", "element_code" to "psychic"),
		)

		val chargeSkipItemRules = queryMaps(
			"""
			select item_id, trigger_timing, effect_policy, consumable
			from battle_item_rule
			where item_id = 248
			""".trimIndent(),
		)
			assertThat(chargeSkipItemRules).containsExactly(
				mapOf(
					"item_id" to 248L,
					"trigger_timing" to "BEFORE_MOVE",
					"effect_policy" to "charge-skip-once",
					"consumable" to true,
				),
			)

			val majorStatusCureItemRules = queryMaps(
				"""
				select item_id, trigger_timing, effect_policy, consumable
				from battle_item_rule
				where item_id = 134
				""".trimIndent(),
			)
			assertThat(majorStatusCureItemRules).containsExactly(
				mapOf(
					"item_id" to 134L,
					"trigger_timing" to "AFTER_STATUS_APPLIED",
					"effect_policy" to "major-status-cure-all",
					"consumable" to true,
				),
			)

			val specificMajorStatusCureItemRules = queryMaps(
				"""
				select item_id, trigger_timing, effect_policy, consumable
				from battle_item_rule
				where item_id in (126, 127, 128, 129, 130)
				order by item_id
				""".trimIndent(),
			)
			assertThat(specificMajorStatusCureItemRules).containsExactly(
				mapOf(
					"item_id" to 126L,
					"trigger_timing" to "AFTER_STATUS_APPLIED",
					"effect_policy" to "major-status-cure-paralysis",
					"consumable" to true,
				),
				mapOf(
					"item_id" to 127L,
					"trigger_timing" to "AFTER_STATUS_APPLIED",
					"effect_policy" to "major-status-cure-sleep",
					"consumable" to true,
				),
				mapOf(
					"item_id" to 128L,
					"trigger_timing" to "AFTER_STATUS_APPLIED",
					"effect_policy" to "major-status-cure-poison",
					"consumable" to true,
				),
				mapOf(
					"item_id" to 129L,
					"trigger_timing" to "AFTER_STATUS_APPLIED",
					"effect_policy" to "major-status-cure-burn",
					"consumable" to true,
				),
				mapOf(
					"item_id" to 130L,
					"trigger_timing" to "AFTER_STATUS_APPLIED",
					"effect_policy" to "major-status-cure-freeze",
					"consumable" to true,
				),
			)

			val volatileStatusCureItemRules = queryMaps(
				"""
				select item_id, trigger_timing, effect_policy, consumable
				from battle_item_rule
				where item_id = 133
				""".trimIndent(),
			)
			assertThat(volatileStatusCureItemRules).containsExactly(
				mapOf(
					"item_id" to 133L,
					"trigger_timing" to "AFTER_VOLATILE_STATUS_APPLIED",
					"effect_policy" to "volatile-status-cure-confusion",
					"consumable" to true,
				),
			)

			val elementDamageBoostItemRules = queryMaps(
				"""
				select item_id, trigger_timing, effect_policy, consumable
				from battle_item_rule
				where effect_policy like 'element-damage-boost-%'
				order by item_id
				""".trimIndent(),
			)
			assertThat(elementDamageBoostItemRules).containsExactly(
				mapOf(
					"item_id" to 199L,
					"trigger_timing" to "BEFORE_DAMAGE",
					"effect_policy" to "element-damage-boost-bug",
					"consumable" to false,
				),
				mapOf(
					"item_id" to 210L,
					"trigger_timing" to "BEFORE_DAMAGE",
					"effect_policy" to "element-damage-boost-steel",
					"consumable" to false,
				),
				mapOf(
					"item_id" to 214L,
					"trigger_timing" to "BEFORE_DAMAGE",
					"effect_policy" to "element-damage-boost-ground",
					"consumable" to false,
				),
				mapOf(
					"item_id" to 215L,
					"trigger_timing" to "BEFORE_DAMAGE",
					"effect_policy" to "element-damage-boost-rock",
					"consumable" to false,
				),
				mapOf(
					"item_id" to 216L,
					"trigger_timing" to "BEFORE_DAMAGE",
					"effect_policy" to "element-damage-boost-grass",
					"consumable" to false,
				),
				mapOf(
					"item_id" to 217L,
					"trigger_timing" to "BEFORE_DAMAGE",
					"effect_policy" to "element-damage-boost-dark",
					"consumable" to false,
				),
				mapOf(
					"item_id" to 218L,
					"trigger_timing" to "BEFORE_DAMAGE",
					"effect_policy" to "element-damage-boost-fighting",
					"consumable" to false,
				),
				mapOf(
					"item_id" to 219L,
					"trigger_timing" to "BEFORE_DAMAGE",
					"effect_policy" to "element-damage-boost-electric",
					"consumable" to false,
				),
				mapOf(
					"item_id" to 220L,
					"trigger_timing" to "BEFORE_DAMAGE",
					"effect_policy" to "element-damage-boost-water",
					"consumable" to false,
				),
				mapOf(
					"item_id" to 221L,
					"trigger_timing" to "BEFORE_DAMAGE",
					"effect_policy" to "element-damage-boost-flying",
					"consumable" to false,
				),
				mapOf(
					"item_id" to 222L,
					"trigger_timing" to "BEFORE_DAMAGE",
					"effect_policy" to "element-damage-boost-poison",
					"consumable" to false,
				),
				mapOf(
					"item_id" to 223L,
					"trigger_timing" to "BEFORE_DAMAGE",
					"effect_policy" to "element-damage-boost-ice",
					"consumable" to false,
				),
				mapOf(
					"item_id" to 224L,
					"trigger_timing" to "BEFORE_DAMAGE",
					"effect_policy" to "element-damage-boost-ghost",
					"consumable" to false,
				),
				mapOf(
					"item_id" to 225L,
					"trigger_timing" to "BEFORE_DAMAGE",
					"effect_policy" to "element-damage-boost-psychic",
					"consumable" to false,
				),
				mapOf(
					"item_id" to 226L,
					"trigger_timing" to "BEFORE_DAMAGE",
					"effect_policy" to "element-damage-boost-fire",
					"consumable" to false,
				),
				mapOf(
					"item_id" to 227L,
					"trigger_timing" to "BEFORE_DAMAGE",
					"effect_policy" to "element-damage-boost-dragon",
					"consumable" to false,
				),
				mapOf(
					"item_id" to 228L,
					"trigger_timing" to "BEFORE_DAMAGE",
					"effect_policy" to "element-damage-boost-normal",
					"consumable" to false,
				),
				mapOf(
					"item_id" to 2105L,
					"trigger_timing" to "BEFORE_DAMAGE",
					"effect_policy" to "element-damage-boost-fairy",
					"consumable" to false,
				),
			)

			val elementDamageReductionItemRules = queryMaps(
				"""
				select item_id, trigger_timing, effect_policy, consumable
				from battle_item_rule
				where effect_policy like 'element-damage-reduction-%'
				order by item_id
				""".trimIndent(),
			)
			assertThat(elementDamageReductionItemRules).containsExactly(
				mapOf(
					"item_id" to 161L,
					"trigger_timing" to "BEFORE_DAMAGE",
					"effect_policy" to "element-damage-reduction-fire",
					"consumable" to true,
				),
				mapOf(
					"item_id" to 162L,
					"trigger_timing" to "BEFORE_DAMAGE",
					"effect_policy" to "element-damage-reduction-water",
					"consumable" to true,
				),
				mapOf(
					"item_id" to 163L,
					"trigger_timing" to "BEFORE_DAMAGE",
					"effect_policy" to "element-damage-reduction-electric",
					"consumable" to true,
				),
				mapOf(
					"item_id" to 164L,
					"trigger_timing" to "BEFORE_DAMAGE",
					"effect_policy" to "element-damage-reduction-grass",
					"consumable" to true,
				),
				mapOf(
					"item_id" to 165L,
					"trigger_timing" to "BEFORE_DAMAGE",
					"effect_policy" to "element-damage-reduction-ice",
					"consumable" to true,
				),
				mapOf(
					"item_id" to 166L,
					"trigger_timing" to "BEFORE_DAMAGE",
					"effect_policy" to "element-damage-reduction-fighting",
					"consumable" to true,
				),
				mapOf(
					"item_id" to 167L,
					"trigger_timing" to "BEFORE_DAMAGE",
					"effect_policy" to "element-damage-reduction-poison",
					"consumable" to true,
				),
				mapOf(
					"item_id" to 168L,
					"trigger_timing" to "BEFORE_DAMAGE",
					"effect_policy" to "element-damage-reduction-ground",
					"consumable" to true,
				),
				mapOf(
					"item_id" to 169L,
					"trigger_timing" to "BEFORE_DAMAGE",
					"effect_policy" to "element-damage-reduction-flying",
					"consumable" to true,
				),
				mapOf(
					"item_id" to 170L,
					"trigger_timing" to "BEFORE_DAMAGE",
					"effect_policy" to "element-damage-reduction-psychic",
					"consumable" to true,
				),
				mapOf(
					"item_id" to 171L,
					"trigger_timing" to "BEFORE_DAMAGE",
					"effect_policy" to "element-damage-reduction-bug",
					"consumable" to true,
				),
				mapOf(
					"item_id" to 172L,
					"trigger_timing" to "BEFORE_DAMAGE",
					"effect_policy" to "element-damage-reduction-rock",
					"consumable" to true,
				),
				mapOf(
					"item_id" to 173L,
					"trigger_timing" to "BEFORE_DAMAGE",
					"effect_policy" to "element-damage-reduction-ghost",
					"consumable" to true,
				),
				mapOf(
					"item_id" to 174L,
					"trigger_timing" to "BEFORE_DAMAGE",
					"effect_policy" to "element-damage-reduction-dragon",
					"consumable" to true,
				),
				mapOf(
					"item_id" to 175L,
					"trigger_timing" to "BEFORE_DAMAGE",
					"effect_policy" to "element-damage-reduction-dark",
					"consumable" to true,
				),
				mapOf(
					"item_id" to 176L,
					"trigger_timing" to "BEFORE_DAMAGE",
					"effect_policy" to "element-damage-reduction-steel",
					"consumable" to true,
				),
				mapOf(
					"item_id" to 177L,
					"trigger_timing" to "BEFORE_DAMAGE",
					"effect_policy" to "element-damage-reduction-normal",
					"consumable" to true,
				),
				mapOf(
					"item_id" to 723L,
					"trigger_timing" to "BEFORE_DAMAGE",
					"effect_policy" to "element-damage-reduction-fairy",
					"consumable" to true,
				),
			)

			val conditionalDamageBoostItemRules = queryMaps(
				"""
				select item_id, trigger_timing, effect_policy, consumable
				from battle_item_rule
				where item_id in (243, 244, 245)
				order by item_id
				""".trimIndent(),
			)
			assertThat(conditionalDamageBoostItemRules).containsExactly(
				mapOf(
					"item_id" to 243L,
					"trigger_timing" to "BEFORE_DAMAGE",
					"effect_policy" to "damage-class-power-boost-physical",
					"consumable" to false,
				),
				mapOf(
					"item_id" to 244L,
					"trigger_timing" to "BEFORE_DAMAGE",
					"effect_policy" to "damage-class-power-boost-special",
					"consumable" to false,
				),
				mapOf(
					"item_id" to 245L,
					"trigger_timing" to "BEFORE_DAMAGE",
					"effect_policy" to "super-effective-damage-boost",
					"consumable" to false,
				),
			)

			val damageDealtHealingItemRules = queryMaps(
				"""
				select item_id, trigger_timing, effect_policy, consumable
				from battle_item_rule
				where item_id = 230
				""".trimIndent(),
			)
			assertThat(damageDealtHealingItemRules).containsExactly(
				mapOf(
					"item_id" to 230L,
					"trigger_timing" to "AFTER_DAMAGE",
					"effect_policy" to "damage-dealt-heal-eighth",
					"consumable" to false,
				),
			)

			val fatalDamageSurvivalAbilityRules = queryMaps(
			"""
			select ability_id, trigger_timing, effect_policy
			from battle_ability_rule
			where ability_id = 5
			""".trimIndent(),
		)
		assertThat(fatalDamageSurvivalAbilityRules).containsExactly(
			mapOf(
				"ability_id" to 5L,
				"trigger_timing" to "BEFORE_FAINT",
				"effect_policy" to "full-hp-fatal-damage-survival",
			),
		)

		val priorityBlockingAbilityRules = queryMaps(
			"""
			select ability_id, trigger_timing, effect_policy
			from battle_ability_rule
			where ability_id in (214, 219, 296)
			order by ability_id
			""".trimIndent(),
		)
		assertThat(priorityBlockingAbilityRules).containsExactly(
			mapOf(
				"ability_id" to 214L,
				"trigger_timing" to "BEFORE_HIT",
				"effect_policy" to "side-priority-move-immunity",
			),
			mapOf(
				"ability_id" to 219L,
				"trigger_timing" to "BEFORE_HIT",
				"effect_policy" to "side-priority-move-immunity",
			),
			mapOf(
				"ability_id" to 296L,
				"trigger_timing" to "BEFORE_HIT",
				"effect_policy" to "side-priority-move-immunity",
			),
		)

		val statusPriorityAbilityRules = queryMaps(
			"""
			select ability_id, trigger_timing, effect_policy
			from battle_ability_rule
			where ability_id = 158
			""".trimIndent(),
		)
		assertThat(statusPriorityAbilityRules).containsExactly(
			mapOf(
				"ability_id" to 158L,
				"trigger_timing" to "BEFORE_MOVE",
				"effect_policy" to "status-skill-priority-boost",
			),
		)

		val elementAbsorbAbilityRules = queryMaps(
			"""
			select ability_id, trigger_timing, effect_policy
			from battle_ability_rule
			where ability_id in (10, 11, 297)
			order by ability_id
			""".trimIndent(),
		)
		assertThat(elementAbsorbAbilityRules).containsExactly(
			mapOf(
				"ability_id" to 10L,
				"trigger_timing" to "BEFORE_HIT",
				"effect_policy" to "element-electric-absorb-heal",
			),
			mapOf(
				"ability_id" to 11L,
				"trigger_timing" to "BEFORE_HIT",
				"effect_policy" to "element-water-absorb-heal",
			),
			mapOf(
				"ability_id" to 297L,
				"trigger_timing" to "BEFORE_HIT",
				"effect_policy" to "element-ground-absorb-heal",
			),
		)

		val elementAbsorbStatAbilityRules = queryMaps(
			"""
			select ability_id, trigger_timing, effect_policy
			from battle_ability_rule
			where ability_id in (78, 157, 273)
			order by ability_id
			""".trimIndent(),
		)
		assertThat(elementAbsorbStatAbilityRules).containsExactly(
			mapOf(
				"ability_id" to 78L,
				"trigger_timing" to "BEFORE_HIT",
				"effect_policy" to "element-electric-absorb-speed-up",
			),
			mapOf(
				"ability_id" to 157L,
				"trigger_timing" to "BEFORE_HIT",
				"effect_policy" to "element-grass-absorb-attack-up",
			),
			mapOf(
				"ability_id" to 273L,
				"trigger_timing" to "BEFORE_HIT",
				"effect_policy" to "element-fire-absorb-defense-up-two",
			),
		)

		val lowHpElementBoostAbilityRules = queryMaps(
			"""
			select ability_id, trigger_timing, effect_policy
			from battle_ability_rule
			where ability_id in (65, 66, 67, 68)
			order by ability_id
			""".trimIndent(),
		)
		assertThat(lowHpElementBoostAbilityRules).containsExactly(
			mapOf(
				"ability_id" to 65L,
				"trigger_timing" to "BEFORE_DAMAGE",
				"effect_policy" to "low-hp-grass-boost",
			),
			mapOf(
				"ability_id" to 66L,
				"trigger_timing" to "BEFORE_DAMAGE",
				"effect_policy" to "low-hp-fire-boost",
			),
			mapOf(
				"ability_id" to 67L,
				"trigger_timing" to "BEFORE_DAMAGE",
				"effect_policy" to "low-hp-water-boost",
			),
			mapOf(
				"ability_id" to 68L,
				"trigger_timing" to "BEFORE_DAMAGE",
				"effect_policy" to "low-hp-bug-boost",
			),
		)

		val elementDamageBoostAbilityRules = queryMaps(
			"""
			select ability_id, trigger_timing, effect_policy
			from battle_ability_rule
			where ability_id in (200, 262, 263, 276)
			order by ability_id
			""".trimIndent(),
		)
		assertThat(elementDamageBoostAbilityRules).containsExactly(
			mapOf(
				"ability_id" to 200L,
				"trigger_timing" to "BEFORE_DAMAGE",
				"effect_policy" to "element-steel-damage-boost",
			),
			mapOf(
				"ability_id" to 262L,
				"trigger_timing" to "BEFORE_DAMAGE",
				"effect_policy" to "element-electric-damage-boost",
			),
			mapOf(
				"ability_id" to 263L,
				"trigger_timing" to "BEFORE_DAMAGE",
				"effect_policy" to "element-dragon-damage-boost",
			),
			mapOf(
				"ability_id" to 276L,
				"trigger_timing" to "BEFORE_DAMAGE",
				"effect_policy" to "element-rock-damage-boost",
			),
		)

		val taggedSkillBoostAbilityRules = queryMaps(
			"""
			select ability_id, trigger_timing, effect_policy
			from battle_ability_rule
			where ability_id in (89, 292)
			order by ability_id
			""".trimIndent(),
		)
		assertThat(taggedSkillBoostAbilityRules).containsExactly(
			mapOf(
				"ability_id" to 89L,
				"trigger_timing" to "BEFORE_DAMAGE",
				"effect_policy" to "punch-based-skill-damage-boost",
			),
			mapOf(
				"ability_id" to 292L,
				"trigger_timing" to "BEFORE_DAMAGE",
				"effect_policy" to "slicing-based-skill-damage-boost",
			),
		)

		val contactSkillBoostAbilityRules = queryMaps(
			"""
			select ability_id, trigger_timing, effect_policy
			from battle_ability_rule
			where ability_id = 181
			""".trimIndent(),
		)
		assertThat(contactSkillBoostAbilityRules).containsExactly(
			mapOf(
				"ability_id" to 181L,
				"trigger_timing" to "BEFORE_DAMAGE",
				"effect_policy" to "contact-based-skill-damage-boost",
			),
		)

		val soundSkillAbilityRules = queryMaps(
			"""
			select ability_id, trigger_timing, effect_policy
			from battle_ability_rule
			where ability_id = 244
			order by effect_policy
			""".trimIndent(),
		)
		assertThat(soundSkillAbilityRules).containsExactly(
			mapOf(
				"ability_id" to 244L,
				"trigger_timing" to "BEFORE_DAMAGE",
				"effect_policy" to "sound-based-skill-damage-boost",
			),
			mapOf(
				"ability_id" to 244L,
				"trigger_timing" to "BEFORE_DAMAGE",
				"effect_policy" to "sound-based-skill-damage-reduction",
			),
		)

		val weatherElementBoostAbilityRules = queryMaps(
			"""
			select ability_id, trigger_timing, effect_policy
			from battle_ability_rule
			where ability_id = 159
			order by effect_policy
			""".trimIndent(),
		)
		assertThat(weatherElementBoostAbilityRules).containsExactly(
			mapOf(
				"ability_id" to 159L,
				"trigger_timing" to "END_TURN_WEATHER",
				"effect_policy" to "weather-damage-immunity-sandstorm",
			),
			mapOf(
				"ability_id" to 159L,
				"trigger_timing" to "BEFORE_DAMAGE",
				"effect_policy" to "weather-sandstorm-rock-ground-steel-damage-boost",
			),
		)

		val superEffectiveDamageReductionAbilityRules = queryMaps(
			"""
			select ability_id, trigger_timing, effect_policy
			from battle_ability_rule
			where ability_id in (111, 116, 232)
			order by ability_id
			""".trimIndent(),
		)
		assertThat(superEffectiveDamageReductionAbilityRules).containsExactly(
			mapOf(
				"ability_id" to 111L,
				"trigger_timing" to "BEFORE_DAMAGE",
				"effect_policy" to "super-effective-damage-reduction",
			),
			mapOf(
				"ability_id" to 116L,
				"trigger_timing" to "BEFORE_DAMAGE",
				"effect_policy" to "super-effective-damage-reduction",
			),
			mapOf(
				"ability_id" to 232L,
				"trigger_timing" to "BEFORE_DAMAGE",
				"effect_policy" to "super-effective-damage-reduction",
			),
		)

		val fullHpDamageReductionAbilityRules = queryMaps(
			"""
			select ability_id, trigger_timing, effect_policy
			from battle_ability_rule
			where ability_id in (136, 231)
			order by ability_id
			""".trimIndent(),
		)
		assertThat(fullHpDamageReductionAbilityRules).containsExactly(
			mapOf(
				"ability_id" to 136L,
				"trigger_timing" to "BEFORE_DAMAGE",
				"effect_policy" to "full-hp-damage-reduction",
			),
			mapOf(
				"ability_id" to 231L,
				"trigger_timing" to "BEFORE_DAMAGE",
				"effect_policy" to "full-hp-damage-reduction",
			),
		)

		val damageClassReductionAbilityRules = queryMaps(
			"""
			select ability_id, trigger_timing, effect_policy
			from battle_ability_rule
			where ability_id = 246
			""".trimIndent(),
		)
		assertThat(damageClassReductionAbilityRules).containsExactly(
			mapOf(
				"ability_id" to 246L,
				"trigger_timing" to "BEFORE_DAMAGE",
				"effect_policy" to "special-damage-reduction",
			),
		)

		val defendingStatAbilityRules = queryMaps(
			"""
			select ability_id, trigger_timing, effect_policy
			from battle_ability_rule
			where ability_id in (169, 179)
			order by ability_id
			""".trimIndent(),
		)
		assertThat(defendingStatAbilityRules).containsExactly(
			mapOf(
				"ability_id" to 169L,
				"trigger_timing" to "BEFORE_DAMAGE",
				"effect_policy" to "defense-stat-double",
			),
			mapOf(
				"ability_id" to 179L,
				"trigger_timing" to "BEFORE_DAMAGE",
				"effect_policy" to "grassy-terrain-defense-stat-boost",
			),
		)

		val attackingStatAbilityRules = queryMaps(
			"""
			select ability_id, trigger_timing, effect_policy
			from battle_ability_rule
			where ability_id in (37, 74)
			order by ability_id
			""".trimIndent(),
		)
		assertThat(attackingStatAbilityRules).containsExactly(
			mapOf(
				"ability_id" to 37L,
				"trigger_timing" to "BEFORE_DAMAGE",
				"effect_policy" to "attack-stat-double",
			),
			mapOf(
				"ability_id" to 74L,
				"trigger_timing" to "BEFORE_DAMAGE",
				"effect_policy" to "attack-stat-double",
			),
		)

		val statusAttackAbilityRules = queryMaps(
			"""
			select ability_id, trigger_timing, effect_policy
			from battle_ability_rule
			where ability_id = 62
			""".trimIndent(),
		)
		assertThat(statusAttackAbilityRules).containsExactly(
			mapOf(
				"ability_id" to 62L,
				"trigger_timing" to "BEFORE_DAMAGE",
				"effect_policy" to "major-status-attack-stat-boost-ignore-burn-drop",
			),
		)

		val sameElementBonusAbilityRules = queryMaps(
			"""
			select ability_id, trigger_timing, effect_policy
			from battle_ability_rule
			where ability_id = 91
			""".trimIndent(),
		)
		assertThat(sameElementBonusAbilityRules).containsExactly(
			mapOf(
				"ability_id" to 91L,
				"trigger_timing" to "BEFORE_DAMAGE",
				"effect_policy" to "same-element-bonus-double",
			),
		)

		val indirectDamageImmunityAbilityRules = queryMaps(
			"""
			select ability_id, trigger_timing, effect_policy
			from battle_ability_rule
			where ability_id = 98
			""".trimIndent(),
		)
		assertThat(indirectDamageImmunityAbilityRules).containsExactly(
			mapOf(
				"ability_id" to 98L,
				"trigger_timing" to "BEFORE_DAMAGE",
				"effect_policy" to "indirect-damage-immunity",
			),
		)

		val skillRecoilImmunityAbilityRules = queryMaps(
			"""
			select ability_id, trigger_timing, effect_policy
			from battle_ability_rule
			where ability_id = 69
			""".trimIndent(),
		)
		assertThat(skillRecoilImmunityAbilityRules).containsExactly(
			mapOf(
				"ability_id" to 69L,
				"trigger_timing" to "BEFORE_DAMAGE",
				"effect_policy" to "skill-recoil-damage-immunity",
			),
		)

		val criticalHitImmunityAbilityRules = queryMaps(
			"""
			select ability_id, trigger_timing, effect_policy
			from battle_ability_rule
			where ability_id in (4, 75)
			order by ability_id
			""".trimIndent(),
		)
		assertThat(criticalHitImmunityAbilityRules).containsExactly(
			mapOf(
				"ability_id" to 4L,
				"trigger_timing" to "BEFORE_DAMAGE",
				"effect_policy" to "critical-hit-immunity",
			),
			mapOf(
				"ability_id" to 75L,
				"trigger_timing" to "BEFORE_DAMAGE",
				"effect_policy" to "critical-hit-immunity",
			),
		)

		val statStageIgnoreAbilityRules = queryMaps(
			"""
			select ability_id, trigger_timing, effect_policy
			from battle_ability_rule
			where ability_id = 109
			order by trigger_timing, effect_policy
			""".trimIndent(),
		)
		assertThat(statStageIgnoreAbilityRules).containsExactly(
			mapOf(
				"ability_id" to 109L,
				"trigger_timing" to "BEFORE_DAMAGE",
				"effect_policy" to "ignore-opponent-damage-stat-stages",
			),
			mapOf(
				"ability_id" to 109L,
				"trigger_timing" to "BEFORE_HIT",
				"effect_policy" to "ignore-opponent-accuracy-stat-stages",
			),
		)

		val targetAbilityIgnoreRules = queryMaps(
			"""
			select ability_id, trigger_timing, effect_policy
			from battle_ability_rule
			where ability_id in (104, 163, 164)
			order by ability_id
			""".trimIndent(),
		)
		assertThat(targetAbilityIgnoreRules).containsExactly(
			mapOf(
				"ability_id" to 104L,
				"trigger_timing" to "BEFORE_HIT",
				"effect_policy" to "ignore-target-ability-effects",
			),
			mapOf(
				"ability_id" to 163L,
				"trigger_timing" to "BEFORE_HIT",
				"effect_policy" to "ignore-target-ability-effects",
			),
			mapOf(
				"ability_id" to 164L,
				"trigger_timing" to "BEFORE_HIT",
				"effect_policy" to "ignore-target-ability-effects",
			),
		)

		val soundImmunityAbilityRules = queryMaps(
			"""
			select ability_id, trigger_timing, effect_policy
			from battle_ability_rule
			where ability_id = 43
			""".trimIndent(),
		)
		assertThat(soundImmunityAbilityRules).containsExactly(
			mapOf(
				"ability_id" to 43L,
				"trigger_timing" to "BEFORE_HIT",
				"effect_policy" to "sound-based-skill-immunity",
			),
		)

		val weightMultiplierAbilityRules = queryMaps(
			"""
			select ability_id, trigger_timing, effect_policy
			from battle_ability_rule
			where ability_id in (134, 135)
			order by ability_id
			""".trimIndent(),
		)
		assertThat(weightMultiplierAbilityRules).containsExactly(
			mapOf(
				"ability_id" to 134L,
				"trigger_timing" to "BEFORE_DAMAGE",
				"effect_policy" to "weight-double",
			),
			mapOf(
				"ability_id" to 135L,
				"trigger_timing" to "BEFORE_DAMAGE",
				"effect_policy" to "weight-half",
			),
		)

		val fatalDamageSurvivalItemRules = queryMaps(
			"""
			select item_id, trigger_timing, effect_policy, consumable
			from battle_item_rule
			where item_id = 252
			""".trimIndent(),
		)
		assertThat(fatalDamageSurvivalItemRules).containsExactly(
			mapOf(
				"item_id" to 252L,
				"trigger_timing" to "BEFORE_FAINT",
				"effect_policy" to "consumable-full-hp-fatal-damage-survival",
				"consumable" to true,
			),
		)

		val weightMultiplierItemRules = queryMaps(
			"""
			select item_id, trigger_timing, effect_policy, consumable
			from battle_item_rule
			where item_id = 582
			""".trimIndent(),
		)
		assertThat(weightMultiplierItemRules).containsExactly(
			mapOf(
				"item_id" to 582L,
				"trigger_timing" to "BEFORE_DAMAGE",
				"effect_policy" to "weight-half",
				"consumable" to false,
			),
		)

		val environmentDurationItemRules = queryMaps(
			"""
			select item_id, trigger_timing, effect_policy, consumable
			from battle_item_rule
			where effect_policy like 'weather-duration-%'
				or effect_policy = 'terrain-duration-all'
			order by item_id
			""".trimIndent(),
		)
		assertThat(environmentDurationItemRules).containsExactly(
			mapOf(
				"item_id" to 259L,
				"trigger_timing" to "AFTER_HIT",
				"effect_policy" to "weather-duration-snow",
				"consumable" to false,
			),
			mapOf(
				"item_id" to 260L,
				"trigger_timing" to "AFTER_HIT",
				"effect_policy" to "weather-duration-sandstorm",
				"consumable" to false,
			),
			mapOf(
				"item_id" to 261L,
				"trigger_timing" to "AFTER_HIT",
				"effect_policy" to "weather-duration-sun",
				"consumable" to false,
			),
			mapOf(
				"item_id" to 262L,
				"trigger_timing" to "AFTER_HIT",
				"effect_policy" to "weather-duration-rain",
				"consumable" to false,
			),
			mapOf(
				"item_id" to 896L,
				"trigger_timing" to "AFTER_HIT",
				"effect_policy" to "terrain-duration-all",
				"consumable" to false,
			),
		)

		val sideConditionDurationItemRules = queryMaps(
			"""
			select item_id, trigger_timing, effect_policy, consumable
			from battle_item_rule
			where item_id = 246
			""".trimIndent(),
		)
		assertThat(sideConditionDurationItemRules).containsExactly(
			mapOf(
				"item_id" to 246L,
				"trigger_timing" to "AFTER_HIT",
				"effect_policy" to "side-condition-duration-screen",
				"consumable" to false,
			),
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
	fun `liquibase game location seed data does not keep generated placeholder names`() {
		// 这条测试只拦截确定无业务含义的机器翻译占位词；普通译名质量继续按资料源逐批校正。
		val placeholderLocations = queryMaps(
			"""
			select 'game_location' as table_name, id, code, name
			from game_location
			where name in ('地点', '小径地点', '洞窟地点', '区域地点', '洞窟区域')
				or name like '%遗留区域%'
				or lower(name) like '%pokemart%'
				or lower(name) like '%pokecenter%'
				or lower(name) like '%poke mart%'
			union all
			select 'game_location_area' as table_name, id, code, name
			from game_location_area
			where name in ('地点', '小径地点', '洞窟地点', '区域地点', '洞窟区域')
				or name like '%遗留区域%'
				or lower(name) like '%pokemart%'
				or lower(name) like '%pokecenter%'
				or lower(name) like '%poke mart%'
			order by table_name, id
			""".trimIndent(),
		)

		assertThat(placeholderLocations).isEmpty()
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
