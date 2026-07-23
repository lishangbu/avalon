package io.github.lishangbu.migration.db

import liquibase.Liquibase
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.ClassLoaderResourceAccessor
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Tag
import org.yaml.snakeyaml.Yaml
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import java.nio.file.Files
import java.nio.file.Path
import java.sql.ResultSet
import java.util.UUID
import javax.sql.DataSource

@Tag("integration")
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
			.contains("db/changelog/baseline")
	}

	@Test
	fun `liquibase changelog contains the initial baseline and match migration`() {
		val resource = javaClass.getResource("/db/changelog/baseline")

		assertThat(resource).isNotNull()
		val changelogFiles = Files.list(Path.of(resource!!.toURI())).use { paths ->
			paths
				.filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(".yaml") }
				.map { it.fileName.toString() }
				.sorted()
				.toList()
		}
		assertThat(changelogFiles).startsWith(
			"001-core.yaml",
			"020-security-permissions.yaml",
			"021-security-token-state.yaml",
			"030-match-schema.yaml",
			"031-match-display-names.yaml",
			"032-match-view-state.yaml",
			"040-admin-audit.yaml",
			"041-content-pack.yaml",
			"042-multiple-trainer-teams.yaml",
			"043-trainer-team-sharing.yaml",
			"044-team-preview.yaml",
			"045-match-total-deadline.yaml",
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
		)
	}

	@Test
	fun `liquibase seed text keeps simplified chinese terminology clean`() {
		val changelogRoot = Path.of(javaClass.getResource("/db/changelog")!!.toURI())
		val forbiddenTerms = listOf("精灵精灵", "生物", "宝可梦", "神奇宝贝")

		// 这里扫描的是给页面、权限树和资料表展示用的中文文本；code 列里的英文资料源命名仍允许保留，
		// 因为它们是稳定外部标识，不直接承担中文展示语义。
		val hits = Files.walk(changelogRoot).use { paths ->
			paths
				.filter { Files.isRegularFile(it) }
				.filter { path -> path.fileName.toString().let { it.endsWith(".csv") || it.endsWith(".yaml") } }
				.flatMap { path ->
					val text = Files.readString(path)
					forbiddenTerms
						.filter(text::contains)
						.map { term -> "${changelogRoot.relativize(path)} contains $term" }
						.stream()
				}
				.toList()
		}

		assertThat(hits).isEmpty()
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
			"battle_session",
			"battle_session_side",
			"battle_session_turn",
		)
	}

	@Test
	fun `liquibase creates rbac and sa token tables without oauth tables`() {
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
			"security_token_state",
		)
		assertThat(tableNames).doesNotContain(
			"security_permission",
			"security_role_permission",
			"oauth2_client",
			"oauth2_authorization",
			"oauth2_authorization_consent",
			"oauth2_jwk",
			"oauth_refresh_token_replay",
		)
		assertThat(
			queryStrings(
				"""
				select column_name
				from information_schema.columns
				where table_schema = 'public'
				  and table_name = 'security_access_node'
				order by ordinal_position
				""".trimIndent(),
			),
		).containsExactly("id", "code", "name", "enabled")

		val accessNodeCodes = queryStrings(
			"select code from security_access_node order by code",
		)
		assertThat(accessNodeCodes).noneMatch { it == "system.oauth" || it.startsWith("system.oauth.") }
		val systemAccessNodeCodes = listOf(
			"security:admin",
			"system.rbac.access-nodes",
			"system.rbac.roles",
			"system.rbac.users",
			"system.scheduler.tasks",
		)
		val gameDataAccessNodeCodes = listOf(
			"game-data:admin",
			"game-data.abilities",
			"game-data.creature-abilities",
			"game-data.creature-elements",
			"game-data.creature-stats",
			"game-data.creatures",
			"game-data.egg-groups",
			"game-data.elements",
			"game-data.habitats",
			"game-data.item-categories",
			"game-data.items",
			"game-data.skill-damage-classes",
			"game-data.skills",
			"game-data.species-colors",
			"game-data.species-egg-groups",
			"game-data.species-shapes",
			"game-data.stats",
		)
		val battleRulesAccessNodeCodes = listOf(
			"battle-rules:admin",
			"battle-rules.action-validation",
			"battle-rules.ability-rules",
			"battle-rules.battle-formats",
			"battle-rules.field-rules",
			"battle-rules.format-clause-bindings",
			"battle-rules.format-clauses",
			"battle-rules.format-restrictions",
			"battle-rules.format-special-mechanics",
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
		val battleSandboxAccessNodeCodes = listOf(
			"battle-sandbox",
			"battle-sandbox:run",
		)
		val battleSessionAccessNodeCodes = listOf(
			"battle-sessions",
			"battle-sessions:run",
		)
		assertThat(accessNodeCodes).containsAll(
			battleRulesAccessNodeCodes +
				battleSandboxAccessNodeCodes +
				battleSessionAccessNodeCodes +
				gameDataAccessNodeCodes +
				systemAccessNodeCodes,
		)

		val roleCodes = queryStrings(
			"select code from security_role order by code",
		)
		assertThat(roleCodes).containsExactly("battle-rules-admin", "battle-sandbox-runner", "game-data-admin", "system-admin")

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
		assertThat(systemAdminAccessNodeCodes)
			.containsExactlyInAnyOrderElementsOf(systemAccessNodeCodes + battleSessionAccessNodeCodes)

		val battleSessionAccessNodes = queryMaps(
			"""
			select code, name, enabled
			from security_access_node
			where code like 'battle-sessions%'
			order by code
			""".trimIndent(),
		)
		assertThat(battleSessionAccessNodes).containsExactly(
			mapOf(
				"code" to "battle-sessions",
				"name" to "战斗会话",
				"enabled" to true,
			),
			mapOf(
				"code" to "battle-sessions:run",
				"name" to "战斗会话执行接口",
				"enabled" to true,
			),
		)

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

		val battleSandboxRunnerAccessNodeCodes = queryStrings(
			"""
			select n.code
			from security_access_node n
			join security_role_access_node ran on ran.access_node_id = n.id
			join security_role r on r.id = ran.role_id
			where r.code = 'battle-sandbox-runner'
			order by n.code
			""".trimIndent(),
		)
		assertThat(battleSandboxRunnerAccessNodeCodes).containsExactlyInAnyOrderElementsOf(battleSandboxAccessNodeCodes)

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

		val tokenStateColumns = queryStrings(
			"""
			select column_name
			from information_schema.columns
			where table_schema = 'public'
				and table_name = 'security_token_state'
			order by ordinal_position
			""".trimIndent(),
		)
		assertThat(tokenStateColumns).containsExactly("id", "state_key", "state_value", "expires_at")

		val tokenStateIdType = queryStrings(
			"""
			select data_type
			from information_schema.columns
			where table_schema = 'public'
				and table_name = 'security_token_state'
				and column_name = 'id'
			""".trimIndent(),
		)
		assertThat(tokenStateIdType).containsExactly("bigint")
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
			"battle_sandbox_replay",
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
		assertThat(seedCounts).containsEntry("battle_ability_rule", 321L)
		assertThat(seedCounts).containsEntry("battle_item_rule", 223L)
		assertThat(seedCounts).containsEntry("battle_format", 4L)
		assertThat(seedCounts).containsEntry("battle_format_clause", 4L)
		assertThat(seedCounts).containsEntry("battle_format_clause_binding", 4L)
		assertThat(seedCounts).containsEntry("battle_format_restriction", 3L)
		assertThat(seedCounts).containsEntry("battle_special_mechanic", 3L)
		assertThat(seedCounts).containsEntry("battle_format_special_mechanic", 6L)
		assertThat(seedCounts).containsEntry("battle_status_rule", 13L)
		assertThat(seedCounts).containsEntry("battle_weather_rule", 5L)
		assertThat(seedCounts).containsEntry("battle_terrain_rule", 4L)
		assertThat(seedCounts).containsEntry("battle_field_rule", 11L)
		assertThat(seedCounts).containsEntry("battle_skill_rule", 691L)
		assertThat(seedCounts).containsEntry("battle_skill_status_effect", 113L)
		assertThat(seedCounts).containsEntry("battle_skill_stat_stage_effect", 200L)
		assertThat(seedCounts).containsEntry("battle_skill_stat_stage_operation", 39L)
		assertThat(seedCounts).containsEntry("battle_skill_field_effect", 10L)
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

		val latestUnusableSkillState = queryMaps(
			"""
			select
				count(*) as unusable_skill_count,
				count(*) filter (where s.enabled = true) as enabled_skill_count,
				count(*) filter (where r.enabled = true) as enabled_rule_count
			from game_skill s
			join game_skill_detail d on d.skill_id = s.id
			left join battle_skill_rule r on r.skill_id = s.id
			where coalesce(d.flavor_text, '') like '%无法使用这个技能%'
				or coalesce(d.effect, '') like '%无法使用这个技能%'
			""".trimIndent(),
		).single()
		assertThat(latestUnusableSkillState)
			.describedAs("最新版资料明确写明无法使用的技能必须保留目录资料但退出现代战斗运行态")
			.containsEntry("unusable_skill_count", 6L)
			.containsEntry("enabled_skill_count", 0L)
			.containsEntry("enabled_rule_count", 0L)

		val enabledUnmodeledStatusSkillRules = queryMaps(
			"""
			select s.id, s.code, s.name
			from battle_skill_rule r
			join game_skill s on s.id = r.skill_id
			join game_skill_damage_class dc on dc.id = s.damage_class_id
			where s.enabled = true
				and r.enabled = true
				and dc.code = 'status'
				and r.effect_policy = 'status-effect'
				and r.damage_policy = 'no-damage'
				and r.description like '基础变化技能规则%'
				and s.code not in ('splash', 'celebrate', 'happy-hour')
				and not exists (select 1 from battle_skill_status_effect e where e.skill_rule_id = r.id and e.enabled = true)
				and not exists (select 1 from battle_skill_stat_stage_effect e where e.skill_rule_id = r.id and e.enabled = true)
				and not exists (select 1 from battle_skill_stat_stage_operation e where e.skill_rule_id = r.id and e.enabled = true)
				and not exists (select 1 from battle_skill_field_effect e where e.skill_rule_id = r.id and e.enabled = true)
				and not exists (select 1 from battle_skill_global_field_effect e where e.skill_rule_id = r.id and e.enabled = true)
			order by s.id
			""".trimIndent(),
		)
		assertThat(enabledUnmodeledStatusSkillRules)
			.describedAs("启用中的变化类技能不能停留在无效果占位规则，否则真实战斗会静默空放")
			.isEmpty()

		val explicitNoBattleEffectSkillDescriptions = queryMaps(
			"""
			select s.code, r.description, s.enabled as skill_enabled, r.enabled as rule_enabled
			from battle_skill_rule r
			join game_skill s on s.id = r.skill_id
			where s.code in ('splash', 'celebrate', 'happy-hour')
			order by s.code
			""".trimIndent(),
		)
		assertThat(explicitNoBattleEffectSkillDescriptions).containsExactly(
			mapOf(
				"code" to "celebrate",
				"description" to "现代战斗中没有需要结算的对战效果；允许选择但只产生正常使用事件。",
				"skill_enabled" to true,
				"rule_enabled" to true,
			),
			mapOf(
				"code" to "happy-hour",
				"description" to "现代战斗中没有需要结算的对战效果；允许选择但只产生正常使用事件。",
				"skill_enabled" to true,
				"rule_enabled" to true,
			),
			mapOf(
				"code" to "splash",
				"description" to "现代战斗中没有需要结算的对战效果；允许选择但只产生正常使用事件。",
				"skill_enabled" to true,
				"rule_enabled" to true,
			),
		)

		val enabledDamagingNoDamageSkillRules = queryMaps(
			"""
			select s.id, s.code, s.name, dc.code as damage_class, r.effect_policy, r.damage_policy
			from battle_skill_rule r
			join game_skill s on s.id = r.skill_id
			join game_skill_damage_class dc on dc.id = s.damage_class_id
			where s.enabled = true
				and r.enabled = true
				and dc.code in ('physical', 'special')
				and r.damage_policy = 'no-damage'
			order by s.id
			""".trimIndent(),
		)
		assertThat(enabledDamagingNoDamageSkillRules)
			.describedAs("物理/特殊技能启用时必须有真实伤害、直接伤害或专项失败规则，不能静默装配成无伤害")
			.isEmpty()

		val enabledUnmodeledBasicDamageSkillRules = queryMaps(
			"""
			select s.id, s.code, s.name, d.short_effect
			from battle_skill_rule r
			join game_skill s on s.id = r.skill_id
			join game_skill_damage_class dc on dc.id = s.damage_class_id
			join game_skill_detail d on d.skill_id = s.id
			where s.enabled = true
				and r.enabled = true
				and dc.code in ('physical', 'special')
				and r.effect_policy = 'standard-damage'
				and r.damage_policy = 'standard-damage'
				and r.description like '基础伤害技能规则%'
				and r.target_policy = 'selected-target'
				and r.hit_policy = 'standard-hit'
				and r.min_hits = 1
				and r.max_hits = 1
				and r.critical_hit_stage = 0
				and r.charges_before_use = false
				and r.recharges_after_use = false
				and r.lock_move_turns_min = 1
				and r.lock_move_turns_max = 1
				and r.confuses_user_after_lock = false
				and r.force_target_switch = false
				and coalesce(d.short_effect, '') not like '%没有额外效果%'
				and coalesce(d.short_effect, '') not like '%造成常规伤害%'
				and not exists (select 1 from battle_skill_status_effect e where e.skill_rule_id = r.id and e.enabled = true)
				and not exists (select 1 from battle_skill_stat_stage_effect e where e.skill_rule_id = r.id and e.enabled = true)
				and not exists (select 1 from battle_skill_stat_stage_operation e where e.skill_rule_id = r.id and e.enabled = true)
				and not exists (select 1 from battle_skill_field_effect e where e.skill_rule_id = r.id and e.enabled = true)
				and not exists (select 1 from battle_skill_global_field_effect e where e.skill_rule_id = r.id and e.enabled = true)
			order by s.id
			""".trimIndent(),
		)
		assertThat(enabledUnmodeledBasicDamageSkillRules)
			.describedAs("启用中的白板普通伤害规则只能承载明确无额外效果的技能，不能吞掉资料文案中的特殊效果")
			.isEmpty()

		val enabledRulesWithUnmaintainedDescriptions = queryMaps(
			"""
			select s.id, s.code, s.name, r.description
			from battle_skill_rule r
			join game_skill s on s.id = r.skill_id
			where r.enabled = true
				and r.description like '%尚未维护%'
			order by s.id
			""".trimIndent(),
		)
		assertThat(enabledRulesWithUnmaintainedDescriptions)
			.describedAs("已经进入现代战斗运行态的规则说明必须描述可执行行为，不能继续显示导入期占位文案")
			.isEmpty()

		val derivedBasicSkillRules = queryMaps(
			"""
			select skill_id, target_policy, hit_policy, min_hits, max_hits, critical_hit_stage
			from battle_skill_rule
			where skill_id in (37, 57, 74, 129)
			order by skill_id
			""".trimIndent(),
		)
		assertThat(derivedBasicSkillRules).containsExactly(
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

		val derivedAccuracyLockSkillRules = queryMaps(
			"""
			select skill_id, effect_policy, target_policy, hit_policy, damage_policy
			from battle_skill_rule
			where skill_id = 199
			order by skill_id
			""".trimIndent(),
		)
		assertThat(derivedAccuracyLockSkillRules).containsExactly(
			mapOf(
				"skill_id" to 199L,
				"effect_policy" to "accuracy-lock-on-target",
				"target_policy" to "selected-target",
				"hit_policy" to "always-hit",
				"damage_policy" to "no-damage",
			),
		)

		val derivedHpSkillRules = queryMaps(
			"""
			select skill_id, effect_policy, target_policy, damage_policy
			from battle_skill_rule
			where skill_id in (36, 138, 344, 457)
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
				"skill_id" to 457L,
				"effect_policy" to "recoil-half-damage",
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
			where sr.skill_id in (40, 92, 305, 866)
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
			mapOf(
				"skill_id" to 866L,
				"status_code" to "poison",
				"chance_percent" to 100,
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

		val modernSelfStatSkillRules = queryMaps(
			"""
			select s.id as skill_id, s.enabled as skill_enabled, r.enabled as rule_enabled,
			       r.effect_policy, r.target_policy, r.hit_policy, r.damage_policy, r.affected_by_protect
			from game_skill s
			join battle_skill_rule r on r.skill_id = s.id
			where s.id in (837, 842, 850)
			order by s.id
			""".trimIndent(),
		)
		assertThat(modernSelfStatSkillRules).containsExactly(
			mapOf(
				"skill_id" to 837L,
				"skill_enabled" to true,
				"rule_enabled" to true,
				"effect_policy" to "stat-stage-change",
				"target_policy" to "self",
				"hit_policy" to "always-hit",
				"damage_policy" to "no-damage",
				"affected_by_protect" to false,
			),
			mapOf(
				"skill_id" to 842L,
				"skill_enabled" to true,
				"rule_enabled" to true,
				"effect_policy" to "stat-stage-change",
				"target_policy" to "self",
				"hit_policy" to "always-hit",
				"damage_policy" to "no-damage",
				"affected_by_protect" to false,
			),
			mapOf(
				"skill_id" to 850L,
				"skill_enabled" to true,
				"rule_enabled" to true,
				"effect_policy" to "self-major-status-cure",
				"target_policy" to "self",
				"hit_policy" to "always-hit",
				"damage_policy" to "no-damage",
				"affected_by_protect" to false,
			),
		)

		val modernSelfStatEffects = queryMaps(
			"""
			select sr.skill_id, st.code as stat_code, se.target_scope, se.stage_delta, se.chance_percent
			from battle_skill_stat_stage_effect se
			join battle_skill_rule sr on sr.id = se.skill_rule_id
			join game_stat st on st.id = se.stat_id
			where sr.skill_id in (837, 842, 850)
			order by sr.skill_id, se.sort_order
			""".trimIndent(),
		)
		assertThat(modernSelfStatEffects).containsExactly(
			mapOf(
				"skill_id" to 837L,
				"stat_code" to "attack",
				"target_scope" to "USER",
				"stage_delta" to 1,
				"chance_percent" to 100,
			),
			mapOf(
				"skill_id" to 837L,
				"stat_code" to "defense",
				"target_scope" to "USER",
				"stage_delta" to 1,
				"chance_percent" to 100,
			),
			mapOf(
				"skill_id" to 837L,
				"stat_code" to "speed",
				"target_scope" to "USER",
				"stage_delta" to 1,
				"chance_percent" to 100,
			),
			mapOf(
				"skill_id" to 842L,
				"stat_code" to "defense",
				"target_scope" to "USER",
				"stage_delta" to 2,
				"chance_percent" to 100,
			),
			mapOf(
				"skill_id" to 850L,
				"stat_code" to "special-attack",
				"target_scope" to "USER",
				"stage_delta" to 1,
				"chance_percent" to 100,
			),
			mapOf(
				"skill_id" to 850L,
				"stat_code" to "special-defense",
				"target_scope" to "USER",
				"stage_delta" to 1,
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

		val userSideActiveQuarterHealingSkillRules = queryMaps(
			"""
			select s.id as skill_id, s.enabled as skill_enabled, r.enabled as rule_enabled,
			       r.effect_policy, r.target_policy, r.hit_policy, r.damage_policy, r.affected_by_protect
			from game_skill s
			join battle_skill_rule r on r.skill_id = s.id
			where s.id in (791, 816, 849)
			order by s.id
			""".trimIndent(),
		)
		assertThat(userSideActiveQuarterHealingSkillRules).containsExactly(
			mapOf(
				"skill_id" to 791L,
				"skill_enabled" to true,
				"rule_enabled" to true,
				"effect_policy" to "target-heal-quarter-max-hp",
				"target_policy" to "user-side-active",
				"hit_policy" to "always-hit",
				"damage_policy" to "no-damage",
				"affected_by_protect" to false,
			),
			mapOf(
				"skill_id" to 816L,
				"skill_enabled" to true,
				"rule_enabled" to true,
				"effect_policy" to "target-heal-quarter-max-hp-user-side-active-major-status-cure",
				"target_policy" to "user-side-active",
				"hit_policy" to "always-hit",
				"damage_policy" to "no-damage",
				"affected_by_protect" to false,
			),
			mapOf(
				"skill_id" to 849L,
				"skill_enabled" to true,
				"rule_enabled" to true,
				"effect_policy" to "target-heal-quarter-max-hp-user-side-active-major-status-cure",
				"target_policy" to "user-side-active",
				"hit_policy" to "always-hit",
				"damage_policy" to "no-damage",
				"affected_by_protect" to false,
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
			where skill_id = 664
			order by skill_id
			""".trimIndent(),
		)
		assertThat(postDamageStatusCureSkillRules).containsExactly(
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
			where skill_id in (500, 681)
			order by skill_id
			""".trimIndent(),
		)
		assertThat(dynamicPowerSkillRules).containsExactly(
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
			where skill_id = 892
			order by skill_id
			""".trimIndent(),
		)
		assertThat(userElementRemovalSkillRules).containsExactly(
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
			select s.id as skill_id, s.enabled as skill_enabled, r.enabled as rule_enabled, r.recharges_after_use
			from battle_skill_rule r
			join game_skill s on s.id = r.skill_id
			where s.id in (63, 307, 308, 338, 416, 439, 459, 711)
			order by s.id
			""".trimIndent(),
		)
		assertThat(rechargeSkillRules).containsExactly(
			mapOf(
				"skill_id" to 63L,
				"skill_enabled" to true,
				"rule_enabled" to true,
				"recharges_after_use" to true,
			),
			mapOf("skill_id" to 307L, "skill_enabled" to true, "rule_enabled" to true, "recharges_after_use" to true),
			mapOf("skill_id" to 308L, "skill_enabled" to true, "rule_enabled" to true, "recharges_after_use" to true),
			mapOf("skill_id" to 338L, "skill_enabled" to true, "rule_enabled" to true, "recharges_after_use" to true),
			mapOf("skill_id" to 416L, "skill_enabled" to true, "rule_enabled" to true, "recharges_after_use" to true),
			mapOf("skill_id" to 439L, "skill_enabled" to true, "rule_enabled" to true, "recharges_after_use" to true),
			mapOf("skill_id" to 459L, "skill_enabled" to true, "rule_enabled" to true, "recharges_after_use" to true),
			mapOf("skill_id" to 711L, "skill_enabled" to true, "rule_enabled" to true, "recharges_after_use" to true),
		)

		val specialDamageTargetDefenseRules = queryMaps(
			"""
			select s.id as skill_id, s.enabled as skill_enabled, r.enabled as rule_enabled, r.effect_policy, r.damage_policy
			from battle_skill_rule r
			join game_skill s on s.id = r.skill_id
			where s.id in (473, 540, 548)
			order by s.id
			""".trimIndent(),
		)
		assertThat(specialDamageTargetDefenseRules).containsExactly(
			mapOf(
				"skill_id" to 473L,
				"skill_enabled" to true,
				"rule_enabled" to true,
				"effect_policy" to "special-damage-target-defense",
				"damage_policy" to "standard-damage",
			),
			mapOf(
				"skill_id" to 540L,
				"skill_enabled" to true,
				"rule_enabled" to true,
				"effect_policy" to "special-damage-target-defense",
				"damage_policy" to "standard-damage",
			),
			mapOf(
				"skill_id" to 548L,
				"skill_enabled" to true,
				"rule_enabled" to true,
				"effect_policy" to "special-damage-target-defense",
				"damage_policy" to "standard-damage",
			),
		)

		val nonFaintingDamageSkillRules = queryMaps(
			"""
			select s.id as skill_id, s.enabled as skill_enabled, r.enabled as rule_enabled, r.effect_policy, r.damage_policy
			from battle_skill_rule r
			join game_skill s on s.id = r.skill_id
			where s.id = 206
			order by s.id
			""".trimIndent(),
		)
		assertThat(nonFaintingDamageSkillRules).containsExactly(
			mapOf(
				"skill_id" to 206L,
				"skill_enabled" to true,
				"rule_enabled" to true,
				"effect_policy" to "leave-target-at-one-hp",
				"damage_policy" to "standard-damage",
			),
		)

		val screenBreakingSkillRules = queryMaps(
			"""
			select s.id as skill_id, s.enabled as skill_enabled, r.enabled as rule_enabled, r.effect_policy, r.damage_policy
			from battle_skill_rule r
			join game_skill s on s.id = r.skill_id
			where s.id in (280, 706)
			order by s.id
			""".trimIndent(),
		)
		assertThat(screenBreakingSkillRules).containsExactly(
			mapOf(
				"skill_id" to 280L,
				"skill_enabled" to true,
				"rule_enabled" to true,
				"effect_policy" to "break-target-side-damage-reductions",
				"damage_policy" to "standard-damage",
			),
			mapOf(
				"skill_id" to 706L,
				"skill_enabled" to true,
				"rule_enabled" to true,
				"effect_policy" to "break-target-side-damage-reductions",
				"damage_policy" to "standard-damage",
			),
		)

		val userHpDynamicPowerSkillRules = queryMaps(
			"""
			select s.id as skill_id, s.enabled as skill_enabled, r.enabled as rule_enabled, r.effect_policy, r.damage_policy
			from battle_skill_rule r
			join game_skill s on s.id = r.skill_id
			where s.id in (175, 179)
			order by s.id
			""".trimIndent(),
		)
		assertThat(userHpDynamicPowerSkillRules).containsExactly(
			mapOf(
				"skill_id" to 175L,
				"skill_enabled" to true,
				"rule_enabled" to true,
				"effect_policy" to "power-by-user-current-hp-ratio",
				"damage_policy" to "standard-damage",
			),
			mapOf(
				"skill_id" to 179L,
				"skill_enabled" to true,
				"rule_enabled" to true,
				"effect_policy" to "power-by-user-current-hp-ratio",
				"damage_policy" to "standard-damage",
			),
		)

		val sideProtectionFieldRules = queryMaps(
			"""
			select id, code, name, effect_scope, effect_policy, min_turns, max_turns, enabled
			from battle_field_rule
			where id in (10, 11)
			order by id
			""".trimIndent(),
		)
		assertThat(sideProtectionFieldRules).containsExactly(
			mapOf(
				"id" to 10L,
				"code" to "mist",
				"name" to "白雾",
				"effect_scope" to "SIDE",
				"effect_policy" to "side-stat-stage-reduction-protection",
				"min_turns" to 5,
				"max_turns" to 5,
				"enabled" to true,
			),
			mapOf(
				"id" to 11L,
				"code" to "safeguard",
				"name" to "神秘守护",
				"effect_scope" to "SIDE",
				"effect_policy" to "side-status-condition-protection",
				"min_turns" to 5,
				"max_turns" to 5,
				"enabled" to true,
			),
		)

		val focusMistSafeguardSkillRules = queryMaps(
			"""
			select s.id as skill_id, s.enabled as skill_enabled, r.enabled as rule_enabled, r.effect_policy, r.target_policy, r.damage_policy
			from battle_skill_rule r
			join game_skill s on s.id = r.skill_id
			where s.id in (54, 116, 219)
			order by s.id
			""".trimIndent(),
		)
		assertThat(focusMistSafeguardSkillRules).containsExactly(
			mapOf(
				"skill_id" to 54L,
				"skill_enabled" to true,
				"rule_enabled" to true,
				"effect_policy" to "side-condition",
				"target_policy" to "self",
				"damage_policy" to "no-damage",
			),
			mapOf(
				"skill_id" to 116L,
				"skill_enabled" to true,
				"rule_enabled" to true,
				"effect_policy" to "self-critical-hit-stage-plus-two",
				"target_policy" to "self",
				"damage_policy" to "no-damage",
			),
			mapOf(
				"skill_id" to 219L,
				"skill_enabled" to true,
				"rule_enabled" to true,
				"effect_policy" to "side-condition",
				"target_policy" to "self",
				"damage_policy" to "no-damage",
			),
		)

		val sideProtectionSkillFieldEffects = queryMaps(
			"""
			select e.id, s.id as skill_id, fr.id as field_rule_id, e.target_side, e.chance_percent, e.effect_timing, e.enabled
			from battle_skill_field_effect e
			join battle_skill_rule r on r.id = e.skill_rule_id
			join game_skill s on s.id = r.skill_id
			join battle_field_rule fr on fr.id = e.field_rule_id
			where e.id in (9, 10)
			order by e.id
			""".trimIndent(),
		)
		assertThat(sideProtectionSkillFieldEffects).containsExactly(
			mapOf(
				"id" to 9L,
				"skill_id" to 54L,
				"field_rule_id" to 10L,
				"target_side" to "USER_SIDE",
				"chance_percent" to 100,
				"effect_timing" to "AFTER_HIT",
				"enabled" to true,
			),
			mapOf(
				"id" to 10L,
				"skill_id" to 219L,
				"field_rule_id" to 11L,
				"target_side" to "USER_SIDE",
				"chance_percent" to 100,
				"effect_timing" to "AFTER_HIT",
				"enabled" to true,
			),
		)

		val restHealBellSkillRules = queryMaps(
			"""
			select s.id as skill_id, s.code, s.enabled as skill_enabled, r.enabled as rule_enabled, r.effect_policy, r.target_policy, r.damage_policy, r.affected_by_protect, r.sound_based
			from battle_skill_rule r
			join game_skill s on s.id = r.skill_id
			where s.code in ('rest', 'heal-bell')
			order by s.id
			""".trimIndent(),
		)
		assertThat(restHealBellSkillRules).containsExactly(
			mapOf(
				"skill_id" to 156L,
				"code" to "rest",
				"skill_enabled" to true,
				"rule_enabled" to true,
				"effect_policy" to "self-rest-full-heal",
				"target_policy" to "self",
				"damage_policy" to "no-damage",
				"affected_by_protect" to false,
				"sound_based" to false,
			),
			mapOf(
				"skill_id" to 215L,
				"code" to "heal-bell",
				"skill_enabled" to true,
				"rule_enabled" to true,
				"effect_policy" to "user-side-major-status-cure",
				"target_policy" to "self",
				"damage_policy" to "no-damage",
				"affected_by_protect" to false,
				"sound_based" to true,
			),
		)

		val leechSeedSkillRules = queryMaps(
			"""
			select s.id as skill_id, s.code, s.enabled as skill_enabled, r.enabled as rule_enabled,
			       r.effect_policy, r.target_policy, r.hit_policy, r.damage_policy, r.affected_by_protect
			from battle_skill_rule r
			join game_skill s on s.id = r.skill_id
			where s.code = 'leech-seed'
			""".trimIndent(),
		)
		assertThat(leechSeedSkillRules).containsExactly(
			mapOf(
				"skill_id" to 73L,
				"code" to "leech-seed",
				"skill_enabled" to true,
				"rule_enabled" to true,
				"effect_policy" to "apply-leech-seed",
				"target_policy" to "selected-target",
				"hit_policy" to "standard-hit",
				"damage_policy" to "no-damage",
				"affected_by_protect" to true,
			),
		)

		val spinCleanupSkillRules = queryMaps(
			"""
			select s.id as skill_id, s.code, s.enabled as skill_enabled, r.enabled as rule_enabled,
			       r.effect_policy, r.target_policy, r.hit_policy, r.damage_policy
			from battle_skill_rule r
			join game_skill s on s.id = r.skill_id
			where s.code in ('rapid-spin', 'mortal-spin')
			order by s.id
			""".trimIndent(),
		)
		assertThat(spinCleanupSkillRules).containsExactly(
			mapOf(
				"skill_id" to 229L,
				"code" to "rapid-spin",
				"skill_enabled" to true,
				"rule_enabled" to true,
				"effect_policy" to "clear-user-side-hazards-and-traps",
				"target_policy" to "selected-target",
				"hit_policy" to "standard-hit",
				"damage_policy" to "standard-damage",
			),
			mapOf(
				"skill_id" to 866L,
				"code" to "mortal-spin",
				"skill_enabled" to true,
				"rule_enabled" to true,
				"effect_policy" to "clear-user-side-hazards-and-traps",
				"target_policy" to "all-opponents",
				"hit_policy" to "standard-hit",
				"damage_policy" to "standard-damage",
			),
		)

		val fieldCleanupSkillRules = queryMaps(
			"""
			select s.id as skill_id, s.code, s.enabled as skill_enabled, r.enabled as rule_enabled,
			       r.effect_policy, r.target_policy, r.hit_policy, r.damage_policy, r.affected_by_protect
			from battle_skill_rule r
			join game_skill s on s.id = r.skill_id
			where s.code = 'tidy-up'
			""".trimIndent(),
		)
		assertThat(fieldCleanupSkillRules).containsExactly(
			mapOf(
				"skill_id" to 882L,
				"code" to "tidy-up",
				"skill_enabled" to true,
				"rule_enabled" to true,
				"effect_policy" to "clear-field-hazards-and-substitutes",
				"target_policy" to "self",
				"hit_policy" to "always-hit",
				"damage_policy" to "no-damage",
				"affected_by_protect" to false,
			),
		)

		val targetSideCleanupSkillRules = queryMaps(
			"""
			select s.id as skill_id, s.code, s.enabled as skill_enabled, r.enabled as rule_enabled,
			       r.effect_policy, r.target_policy, r.hit_policy, r.damage_policy, r.affected_by_protect
			from battle_skill_rule r
			join game_skill s on s.id = r.skill_id
			where s.code = 'defog'
			""".trimIndent(),
		)
		assertThat(targetSideCleanupSkillRules).containsExactly(
			mapOf(
				"skill_id" to 432L,
				"code" to "defog",
				"skill_enabled" to true,
				"rule_enabled" to true,
				"effect_policy" to "clear-target-side-barriers-and-field-hazards",
				"target_policy" to "selected-target",
				"hit_policy" to "always-hit",
				"damage_policy" to "no-damage",
				"affected_by_protect" to true,
			),
		)

		val firstActionOnlySkillRules = queryMaps(
			"""
			select s.id as skill_id, s.code, s.enabled as skill_enabled, r.enabled as rule_enabled,
			       r.effect_policy, r.target_policy, r.hit_policy, r.damage_policy, r.makes_contact, r.affected_by_protect
			from battle_skill_rule r
			join game_skill s on s.id = r.skill_id
			where s.code in ('fake-out', 'first-impression')
			order by s.id
			""".trimIndent(),
		)
		assertThat(firstActionOnlySkillRules).containsExactly(
			mapOf(
				"skill_id" to 252L,
				"code" to "fake-out",
				"skill_enabled" to true,
				"rule_enabled" to true,
				"effect_policy" to "first-skill-action-only-damage",
				"target_policy" to "selected-target",
				"hit_policy" to "standard-hit",
				"damage_policy" to "standard-damage",
				"makes_contact" to true,
				"affected_by_protect" to true,
			),
			mapOf(
				"skill_id" to 660L,
				"code" to "first-impression",
				"skill_enabled" to true,
				"rule_enabled" to true,
				"effect_policy" to "first-skill-action-only-damage",
				"target_policy" to "selected-target",
				"hit_policy" to "standard-hit",
				"damage_policy" to "standard-damage",
				"makes_contact" to true,
				"affected_by_protect" to true,
			),
		)

		val pendingTargetActionSkillRules = queryMaps(
			"""
			select s.id as skill_id, s.code, s.enabled as skill_enabled, r.enabled as rule_enabled,
			       r.effect_policy, r.target_policy, r.hit_policy, r.damage_policy, r.makes_contact, r.affected_by_protect
			from battle_skill_rule r
			join game_skill s on s.id = r.skill_id
			where s.code in ('sucker-punch', 'upper-hand')
			order by s.id
			""".trimIndent(),
		)
		assertThat(pendingTargetActionSkillRules).containsExactly(
			mapOf(
				"skill_id" to 389L,
				"code" to "sucker-punch",
				"skill_enabled" to true,
				"rule_enabled" to true,
				"effect_policy" to "target-pending-damaging-skill-damage",
				"target_policy" to "selected-target",
				"hit_policy" to "standard-hit",
				"damage_policy" to "standard-damage",
				"makes_contact" to true,
				"affected_by_protect" to true,
			),
			mapOf(
				"skill_id" to 918L,
				"code" to "upper-hand",
				"skill_enabled" to true,
				"rule_enabled" to true,
				"effect_policy" to "target-pending-priority-damaging-skill-damage",
				"target_policy" to "selected-target",
				"hit_policy" to "standard-hit",
				"damage_policy" to "standard-damage",
				"makes_contact" to true,
				"affected_by_protect" to true,
			),
		)

		val upperHandFlinchEffects = queryMaps(
			"""
			select sr.skill_id, br.code as status_code, se.chance_percent
			from battle_skill_status_effect se
			join battle_skill_rule sr on sr.id = se.skill_rule_id
			join battle_status_rule br on br.id = se.status_rule_id
			where sr.skill_id = 918
			order by se.sort_order
			""".trimIndent(),
		)
		assertThat(upperHandFlinchEffects).containsExactly(
			mapOf(
				"skill_id" to 918L,
				"status_code" to "flinch",
				"chance_percent" to 100,
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
				  and effect_policy <> 'berry-marker'
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
				  and effect_policy <> 'berry-marker'
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
				  and effect_policy <> 'berry-marker'
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
			assertThat(elementDamageBoostItemRules).contains(
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

			val punchBasedItemRules = queryMaps(
				"""
				select item_id, trigger_timing, effect_policy, consumable
				from battle_item_rule
				where item_id = 1700
				order by trigger_timing, effect_policy
				""".trimIndent(),
			)
			assertThat(punchBasedItemRules).containsExactly(
				mapOf(
					"item_id" to 1700L,
					"trigger_timing" to "BEFORE_DAMAGE",
					"effect_policy" to "punch-based-skill-power-boost",
					"consumable" to false,
				),
				mapOf(
					"item_id" to 1700L,
					"trigger_timing" to "BEFORE_HIT",
					"effect_policy" to "punch-based-contact-suppression",
					"consumable" to false,
				),
			)

			val contactDamageItemRules = queryMaps(
				"""
				select item_id, trigger_timing, effect_policy, consumable
				from battle_item_rule
				where item_id = 583
				""".trimIndent(),
			)
			assertThat(contactDamageItemRules).containsExactly(
				mapOf(
					"item_id" to 583L,
					"trigger_timing" to "AFTER_DAMAGE",
					"effect_policy" to "contact-damage-to-attacker-sixth",
					"consumable" to false,
				),
			)

			val contactTransferItemRules = queryMaps(
				"""
				select item_id, trigger_timing, effect_policy, consumable
				from battle_item_rule
				where item_id = 265
				order by trigger_order, sort_order
				""".trimIndent(),
			)
			assertThat(contactTransferItemRules).containsExactly(
				mapOf(
					"item_id" to 265L,
					"trigger_timing" to "AFTER_DAMAGE",
					"effect_policy" to "contact-transfer-to-attacker",
					"consumable" to false,
				),
				mapOf(
					"item_id" to 265L,
					"trigger_timing" to "HELD_END_TURN",
					"effect_policy" to "held-end-turn-damage-eighth",
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

		val contactDamageAbilityRules = queryMaps(
			"""
			select ability_id, trigger_timing, effect_policy
			from battle_ability_rule
			where ability_id in (24, 160)
			order by ability_id
			""".trimIndent(),
		)
		assertThat(contactDamageAbilityRules).containsExactly(
			mapOf(
				"ability_id" to 24L,
				"trigger_timing" to "AFTER_DAMAGE",
				"effect_policy" to "contact-damage-to-attacker-eighth",
			),
			mapOf(
				"ability_id" to 160L,
				"trigger_timing" to "AFTER_DAMAGE",
				"effect_policy" to "contact-damage-to-attacker-eighth",
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

		val targetLastSkillPpReductionRules = queryMaps(
			"""
			select s.code, r.effect_policy, r.target_policy, r.hit_policy, r.damage_policy,
			       r.affected_by_protect, r.enabled, s.enabled as skill_enabled
			from battle_skill_rule r
			join game_skill s on s.id = r.skill_id
			where s.code = 'spite'
			""".trimIndent(),
		)
		assertThat(targetLastSkillPpReductionRules).containsExactly(
			mapOf(
				"code" to "spite",
				"effect_policy" to "target-last-skill-pp-reduction-four",
				"target_policy" to "selected-target",
				"hit_policy" to "standard-hit",
				"damage_policy" to "no-damage",
				"affected_by_protect" to true,
				"enabled" to true,
				"skill_enabled" to true,
			),
		)

		val userSideGuardRules = queryMaps(
			"""
			select s.code, r.effect_policy, r.target_policy, r.hit_policy, r.damage_policy,
			       r.min_hits, r.max_hits, r.makes_contact, r.affected_by_protect,
			       r.protects_user, r.endures_fatal_damage, r.enabled, s.enabled as skill_enabled
			from battle_skill_rule r
			join game_skill s on s.id = r.skill_id
			where s.code in ('quick-guard', 'wide-guard')
			order by s.code
			""".trimIndent(),
		)
		assertThat(userSideGuardRules).containsExactly(
			mapOf(
				"code" to "quick-guard",
				"effect_policy" to "user-side-priority-skill-protection",
				"target_policy" to "self",
				"hit_policy" to "always-hit",
				"damage_policy" to "no-damage",
				"min_hits" to 1,
				"max_hits" to 1,
				"makes_contact" to false,
				"affected_by_protect" to false,
				"protects_user" to false,
				"endures_fatal_damage" to false,
				"enabled" to true,
				"skill_enabled" to true,
			),
			mapOf(
				"code" to "wide-guard",
				"effect_policy" to "user-side-multi-target-skill-protection",
				"target_policy" to "self",
				"hit_policy" to "always-hit",
				"damage_policy" to "no-damage",
				"min_hits" to 1,
				"max_hits" to 1,
				"makes_contact" to false,
				"affected_by_protect" to false,
				"protects_user" to false,
				"endures_fatal_damage" to false,
				"enabled" to true,
				"skill_enabled" to true,
			),
		)

		val protectionBreakingRules = queryMaps(
			"""
			select s.code, r.effect_policy, r.target_policy, r.hit_policy, r.damage_policy,
			       r.min_hits, r.max_hits, r.makes_contact, r.affected_by_protect,
			       r.protects_user, r.endures_fatal_damage, r.enabled, s.enabled as skill_enabled
			from battle_skill_rule r
			join game_skill s on s.id = r.skill_id
			where s.code = 'feint'
			""".trimIndent(),
		)
		assertThat(protectionBreakingRules).containsExactly(
			mapOf(
				"code" to "feint",
				"effect_policy" to "break-target-protection-damage",
				"target_policy" to "selected-target",
				"hit_policy" to "standard-hit",
				"damage_policy" to "standard-damage",
				"min_hits" to 1,
				"max_hits" to 1,
				"makes_contact" to false,
				"affected_by_protect" to false,
				"protects_user" to false,
				"endures_fatal_damage" to false,
				"enabled" to true,
				"skill_enabled" to true,
			),
		)

		val directStatusHpSkillRules = queryMaps(
			"""
			select s.code, r.effect_policy, r.target_policy, r.hit_policy, r.damage_policy,
			       r.affected_by_protect, r.sound_based, r.enabled, s.enabled as skill_enabled
			from battle_skill_rule r
			join game_skill s on s.id = r.skill_id
			where s.code in ('belly-drum', 'pain-split', 'howl')
			order by s.code
			""".trimIndent(),
		)
		assertThat(directStatusHpSkillRules).containsExactly(
			mapOf(
				"code" to "belly-drum",
				"effect_policy" to "maximize-user-attack-half-max-hp-cost",
				"target_policy" to "self",
				"hit_policy" to "always-hit",
				"damage_policy" to "no-damage",
				"affected_by_protect" to false,
				"sound_based" to false,
				"enabled" to true,
				"skill_enabled" to true,
			),
			mapOf(
				"code" to "howl",
				"effect_policy" to "status-effect",
				"target_policy" to "user-side-active",
				"hit_policy" to "always-hit",
				"damage_policy" to "no-damage",
				"affected_by_protect" to false,
				"sound_based" to true,
				"enabled" to true,
				"skill_enabled" to true,
			),
			mapOf(
				"code" to "pain-split",
				"effect_policy" to "average-user-target-current-hp",
				"target_policy" to "selected-target",
				"hit_policy" to "always-hit",
				"damage_policy" to "no-damage",
				"affected_by_protect" to true,
				"sound_based" to false,
				"enabled" to true,
				"skill_enabled" to true,
			),
		)

		val howlStatStageEffect = queryMaps(
			"""
			select st.code as stat_code, se.target_scope, se.stage_delta, se.chance_percent
			from battle_skill_stat_stage_effect se
			join battle_skill_rule r on r.id = se.skill_rule_id
			join game_skill s on s.id = r.skill_id
			join game_stat st on st.id = se.stat_id
			where s.code = 'howl'
			""".trimIndent(),
		)
		assertThat(howlStatStageEffect).containsExactly(
			mapOf(
				"stat_code" to "attack",
				"target_scope" to "TARGET",
				"stage_delta" to 1,
				"chance_percent" to 100,
			),
		)
	}

	@Test
	fun `liquibase creates one current game data snapshot`() {
		val tableNames = queryStrings(
			"""
			select table_name
			from information_schema.tables
			where table_schema = 'public'
			""".trimIndent(),
		)

		assertThat(tableNames).doesNotContain(
			"game_advanced_contest_effect",
			"game_advanced_contest_effect_skill",
			"game_berry",
			"game_berry_firmness",
			"game_berry_flavor",
			"game_berry_flavor_potency",
			"game_catalog",
			"game_catalog_entry",
			"game_characteristic",
			"game_characteristic_value",
			"game_contest_effect",
			"game_contest_type",
			"game_creature_game_index",
			"game_element_game_index",
			"game_event_stat",
			"game_event_stat_nature_effect",
			"game_gender_evolution_requirement",
			"game_gender_species_rate",
			"game_growth_rate_level",
			"game_item_game_index",
			"game_location_game_index",
			"game_machine",
			"game_nature_battle_style_preference",
			"game_nature_event_stat_change",
			"game_skill_battle_style",
			"game_skill_contest_combo",
			"game_species_catalog_number",
			"game_species_creature_variety",
			"game_stat_characteristic",
			"game_stat_nature_effect",
			"game_stat_skill_effect",
			"game_transfer_area",
			"game_transfer_area_species",
		)

		val creatureColumns = queryStrings(
			"""
			select column_name
			from information_schema.columns
			where table_schema = 'public' and table_name = 'game_creature'
			order by ordinal_position
			""".trimIndent(),
		)
		assertThat(creatureColumns).contains("inherits_from_creature_id")

		val speciesColumns = queryStrings(
			"""
			select column_name
			from information_schema.columns
			where table_schema = 'public' and table_name = 'game_species'
			order by ordinal_position
			""".trimIndent(),
		)
		assertThat(speciesColumns).contains("national_number")
	}

	@Test
	fun `liquibase stores each current evolution edge once`() {
		val speciesDetailColumns = queryStrings(
			"""
			select column_name
			from information_schema.columns
			where table_schema = 'public' and table_name = 'game_species_detail'
			order by ordinal_position
			""".trimIndent(),
		)
		assertThat(speciesDetailColumns).doesNotContain("evolves_from_species_id", "evolution_chain_id")

		val duplicateSpeciesNodes = queryMaps(
			"""
			select species_id, count(*) as node_count
			from game_evolution_node
			group by species_id
			having count(*) <> 1
			order by species_id
			""".trimIndent(),
		)
		assertThat(duplicateSpeciesNodes).isEmpty()

		val edgesWithoutMatchingDetails = queryMaps(
			"""
			select node.chain_id, node.parent_species_id, node.species_id
			from game_evolution_node node
			where node.parent_species_id is not null
				and not exists (
					select 1
					from game_evolution_detail detail
					where detail.chain_id = node.chain_id
						and detail.from_species_id = node.parent_species_id
						and detail.to_species_id = node.species_id
				)
			order by node.chain_id, node.species_id
			""".trimIndent(),
		)
		assertThat(edgesWithoutMatchingDetails).isEmpty()

		val correctedEdges = queryMaps(
			"""
			select species_id, parent_species_id
			from game_evolution_node
			where species_id in (490, 809)
			order by species_id
			""".trimIndent(),
		)
		assertThat(correctedEdges).containsExactly(
			mapOf("species_id" to 490L, "parent_species_id" to null),
			mapOf("species_id" to 809L, "parent_species_id" to 808L),
		)
	}

	@Test
	fun `liquibase resolves every enabled creature current profile`() {
		val inheritanceCycles = queryMaps(
			"""
			with recursive inheritance(origin_id, creature_id, path, cycle) as (
				select id, id, array[id], false
				from game_creature
				where enabled = true
				union all
				select inheritance.origin_id,
					parent.id,
					inheritance.path || parent.id,
					parent.id = any(inheritance.path)
				from inheritance
				join game_creature current_creature on current_creature.id = inheritance.creature_id
				join game_creature parent on parent.id = current_creature.inherits_from_creature_id
				where not inheritance.cycle
			)
			select origin_id, path
			from inheritance
			where cycle
			order by origin_id
			""".trimIndent(),
		)
		assertThat(inheritanceCycles).isEmpty()

		val incompleteProfiles = queryMaps(
			"""
			with recursive ancestry(origin_id, creature_id, depth, path) as (
				select id, id, 0, array[id]
				from game_creature
				where enabled = true
				union all
				select ancestry.origin_id,
					parent.id,
					ancestry.depth + 1,
					ancestry.path || parent.id
				from ancestry
				join game_creature current_creature on current_creature.id = ancestry.creature_id
				join game_creature parent on parent.id = current_creature.inherits_from_creature_id
				where not parent.id = any(ancestry.path)
			)
			select creature.id,
				creature.code,
				coalesce(stats.relation_count, 0) as stat_count,
				coalesce(elements.relation_count, 0) as element_count,
				coalesce(abilities.relation_count, 0) as ability_count,
				coalesce(learnset.relation_count, 0) as learnset_count
			from game_creature creature
			left join lateral (
				select count(distinct relation.stat_id) as relation_count
				from ancestry
				join game_creature_stat relation on relation.creature_id = ancestry.creature_id
				where ancestry.origin_id = creature.id
				group by ancestry.depth
				order by ancestry.depth
				limit 1
			) stats on true
			left join lateral (
				select count(distinct relation.element_id) as relation_count
				from ancestry
				join game_creature_element relation on relation.creature_id = ancestry.creature_id
				where ancestry.origin_id = creature.id
				group by ancestry.depth
				order by ancestry.depth
				limit 1
			) elements on true
			left join lateral (
				select count(distinct relation.ability_id) as relation_count
				from ancestry
				join game_creature_ability relation on relation.creature_id = ancestry.creature_id
				where ancestry.origin_id = creature.id
				group by ancestry.depth
				order by ancestry.depth
				limit 1
			) abilities on true
			left join lateral (
				select count(distinct relation.skill_id) as relation_count
				from ancestry
				join game_creature_skill_learn relation on relation.creature_id = ancestry.creature_id
				join game_skill skill on skill.id = relation.skill_id and skill.enabled = true
				where ancestry.origin_id = creature.id
				group by ancestry.depth
				order by ancestry.depth
				limit 1
			) learnset on true
			where creature.enabled = true
				and (
					coalesce(stats.relation_count, 0) <> 6
					or coalesce(elements.relation_count, 0) not between 1 and 2
					or coalesce(abilities.relation_count, 0) < 1
					or coalesce(learnset.relation_count, 0) < 1
				)
			order by creature.id
			""".trimIndent(),
		)
		assertThat(incompleteProfiles).isEmpty()

		val unavailableCreatures = queryMaps(
			"""
			select code, enabled
			from game_creature
			where code in ('ditto', 'smeargle', 'unown')
			order by code
			""".trimIndent(),
		)
		assertThat(unavailableCreatures).containsExactly(
			mapOf("code" to "ditto", "enabled" to false),
			mapOf("code" to "smeargle", "enabled" to false),
			mapOf("code" to "unown", "enabled" to false),
		)

		val placeholderNames = queryMaps(
			"""
			select id, code, name
			from game_creature
			where name ~ '（形态 [0-9]+）'
			order by id
			""".trimIndent(),
		)
		assertThat(placeholderNames).isEmpty()
	}

	@Test
	fun `liquibase uses current canonical website codes`() {
		val legacyCodes = queryMaps(
			"""
			select 'skill' as resource_type, id, code from game_skill where code = 'vice-grip'
			union all
			select 'item', id, code from game_item where code = 'pretty-wing'
			union all
			select 'ability', id, code from game_ability where code = 'embody-aspect'
			order by resource_type, id
			""".trimIndent(),
		)
		assertThat(legacyCodes).isEmpty()

		val embodyAspectCodes = queryStrings(
			"""
			select code
			from game_ability
			where code like 'embody-aspect-%'
			order by code
			""".trimIndent(),
		)
		assertThat(embodyAspectCodes).containsExactly(
			"embody-aspect-cornerstone",
			"embody-aspect-hearthflame",
			"embody-aspect-teal",
			"embody-aspect-wellspring",
		)
	}

	@Test
	fun `battle rules changelog references only current skill codes`() {
		val changelog = javaClass.getResource("/db/changelog/baseline/001-core.yaml")

		assertThat(changelog).isNotNull()
		val explicitSkillCodes = explicitBattleRuleSkillCodes(changelog!!.readText())
		val currentSkillCodes = queryStrings("select code from game_skill").toSet()
		val missingSkillCodes = explicitSkillCodes - currentSkillCodes

		assertThat(explicitSkillCodes).isNotEmpty()
		assertThat(missingSkillCodes).isEmpty()
	}

	@Test
	fun `liquibase keeps only required executable support outside website lists`() {
		val battleSupportSkills = queryStrings(
			"""
			select code
			from game_skill
			where code in ('cut', 'sonic-boom', 'dragon-rage', 'heal-block', 'oblivion-wing', 'natures-madness')
			order by code
			""".trimIndent(),
		)
		assertThat(battleSupportSkills).containsExactly(
			"cut",
			"dragon-rage",
			"heal-block",
			"natures-madness",
			"oblivion-wing",
			"sonic-boom",
		)

		val evolutionSupportItems = queryStrings(
			"""
			select code
			from game_item
			where code in (
				'deep-sea-tooth',
				'deep-sea-scale',
				'whipped-dream',
				'sachet',
				'scroll-of-darkness',
				'scroll-of-waters',
				'black-augurite',
				'peat-block'
			)
			order by code
			""".trimIndent(),
		)
		assertThat(evolutionSupportItems).containsExactly(
			"black-augurite",
			"deep-sea-scale",
			"deep-sea-tooth",
			"peat-block",
			"sachet",
			"scroll-of-darkness",
			"scroll-of-waters",
			"whipped-dream",
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
			"game_item_attribute",
			"game_skill_ailment",
			"game_growth_rate",
			"game_region",
			"game_location_area_encounter",
			"game_evolution_chain",
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
			union all select 'game_region', count(*) from game_region
			union all select 'game_location', count(*) from game_location
			union all select 'game_location_area_encounter', count(*) from game_location_area_encounter
			union all select 'game_creature_skill_learn', count(*) from game_creature_skill_learn
			order by table_name
			""".trimIndent(),
		).associate { it["table_name"] to it["row_count"].toString().toLong() }
		assertThat(seedCounts).containsEntry("game_element", 21L)
		assertThat(seedCounts).containsEntry("game_ability", 310L)
		assertThat(seedCounts).containsEntry("game_skill", 691L)
		assertThat(seedCounts).containsEntry("game_item", 257L)
		assertThat(seedCounts).containsEntry("game_species", 1025L)
		assertThat(seedCounts).containsEntry("game_creature", 1351L)
		assertThat(seedCounts).containsEntry("game_creature_stat", 8100L)
		assertThat(seedCounts).containsEntry("game_region", 11L)
		assertThat(seedCounts).containsEntry("game_location", 1008L)
		assertThat(seedCounts).containsEntry("game_location_area_encounter", 22491L)
		assertThat(seedCounts).containsEntry("game_creature_skill_learn", 129237L)

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
	fun `liquibase display data uses normalized creature terminology`() {
		// 这些列都会直接出现在管理端或接口返回中；全表断言可以防止后续导入重新带回旧展示术语。
		val legacyTermTexts = queryMaps(
			"""
			with display_texts(table_name, row_id, display_text) as (
				select 'game_ability_detail', id, concat_ws(' ', effect, flavor_text, short_effect)
				from game_ability_detail
				union all
				select 'game_encounter_condition', id, name
				from game_encounter_condition
				union all
				select 'game_encounter_condition_value', id, name
				from game_encounter_condition_value
				union all
				select 'game_encounter_method', id, name
				from game_encounter_method
				union all
				select 'game_item', id, name
				from game_item
				union all
				select 'game_item_attribute', id, description
				from game_item_attribute
				union all
				select 'game_item_detail', id, concat_ws(' ', effect, flavor_text, short_effect)
				from game_item_detail
				union all
				select 'game_location', id, name
				from game_location
				union all
				select 'game_location_area', id, name
				from game_location_area
				union all
				select 'game_skill_detail', id, concat_ws(' ', effect, flavor_text, short_effect)
				from game_skill_detail
				union all
				select 'game_skill_learn_method', id, description
				from game_skill_learn_method
				union all
				select 'game_skill_target', id, concat_ws(' ', name, description)
				from game_skill_target
				union all
				select 'game_species_detail', id, concat_ws(' ', genus, flavor_text)
				from game_species_detail
				union all
				select 'security_access_node', id, name
				from security_access_node
			)
			select table_name, row_id, display_text
			from display_texts
			where display_text like '%' || '生' || '物' || '%'
			order by table_name, row_id
			""".trimIndent(),
		)

		assertThat(legacyTermTexts)
			.describedAs("展示给前端的资料文本必须统一使用“精灵”术语")
			.isEmpty()
	}

	@Test
	fun `liquibase game location seed data does not keep generated placeholder names`() {
		// 这条测试只拦截确定无业务含义的机器翻译占位词；普通译名质量继续按资料源逐批校正。
		val placeholderLocations = queryMaps(
			"""
			with seeded_location_names as (
				select 'game_location' as table_name, id, code, name
				from game_location
				union all
				select 'game_location_area' as table_name, id, code, name
				from game_location_area
			),
			blocked_exact_names(name) as (
				values ('地点'), ('小径地点'), ('洞窟地点'), ('区域地点'), ('洞窟区域')
			),
			blocked_name_patterns(pattern) as (
				values
					('%遗留区域%'),
					('%pokemart%'),
					('%pokecenter%'),
					('%t.g.%'),
					('%s.s.%'),
					('%pc福%'),
					('%pc名%'),
					('%pc横%'),
					('%ns城堡%'),
					('%影子精灵%'),
					('%poke mart%'),
					('%阿罗拉route%'),
					('%伽勒尔route%'),
					('翡翠%'),
					('%hisui%'),
					('%templeof%'),
					('%tombolo%area%'),
					('%heatharea%'),
					('%settlementarea%'),
					('%bogarea%'),
					('%洞窟area%'),
					('%森林area%'),
					('%lowtide%'),
					('%b1f%'),
					('%b2f%'),
					('%b3f%'),
					('%2f%'),
					('%route104%'),
					('%offocusarea%'),
					('%icebergruinsarea%'),
					('%ironruinsarea%'),
					('%rockpeakruinsarea%'),
					('%tunnelarea%'),
					('%heaheacity%'),
					('%lushjungle%'),
					('%mountlanakila%'),
					('%ultraspace%'),
					('%poke pelago%'),
					('%jubilife%'),
					('%floaroma%'),
					('%solaceon%'),
					('%virbank%'),
					('%pinwheel%'),
					('%ruminationfield%'),
					('%unova%'),
					('%breeder%'),
					('%（met）%'),
					('%poké%'),
					('%kindle%'),
					('%mount ember%'),
					('%cianwood%'),
					('%game freak%'),
					('%verdanturf%'),
					('%lilycove%'),
					('%rayquaza%'),
					('%heatran%'),
					('%pacifidlog%'),
					('%mirage cave%'),
					('%gateon%'),
					('%citadark%'),
					('%野生动物园区%'),
					('%（pwt%'),
					('%onbs%')
			)
			select table_name, id, code, name
			from seeded_location_names seeded
			where exists (
					select 1
					from blocked_exact_names blocked
					where seeded.name = blocked.name
				)
				or exists (
					select 1
					from blocked_name_patterns blocked
					where lower(seeded.name) like blocked.pattern
				)
			order by table_name, id
			""".trimIndent(),
		)

		assertThat(placeholderLocations).isEmpty()
	}

	@Test
	fun `liquibase game encounter seed data does not keep generated display names`() {
		// 遭遇条件、取值和方式既会出现在资料维护页，也会作为地点遭遇表格的筛选项；这里拦截本批已经确认过的英文残留和机翻词。
		// 断言只覆盖明确无业务展示价值的文本，不用宽泛英文正则，避免把还没有逐条校正的历史资料误判成同一个问题。
		val generatedEncounterNames = queryMaps(
			"""
			with encounter_names as (
				select 'game_encounter_condition' as table_name, id, code, name
				from game_encounter_condition
				union all
				select 'game_encounter_condition_value' as table_name, id, code, name
				from game_encounter_condition_value
				union all
				select 'game_encounter_method' as table_name, id, code, name
				from game_encounter_method
			),
			blocked_name_patterns(pattern) as (
				values
					('%sudowoodo%'),
					('%wailmer%'),
					('%feebas%'),
					('%npc%'),
					('% pal %'),
					('%colosseum%'),
					('%gale of darkness%'),
					('%friend safari%'),
					('%backlot%'),
					('%leafgreen%'),
					('%zephyr%'),
					('%dragonair%'),
					('%drowzee%'),
					('%dugtrio%'),
					('%volbeat%'),
					('%munchlax%'),
					('%uxie%'),
					('%mesprit%'),
					('%azelf%'),
					('%articuno%'),
					('%zapdos%'),
					('%moltres%'),
					('%regirock%'),
					('%regice%'),
					('%registeel%'),
					('%starter%'),
					('%tm59%'),
					('%老虎机%'),
					('%开胃菜%'),
					('%港龙航空%'),
					('%卡纳拉克斯%')
			)
			select table_name, id, code, name
			from encounter_names seeded
			where exists (
				select 1
				from blocked_name_patterns blocked
				where lower(seeded.name) like blocked.pattern
			)
			order by table_name, id
			""".trimIndent(),
		)

		assertThat(generatedEncounterNames).isEmpty()
	}

	@Test
	fun `liquibase game metadata seed data does not keep generated display text`() {
		// 这些资料表都用于维护页的小型字典或形态展示；断言只检查 name、form_name、description 等展示列。
		// 公式和 code 允许保留资料源英文/LaTeX 标识，因此这里显式排除非展示字段，避免把内部稳定编码当成文案问题。
		val generatedMetadataTexts = queryMaps(
			"""
			with metadata_texts as (
				select 'game_creature_form' as table_name, id, code, concat_ws(' ', name, form_name) as display_text
				from game_creature_form
				union all
				select 'game_growth_rate' as table_name, id, code, concat_ws(' ', name, description) as display_text
				from game_growth_rate
				union all
				select 'game_skill_category' as table_name, id, code, concat_ws(' ', name, description) as display_text
				from game_skill_category
				union all
				select 'game_skill_learn_method' as table_name, id, code, concat_ws(' ', name, description) as display_text
				from game_skill_learn_method
				union all
				select 'game_skill_target' as table_name, id, code, concat_ws(' ', name, description) as display_text
				from game_skill_target
			),
			blocked_text_patterns(pattern) as (
				values
					('%paldea blaze%'),
					('%paldea aqua%'),
					('%slowthen%'),
					('%fastthen%'),
					('%无损坏%'),
					('%具体举措%'),
					('%对立的精灵%'),
					('%整个领域%'),
					('%prime cup%'),
					('%master ball%'),
					('%volt 钓具%'),
					('%pichu%'),
					('%tm %'),
					('% hm%'),
					('%影子精灵%'),
					('%zygarde cube%'),
					('%zygarde core%'),
					('%基加德%'),
					('%火车%')
			)
			select table_name, id, code, display_text
			from metadata_texts seeded
			where exists (
				select 1
				from blocked_text_patterns blocked
				where lower(seeded.display_text) like blocked.pattern
			)
			order by table_name, id
			""".trimIndent(),
		)

		assertThat(generatedMetadataTexts).isEmpty()
	}

	@Test
	fun `liquibase game item effects do not keep generated placeholder text`() {
		// 这些字段面向管理端展示，模板占位和机翻术语都会直接暴露给用户；全表断言可以防止后续导入重新带回生成文本。
		val placeholderItemEffects = queryMaps(
			"""
			with item_texts as (
				select item_id, 'effect' as column_name, effect as display_text
				from game_item_detail
				union all
				select item_id, 'short_effect' as column_name, short_effect as display_text
				from game_item_detail
				union all
				select item_id, 'flavor_text' as column_name, flavor_text as display_text
				from game_item_detail
			),
			blocked_text_patterns(pattern) as (
				values
					('%xxx%'),
					('%silvally%'),
					('%ingrain%'),
					('%vermilion%'),
					('%sevii%'),
					('%navel rock%'),
					('%celio%'),
					('%network machine%'),
					('%rainbow pass%'),
					('%cottonee%'),
					('%whimsicott%'),
					('%clefairy%'),
					('%vaporeon%'),
					('%cubone%'),
					('%flareon%'),
					('%growlithe%'),
					('%arcanine%'),
					('%pansear%'),
					('%simisear%'),
					('%ninetales%'),
					('%exeggcute%'),
					('%exeggutor%'),
					('%gloom%'),
					('%vileplume%'),
					('%nuzleaf%'),
					('%shiftry%'),
					('%pansage%'),
					('%simisage%'),
					('%weepinbell%'),
					('%victreebel%'),
					('%lileep%'),
					('%kabuto%'),
					('%cranidos%'),
					('%minccino%'),
					('%cinccino%'),
					('%roselia%'),
					('%roserade%'),
					('%togetic%'),
					('%togekiss%'),
					('%kirlia%'),
					('%gallade%'),
					('%snorunt%'),
					('%froslass%'),
					('%happiny%'),
					('%chansey%'),
					('%blissey%'),
					('%spiritomb%'),
					('%budew egg%'),
					('%小树%'),
					('%潘萨尔%'),
					('%西米萨尔%'),
					('%阿曼人%'),
					('%恐龙%'),
					('%盾盾%'),
					('%误判%'),
					('%魔神%'),
					('%钱西%'),
					('%灵魂坟墓%'),
					('%clamperl%'),
					('%huntail%'),
					('%gorebyss%'),
					('%onix%'),
					('%scyther%'),
					('%steelix%'),
					('%scizor%'),
					('%seadra%'),
					('%kingdra%'),
					('%porygon%'),
					('%marill%'),
					('%azumarill%'),
					('%azurill%'),
					('%wobbuffet%'),
					('%wynaut%'),
					('%farfetch%'),
					('%ditto%'),
					('%sudowoodo%'),
					('%bonsly%'),
					('%chimecho%'),
					('%rhydon%'),
					('%rhyperior%'),
					('%electabuzz%'),
					('%electivire%'),
					('%magmor%'),
					('%magmortar%'),
					('%dusclops%'),
					('%dusknoir%'),
					('%weavile%'),
					('%gligar%'),
					('%gliscor%'),
					('%stones%'),
					('%mega steelix%'),
					('%克拉普尔%'),
					('%克勒普尔%'),
					('%亨泰尔%'),
					('%戈比斯%'),
					('%迪托%'),
					('%卡氏蜥蜴%'),
					('%螳螂蛋%'),
					('%玛格玛塔%'),
					('%杜斯洛普斯%'),
					('%斯内塞尔%'),
					('%狂战%'),
					('%valley windworks%'),
					('%cynthia%'),
					('%surf hm%'),
					('%seabreak%'),
					('%shaymin%'),
					('%s.s. tidal%'),
					('%s.s. anne%'),
					('%s.s. aqua%'),
					('%pokétch%'),
					('%battle frontier%'),
					('%blu apricorn%'),
					('%blk apricorn%'),
					('%grn 杏子%'),
					('%pnk 杏子%'),
					('%buena%'),
					('%silph co%'),
					('%gameboy%'),
					('%pokéathlon%'),
					('%devon corporation%'),
					('%berry blender%'),
					('%pokéblock%'),
					('%deepseatooth%'),
					('%deepseascale%'),
					('%cozmo%'),
					('%kecleon%'),
					('%cerulean city%'),
					('%bike shop%'),
					('%team magma%'),
					('%feebas%'),
					('%milotic%'),
					('%archen%'),
					('%entree%'),
					('%entralink%'),
					('%教练战%'),
					('%狂野的对战%'),
					('%狂野的战斗%'),
					('%不会发生任何事情并且球会丢失%'),
					('%红杏%'),
					('%由杏子制成%'),
					('%zigzagoon%'),
					('%wingull%'),
					('%magnemite%'),
					('%wailmer%'),
					('%duskull%'),
					('%bellossom%'),
					('%sla击倒th%'),
					('%wonder 启动器%'),
					('%通过 神奇发射器%'),
					('%swirlix%'),
					('%slurpuff%'),
					('%spritzee%'),
					('%holder%'),
					('%aromatisse%'),
					('%houndoom%'),
					('%mega houndoom%'),
					('%looker%'),
					('%korrina%'),
					('%npc 钥匙石%'),
					('%sea mauville%'),
					('%sceptile%'),
					('%mega sceptile%'),
					('%audino%'),
					('%mega audino%'),
					('%bug 技能%'),
					('%hyper 先生%'),
					('%primarina%'),
					('%sparkling aria%'),
					('%oceanic operetta%'),
					('%eevee%'),
					('%last resort%'),
					('%extreme evoboost%'),
					('%zygarde%'),
					('%oricorio%'),
					('%baile%'),
					('%pom-pom%'),
					('%pa''u%'),
					('%sensu%'),
					('%patrat%'),
					('%tornadus%'),
					('%thundurus%'),
					('%landorus%'),
					('%兽人形态%'),
					('%focus punch%'),
					('%dynamicpunch%'),
					('%mega punch%'),
					('%psyshock%'),
					('%bulk up%'),
					('%bullet seed%'),
					('%psych up%'),
					('%take down%'),
					('%bubblebeam%'),
					('%giga drain%'),
					('%solarbeam%'),
					('%smack down%'),
					('%swift%'),
					('%thunderpunch%'),
					('%softboiled%'),
					('%psywave%'),
					('%snatch%'),
					('%fling%'),
					('%sky drop%'),
					('%giga impact%'),
					('%stone edge%'),
					('%volt switch%'),
					('%work up%'),
					('%tm64%'),
					('%wi-fi%'),
					('%精度%'),
					('%损坏%'),
					('%心灵领域%'),
					('%在精神领域%'),
					('%每次移动恢复%'),
					('%每次移动都会将%'),
					('%破裂的地板%'),
					('% hp%'),
					('%hp %'),
					('% pp%'),
					('%pp %'),
					('%tm%'),
					('%hm%'),
					('%gen iii%'),
					('%trick%'),
					('%switcheroo%'),
					('%pokérus%'),
					('%pokerus%'),
					('%u-回合%'),
					('%psyducks%'),
					('%snorlax%'),
					('%c-gear%'),
					('%gram 1%'),
					('%gram 2%'),
					('%gram 3%'),
					('%tm89%'),
					('%bug型%'),
					('%向兼容的精灵教授%'),
					('%向兼容的精灵传授%'),
					('%教导兼容的精灵%'),
					('%教授兼容%'),
					('%教兼容的精灵%'),
					('%教%兼容的精灵%'),
					('%传授兼容的精灵%'),
					('%教给兼容的精灵%'),
					('%传授给兼容的精灵%'),
					-- 学习器说明的技能名来自早期机器翻译和英文直译，统一后需要把旧译名作为回归保护。
					-- 部分旧词是现行译名的前缀，例如“反射/反射壁”“回收/回收利用”，因此这里用带标点的模式避免误伤正确文本。
					('%学会集中拳%'),
					('%学会磨砂爪%'),
					('%学会水脉冲%'),
					('%学会冷静的头脑%'),
					('%学会咆哮%'),
					('%学会有毒%'),
					('%学会隐藏力量%'),
					('%学会晴天%'),
					('%学会嘲讽%'),
					('%学会冰束%'),
					('%学会超级光束%'),
					('%学会光幕%'),
					('%学会保护%'),
					('%学会雨之舞%'),
					('%学会防护%'),
					('%学会挫败感%'),
					('%学会回归%'),
					('%学会打砖块%'),
					('%学会双人包夹%'),
					('%学会反射。%'),
					('%学会冲击波%'),
					('%学会火焰冲击%'),
					('%学会火焰冲锋%'),
					('%学会火焰喷射器%'),
					('%学会岩石坟墓%'),
					('%学会空中王牌%'),
					('%学会外观%'),
					('%学会秘密力量%'),
					('%学会吸引%'),
					('%学会低扫%'),
					('%学会回合%'),
					('%学会回声之声%'),
					('%学会集中爆发%'),
					('%学会龙脉%'),
					('%学会焚化%'),
					('%学会排水拳%'),
					('%学会奎什%'),
					('%学会银风%'),
					('%学会岩石抛光%'),
					('%学会雷霆波%'),
					('%学会雷电%'),
					('%学会雷霆%'),
					('%学会毒刺%'),
					('%学会草结%'),
					('%学会勇气%'),
					('%学会掉头%'),
					('%学会闪光炮%'),
					('%学会岩石粉碎%'),
					('%学会岩石滑行%'),
					('%学会天赋%'),
					('%学会栖息%'),
					('%学会力量%'),
					('%学会除雾%'),
					('%学会瀑布%'),
					('%学会烫伤%'),
					('%学会影子球%'),
					('%学会折磨%'),
					('%学会休息%'),
					('%学会禁运%'),
					('%学会爆炸%'),
					('%学会回收。%'),
					('%学会报复%'),
					('%学会推土机%'),
					('%学会隐形岩石%'),
					('%：水脉%'),
					('%：冲击波%'),
					('%：拜德%'),
					('%：排水冲孔%'),
					('%第四代：天赋%'),
					('%第四代：栖息%'),
					('%：龙脉%'),
					('%：银风%'),
					('%：隐形岩石%'),
					('%雷霆波动%'),
					('%检测%'),
					('%天空攻击%'),
					('%岩石滑行%'),
					('%火拳%'),
					('%拍泥%'),
					('%巨型排水管%'),
					('%第四代：回收）%'),
					('%钻石／珍珠／白金：除雾%'),
					-- 属性道具和属性宝石说明统一使用“效果绝佳”和“属性技能”口径，避免旧导入再次带回“某类/某系/破坏性”等机器译词。
					('%对战类%'),
					('%精神类%'),
					('%虫类%'),
					('%心灵类%'),
					('%暗系%'),
					('%钢系%'),
					('%妖精类%'),
					('%超级有效%'),
					('%超有效%'),
					('%普通属性%'),
					('%普通系%'),
					('%普通技能%'),
					('%伤害技能%'),
					('%电技能%'),
					('%格斗技能%'),
					('%毒技能%'),
					('%地面技能%'),
					('%岩石技能%'),
					('%幽灵技能%'),
					('%钢技能%'),
					('%妖精技能%'),
					('%龙技能%'),
					('%飞行技能%'),
					('%草类%'),
					('%毒类%'),
					('%幽灵类%'),
					('%岩石型%'),
					('%科技爆炸%'),
					('%技术爆炸%'),
					('%科技冲击波%'),
					('%火焰旋转%'),
					('%冰霜吐息%'),
					('%学会迷惑%'),
					('%负伤到%'),
					('%C装置%'),
					('%持有者：%'),
					('%保持：%'),
					('%必杀技%'),
					('%元体%'),
					('%破坏性的%'),
					('%具有1.5倍%'),
					('%力量为%'),
					('%电攻击%'),
					('%发生烧伤%'),
					('%收到该物品%'),
					('%[var (0000)]%')
			)
			select item_id, column_name, display_text
			from item_texts
			where lower(display_text) in ('效果', '未知。', '未知。目前未使用。')
				or lower(display_text) like any (array[
					'%举行：%',
					'%希尔瓦利%',
					'%席尔瓦利%',
					'%多重攻击%',
					'%卖家垃圾%',
					'%斗争虫%',
					'%定期伤害%'
				])
				or exists (
					select 1
					from blocked_text_patterns blocked
					where lower(display_text) like blocked.pattern
				)
			order by item_id, column_name
			""".trimIndent(),
		)

		assertThat(placeholderItemEffects).isEmpty()
	}

	@Test
	fun `liquibase game species details do not keep corrected english display text`() {
		// 物种分类和说明是资料详情页的正文内容；本测试记录已经人工校正过的英文名残留，避免后续重新导入时覆盖回旧文本。
		// 这里依旧采用定向词表，因为历史物种说明中仍有其他待校正文本，先把已处理的数据钉住，比一次性写宽泛规则更可维护。
		val correctedSpeciesDetails = queryMaps(
			"""
			with species_texts as (
				select species_id, 'genus' as column_name, genus as display_text
				from game_species_detail
				union all
				select species_id, 'flavor_text' as column_name, flavor_text as display_text
				from game_species_detail
			),
			blocked_text_patterns(pattern) as (
				values
					('%meditite%'),
					('%plusle%'),
					('%minun%'),
					('%volbeat%'),
					('%illumise%'),
					('%wugtrio%'),
					('%dugtrio%'),
					('%quilava%'),
					('%furret%'),
					('%sunkern%'),
					('%sunflora%'),
					('%magcargo%'),
					('%phanpy%'),
					('%entei%'),
					('%treecko%'),
					('%sceptile%'),
					('%blaziken%'),
					('%poochyena%'),
					('%mightyena%'),
					('%cascoon%'),
					('%silcoon%'),
					('%numel%'),
					('%mega进化%'),
					('%bidoof%'),
					('%bibarel%'),
					('%floatzel%'),
					('%uxie%'),
					('%hisuian%'),
					('%scalhop%'),
					('%zebstrika%'),
					('%sewaddle%'),
					('%this 精灵%'),
					('%team plasma%'),
					('%floragato%'),
					('%skeledirge%'),
					('%pawmo%'),
					('%pawmot%'),
					('%nacli%'),
					('%tadbulb%'),
					('%mabosstiff%'),
					('%grafaiai%'),
					('%tentacool%'),
					('%toedscool%'),
					('%rabsca%'),
					('%tinkaton%'),
					('%varoom%'),
					('%revavroom%'),
					('%cyclizar%'),
					('%glimmet%'),
					('%houndstone%'),
					('%veluza%'),
					('%arctabax%'),
					('%poltchageist%'),
					('%sinistea%')
			)
			select species_id, column_name, display_text
			from species_texts seeded
			where exists (
				select 1
				from blocked_text_patterns blocked
				where lower(seeded.display_text) like blocked.pattern
			)
			order by species_id, column_name
			""".trimIndent(),
		)

		assertThat(correctedSpeciesDetails).isEmpty()
	}

	@Test
	fun `liquibase game item effects do not leave display text blank`() {
		// 道具说明同样会出现在管理端表格和详情里；缺少来源文本时使用中文兜底，不能把空字符串交给前端。
		val blankItemEffects = queryMaps(
			"""
			select item_id, effect, short_effect, flavor_text
			from game_item_detail
			where effect = '' or short_effect = ''
			order by item_id
			""".trimIndent(),
		)

		assertThat(blankItemEffects).isEmpty()
	}

	@Test
	fun `liquibase game item fling effects do not keep generated placeholders`() {
		// 投掷效果是小型枚举表，effect 字段必须能直接解释命中后的附加效果。
		val placeholderFlingEffects = queryMaps(
			"""
			select id, code, name, effect
			from game_item_fling_effect
			where effect = '效果'
			order by id
			""".trimIndent(),
		)

		assertThat(placeholderFlingEffects).isEmpty()
	}

	@Test
	fun `liquibase game ability effects do not keep generated machine terms`() {
		val machineAbilityEffects = queryMaps(
			"""
			select ability_id, effect, short_effect, flavor_text
			from game_ability_detail
			where effect like '%希尔瓦利%'
				or short_effect like '%希尔瓦利%'
				or flavor_text like '%希尔瓦利%'
				or effect like '%多重攻击%'
				or short_effect like '%多重攻击%'
				or flavor_text like '%多重攻击%'
				or effect like '%心灵攻击%'
				or short_effect like '%心灵攻击%'
				or flavor_text like '%心灵攻击%'
				or effect like '%加号或减号%'
				or short_effect like '%加号或减号%'
				or flavor_text like '%加号或减号%'
				or effect like '%正值或负值%'
				or short_effect like '%正值或负值%'
				or flavor_text like '%正值或负值%'
				or effect like '%物理移动%'
				or short_effect like '%物理移动%'
				or flavor_text like '%物理移动%'
				or effect like '%火力移动%'
				or short_effect like '%火力移动%'
				or flavor_text like '%火力移动%'
				or effect like '%水移动%'
				or short_effect like '%水移动%'
				or flavor_text like '%水移动%'
				or effect like '%因移动而受到伤害%'
				or short_effect like '%因移动而受到伤害%'
				or flavor_text like '%因移动而受到伤害%'
				or lower(concat_ws(' ', effect, short_effect, flavor_text)) like any (array[
					'%hp%',
					'%bug%',
					'%teravolt%',
					'%turboblaze%',
					'%solid rock%',
					'%switcheroo%',
					'%trick%',
					'%cherrim%',
					'%zen mode%',
					'%darmanitan%',
					'%mold breaker%',
					'%wishiwashi%',
					'%schooling form%',
					'%zygarde%',
					'%sleep talk%',
					'%sp。%',
					'%sp.%',
					'%dondozo%',
					'%损坏%',
					'%精度%',
					'%狂野的对战%',
					'%电动地形%',
					'%火焰技能%'
				])
				or effect = '效果'
				or short_effect = '效果'
			order by ability_id
			""".trimIndent(),
		)

		assertThat(machineAbilityEffects).isEmpty()
	}

	@Test
	fun `liquibase game ability effects do not leave display text blank`() {
		// 扩展能力编号也会进入管理端；没有来源说明时使用中文兜底，避免详情页出现空白字段。
		val blankAbilityEffects = queryMaps(
			"""
			select ability_id, effect, short_effect, flavor_text
			from game_ability_detail
			where effect = '' or short_effect = '' or flavor_text = ''
			order by ability_id
			""".trimIndent(),
		)

		assertThat(blankAbilityEffects).isEmpty()
	}

	@Test
	fun `liquibase game extended details do not keep corrected machine terms`() {
		// 本测试覆盖特性、道具、技能三类最长说明文本。这些字段直接进入管理端表格和详情页，
		// 旧导入源里容易把属性、命中率、回复、训练家和持有物语境翻成机翻词；这里使用定向词表，
		// 只锁定已经人工修正过的短语，避免误伤“视觉项目”“游乐项目”这类本身合理的中文表达。
		val correctedMachineTerms = queryMaps(
			"""
			with display_texts(table_name, row_id, column_name, display_text) as (
				select 'game_ability_detail', ability_id::text, 'effect', effect
				from game_ability_detail
				union all
				select 'game_ability_detail', ability_id::text, 'short_effect', short_effect
				from game_ability_detail
				union all
				select 'game_ability_detail', ability_id::text, 'flavor_text', flavor_text
				from game_ability_detail
				union all
				select 'game_item_detail', item_id::text, 'effect', effect
				from game_item_detail
				union all
				select 'game_item_detail', item_id::text, 'short_effect', short_effect
				from game_item_detail
				union all
				select 'game_item_detail', item_id::text, 'flavor_text', flavor_text
				from game_item_detail
				union all
				select 'game_skill_detail', skill_id::text, 'effect', effect
				from game_skill_detail
				union all
				select 'game_skill_detail', skill_id::text, 'short_effect', short_effect
				from game_skill_detail
				union all
				select 'game_skill_detail', skill_id::text, 'flavor_text', flavor_text
				from game_skill_detail
			),
			blocked_text_patterns(pattern) as (
				values
					('%主世界%'),
					('%电力%'),
					('%电动%'),
					('%治疗最大生命值%'),
					('%治愈最大生命值%'),
					('%治疗使用者%'),
					('%治愈使用者%'),
					('%水技能%'),
					('%火技能%'),
					('%心灵技能%'),
					('%黑暗技能%'),
					('%黑暗系%'),
					('%黑暗套索%'),
					('%黑暗脉冲%'),
					('%黑暗光环%'),
					('%黑暗气息%'),
					('%雷霆波动%'),
					('%雷电波%'),
					('%恶毒%'),
					('%毒药%'),
					('%常规伤害%'),
					('%普通伤害%'),
					('%普通属性%'),
					('%超级有效%'),
					('%超有效%'),
					('%地面类技能%'),
					('%地面技能%'),
					('%每个人有%'),
					('%正常伤害%'),
					('%正常技能%'),
					('%损伤公式%'),
					('%地面攻击%'),
					('%无人守卫%'),
					('%无守卫%'),
					('%拥有预测功能%'),
					('%准确度%'),
					('%准确性%'),
					('%精准度%'),
					('%读心术%'),
					('%雷霆震击%'),
					('%雷霆准确率%'),
					('%雷霆的准确率%'),
					('%雷霆有%'),
					('%雷霆和旋风%'),
					('%镜面涂层%'),
					('%普通技能%'),
					('%通常技能%'),
					('%对战技能%'),
					('%角色扮演%'),
					('%技能交换%'),
					('%忧虑种子%'),
					('%忧心种子%'),
					('%多种属性%'),
					('%由多属性精灵%'),
					('%多种类型%'),
					('%多重属性%'),
					('%多类型%'),
					('%粘滞持有%'),
					('%粘滞保持%'),
					('%粘性保留%'),
					('%粘性保持%'),
					('%粘性固定%'),
					('%多重类型%'),
					('%预测功能%'),
					('%花之礼物、预测%'),
					('%颜色变化或预测%'),
					('%与预测、多属性%'),
					('%幻象%'),
					('%冒名顶替者%'),
					('%冒充者%'),
					('%奇迹守卫%'),
					('%神奇守卫%'),
					('%林伯%'),
					('%无意识%'),
					('%元气%'),
					('%失眠能力%'),
					('%对战中的失眠%'),
					('%失眠和干劲%'),
					('%失眠或干劲%'),
					('%技能变换%'),
					('%花之礼物%'),
					('%花礼%'),
					('%鲜花礼物%'),
					('%逃学%'),
					('%禅宗模式%'),
					('%清体功能%'),
					('%骚乱%'),
					('%贪婪%'),
					('%觊觎%'),
					('%打倒的物品%'),
					('%因打倒而掉落%'),
					('%被打倒移除%'),
					('%和打倒仍然%'),
					('%水色戒指%'),
					('%巴顿帕斯%'),
					('%卑鄙目光%'),
					('%蜘蛛网%'),
					('%招摇%'),
					('%力量技巧%'),
					('%磁铁上升%'),
					('%内根%'),
					('%胃酸%'),
					('%雷波%'),
					('%天空上勾拳%'),
					('%重击、打雷%'),
					('%反击、金属爆裂%'),
					('%反击，戏法防守%'),
					('%雪崩、反击%'),
					('%雨舞、恢复、回收%'),
					('%该项目可以重新使用%'),
					('%从支架上移除该物品%'),
					('%尚未技能%'),
					('%已技能%'),
					('%每隔一圈%'),
					('%最后技能时%'),
					('%偶尔使精灵先技能%'),
					('%机会先技能%'),
					('%技能速度%'),
					('%抬前轮技能%'),
					('%此举动%'),
					('%这一举动%'),
					('%该举动%'),
					('%这个举动%'),
					('%此举%'),
					('%这一招%'),
					('%该行动将会失败%'),
					('%下一步行动%'),
					('%最后一步%'),
					('%前一步%'),
					('%这样的一步%'),
					('%移动仍会被复制%'),
					('%本回合中移动的一对%'),
					('%使用过回合%'),
					-- 对战说明里的 turn、field 和 ally 旧数据曾被直译成“转弯”“字段”“友好精灵”；
					-- 这些词会削弱规则文本的可读性，因此在导入测试中固定拦截并统一改成“回合”“场地”“友方精灵”。
					('%转弯%'),
					('%该字段%'),
					('%进入该字段%'),
					('%离开该字段%'),
					('%友好的精灵%'),
					('%友好精灵%'),
					('%友好的草精灵%'),
					-- substitute 和 replacement 在不同规则里分别表示“替身”和“换入精灵”；
					-- 旧词“替代品/替代者/替补”会混淆这两类规则，因此只保留语义明确的中文术语。
					('%替代品%'),
					('%替代精灵%'),
					('%替代目标%'),
					('%替代者%'),
					('%有替补%'),
					('%替补受到%'),
					('%无法替代%'),
					('%复制或替代%'),
					('%替代、%'),
					('%替代；%'),
					-- 对战文本中的场地、对手和换入语境需要用玩家更熟悉的词；
					-- “战场”“敌对精灵”“替换精灵”容易像直译说明书，因此在种子数据层直接拦截。
					('%进入战场%'),
					('%自进入战场%'),
					('%战场上存在%'),
					('%敌对精灵%'),
					('%敌对的精灵%'),
					('%对抗精灵%'),
					('%对手精灵的所有技能%'),
					('%对抗该精灵%'),
					('%对抗飞行%'),
					('%对抗目标%'),
					('%技能对抗它%'),
					-- damage/status move 的旧机翻曾残留为“破坏性/非破坏性”，incoming 也被直译为“传入”；
					-- 展示层统一使用“伤害性技能”“变化技能”“受到的技能”，规则含义更直接。
					('%破坏性技能%'),
					('%非破坏性技能%'),
					('%传入技能%'),
					('%传入的龙技能%'),
					('%非伤害性技能%'),
					('%非伤害技能%'),
					('%效果技能%'),
					('%伤害类招式%'),
					('%伤害性招式%'),
					('%变化类技能%'),
					('%让替换精灵的%'),
					('%选择替换精灵%'),
					('%使用替换精灵的%'),
					('%替换的精灵不会触发%'),
					('%不会给此技能%'),
					('%上一个技能是这类技能%'),
					('%如果每回合使用多次%'),
					('%元技能%'),
					('%“元”技能%'),
					('%照常技能%'),
					('%状态状态%'),
					('%防御和 特防%'),
					('%特攻和 特防%'),
					('%攻击力、 防御%'),
					('%提升三个级%'),
					('%每个级%'),
					('%两个级%'),
					('%三个级%'),
					('%六个级%'),
					('%速度能力值值%'),
					('%特防强度%'),
					('%技能威力仅会增强%'),
					('%技能威力会仅增强%'),
					('%致命一击率%'),
					('%状态疾病%'),
					('%疾病状态%'),
					('%这种疾病%'),
					('%严重疾病%'),
					('%重大状态%'),
					('%主要状态影响%'),
					('%主要状态效果%'),
					('%状态状况%'),
					('%疾病免疫%'),
					('%治愈疾病%'),
					-- stat stage 和行动顺序相关文本需要固定为“能力值变化”和“行动”；
					-- 旧导入里常把 stat 翻成“属性”，把 action/order 翻成“技能”，这里集中防止回流。
					('%攻击攻击%'),
					('%提前技能%'),
					('%后技能%'),
					('%最后技能%'),
					('%能力值更改%'),
					('%能力值修饰符%'),
					('%双倍修饰符%'),
					('%随机属性提升%'),
					('%随机属性提升两级%'),
					('%所有属性提升%'),
					('%属性提升较多%'),
					('%属性增加%'),
					('%降低的属性%'),
					('%能力值增加%'),
					('%能力值超过+6%'),
					('%对手的属性得到提升%'),
					('%目标的属性在%'),
					('%在失速状态下在%'),
					-- 这些词来自直译或半替换文本；“精灵球”“出场”“可对战”已经是现有数据里更稳定的表达。
					-- 旧术语用 SQL 拼接表达，既能继续拦截导入回流，也避免源码重新出现已替换术语。
					('%生' || '物%'),
					('%有意识的精灵%'),
					('%有意识的队伍精灵%'),
					('%被送出%'),
					-- 野外效果里的 lead party member 统一翻成“同行队伍首位的精灵”，避免“主力/领头/排名第一”混用。
					('%主力精灵%'),
					('%领头的精灵%'),
					('%队伍中的第一只精灵%'),
					('%队伍中排名第一%'),
					('%成威力%'),
					('%功率和%'),
					('%功率随着%'),
					('%功率在%'),
					('%功率等于%'),
					('%功率是储存%'),
					('%功率由%'),
					('%功率范围%'),
					('%太阳光束的功率%'),
					('%基础力量%'),
					('%可变力量的技能%'),
					('%技能的力量%'),
					('%此技能的力量%'),
					('%该技能的力量%'),
					('%它们的力量为%'),
					('%力量提升%'),
					('%力量增强至%'),
					('%基于声音的技能的力量%'),
					('%下一个电属性技能的力量%'),
					('%力量会加倍%'),
					('%力量加倍%'),
					('%双倍力量%'),
					('%力量与使用者%'),
					('%力量与目标%'),
					('%力量从 60%'),
					('%力量就会增加%'),
					('%力量会增加%'),
					('%力量为%'),
					('%该使用的力量%'),
					('%连续使用，力量%'),
					('%其力量%'),
					('%双重力量%'),
					('%力量随着幸福度%'),
					('%点力量%'),
					('%80 力量%'),
					('%120 力量%'),
					('%权力%'),
					('%现有的疾病%'),
					('%力量都会加倍%'),
					('%具有双倍的力量%'),
					('%探测%'),
					('%检测%'),
					('%侦查%'),
					('%快速防御%'),
					('%广泛防御%'),
					('%快速守护%'),
					('%快速守卫%'),
					('%宽卫%'),
					('%守卫交换%'),
					('%防护交换%'),
					('%防御交换%'),
					('%心脏交换%'),
					('%力量交换%'),
					('%守卫分裂%'),
					('%力量分裂%'),
					('%狡猾的盾牌%'),
					('%尖刺盾牌%'),
					('%王之盾%'),
					('%保护和检测%'),
					('%保护或检测%'),
					('%检测或保护%'),
					('%探测或保护%'),
					('%探测或者防护%'),
					('%探测或防护%'),
					('%被保护或探测阻挡%'),
					('%接触技能可以击穿保护/检测%'),
					('%通过守住和看穿命中%'),
					('%忍耐仍然可以防止目标濒死%'),
					('%“看穿”、“忍耐”、“守住”%'),
					('%集中腰带%'),
					('%光幕、%'),
					('%光幕和反射%'),
					('%反射和光幕%'),
					('%魔法外套%'),
					('%磁力上升%'),
					('%隐形岩石%'),
					('%立即开枪%'),
					('%选择项目%'),
					('%恢复其项目%'),
					('%交换项目%'),
					('%功率取决于项目%'),
					('%该项目忽略%'),
					('%培训师%'),
					('%健身房%'),
					('%通灵能力%'),
					('%错误滑动%'),
					('%摇滚粉碎%'),
					('%电系技能%'),
					('%火系技能%'),
					('%水系技能%'),
					('%草系技能%'),
					('%冰系技能%'),
					('%钢系技能%'),
					('%幽灵系技能%'),
					('%电系伤害%'),
					('%火系伤害%'),
					('%水系伤害%'),
					('%草系伤害%'),
					('%冰系伤害%'),
					('%钢系伤害%'),
					('%幽灵系伤害%'),
					('%草系攻击%'),
					('%飞行型%'),
					('%幽灵型%'),
					('%格斗型%'),
					('%普通型%'),
					('%对战型%'),
					('%草系精灵%'),
					('%水系精灵%'),
					('%钢系精灵%'),
					('%钢铁对手%'),
					('%钢铁以外%')
			)
			select table_name, row_id, column_name, display_text
			from display_texts
			where exists (
				select 1
				from blocked_text_patterns blocked
				where display_text like blocked.pattern
			)
			order by table_name, row_id, column_name
			""".trimIndent(),
		)

		assertThat(correctedMachineTerms).isEmpty()
	}

	@Test
	fun `liquibase game skill effects do not keep generated terms`() {
		// 技能说明直接支撑管理端展示和战斗规则校验，短效果里的“下层”等机翻残留会比长说明更容易被用户看到。
		val machineSkillEffects = queryMaps(
			"""
			select skill_id, effect, short_effect, flavor_text
			from game_skill_detail
			where effect like '%正值或负值%'
				or short_effect like '%正值或负值%'
				or flavor_text like '%正值或负值%'
				or effect like '%加号或减号%'
				or short_effect like '%加号或减号%'
				or flavor_text like '%加号或减号%'
				or short_effect like '%下层%'
				or effect = '效果'
				or short_effect = '效果'
				or effect like '%定期伤害%'
				or short_effect like '%定期伤害%'
				or flavor_text like '%定期伤害%'
				or effect like '%英格莱恩%'
				or short_effect like '%英格莱恩%'
				or flavor_text like '%英格莱恩%'
				or short_effect ~ '^[0-9]+$'
				or effect like '%Ingrain%'
				or short_effect like '%Ingrain%'
				or flavor_text like '%Ingrain%'
				or effect like '%Baton Pass%'
				or short_effect like '%Baton Pass%'
				or flavor_text like '%Baton Pass%'
				or effect like '%警棍通行证%'
				or short_effect like '%警棍通行证%'
				or flavor_text like '%警棍通行证%'
				or lower(concat_ws(' ', effect, short_effect, flavor_text)) like any (array[
					'%protect%',
					'%detect%',
					'%chatter%',
					'%metronome%',
					'%mimic%',
					'%sketch%',
					'%struggle%',
					'%stomp%',
					'%steamroller%',
					'%bulk up%',
					'%counter%',
					'%endeavour%',
					'%metal burst%',
					'%mirror coat%',
					'%magnet rise%',
					'%covet%',
					'%knock off%',
					'%switcheroo%',
					'%thief%',
					'%trick%',
					'%psych up%',
					'%jaboca%',
					'%rowap%',
					'%techno blast%',
					'%judgment%',
					'%judgement%',
					'%meloetta%',
					'%aria%',
					'%pirouette%',
					'%rapid spin%',
					'%sticky web%',
					'%chatot%',
					'%terastallized%',
					'%tera属性%',
					'%iv%',
					'%stab%',
					'%stockpile%',
					'%gateway%',
					'%sunny park%',
					'%tri attack%',
					'%sp。%',
					'%sp.%',
					'%inflicts a burn%',
					'% hp%',
					'% pp%',
					'%损坏%',
					'%精度%',
					'%电源%',
					'%领域%',
					'%已经技能%',
					'%技能两次%',
					'%默认能力值%',
					'%绝对能力值%',
					'%攻击力能力值%',
					'%防御属性%',
					'%水利属性%',
					'%治疗目标%',
					'%治愈目标最大生命值%',
					'%地板%',
					'%黑暗精灵%',
					'%狂野的对战%',
					'%狂野对战%',
					'%充电回合%',
					'%电动地形%',
					'%电动技能%',
					'%火焰技能%',
					'%正常技能%'
				])
			order by skill_id
			""".trimIndent(),
		)

		assertThat(machineSkillEffects).isEmpty()
	}

	@Test
	fun `liquibase game skill effects use flavor text fallback when available`() {
		// 部分较新的技能暂时只有官方风味文本；这种情况下先回填到效果字段，避免管理端表格出现空白说明。
		val missingSkillEffects = queryMaps(
			"""
			select skill_id, effect, short_effect, flavor_text
			from game_skill_detail
			where flavor_text <> ''
				and (effect = '' or short_effect = '')
			order by skill_id
			""".trimIndent(),
		)

		assertThat(missingSkillEffects).isEmpty()
	}

	@Test
	fun `liquibase game skill effects do not leave display text blank`() {
		// 少数技能连风味文本也缺失时，也要给管理端一个保守的兜底说明，避免表格和详情页出现空白。
		val blankSkillEffects = queryMaps(
			"""
			select skill_id, effect, short_effect, flavor_text
			from game_skill_detail
			where effect = '' or short_effect = ''
			order by skill_id
			""".trimIndent(),
		)

		assertThat(blankSkillEffects).isEmpty()
	}

	@Test
	fun `liquibase game evolution triggers use skill terminology`() {
		// 进化触发器是管理端筛选项，“move” 在这里表示技能条件，不是位置移动。
		val generatedTriggerNames = queryMaps(
			"""
			select id, code, name
			from game_evolution_trigger
			where name like '%移动%'
			order by id
			""".trimIndent(),
		)

		assertThat(generatedTriggerNames).isEmpty()
	}

	@Test
	fun `liquibase type changing form names use localized type terms`() {
		val machineFormNames = queryMaps(
			"""
			select code, name, form_name
			from game_creature_form
			where (code like 'arceus-%' or code like 'silvally-%')
				and (
					form_name in ('普通的', '斗争', '中毒', '漏洞', '鬼', '电的', '精神', '黑暗的', '仙女')
					or name like '%（斗争）%'
					or name like '%（中毒）%'
					or name like '%（漏洞）%'
					or name like '%（鬼）%'
					or name like '%（电的）%'
					or name like '%（精神）%'
					or name like '%（黑暗的）%'
					or name like '%（仙女）%'
				)
			order by code
			""".trimIndent(),
		)

		assertThat(machineFormNames).isEmpty()
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
	fun `liquibase bigint primary keys do not use database generated values`() {
		// CosId 在应用写入前生成主键；这里同时排除 PostgreSQL identity 和序列默认值，
		// 防止实体策略与实际表结构分叉。
		val generatedPrimaryKeys = queryMaps(
			"""
			select columns.table_name, columns.column_name, columns.is_identity, columns.column_default
			from information_schema.table_constraints constraints
			join information_schema.key_column_usage key_columns
				on key_columns.constraint_schema = constraints.constraint_schema
				and key_columns.constraint_name = constraints.constraint_name
			join information_schema.columns columns
				on columns.table_schema = key_columns.table_schema
				and columns.table_name = key_columns.table_name
				and columns.column_name = key_columns.column_name
			where constraints.table_schema = 'public'
				and constraints.constraint_type = 'PRIMARY KEY'
				and columns.data_type = 'bigint'
				and (
					columns.is_identity = 'YES'
					or columns.column_default like 'nextval(%'
				)
			order by columns.table_name, columns.column_name
			""".trimIndent(),
		)

		assertThat(generatedPrimaryKeys).isEmpty()
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

		val passwordHash = admin["password_hash"] as String
		assertThat(passwordHash).startsWith("{bcrypt}\$2a\$12\$")
		assertThat(passwordHash.removePrefix("{bcrypt}")).hasSize(60)
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
		assertThat(roleCodes).containsExactly("battle-rules-admin", "battle-sandbox-runner", "game-data-admin", "system-admin")
	}

	@Test
	fun `published content pack owns current creatures and each enabled creature has one default skin`() {
		assertThat(
			queryStrings(
				"""
				select table_name
				from information_schema.tables
				where table_schema = 'public'
					and table_name in ('game_content_pack', 'game_creature_skin')
				order by table_name
				""".trimIndent(),
			),
		).containsExactly("game_content_pack", "game_creature_skin")

		assertThat(
			queryMaps(
				"""
				select code, status, content_kind
				from game_content_pack
				where status = 'PUBLISHED'
				""".trimIndent(),
			),
		).containsExactly(
			mapOf(
				"code" to "pokemon-reference",
				"status" to "PUBLISHED",
				"content_kind" to "REFERENCE",
			),
		)

		val invalidCreatures = queryMaps(
			"""
			select c.id, count(s.id) as default_skin_count
			from game_creature c
			left join game_creature_skin s
				on s.creature_id = c.id
				and s.default_skin = true
				and s.enabled = true
			where c.enabled = true
			group by c.id, c.content_pack_id
			having c.content_pack_id is null or count(s.id) <> 1
			""".trimIndent(),
		)
		assertThat(invalidCreatures).isEmpty()
	}

	@Test
	fun `published content pack owns player catalog resources and items have explicit usage`() {
		val resourcesWithoutPack = queryMaps(
			"""
			select 'ability' as resource, id from game_ability where content_pack_id is null
			union all select 'item', id from game_item where content_pack_id is null
			union all select 'skill', id from game_skill where content_pack_id is null
			union all select 'species', id from game_species where content_pack_id is null
			""".trimIndent(),
		)
		assertThat(resourcesWithoutPack).isEmpty()

		assertThat(
			queryMaps(
				"""
				select code, usage_type
				from game_item
				where code in ('master-ball', 'fire-stone', 'leftovers', 'big-nugget')
				order by code
				""".trimIndent(),
			),
		).containsExactly(
			mapOf("code" to "big-nugget", "usage_type" to "MATERIAL"),
			mapOf("code" to "fire-stone", "usage_type" to "EVOLUTION"),
			mapOf("code" to "leftovers", "usage_type" to "HELD"),
			mapOf("code" to "master-ball", "usage_type" to "CAPTURE"),
		)
	}

	@Test
	fun `match migration creates the complete persistent boundary`() {
		val tableNames = queryStrings(
			"""
			select table_name
			from information_schema.tables
			where table_schema = 'public'
			""".trimIndent(),
		)

		assertThat(tableNames).contains(
			"match_trainer",
			"match_sensitive_name_rule",
			"match_trainer_team",
			"match_trainer_team_member",
			"match_trainer_team_member_skill",
			"match_team_snapshot",
			"match_challenge",
			"match_game",
			"match_participant",
			"match_active_account_reservation",
			"match_turn_submission",
			"match_disclosure_ledger",
		)

		assertThat(
			queryStrings("select normalized_term from match_sensitive_name_rule order by id"),
		).containsExactly("admin", "administrator", "system", "root", "官方", "管理员")
	}

	@Test
	fun `trainer owns named teams with one active choice and skinned tera members`() {
		assertThat(
			queryStrings(
				"""
				select column_name
				from information_schema.columns
				where table_schema = 'public'
					and table_name = 'match_trainer_team'
				order by ordinal_position
				""".trimIndent(),
			),
		).contains("name", "name_key", "active")
		assertThat(
			queryStrings(
				"""
				select column_name
				from information_schema.columns
				where table_schema = 'public'
					and table_name = 'match_trainer_team_member'
				order by ordinal_position
				""".trimIndent(),
			),
		).contains("gender", "skin_id", "tera_element_id")
		assertThat(queryStrings("select indexname from pg_indexes where indexname = 'uk_match_trainer_team__active'"))
			.containsExactly("uk_match_trainer_team__active")
		assertThat(
			queryStrings(
				"""
				select constraint_name
				from information_schema.table_constraints
				where table_name = 'match_trainer_team_member'
					and constraint_type = 'FOREIGN KEY'
				""".trimIndent(),
			),
		).contains("fk_match_trainer_team_member__skin_id", "fk_match_trainer_team_member__tera_element_id")
	}

	@Test
	fun `ability shield declares ability ignore protection`() {
		val rules = queryMaps(
			"""
			select rule.effect_policy, rule.consumable
			from game_item item
			join battle_item_rule rule on rule.item_id = item.id and rule.enabled = true
			where item.code = 'ability-shield'
			""".trimIndent(),
		)

		assertThat(rules).containsExactly(
			mapOf("effect_policy" to "ability-ignore-protection", "consumable" to false),
		)
	}

	@Test
	fun `rusted sword and shield declare matching crowned form overrides`() {
		val rules = queryMaps(
			"""
			select item.code, rule.effect_policy, rule.consumable
			from game_item item
			join battle_item_rule rule on rule.item_id = item.id and rule.enabled = true
			where item.code in ('rusted-sword', 'rusted-shield')
			order by item.code
			""".trimIndent(),
		)

		assertThat(rules).containsExactly(
			mapOf(
				"code" to "rusted-shield",
				"effect_policy" to "creature-form-override-zamazenta-crowned",
				"consumable" to false,
			),
			mapOf(
				"code" to "rusted-sword",
				"effect_policy" to "creature-form-override-zacian-crowned",
				"consumable" to false,
			),
		)
	}

	@Test
	fun `utility umbrella declares sun and rain effect immunity`() {
		val rules = queryMaps(
			"""
			select rule.effect_policy, rule.consumable
			from game_item item
			join battle_item_rule rule on rule.item_id = item.id and rule.enabled = true
			where item.code = 'utility-umbrella'
			""".trimIndent(),
		)

		assertThat(rules).containsExactly(
			mapOf("effect_policy" to "sun-rain-effect-immunity", "consumable" to false),
		)
	}

	@Test
	fun `reactive stat copy items declare executable policies`() {
		val rules = queryMaps(
			"""
			select item.code, rule.effect_policy, rule.consumable
			from game_item item
			join battle_item_rule rule on rule.item_id = item.id and rule.enabled = true
			where item.code in ('adrenaline-orb', 'mirror-herb')
			order by item.code
			""".trimIndent(),
		)

		assertThat(rules).containsExactly(
			mapOf(
				"code" to "adrenaline-orb",
				"effect_policy" to "ability-stat-reduction-speed-stage-plus-one",
				"consumable" to true,
			),
			mapOf(
				"code" to "mirror-herb",
				"effect_policy" to "opponent-positive-stat-stage-copy",
				"consumable" to true,
			),
		)
	}

	@Test
	fun `forced switch items declare executable policies`() {
		val rules = queryMaps(
			"""
			select item.code, rule.effect_policy, rule.consumable
			from game_item item
			join battle_item_rule rule on rule.item_id = item.id and rule.enabled = true
			where item.code in ('eject-button', 'eject-pack', 'red-card')
			order by item.code
			""".trimIndent(),
		)

		assertThat(rules).containsExactly(
			mapOf("code" to "eject-button", "effect_policy" to "damaged-force-self-switch", "consumable" to true),
			mapOf("code" to "eject-pack", "effect_policy" to "negative-stat-stage-force-self-switch", "consumable" to true),
			mapOf("code" to "red-card", "effect_policy" to "damaged-force-attacker-switch", "consumable" to true),
		)
	}

	@Test
	fun `mental herb and destiny knot declare volatile status policies`() {
		val rules = queryMaps(
			"""
			select item.code, rule.effect_policy, rule.consumable
			from game_item item
			join battle_item_rule rule on rule.item_id = item.id and rule.enabled = true
			where item.code in ('mental-herb', 'destiny-knot')
			order by item.code
			""".trimIndent(),
		)

		assertThat(rules).containsExactly(
			mapOf("code" to "destiny-knot", "effect_policy" to "infatuation-reflect-to-source", "consumable" to false),
			mapOf("code" to "mental-herb", "effect_policy" to "volatile-status-cure-mental-herb", "consumable" to true),
		)
	}

	@Test
	fun `booster energy declares highest stat activation policy`() {
		val rules = queryMaps(
			"""
			select rule.effect_policy, rule.consumable
			from game_item item
			join battle_item_rule rule on rule.item_id = item.id and rule.enabled = true
			where item.code = 'booster-energy'
			""".trimIndent(),
		)

		assertThat(rules).containsExactly(
			mapOf(
				"effect_policy" to "highest-stat-booster-abilities-protosynthesis-quark-drive",
				"consumable" to true,
			),
		)
	}

	@Test
	fun `status and stat immunity abilities declare executable policies`() {
		val abilityCodes = queryStrings(
			"""
			select distinct ability.code
			from game_ability ability
			join battle_ability_rule rule on rule.ability_id = ability.id and rule.enabled = true
			where ability.code in (
			  'big-pecks', 'clear-body', 'full-metal-body', 'hyper-cutter', 'keen-eye', 'white-smoke',
			  'immunity', 'limber', 'insomnia', 'vital-spirit', 'magma-armor', 'water-veil',
			  'own-tempo', 'inner-focus', 'oblivious', 'purifying-salt'
			)
			order by ability.code
			""".trimIndent(),
		)

		assertThat(abilityCodes).hasSize(16)
	}

	@Test
	fun `conditional stat abilities declare executable policies`() {
		val abilityCodes = queryStrings(
			"""
			select distinct ability.code
			from game_ability ability
			join battle_ability_rule rule on rule.ability_id = ability.id and rule.enabled = true
			where ability.code in (
			  'gorilla-tactics', 'hustle', 'flare-boost', 'toxic-boost', 'marvel-scale',
			  'quick-feet', 'solar-power', 'flower-gift', 'hadron-engine', 'orichalcum-pulse', 'defeatist'
			)
			order by ability.code
			""".trimIndent(),
		)

		assertThat(abilityCodes).hasSize(11)
	}

	@Test
	fun `received damage abilities declare executable policies`() {
		val abilityCodes = queryStrings(
			"""
			select distinct ability.code
			from game_ability ability
			join battle_ability_rule rule on rule.ability_id = ability.id and rule.enabled = true
			where ability.code in (
			  'flame-body', 'poison-point', 'gooey', 'tangling-hair', 'stamina', 'weak-armor',
			  'water-compaction', 'steam-engine', 'justified', 'rattled', 'thermal-exchange'
			)
			order by ability.code
			""".trimIndent(),
		)

		assertThat(abilityCodes).hasSize(11)
	}

	@Test
	fun `element damage abilities declare executable policies`() {
		val abilityCodes = queryStrings(
			"""
			select distinct ability.code
			from game_ability ability
			join battle_ability_rule rule on rule.ability_id = ability.id and rule.enabled = true
			where ability.code in (
			  'dark-aura', 'fairy-aura', 'steely-spirit', 'water-bubble', 'heatproof',
			  'thick-fat', 'purifying-salt', 'neuroforce', 'tinted-lens', 'sniper'
			)
			order by ability.code
			""".trimIndent(),
		)

		assertThat(abilityCodes).hasSize(10)
	}

	@Test
	fun `accuracy abilities declare executable policies`() {
		val abilityCodes = queryStrings(
			"""
			select distinct ability.code
			from game_ability ability
			join battle_ability_rule rule on rule.ability_id = ability.id and rule.enabled = true
			where ability.code in (
			  'compound-eyes', 'illuminate', 'victory-star', 'hustle', 'sand-veil',
			  'snow-cloak', 'tangled-feet', 'no-guard', 'wonder-skin', 'keen-eye'
			)
			order by ability.code
			""".trimIndent(),
		)

		assertThat(abilityCodes).hasSize(10)
	}

	@Test
	fun `switch in stat abilities declare executable policies`() {
		val abilityCodes = queryStrings(
			"""
			select distinct ability.code
			from game_ability ability
			join battle_ability_rule rule on rule.ability_id = ability.id and rule.enabled = true
			where ability.code in ('intrepid-sword', 'dauntless-shield', 'supersweet-syrup')
			order by ability.code
			""".trimIndent(),
		)

		assertThat(abilityCodes).hasSize(3)
	}

	@Test
	fun `faint boost abilities declare executable policies`() {
		val abilityCodes = queryStrings(
			"""
			select distinct ability.code
			from game_ability ability
			join battle_ability_rule rule on rule.ability_id = ability.id and rule.enabled = true
			where ability.code in (
			  'moxie', 'chilling-neigh', 'grim-neigh', 'as-one-glastrier',
			  'as-one-spectrier', 'beast-boost', 'soul-heart'
			)
			order by ability.code
			""".trimIndent(),
		)

		assertThat(abilityCodes).hasSize(7)
	}

	@Test
	fun `end turn abilities declare executable policies`() {
		val abilityCodes = queryStrings(
			"""
			select distinct ability.code
			from game_ability ability
			join battle_ability_rule rule on rule.ability_id = ability.id and rule.enabled = true
			where ability.code in (
			  'speed-boost', 'shed-skin', 'hydration', 'healer', 'bad-dreams',
			  'poison-heal', 'solar-power', 'dry-skin', 'moody'
			)
			order by ability.code
			""".trimIndent(),
		)

		assertThat(abilityCodes).hasSize(9)
	}

	@Test
	fun `eviolite declares evolvable defense multipliers`() {
		val rules = queryMaps(
			"""
			select rule.effect_policy, rule.consumable
			from game_item item
			join battle_item_rule rule on rule.item_id = item.id and rule.enabled = true
			where item.code = 'eviolite'
			""".trimIndent(),
		)

		assertThat(rules).containsExactly(
			mapOf("effect_policy" to "evolvable-defense-special-defense-one-and-half", "consumable" to false),
		)
	}

	@Test
	fun `metronome item declares consecutive skill damage boost`() {
		val rules = queryMaps(
			"""
			select rule.effect_policy, rule.consumable
			from game_item item
			join battle_item_rule rule on rule.item_id = item.id and rule.enabled = true
			where item.code = 'metronome'
			""".trimIndent(),
		)

		assertThat(rules).containsExactly(
			mapOf("effect_policy" to "consecutive-skill-damage-boost-twenty-percent", "consumable" to false),
		)
	}

	@Test
	fun `covert cloak declares damaging skill secondary immunity`() {
		val rules = queryMaps(
			"""
			select rule.effect_policy, rule.consumable
			from game_item item
			join battle_item_rule rule on rule.item_id = item.id and rule.enabled = true
			where item.code = 'covert-cloak'
			""".trimIndent(),
		)

		assertThat(rules).containsExactly(
			mapOf("effect_policy" to "damaging-skill-secondary-effect-immunity", "consumable" to false),
		)
	}

	@Test
	fun `clear amulet and white herb declare stat protection rules`() {
		val rules = queryMaps(
			"""
			select item.code, rule.effect_policy, rule.consumable
			from game_item item
			join battle_item_rule rule on rule.item_id = item.id and rule.enabled = true
			where item.code in ('clear-amulet', 'white-herb')
			order by item.code
			""".trimIndent(),
		)

		assertThat(rules).containsExactly(
			mapOf("code" to "clear-amulet", "effect_policy" to "opponent-stat-stage-reduction-immunity", "consumable" to false),
			mapOf("code" to "white-herb", "effect_policy" to "negative-stat-stage-reset", "consumable" to true),
		)
	}

	@Test
	fun `micle berry declares next skill accuracy boost`() {
		val rules = queryMaps(
			"""
			select rule.effect_policy, rule.consumable
			from game_item item
			join battle_item_rule rule on rule.item_id = item.id and rule.enabled = true
			where item.code = 'micle-berry'
			  and rule.effect_policy <> 'berry-marker'
			""".trimIndent(),
		)

		assertThat(rules).containsExactly(
			mapOf("effect_policy" to "low-hp-next-skill-accuracy-six-fifths", "consumable" to true),
		)
	}

	@Test
	fun `starf berry declares random battle stat boost`() {
		val rules = queryMaps(
			"""
			select rule.effect_policy, rule.consumable
			from game_item item
			join battle_item_rule rule on rule.item_id = item.id and rule.enabled = true
			where item.code = 'starf-berry'
			  and rule.effect_policy <> 'berry-marker'
			""".trimIndent(),
		)

		assertThat(rules).containsExactly(
			mapOf("effect_policy" to "low-hp-random-battle-stat-stage-plus-two", "consumable" to true),
		)
	}

	@Test
	fun `flavor berries declare nature sensitive healing rules`() {
		val rules = queryMaps(
			"""
			select item.code, rule.effect_policy, rule.consumable
			from game_item item
			join battle_item_rule rule on rule.item_id = item.id and rule.enabled = true
			where item.code in ('figy-berry', 'wiki-berry', 'mago-berry', 'aguav-berry', 'iapapa-berry')
			  and rule.effect_policy <> 'berry-marker'
			order by item.code
			""".trimIndent(),
		)

		assertThat(rules).containsExactly(
			mapOf("code" to "aguav-berry", "effect_policy" to "quarter-hp-third-heal-confuse-special-defense", "consumable" to true),
			mapOf("code" to "figy-berry", "effect_policy" to "quarter-hp-third-heal-confuse-attack", "consumable" to true),
			mapOf("code" to "iapapa-berry", "effect_policy" to "quarter-hp-third-heal-confuse-defense", "consumable" to true),
			mapOf("code" to "mago-berry", "effect_policy" to "quarter-hp-third-heal-confuse-speed", "consumable" to true),
			mapOf("code" to "wiki-berry", "effect_policy" to "quarter-hp-third-heal-confuse-special-attack", "consumable" to true),
		)
	}

	@Test
	fun `zoom lens declares target acted accuracy boost`() {
		val rules = queryMaps(
			"""
			select rule.effect_policy, rule.consumable
			from game_item item
			join battle_item_rule rule on rule.item_id = item.id and rule.enabled = true
			where item.code = 'zoom-lens'
			""".trimIndent(),
		)

		assertThat(rules).containsExactly(
			mapOf("effect_policy" to "accuracy-multiplier-six-fifths-after-target-acted", "consumable" to false),
		)
	}

	@Test
	fun `air balloon declares airborne until damaged rule`() {
		val rules = queryMaps(
			"""
			select rule.effect_policy, rule.consumable
			from game_item item
			join battle_item_rule rule on rule.item_id = item.id and rule.enabled = true
			where item.code = 'air-balloon'
			""".trimIndent(),
		)

		assertThat(rules).containsExactly(
			mapOf("effect_policy" to "airborne-until-damaged", "consumable" to true),
		)
	}

	@Test
	fun `iron ball and ring target declare grounding and immunity rules`() {
		val rules = queryMaps(
			"""
			select item.code, rule.effect_policy, rule.consumable
			from game_item item
			join battle_item_rule rule on rule.item_id = item.id and rule.enabled = true
			where item.code in ('iron-ball', 'ring-target')
			order by item.code, rule.sort_order
			""".trimIndent(),
		)

		assertThat(rules).containsExactly(
			mapOf("code" to "iron-ball", "effect_policy" to "force-grounded", "consumable" to false),
			mapOf("code" to "iron-ball", "effect_policy" to "speed-multiplier-half", "consumable" to false),
			mapOf("code" to "ring-target", "effect_policy" to "type-immunity-suppression", "consumable" to false),
		)
	}

	@Test
	fun `room service declares trick room speed reduction`() {
		val rules = queryMaps(
			"""
			select rule.effect_policy, rule.consumable
			from game_item item
			join battle_item_rule rule on rule.item_id = item.id and rule.enabled = true
			where item.code = 'room-service'
			""".trimIndent(),
		)

		assertThat(rules).containsExactly(
			mapOf("effect_policy" to "field-speed-order-trick-room-speed-stage-minus-one", "consumable" to true),
		)
	}

	@Test
	fun `normal gem declares consumable normal damage boost`() {
		val rules = queryMaps(
			"""
			select rule.effect_policy, rule.consumable
			from game_item item
			join battle_item_rule rule on rule.item_id = item.id and rule.enabled = true
			where item.code = 'normal-gem'
			""".trimIndent(),
		)

		assertThat(rules).containsExactly(
			mapOf("effect_policy" to "consumable-element-damage-boost-normal-thirty-percent", "consumable" to true),
		)
	}

	@Test
	fun `species held items declare restricted stat and damage rules`() {
		val rules = queryMaps(
			"""
			select item.code, rule.effect_policy
			from game_item item
			join battle_item_rule rule on rule.item_id = item.id and rule.enabled = true
			where item.code in (
			  'adamant-orb', 'lustrous-orb', 'griseous-orb', 'soul-dew',
			  'deep-sea-tooth', 'deep-sea-scale', 'light-ball',
			  'wellspring-mask', 'hearthflame-mask', 'cornerstone-mask'
			)
			order by item.code
			""".trimIndent(),
		)

		assertThat(rules).hasSize(10)
		assertThat(rules.map { it["code"] }).containsExactly(
			"adamant-orb", "cornerstone-mask", "deep-sea-scale", "deep-sea-tooth", "griseous-orb",
			"hearthflame-mask", "light-ball", "lustrous-orb", "soul-dew", "wellspring-mask",
		)
	}

	@Test
	fun `focus band declares non consumable random fatal damage survival`() {
		val rules = queryMaps(
			"""
			select rule.effect_policy, rule.consumable
			from game_item item
			join battle_item_rule rule on rule.item_id = item.id and rule.enabled = true
			where item.code = 'focus-band'
			""".trimIndent(),
		)

		assertThat(rules).containsExactly(
			mapOf("effect_policy" to "random-fatal-damage-survival-ten-percent", "consumable" to false),
		)
	}

	@Test
	fun `action order items declare random forced last and low hp rules`() {
		val rules = queryMaps(
			"""
			select item.code, rule.effect_policy, rule.consumable
			from game_item item
			join battle_item_rule rule on rule.item_id = item.id and rule.enabled = true
			where item.code in ('quick-claw', 'lagging-tail', 'custap-berry')
			  and rule.effect_policy <> 'berry-marker'
			order by item.code
			""".trimIndent(),
		)

		assertThat(rules).containsExactly(
			mapOf("code" to "custap-berry", "effect_policy" to "low-hp-action-order-boost-quarter", "consumable" to true),
			mapOf("code" to "lagging-tail", "effect_policy" to "forced-last-action-order", "consumable" to false),
			mapOf("code" to "quick-claw", "effect_policy" to "random-action-order-boost-twenty-percent", "consumable" to false),
		)
	}

	@Test
	fun `drain and binding items declare their lifecycle modifiers`() {
		val rules = queryMaps(
			"""
			select item.code, rule.effect_policy
			from game_item item
			join battle_item_rule rule on rule.item_id = item.id and rule.enabled = true
			where item.code in ('big-root', 'binding-band', 'grip-claw')
			order by item.code
			""".trimIndent(),
		)

		assertThat(rules).containsExactly(
			mapOf("code" to "big-root", "effect_policy" to "drain-healing-multiplier-thirteen-tenths"),
			mapOf("code" to "binding-band", "effect_policy" to "binding-damage-denominator-six"),
			mapOf("code" to "grip-claw", "effect_policy" to "binding-duration-seven"),
		)
	}

	@Test
	fun `flinch held items add ten percent chance to eligible damaging skills`() {
		val flinchItemRules = queryMaps(
			"""
			select item.code, rule.trigger_timing, rule.effect_policy, rule.consumable
			from game_item item
			join battle_item_rule rule on rule.item_id = item.id and rule.enabled = true
			where item.code in ('kings-rock', 'razor-fang')
			order by item.code
			""".trimIndent(),
		)

		assertThat(flinchItemRules).containsExactly(
			mapOf("code" to "kings-rock", "trigger_timing" to "AFTER_SKILL", "effect_policy" to "additional-flinch-chance-ten-percent", "consumable" to false),
			mapOf("code" to "razor-fang", "trigger_timing" to "AFTER_SKILL", "effect_policy" to "additional-flinch-chance-ten-percent", "consumable" to false),
		)
	}

	@Test
	fun `blunder policy triggers after an accuracy miss`() {
		val blunderPolicyRules = queryMaps(
			"""
			select rule.trigger_timing, rule.effect_policy, rule.consumable
			from game_item item
			join battle_item_rule rule on rule.item_id = item.id and rule.enabled = true
			where item.code = 'blunder-policy'
			""".trimIndent(),
		)

		assertThat(blunderPolicyRules).containsExactly(
			mapOf(
				"trigger_timing" to "AFTER_ACCURACY_MISS",
				"effect_policy" to "accuracy-miss-speed-stage-plus-two",
				"consumable" to true,
			),
		)
	}

	@Test
	fun `throat spray triggers after a successful sound skill`() {
		val throatSprayRules = queryMaps(
			"""
			select rule.trigger_timing, rule.effect_policy, rule.consumable
			from game_item item
			join battle_item_rule rule on rule.item_id = item.id and rule.enabled = true
			where item.code = 'throat-spray'
			""".trimIndent(),
		)

		assertThat(throatSprayRules).containsExactly(
			mapOf(
				"trigger_timing" to "AFTER_SKILL",
				"effect_policy" to "successful-sound-skill-special-attack-stage-plus-one",
				"consumable" to true,
			),
		)
	}

	@Test
	fun `reactive stat items trigger after matching received damage`() {
		val reactiveItemRules = queryMaps(
			"""
			select item.code, rule.trigger_timing, rule.effect_policy, rule.consumable
			from game_item item
			join battle_item_rule rule on rule.item_id = item.id and rule.enabled = true
			where item.code in ('absorb-bulb', 'cell-battery', 'luminous-moss', 'snowball', 'weakness-policy')
			order by item.code
			""".trimIndent(),
		)

		assertThat(reactiveItemRules).containsExactly(
			mapOf("code" to "absorb-bulb", "trigger_timing" to "AFTER_DAMAGE", "effect_policy" to "received-water-special-attack-stage-plus-one", "consumable" to true),
			mapOf("code" to "cell-battery", "trigger_timing" to "AFTER_DAMAGE", "effect_policy" to "received-electric-attack-stage-plus-one", "consumable" to true),
			mapOf("code" to "luminous-moss", "trigger_timing" to "AFTER_DAMAGE", "effect_policy" to "received-water-special-defense-stage-plus-one", "consumable" to true),
			mapOf("code" to "snowball", "trigger_timing" to "AFTER_DAMAGE", "effect_policy" to "received-ice-attack-stage-plus-one", "consumable" to true),
			mapOf("code" to "weakness-policy", "trigger_timing" to "AFTER_DAMAGE", "effect_policy" to "received-super-effective-attack-special-attack-stage-plus-two", "consumable" to true),
		)
	}

	@Test
	fun `terrain seeds boost matching defense stat while grounded`() {
		val terrainSeedRules = queryMaps(
			"""
			select item.code, rule.trigger_timing, rule.effect_policy, rule.consumable
			from game_item item
			join battle_item_rule rule on rule.item_id = item.id and rule.enabled = true
			where item.code in ('electric-seed', 'grassy-seed', 'misty-seed', 'psychic-seed')
			order by item.code
			""".trimIndent(),
		)

		assertThat(terrainSeedRules).containsExactly(
			mapOf("code" to "electric-seed", "trigger_timing" to "FIELD_CHANGE", "effect_policy" to "terrain-electric-defense-stage-plus-one", "consumable" to true),
			mapOf("code" to "grassy-seed", "trigger_timing" to "FIELD_CHANGE", "effect_policy" to "terrain-grassy-defense-stage-plus-one", "consumable" to true),
			mapOf("code" to "misty-seed", "trigger_timing" to "FIELD_CHANGE", "effect_policy" to "terrain-misty-special-defense-stage-plus-one", "consumable" to true),
			mapOf("code" to "psychic-seed", "trigger_timing" to "FIELD_CHANGE", "effect_policy" to "terrain-psychic-special-defense-stage-plus-one", "consumable" to true),
		)
	}

	@Test
	fun `assault vest raises special defense and prevents status skill selection`() {
		val assaultVestRules = queryMaps(
			"""
			select rule.trigger_timing, rule.effect_policy, rule.consumable
			from game_item item
			join battle_item_rule rule on rule.item_id = item.id and rule.enabled = true
			where item.code = 'assault-vest'
			order by rule.effect_policy
			""".trimIndent(),
		)

		assertThat(assaultVestRules).containsExactly(
			mapOf("trigger_timing" to "BEFORE_DAMAGE", "effect_policy" to "special-defense-stat-one-and-half", "consumable" to false),
			mapOf("trigger_timing" to "BEFORE_ACTION", "effect_policy" to "status-skill-selection-restriction", "consumable" to false),
		)
	}

	@Test
	fun `lansat berry starts low hp critical boost`() {
		val lansatBerryRules = queryMaps(
			"""
			select rule.trigger_timing, rule.effect_policy, rule.consumable
			from game_item item
			join battle_item_rule rule on rule.item_id = item.id and rule.enabled = true
			where item.code = 'lansat-berry'
			  and rule.effect_policy <> 'berry-marker'
			""".trimIndent(),
		)

		assertThat(lansatBerryRules).containsExactly(
			mapOf(
				"trigger_timing" to "AFTER_DAMAGE",
				"effect_policy" to "low-hp-critical-hit-stage-plus-two",
				"consumable" to true,
			),
		)
	}

	@Test
	fun `low hp stat berries raise their matching stat`() {
		val statBerryRules = queryMaps(
			"""
			select item.code, rule.trigger_timing, rule.effect_policy, rule.consumable
			from game_item item
			join battle_item_rule rule on rule.item_id = item.id and rule.enabled = true
			where item.code in ('liechi-berry', 'ganlon-berry', 'salac-berry', 'petaya-berry', 'apicot-berry')
			  and rule.effect_policy <> 'berry-marker'
			order by item.code
			""".trimIndent(),
		)

		assertThat(statBerryRules).containsExactly(
			mapOf("code" to "apicot-berry", "trigger_timing" to "AFTER_DAMAGE", "effect_policy" to "low-hp-special-defense-stage-plus-one", "consumable" to true),
			mapOf("code" to "ganlon-berry", "trigger_timing" to "AFTER_DAMAGE", "effect_policy" to "low-hp-defense-stage-plus-one", "consumable" to true),
			mapOf("code" to "liechi-berry", "trigger_timing" to "AFTER_DAMAGE", "effect_policy" to "low-hp-attack-stage-plus-one", "consumable" to true),
			mapOf("code" to "petaya-berry", "trigger_timing" to "AFTER_DAMAGE", "effect_policy" to "low-hp-special-attack-stage-plus-one", "consumable" to true),
			mapOf("code" to "salac-berry", "trigger_timing" to "AFTER_DAMAGE", "effect_policy" to "low-hp-speed-stage-plus-one", "consumable" to true),
		)
	}

	@Test
	fun `shed shell ignores opponent switch restrictions`() {
		val shedShellRules = queryMaps(
			"""
			select rule.trigger_timing, rule.effect_policy, rule.consumable
			from game_item item
			join battle_item_rule rule on rule.item_id = item.id and rule.enabled = true
			where item.code = 'shed-shell'
			""".trimIndent(),
		)

		assertThat(shedShellRules).containsExactly(
			mapOf(
				"trigger_timing" to "BEFORE_ACTION",
				"effect_policy" to "switch-restriction-immunity",
				"consumable" to false,
			),
		)
	}

	@Test
	fun `wide lens and bright powder modify ordinary accuracy`() {
		val accuracyItemRules = queryMaps(
			"""
			select item.code, rule.trigger_timing, rule.effect_policy, rule.consumable
			from game_item item
			join battle_item_rule rule on rule.item_id = item.id and rule.enabled = true
			where item.code in ('bright-powder', 'wide-lens')
			order by item.code
			""".trimIndent(),
		)

		assertThat(accuracyItemRules).containsExactly(
			mapOf(
				"code" to "bright-powder",
				"trigger_timing" to "BEFORE_HIT",
				"effect_policy" to "opponent-accuracy-multiplier-nine-tenths",
				"consumable" to false,
			),
			mapOf(
				"code" to "wide-lens",
				"trigger_timing" to "BEFORE_HIT",
				"effect_policy" to "accuracy-multiplier-eleven-tenths",
				"consumable" to false,
			),
		)
	}

	@Test
	fun `black sludge heals poison holders and damages other holders`() {
		val blackSludgeRules = queryMaps(
			"""
			select rule.trigger_timing, rule.effect_policy, rule.consumable
			from game_item item
			join battle_item_rule rule on rule.item_id = item.id and rule.enabled = true
			where item.code = 'black-sludge'
			order by rule.effect_policy
			""".trimIndent(),
		)

		assertThat(blackSludgeRules).containsExactly(
			mapOf(
				"trigger_timing" to "HELD_END_TURN",
				"effect_policy" to "held-end-turn-damage-non-poison-eighth",
				"consumable" to false,
			),
			mapOf(
				"trigger_timing" to "HELD_END_TURN",
				"effect_policy" to "held-end-turn-heal-poison-sixteenth",
				"consumable" to false,
			),
		)
	}

	@Test
	fun `loaded dice narrows standard multi hit skills to four or five hits`() {
		val loadedDiceRules = queryMaps(
			"""
			select rule.trigger_timing, rule.effect_policy, rule.consumable
			from game_item item
			join battle_item_rule rule on rule.item_id = item.id and rule.enabled = true
			where item.code = 'loaded-dice'
			""".trimIndent(),
		)

		assertThat(loadedDiceRules).containsExactly(
			mapOf(
				"trigger_timing" to "BEFORE_HIT",
				"effect_policy" to "standard-multi-hit-count-four-to-five",
				"consumable" to false,
			),
		)
	}

	@Test
	fun `heavy duty boots ignore all entry hazards`() {
		val heavyDutyBootsRules = queryMaps(
			"""
			select rule.trigger_timing, rule.effect_policy, rule.consumable
			from game_item item
			join battle_item_rule rule on rule.item_id = item.id and rule.enabled = true
			where item.code = 'heavy-duty-boots'
			""".trimIndent(),
		)

		assertThat(heavyDutyBootsRules).containsExactly(
			mapOf(
				"trigger_timing" to "ON_SWITCH_IN",
				"effect_policy" to "entry-hazard-immunity",
				"consumable" to false,
			),
		)
	}

	@Test
	fun `safety goggles block powder skills and sandstorm damage`() {
		val safetyGogglesRules = queryMaps(
			"""
			select rule.trigger_timing, rule.effect_policy, rule.consumable
			from game_item item
			join battle_item_rule rule on rule.item_id = item.id and rule.enabled = true
			where item.code = 'safety-goggles'
			order by rule.effect_policy
			""".trimIndent(),
		)

		assertThat(safetyGogglesRules).containsExactly(
			mapOf(
				"trigger_timing" to "BEFORE_HIT",
				"effect_policy" to "powder-skill-immunity",
				"consumable" to false,
			),
			mapOf(
				"trigger_timing" to "HELD_END_TURN",
				"effect_policy" to "weather-damage-immunity-sandstorm",
				"consumable" to false,
			),
		)
	}

	@Test
	fun `critical hit held items add one critical stage`() {
		val criticalItemRules = queryMaps(
			"""
			select item.code, rule.trigger_timing, rule.effect_policy, rule.consumable
			from game_item item
			join battle_item_rule rule on rule.item_id = item.id and rule.enabled = true
			where item.code in ('razor-claw', 'scope-lens')
			order by item.code
			""".trimIndent(),
		)

		assertThat(criticalItemRules).containsExactly(
			mapOf(
				"code" to "razor-claw",
				"trigger_timing" to "BEFORE_DAMAGE",
				"effect_policy" to "critical-hit-stage-plus-one",
				"consumable" to false,
			),
			mapOf(
				"code" to "scope-lens",
				"trigger_timing" to "BEFORE_DAMAGE",
				"effect_policy" to "critical-hit-stage-plus-one",
				"consumable" to false,
			),
		)
	}

	@Test
	fun `status orbs apply their major status at end of turn`() {
		val statusOrbRules = queryMaps(
			"""
			select item.code, rule.trigger_timing, rule.effect_policy, rule.consumable
			from game_item item
			join battle_item_rule rule on rule.item_id = item.id and rule.enabled = true
			where item.code in ('flame-orb', 'toxic-orb')
			order by item.code
			""".trimIndent(),
		)

		assertThat(statusOrbRules).containsExactly(
			mapOf(
				"code" to "flame-orb",
				"trigger_timing" to "HELD_END_TURN",
				"effect_policy" to "held-end-turn-major-status-burn",
				"consumable" to false,
			),
			mapOf(
				"code" to "toxic-orb",
				"trigger_timing" to "HELD_END_TURN",
				"effect_policy" to "held-end-turn-major-status-bad-poison",
				"consumable" to false,
			),
		)
	}

	@Test
	fun `choice band and specs combine skill lock with matching damage boost`() {
		val choiceRules = queryMaps(
			"""
			select item.code, rule.trigger_timing, rule.effect_policy, rule.consumable
			from game_item item
			join battle_item_rule rule on rule.item_id = item.id and rule.enabled = true
			where item.code in ('choice-band', 'choice-specs')
			order by item.code, rule.effect_policy
			""".trimIndent(),
		)

		assertThat(choiceRules).containsExactly(
			mapOf(
				"code" to "choice-band",
				"trigger_timing" to "BEFORE_ACTION",
				"effect_policy" to "choice-skill-lock",
				"consumable" to false,
			),
			mapOf(
				"code" to "choice-band",
				"trigger_timing" to "BEFORE_DAMAGE",
				"effect_policy" to "damage-class-power-boost-physical-50",
				"consumable" to false,
			),
			mapOf(
				"code" to "choice-specs",
				"trigger_timing" to "BEFORE_ACTION",
				"effect_policy" to "choice-skill-lock",
				"consumable" to false,
			),
			mapOf(
				"code" to "choice-specs",
				"trigger_timing" to "BEFORE_DAMAGE",
				"effect_policy" to "damage-class-power-boost-special-50",
				"consumable" to false,
			),
		)
	}

	@Test
	fun `all elemental plates have executable damage boost rules`() {
		val plateRules = queryMaps(
			"""
			select item.code, rule.trigger_timing, rule.effect_policy, rule.consumable
			from game_item item
			join battle_item_rule rule on rule.item_id = item.id and rule.enabled = true
			where item.code in (
				'flame-plate', 'splash-plate', 'zap-plate', 'meadow-plate', 'icicle-plate',
				'fist-plate', 'toxic-plate', 'earth-plate', 'sky-plate', 'mind-plate',
				'insect-plate', 'stone-plate', 'spooky-plate', 'draco-plate', 'dread-plate',
				'iron-plate', 'pixie-plate'
			)
			order by item.code
			""".trimIndent(),
		)

		val expectedElementByPlate = mapOf(
			"draco-plate" to "dragon",
			"dread-plate" to "dark",
			"earth-plate" to "ground",
			"fist-plate" to "fighting",
			"flame-plate" to "fire",
			"icicle-plate" to "ice",
			"insect-plate" to "bug",
			"iron-plate" to "steel",
			"meadow-plate" to "grass",
			"mind-plate" to "psychic",
			"pixie-plate" to "fairy",
			"sky-plate" to "flying",
			"splash-plate" to "water",
			"spooky-plate" to "ghost",
			"stone-plate" to "rock",
			"toxic-plate" to "poison",
			"zap-plate" to "electric",
		)
		assertThat(plateRules).containsExactlyElementsOf(
			expectedElementByPlate.map { (plateCode, elementCode) ->
				mapOf(
					"code" to plateCode,
					"trigger_timing" to "BEFORE_DAMAGE",
					"effect_policy" to "element-damage-boost-$elementCode",
					"consumable" to false,
				)
			},
		)
	}

	@Test
	fun `every selectable battle resource has an enabled executable rule`() {
		val skillsWithoutRules = queryStrings(
			"""
			select distinct s.code
			from game_skill s
			join game_creature_skill_learn learn on learn.skill_id = s.id
			left join battle_skill_rule rule on rule.skill_id = s.id and rule.enabled = true
			where s.enabled = true and rule.id is null
			order by s.code
			""".trimIndent(),
		)
		val abilitiesWithoutRules = queryStrings(
			"""
			select distinct ability.code
			from game_ability ability
			join game_creature_ability binding on binding.ability_id = ability.id
			left join battle_ability_rule rule on rule.ability_id = ability.id and rule.enabled = true
			where ability.enabled = true and rule.id is null
			order by ability.code
			""".trimIndent(),
		)
		val heldItemsWithoutRules = queryStrings(
			"""
			select item.code
			from game_item item
			left join battle_item_rule rule on rule.item_id = item.id and rule.enabled = true
			where item.enabled = true and item.usage_type = 'HELD' and rule.id is null
			order by item.code
			""".trimIndent(),
		)

		assertThat(skillsWithoutRules).describedAs("缺少技能执行规则").isEmpty()
		assertThat(heldItemsWithoutRules).describedAs("缺少携带道具执行规则").isEmpty()
		assertThat(abilitiesWithoutRules).describedAs("缺少特性执行规则").isEmpty()
	}

	private fun queryStrings(sql: String): List<String> =
		query(sql) { resultSet ->
			resultSet.getString(1)
		}

	private fun explicitBattleRuleSkillCodes(changelog: String): Set<String> {
		val codePattern = "[a-z0-9]+(?:-[a-z0-9]+)*"
		val quotedCode = Regex("'($codePattern)'")
		val dotMatchesAll = setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)

		return buildSet {
			Regex("""\bs\.code\s*=\s*'($codePattern)'""", RegexOption.IGNORE_CASE)
				.findAll(changelog)
				.forEach { add(it.groupValues[1]) }
			Regex("""\bs\.code\s+(?:not\s+)?in\s*\(([^)]*)\)""", dotMatchesAll)
				.findAll(changelog)
				.flatMap { quotedCode.findAll(it.groupValues[1]) }
				.forEach { add(it.groupValues[1]) }
			Regex("""case\s+s\.code(.*?)\bend\b""", dotMatchesAll)
				.findAll(changelog)
				.flatMap {
					Regex("""when\s+'($codePattern)'""", RegexOption.IGNORE_CASE).findAll(it.groupValues[1])
				}
				.forEach { add(it.groupValues[1]) }
			Regex(
				"""with\s+\w+\(skill_code\b[^)]*\)\s+as\s*\(\s*values(.*?)\)\s*(?:insert|update)\b""",
				dotMatchesAll,
			)
				.findAll(changelog)
				.flatMap { Regex("""\(\s*'($codePattern)'""").findAll(it.groupValues[1]) }
				.forEach { add(it.groupValues[1]) }
			Regex("""update\s+game_skill(?:\s+\w+)?\s+set\b(.*?)(?:;|$)""", dotMatchesAll)
				.findAll(changelog)
				.flatMap {
					Regex("""where\s+(?:s\.)?code\s*=\s*'($codePattern)'""", RegexOption.IGNORE_CASE)
						.findAll(it.groupValues[1])
				}
				.forEach { add(it.groupValues[1]) }
		}
	}

	@Suppress("UNCHECKED_CAST")
	private fun initialChanges(): List<Map<String, Any?>> {
		val resource = javaClass.getResource("/db/changelog/baseline/001-core.yaml")
		val root = Yaml().load<Map<String, Any?>>(resource!!.readText())
		val databaseChangeLog = root["databaseChangeLog"] as List<Map<String, Any?>>
		val changeSet = databaseChangeLog.first()["changeSet"] as Map<String, Any?>
		return changeSet["changes"] as List<Map<String, Any?>>
	}

	@Suppress("UNCHECKED_CAST")
	private fun changelogChanges(): List<Map<String, Any?>> {
		val resource = javaClass.getResource("/db/changelog/baseline")
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
