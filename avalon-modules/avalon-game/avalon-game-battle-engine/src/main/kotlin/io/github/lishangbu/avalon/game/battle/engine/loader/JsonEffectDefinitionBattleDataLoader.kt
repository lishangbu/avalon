package io.github.lishangbu.avalon.game.battle.engine.loader

import io.github.lishangbu.avalon.game.battle.engine.dsl.ActionNode
import io.github.lishangbu.avalon.game.battle.engine.dsl.ConditionNode
import io.github.lishangbu.avalon.game.battle.engine.dsl.EffectDefinition
import io.github.lishangbu.avalon.game.battle.engine.dsl.HookRule
import io.github.lishangbu.avalon.game.battle.engine.dsl.action.AddRelayActionNode
import io.github.lishangbu.avalon.game.battle.engine.dsl.action.AddStatusActionNode
import io.github.lishangbu.avalon.game.battle.engine.dsl.action.AddVolatileActionNode
import io.github.lishangbu.avalon.game.battle.engine.dsl.action.ApplyConditionActionNode
import io.github.lishangbu.avalon.game.battle.engine.dsl.action.BoostActionNode
import io.github.lishangbu.avalon.game.battle.engine.dsl.action.ChangeTypeActionNode
import io.github.lishangbu.avalon.game.battle.engine.dsl.action.ClearBoostsActionNode
import io.github.lishangbu.avalon.game.battle.engine.dsl.action.ClearFlagActionNode
import io.github.lishangbu.avalon.game.battle.engine.dsl.action.ClearTerrainActionNode
import io.github.lishangbu.avalon.game.battle.engine.dsl.action.ClearWeatherActionNode
import io.github.lishangbu.avalon.game.battle.engine.dsl.action.ConsumeItemActionNode
import io.github.lishangbu.avalon.game.battle.engine.dsl.action.DamageActionNode
import io.github.lishangbu.avalon.game.battle.engine.dsl.action.FailMoveActionNode
import io.github.lishangbu.avalon.game.battle.engine.dsl.action.ForceSwitchActionNode
import io.github.lishangbu.avalon.game.battle.engine.dsl.action.ModifyMultiplierActionNode
import io.github.lishangbu.avalon.game.battle.engine.dsl.action.RemoveConditionActionNode
import io.github.lishangbu.avalon.game.battle.engine.dsl.action.RemoveStatusActionNode
import io.github.lishangbu.avalon.game.battle.engine.dsl.action.RemoveVolatileActionNode
import io.github.lishangbu.avalon.game.battle.engine.dsl.action.RestorePpActionNode
import io.github.lishangbu.avalon.game.battle.engine.dsl.action.SetFlagActionNode
import io.github.lishangbu.avalon.game.battle.engine.dsl.action.SetTerrainActionNode
import io.github.lishangbu.avalon.game.battle.engine.dsl.action.SetWeatherActionNode
import io.github.lishangbu.avalon.game.battle.engine.dsl.action.TriggerEventActionNode
import io.github.lishangbu.avalon.game.battle.engine.dsl.condition.AllConditionNode
import io.github.lishangbu.avalon.game.battle.engine.dsl.condition.AnyConditionNode
import io.github.lishangbu.avalon.game.battle.engine.dsl.condition.AttributeEqualsConditionNode
import io.github.lishangbu.avalon.game.battle.engine.dsl.condition.BoostCompareConditionNode
import io.github.lishangbu.avalon.game.battle.engine.dsl.condition.ChanceConditionNode
import io.github.lishangbu.avalon.game.battle.engine.dsl.condition.HasAbilityConditionNode
import io.github.lishangbu.avalon.game.battle.engine.dsl.condition.HasItemConditionNode
import io.github.lishangbu.avalon.game.battle.engine.dsl.condition.HasStatusConditionNode
import io.github.lishangbu.avalon.game.battle.engine.dsl.condition.HasTypeConditionNode
import io.github.lishangbu.avalon.game.battle.engine.dsl.condition.HasVolatileConditionNode
import io.github.lishangbu.avalon.game.battle.engine.dsl.condition.HpRatioConditionNode
import io.github.lishangbu.avalon.game.battle.engine.dsl.condition.MoveHasTagConditionNode
import io.github.lishangbu.avalon.game.battle.engine.dsl.condition.NotConditionNode
import io.github.lishangbu.avalon.game.battle.engine.dsl.condition.StatCompareConditionNode
import io.github.lishangbu.avalon.game.battle.engine.dsl.condition.TargetRelationConditionNode
import io.github.lishangbu.avalon.game.battle.engine.dsl.condition.TerrainIsConditionNode
import io.github.lishangbu.avalon.game.battle.engine.dsl.condition.TurnCompareConditionNode
import io.github.lishangbu.avalon.game.battle.engine.dsl.condition.WeatherIsConditionNode
import io.github.lishangbu.avalon.game.battle.engine.type.ActionTypeId
import io.github.lishangbu.avalon.game.battle.engine.type.ActorId
import io.github.lishangbu.avalon.game.battle.engine.type.ConditionTypeId
import io.github.lishangbu.avalon.game.battle.engine.type.EffectKindId
import io.github.lishangbu.avalon.game.battle.engine.type.HookName
import io.github.lishangbu.avalon.game.battle.engine.type.SpecialHandlerId
import io.github.lishangbu.avalon.game.battle.engine.type.TargetSelectorId
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper

/**
 * 从 JSON 资源加载 [EffectDefinition] 的基础实现。
 *
 * 设计意图：
 * - 为第一版测试与样例数据提供真实文件加载入口。
 * - 先支持当前已落地测试需要的 DSL 子集，再逐步扩展。
 *
 * 当前支持的条件类型：
 * - `all`
 * - `any`
 * - `not`
 * - `chance`
 * - `hp_ratio`
 * - `has_status`
 * - `has_volatile`
 * - `has_type`
 * - `has_item`
 * - `has_ability`
 * - `weather_is`
 * - `terrain_is`
 * - `boost_compare`
 * - `stat_compare`
 * - `move_has_tag`
 * - `target_relation`
 * - `turn_compare`
 *
 * 当前支持的动作类型：
 * - `add_status`
 * - `remove_status`
 * - `add_volatile`
 * - `remove_volatile`
 * - `damage`
 * - `heal`
 * - `boost`
 * - `clear_boosts`
 * - `set_weather`
 * - `clear_weather`
 * - `set_terrain`
 * - `clear_terrain`
 * - `consume_item`
 * - `restore_pp`
 * - `change_type`
 * - `force_switch`
 * - `fail_move`
 * - `trigger_event`
 * - `apply_condition`
 * - `remove_condition`
 * - `modify_multiplier`
 * - `add_relay`
 * - `set_flag`
 * - `clear_flag`
 *
 * @property resourcePaths 要加载的 classpath 资源路径列表。
 * @property objectMapper JSON 解析器。
 */
class JsonEffectDefinitionBattleDataLoader(
    private val resourcePaths: List<String>,
    private val objectMapper: ObjectMapper = ObjectMapper(),
) : BattleDataLoader {
    override fun loadEffects(): List<EffectDefinition> = resourcePaths.map(::loadEffect)

    private fun loadEffect(resourcePath: String): EffectDefinition {
        val stream =
            requireNotNull(javaClass.classLoader.getResourceAsStream(resourcePath)) {
                "Resource '$resourcePath' was not found."
            }
        val rootNode = stream.use(objectMapper::readTree)
        return parseEffect(rootNode)
    }

    private fun parseEffect(rootNode: JsonNode): EffectDefinition =
        EffectDefinition(
            id = rootNode.requiredText("id"),
            kind = EffectKindId(rootNode.requiredText("kind")),
            name = rootNode.requiredText("name"),
            tags = rootNode.readStringSet("tags"),
            data = rootNode.readObjectMap("data"),
            hooks =
                rootNode.path("hooks").properties().asSequence().associate { entry ->
                    HookName(entry.key) to entry.value.mapArray(::parseHookRule)
                },
            specialHandler = rootNode.optionalText("specialHandler")?.let(::SpecialHandlerId),
        )

    private fun parseHookRule(node: JsonNode): HookRule =
        HookRule(
            priority = node.path("priority").asInt(0),
            subOrder = node.path("subOrder").asInt(0),
            condition = node.get("if")?.let(::parseCondition),
            thenActions = node.path("then").mapArray(::parseAction),
            elseActions = node.path("else").mapArray(::parseAction),
            tags = node.readStringSet("tags"),
        )

    private fun parseCondition(node: JsonNode): ConditionNode {
        val typeId = ConditionTypeId(node.requiredText("type"))
        return when (typeId.value) {
            "all" -> {
                AllConditionNode(
                    conditions = node.path("conditions").mapArray(::parseCondition),
                )
            }

            "any" -> {
                AnyConditionNode(
                    conditions = node.path("conditions").mapArray(::parseCondition),
                )
            }

            "chance" -> {
                ChanceConditionNode(
                    value = node.path("value").asInt(),
                )
            }

            "not" -> {
                NotConditionNode(
                    condition = parseCondition(node.required("condition")),
                )
            }

            "hp_ratio" -> {
                HpRatioConditionNode(
                    actor = ActorId(node.requiredText("actor")),
                    operator = node.requiredText("operator"),
                    value = node.path("value").asDouble(),
                )
            }

            "has_status" -> {
                HasStatusConditionNode(
                    actor = ActorId(node.requiredText("actor")),
                    value = node.optionalText("value"),
                )
            }

            "has_volatile" -> {
                HasVolatileConditionNode(
                    actor = ActorId(node.requiredText("actor")),
                    value = node.requiredText("value"),
                )
            }

            "has_type" -> {
                HasTypeConditionNode(
                    actor = ActorId(node.requiredText("actor")),
                    value = node.requiredText("value"),
                )
            }

            "has_item" -> {
                HasItemConditionNode(
                    actor = ActorId(node.requiredText("actor")),
                    value = node.requiredText("value"),
                )
            }

            "has_ability" -> {
                HasAbilityConditionNode(
                    actor = ActorId(node.requiredText("actor")),
                    value = node.requiredText("value"),
                )
            }

            "weather_is" -> {
                WeatherIsConditionNode(
                    value = node.requiredText("value"),
                )
            }

            "terrain_is" -> {
                TerrainIsConditionNode(
                    value = node.requiredText("value"),
                )
            }

            "boost_compare" -> {
                BoostCompareConditionNode(
                    actor = ActorId(node.requiredText("actor")),
                    stat = node.requiredText("stat"),
                    operator = node.requiredText("operator"),
                    value = node.path("value").asInt(),
                )
            }

            "stat_compare" -> {
                StatCompareConditionNode(
                    actor = ActorId(node.requiredText("actor")),
                    stat = node.requiredText("stat"),
                    operator = node.requiredText("operator"),
                    value = node.path("value").asInt(),
                )
            }

            "move_has_tag" -> {
                MoveHasTagConditionNode(
                    actor = ActorId(node.requiredText("actor")),
                    value = node.requiredText("value"),
                )
            }

            "target_relation" -> {
                TargetRelationConditionNode(
                    value = node.requiredText("value"),
                )
            }

            "attribute_equals" -> {
                AttributeEqualsConditionNode(
                    key = node.requiredText("key"),
                    value = node.requiredText("value"),
                )
            }

            "turn_compare" -> {
                TurnCompareConditionNode(
                    operator = node.requiredText("operator"),
                    value = node.path("value").asInt(),
                )
            }

            else -> {
                error("Unsupported condition type '${typeId.value}'.")
            }
        }
    }

    private fun parseAction(node: JsonNode): ActionNode {
        val typeId = ActionTypeId(node.requiredText("type"))
        return when (typeId.value) {
            "add_status" -> {
                AddStatusActionNode(
                    target = TargetSelectorId(node.requiredText("target")),
                    value = node.requiredText("value"),
                )
            }

            "remove_status" -> {
                RemoveStatusActionNode(
                    target = TargetSelectorId(node.requiredText("target")),
                )
            }

            "add_volatile" -> {
                AddVolatileActionNode(
                    target = TargetSelectorId(node.requiredText("target")),
                    value = node.requiredText("value"),
                )
            }

            "remove_volatile" -> {
                RemoveVolatileActionNode(
                    target = TargetSelectorId(node.requiredText("target")),
                    value = node.requiredText("value"),
                )
            }

            "damage" -> {
                DamageActionNode(
                    target = TargetSelectorId(node.requiredText("target")),
                    mode = node.optionalText("mode"),
                    value = node.path("value").asDouble(),
                )
            }

            "heal" -> {
                io.github.lishangbu.avalon.game.battle.engine.dsl.action.HealActionNode(
                    target = TargetSelectorId(node.requiredText("target")),
                    mode = node.optionalText("mode"),
                    value = node.path("value").asDouble(),
                )
            }

            "boost" -> {
                BoostActionNode(
                    target = TargetSelectorId(node.requiredText("target")),
                    stats =
                        node.required("stats").properties().asSequence().associate { entry ->
                            entry.key to entry.value.asInt()
                        },
                )
            }

            "clear_boosts" -> {
                ClearBoostsActionNode(
                    target = TargetSelectorId(node.requiredText("target")),
                )
            }

            "set_weather" -> {
                SetWeatherActionNode(
                    value = node.requiredText("value"),
                )
            }

            "clear_weather" -> {
                ClearWeatherActionNode()
            }

            "set_terrain" -> {
                SetTerrainActionNode(
                    value = node.requiredText("value"),
                )
            }

            "clear_terrain" -> {
                ClearTerrainActionNode()
            }

            "consume_item" -> {
                ConsumeItemActionNode(
                    target = TargetSelectorId(node.requiredText("target")),
                )
            }

            "restore_pp" -> {
                RestorePpActionNode(
                    target = TargetSelectorId(node.requiredText("target")),
                    moveId = node.optionalText("moveId"),
                    value = node.path("value").asInt(),
                )
            }

            "change_type" -> {
                ChangeTypeActionNode(
                    target = TargetSelectorId(node.requiredText("target")),
                    values = node.path("values").mapArray { child -> child.asString() },
                )
            }

            "force_switch" -> {
                ForceSwitchActionNode(
                    target = TargetSelectorId(node.requiredText("target")),
                )
            }

            "fail_move" -> {
                FailMoveActionNode()
            }

            "trigger_event" -> {
                TriggerEventActionNode(
                    hookName = HookName(node.requiredText("hookName")),
                )
            }

            "apply_condition" -> {
                ApplyConditionActionNode(
                    target = TargetSelectorId(node.requiredText("target")),
                    value = node.requiredText("value"),
                )
            }

            "remove_condition" -> {
                RemoveConditionActionNode(
                    target = TargetSelectorId(node.requiredText("target")),
                    value = node.requiredText("value"),
                )
            }

            "modify_multiplier" -> {
                ModifyMultiplierActionNode(
                    value = node.path("value").asDouble(),
                    rounding = node.optionalText("rounding"),
                )
            }

            "add_relay" -> {
                AddRelayActionNode(
                    value = node.path("value").asDouble(),
                )
            }

            "set_flag" -> {
                SetFlagActionNode(
                    target = TargetSelectorId(node.requiredText("target")),
                    key = node.requiredText("key"),
                    value = node.requiredText("value"),
                )
            }

            "clear_flag" -> {
                ClearFlagActionNode(
                    target = TargetSelectorId(node.requiredText("target")),
                    key = node.requiredText("key"),
                )
            }

            else -> {
                error("Unsupported action type '${typeId.value}'.")
            }
        }
    }

    private fun JsonNode.required(name: String): JsonNode =
        requireNotNull(get(name)) {
            "Field '$name' is required."
        }

    private fun JsonNode.requiredText(name: String): String = required(name).asString()

    private fun JsonNode.optionalText(name: String): String? = get(name)?.asString()

    private fun JsonNode.readStringSet(name: String): Set<String> =
        path(name)
            .mapArray { it.asString() }
            .toSet()

    private fun JsonNode.readObjectMap(name: String): Map<String, Any?> =
        path(name)
            .properties()
            .asSequence()
            .associate { entry -> entry.key to readScalar(entry.value) }

    private fun readScalar(node: JsonNode): Any? =
        when {
            node.isNull -> null
            node.isInt -> node.asInt()
            node.isLong -> node.asLong()
            node.isDouble || node.isFloat || node.isBigDecimal -> node.asDouble()
            node.isBoolean -> node.asBoolean()
            else -> node.asString()
        }

    private fun <T> JsonNode.mapArray(transform: (JsonNode) -> T): List<T> = if (isArray) asSequence().map(transform).toList() else emptyList()
}
