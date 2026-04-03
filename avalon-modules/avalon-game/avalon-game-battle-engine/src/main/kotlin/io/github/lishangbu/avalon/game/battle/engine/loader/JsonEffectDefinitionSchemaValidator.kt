package io.github.lishangbu.avalon.game.battle.engine.loader

import io.github.lishangbu.avalon.game.battle.engine.event.StandardHookNames
import io.github.lishangbu.avalon.game.battle.engine.type.StandardActionTypeIds
import io.github.lishangbu.avalon.game.battle.engine.type.StandardActorIds
import io.github.lishangbu.avalon.game.battle.engine.type.StandardConditionTypeIds
import io.github.lishangbu.avalon.game.battle.engine.type.StandardEffectKindIds
import io.github.lishangbu.avalon.game.battle.engine.type.StandardTargetSelectorIds
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper

/**
 * 面向 EffectDefinition JSON 的最小 schema 校验器。
 *
 * 设计意图：
 * - 在 loader 之前尽早发现结构问题。
 * - 提供稳定、清晰的错误信息，而不是把格式错误泄漏到运行时解释阶段。
 *
 * 该实现不是完整 JSON Schema 引擎，而是当前 DSL 的结构校验器。
 */
class JsonEffectDefinitionSchemaValidator(
    private val objectMapper: ObjectMapper = ObjectMapper(),
) : SchemaValidator {
    override fun validate(rawDocument: String) {
        val root = objectMapper.readTree(rawDocument)
        validateTopLevel(root)
        validateHooks(root.path("hooks"))
    }

    private fun validateTopLevel(root: JsonNode) {
        require(root.isObject) { "Effect document must be a JSON object." }
        require(root.hasNonNull("id")) { "Field 'id' is required." }
        require(root.hasNonNull("kind")) { "Field 'kind' is required." }
        require(root.hasNonNull("name")) { "Field 'name' is required." }
        require(root.has("hooks")) { "Field 'hooks' is required." }
        require(root.required("id").isString) { "Field 'id' must be a string." }
        require(root.required("kind").isString) { "Field 'kind' must be a string." }
        require(root.required("name").isString) { "Field 'name' must be a string." }
        require(root.required("kind").asString() in supportedEffectKinds) {
            "Field 'kind' has unsupported value '${root.required("kind").asString()}'."
        }
        require(root.path("hooks").isObject) { "Field 'hooks' must be an object." }
        if (root.has("tags")) {
            require(root.path("tags").isArray) { "Field 'tags' must be an array." }
            root.path("tags").forEach { tagNode ->
                require(tagNode.isString) { "Each tag must be a string." }
            }
        }
        if (root.has("data")) {
            require(root.path("data").isObject) { "Field 'data' must be an object." }
        }
        if (root.has("specialHandler")) {
            require(root.path("specialHandler").isString) { "Field 'specialHandler' must be a string." }
        }
    }

    private fun validateHooks(hooksNode: JsonNode) {
        hooksNode.properties().forEach { entry ->
            require(entry.key.isNotBlank()) { "Hook name must not be blank." }
            require(entry.key in supportedHookNames) { "Hook '${entry.key}' is not supported." }
            require(entry.value.isArray) { "Hook '${entry.key}' must be an array." }
            entry.value.forEach { ruleNode -> validateRule(entry.key, ruleNode) }
        }
    }

    private fun validateRule(
        hookName: String,
        ruleNode: JsonNode,
    ) {
        require(ruleNode.isObject) { "Rule under hook '$hookName' must be an object." }
        if (ruleNode.has("priority")) {
            require(ruleNode.path("priority").isNumber) { "Field 'priority' under hook '$hookName' must be numeric." }
        }
        if (ruleNode.has("subOrder")) {
            require(ruleNode.path("subOrder").isNumber) { "Field 'subOrder' under hook '$hookName' must be numeric." }
        }
        if (ruleNode.has("tags")) {
            require(ruleNode.path("tags").isArray) { "Field 'tags' under hook '$hookName' must be an array." }
        }
        if (ruleNode.has("if")) {
            validateCondition(ruleNode.required("if"))
        }
        validateActionArray(hookName, "then", ruleNode.path("then"))
        validateActionArray(hookName, "else", ruleNode.path("else"))
    }

    private fun validateCondition(conditionNode: JsonNode) {
        require(conditionNode.isObject) { "Condition node must be an object." }
        val type = conditionNode.required("type").asString()
        require(type in supportedConditionTypes) { "Unsupported condition type '$type'." }
        when (type) {
            "all", "any" -> {
                val conditionsNode = conditionNode.path("conditions")
                require(conditionsNode.isArray) { "Condition '$type' requires array field 'conditions'." }
                require(!conditionsNode.isEmpty) { "Condition '$type' requires at least one child condition." }
                conditionsNode.forEach(::validateCondition)
            }

            "not" -> {
                validateCondition(conditionNode.required("condition"))
            }

            "chance" -> {
                require(conditionNode.has("value")) { "Condition 'chance' requires field 'value'." }
                require(conditionNode.path("value").isInt) { "Condition 'chance.value' must be an integer." }
                val chance = conditionNode.path("value").asInt()
                require(chance in 0..100) { "Condition 'chance.value' must be between 0 and 100." }
            }

            "hp_ratio" -> {
                requireActorField(conditionNode, "actor", "Condition 'hp_ratio'")
                requireTextField(conditionNode, "operator", "Condition 'hp_ratio'")
                require(conditionNode.has("value")) { "Condition 'hp_ratio' requires field 'value'." }
                require(conditionNode.path("value").isNumber) { "Condition 'hp_ratio.value' must be numeric." }
                val ratio = conditionNode.path("value").asDouble()
                require(ratio in 0.0..1.0) { "Condition 'hp_ratio.value' must be between 0.0 and 1.0." }
                validateOperator(conditionNode.path("operator").asString(), "Condition 'hp_ratio'")
            }

            "has_status" -> {
                requireActorField(conditionNode, "actor", "Condition 'has_status'")
                if (conditionNode.has("value")) {
                    require(conditionNode.path("value").isString || conditionNode.path("value").isNull) {
                        "Condition 'has_status.value' must be a string or null."
                    }
                }
            }

            "has_volatile", "has_type", "has_item", "has_ability", "move_has_tag" -> {
                requireActorField(conditionNode, "actor", "Condition '$type'")
                requireTextField(conditionNode, "value", "Condition '$type'")
            }

            "weather_is", "terrain_is", "target_relation" -> {
                requireTextField(conditionNode, "value", "Condition '$type'")
            }

            "attribute_equals" -> {
                requireTextField(conditionNode, "key", "Condition 'attribute_equals'")
                requireTextField(conditionNode, "value", "Condition 'attribute_equals'")
            }

            "boost_compare", "stat_compare" -> {
                requireActorField(conditionNode, "actor", "Condition '$type'")
                requireTextField(conditionNode, "stat", "Condition '$type'")
                requireTextField(conditionNode, "operator", "Condition '$type'")
                require(conditionNode.has("value")) { "Condition '$type' requires field 'value'." }
                require(conditionNode.path("value").isInt) { "Condition '$type.value' must be an integer." }
                validateOperator(conditionNode.path("operator").asString(), "Condition '$type'")
            }

            "turn_compare" -> {
                requireTextField(conditionNode, "operator", "Condition 'turn_compare'")
                require(conditionNode.has("value")) { "Condition 'turn_compare' requires field 'value'." }
                require(conditionNode.path("value").isInt) { "Condition 'turn_compare.value' must be an integer." }
                validateOperator(conditionNode.path("operator").asString(), "Condition 'turn_compare'")
            }
        }
    }

    private fun validateActionArray(
        hookName: String,
        fieldName: String,
        actionArrayNode: JsonNode,
    ) {
        if (!actionArrayNode.isArray) {
            require(actionArrayNode.isMissingNode) { "Field '$fieldName' under hook '$hookName' must be an array." }
            return
        }
        actionArrayNode.forEach { actionNode -> validateAction(hookName, fieldName, actionNode) }
    }

    private fun validateAction(
        hookName: String,
        fieldName: String,
        actionNode: JsonNode,
    ) {
        require(actionNode.isObject) { "Action under '$hookName.$fieldName' must be an object." }
        val type = actionNode.required("type").asString()
        require(type in supportedActionTypes) { "Unsupported action type '$type'." }

        when (type) {
            "add_status", "add_volatile", "apply_condition", "remove_condition" -> {
                requireTargetField(actionNode, "target", "Action '$type'")
                requireTextField(actionNode, "value", "Action '$type'")
            }

            "remove_status", "clear_boosts", "consume_item", "force_switch" -> {
                requireTargetField(actionNode, "target", "Action '$type'")
            }

            "remove_volatile" -> {
                requireTargetField(actionNode, "target", "Action 'remove_volatile'")
                requireTextField(actionNode, "value", "Action 'remove_volatile'")
            }

            "damage", "heal" -> {
                requireTargetField(actionNode, "target", "Action '$type'")
                require(actionNode.has("value")) { "Action '$type' requires field 'value'." }
                require(actionNode.path("value").isNumber) { "Action '$type.value' must be numeric." }
                if (actionNode.has("mode")) {
                    require(actionNode.path("mode").isString) { "Action '$type.mode' must be a string." }
                }
            }

            "boost" -> {
                requireTargetField(actionNode, "target", "Action 'boost'")
                require(actionNode.path("stats").isObject) { "Action 'boost.stats' must be an object." }
                require(!actionNode.path("stats").isEmpty) { "Action 'boost.stats' must not be empty." }
                actionNode.path("stats").properties().forEach { entry ->
                    require(entry.value.isInt) { "Action 'boost.stats.${entry.key}' must be an integer." }
                }
            }

            "set_weather", "set_terrain" -> {
                requireTextField(actionNode, "value", "Action '$type'")
            }

            "restore_pp" -> {
                requireTargetField(actionNode, "target", "Action 'restore_pp'")
                require(actionNode.has("value")) { "Action 'restore_pp' requires field 'value'." }
                require(actionNode.path("value").isInt) { "Action 'restore_pp.value' must be an integer." }
                if (actionNode.has("moveId")) {
                    require(actionNode.path("moveId").isString) { "Action 'restore_pp.moveId' must be a string." }
                }
            }

            "change_type" -> {
                requireTargetField(actionNode, "target", "Action 'change_type'")
                require(actionNode.path("values").isArray) { "Action 'change_type.values' must be an array." }
                require(!actionNode.path("values").isEmpty) { "Action 'change_type.values' must not be empty." }
                actionNode.path("values").forEach { valueNode ->
                    require(valueNode.isString) { "Each 'change_type.values' item must be a string." }
                }
            }

            "trigger_event" -> {
                requireHookField(actionNode, "hookName", "Action 'trigger_event'")
            }

            "modify_multiplier" -> {
                require(actionNode.has("value")) { "Action 'modify_multiplier' requires field 'value'." }
                require(actionNode.path("value").isNumber) { "Action 'modify_multiplier.value' must be numeric." }
                if (actionNode.has("rounding")) {
                    require(actionNode.path("rounding").isString) { "Action 'modify_multiplier.rounding' must be a string." }
                }
            }

            "add_relay" -> {
                require(actionNode.has("value")) { "Action 'add_relay' requires field 'value'." }
                require(actionNode.path("value").isNumber) { "Action 'add_relay.value' must be numeric." }
            }

            "set_flag" -> {
                requireTargetField(actionNode, "target", "Action 'set_flag'")
                requireTextField(actionNode, "key", "Action 'set_flag'")
                requireTextField(actionNode, "value", "Action 'set_flag'")
            }

            "clear_flag" -> {
                requireTargetField(actionNode, "target", "Action 'clear_flag'")
                requireTextField(actionNode, "key", "Action 'clear_flag'")
            }
        }
    }

    private fun JsonNode.required(name: String): JsonNode =
        requireNotNull(get(name)) {
            "Field '$name' is required."
        }

    private fun requireTextField(
        node: JsonNode,
        fieldName: String,
        owner: String,
    ) {
        require(node.hasNonNull(fieldName)) { "$owner requires field '$fieldName'." }
        require(node.path(fieldName).isString) { "$owner field '$fieldName' must be a string." }
    }

    private fun requireActorField(
        node: JsonNode,
        fieldName: String,
        owner: String,
    ) {
        requireTextField(node, fieldName, owner)
        val actor = node.path(fieldName).asString()
        require(actor in supportedActorIds) { "$owner field '$fieldName' has unsupported actor '$actor'." }
    }

    private fun requireTargetField(
        node: JsonNode,
        fieldName: String,
        owner: String,
    ) {
        requireTextField(node, fieldName, owner)
        val target = node.path(fieldName).asString()
        require(target in supportedTargetSelectors) {
            "$owner field '$fieldName' has unsupported target selector '$target'."
        }
    }

    private fun requireHookField(
        node: JsonNode,
        fieldName: String,
        owner: String,
    ) {
        requireTextField(node, fieldName, owner)
        val hookName = node.path(fieldName).asString()
        require(hookName in supportedHookNames) { "$owner field '$fieldName' has unsupported hook '$hookName'." }
    }

    private fun validateOperator(
        operator: String,
        owner: String,
    ) {
        require(operator in supportedOperators) { "$owner operator '$operator' is not supported." }
    }

    private companion object {
        private val supportedConditionTypes =
            setOf(
                StandardConditionTypeIds.ALL.value,
                StandardConditionTypeIds.ANY.value,
                StandardConditionTypeIds.NOT.value,
                StandardConditionTypeIds.CHANCE.value,
                StandardConditionTypeIds.HP_RATIO.value,
                StandardConditionTypeIds.HAS_STATUS.value,
                StandardConditionTypeIds.HAS_VOLATILE.value,
                StandardConditionTypeIds.HAS_TYPE.value,
                StandardConditionTypeIds.HAS_ITEM.value,
                StandardConditionTypeIds.HAS_ABILITY.value,
                StandardConditionTypeIds.WEATHER_IS.value,
                StandardConditionTypeIds.TERRAIN_IS.value,
                StandardConditionTypeIds.BOOST_COMPARE.value,
                StandardConditionTypeIds.STAT_COMPARE.value,
                StandardConditionTypeIds.MOVE_HAS_TAG.value,
                StandardConditionTypeIds.TARGET_RELATION.value,
                StandardConditionTypeIds.TURN_COMPARE.value,
                StandardConditionTypeIds.ATTRIBUTE_EQUALS.value,
            )

        private val supportedActionTypes =
            setOf(
                StandardActionTypeIds.DAMAGE.value,
                StandardActionTypeIds.HEAL.value,
                StandardActionTypeIds.ADD_STATUS.value,
                StandardActionTypeIds.REMOVE_STATUS.value,
                StandardActionTypeIds.ADD_VOLATILE.value,
                StandardActionTypeIds.REMOVE_VOLATILE.value,
                StandardActionTypeIds.BOOST.value,
                StandardActionTypeIds.CLEAR_BOOSTS.value,
                StandardActionTypeIds.SET_WEATHER.value,
                StandardActionTypeIds.CLEAR_WEATHER.value,
                StandardActionTypeIds.SET_TERRAIN.value,
                StandardActionTypeIds.CLEAR_TERRAIN.value,
                StandardActionTypeIds.CONSUME_ITEM.value,
                StandardActionTypeIds.RESTORE_PP.value,
                StandardActionTypeIds.CHANGE_TYPE.value,
                StandardActionTypeIds.FORCE_SWITCH.value,
                StandardActionTypeIds.FAIL_MOVE.value,
                StandardActionTypeIds.TRIGGER_EVENT.value,
                StandardActionTypeIds.APPLY_CONDITION.value,
                StandardActionTypeIds.REMOVE_CONDITION.value,
                StandardActionTypeIds.MODIFY_MULTIPLIER.value,
                StandardActionTypeIds.ADD_RELAY.value,
                StandardActionTypeIds.SET_FLAG.value,
                StandardActionTypeIds.CLEAR_FLAG.value,
            )

        private val supportedEffectKinds =
            setOf(
                StandardEffectKindIds.MOVE.value,
                StandardEffectKindIds.ABILITY.value,
                StandardEffectKindIds.ITEM.value,
                StandardEffectKindIds.STATUS.value,
                StandardEffectKindIds.VOLATILE.value,
                StandardEffectKindIds.SIDE_CONDITION.value,
                StandardEffectKindIds.PSEUDO_WEATHER.value,
                StandardEffectKindIds.WEATHER.value,
                StandardEffectKindIds.TERRAIN.value,
                StandardEffectKindIds.FORMAT_RULE.value,
            )

        private val supportedHookNames =
            setOf(
                StandardHookNames.ON_SWITCH_IN.value,
                StandardHookNames.ON_SWITCH_OUT.value,
                StandardHookNames.ON_BEFORE_TURN.value,
                StandardHookNames.ON_BEFORE_MOVE.value,
                StandardHookNames.ON_TRY_MOVE.value,
                StandardHookNames.ON_PREPARE_HIT.value,
                StandardHookNames.ON_TRY_HIT.value,
                StandardHookNames.ON_MODIFY_ACCURACY.value,
                StandardHookNames.ON_MODIFY_EVASION.value,
                StandardHookNames.ON_MODIFY_BASE_POWER.value,
                StandardHookNames.ON_MODIFY_ATTACK.value,
                StandardHookNames.ON_MODIFY_DEFENSE.value,
                StandardHookNames.ON_MODIFY_CRIT_RATIO.value,
                StandardHookNames.ON_MODIFY_STAB.value,
                StandardHookNames.ON_MODIFY_DAMAGE.value,
                StandardHookNames.ON_DAMAGE.value,
                StandardHookNames.ON_HEAL.value,
                StandardHookNames.ON_HIT.value,
                StandardHookNames.ON_AFTER_HIT.value,
                StandardHookNames.ON_AFTER_MOVE.value,
                StandardHookNames.ON_SET_STATUS.value,
                StandardHookNames.ON_TRY_ADD_VOLATILE.value,
                StandardHookNames.ON_BOOST.value,
                StandardHookNames.ON_RESIDUAL.value,
                StandardHookNames.ON_WEATHER_CHANGE.value,
                StandardHookNames.ON_TERRAIN_CHANGE.value,
                StandardHookNames.ON_FAINT.value,
            )

        private val supportedActorIds =
            setOf(
                StandardActorIds.SELF.value,
                StandardActorIds.TARGET.value,
                StandardActorIds.SOURCE.value,
                StandardActorIds.MOVE.value,
                StandardActorIds.FIELD.value,
                StandardActorIds.SIDE.value,
                StandardActorIds.FOE_SIDE.value,
            )

        private val supportedTargetSelectors =
            setOf(
                StandardTargetSelectorIds.SELF.value,
                StandardTargetSelectorIds.TARGET.value,
                StandardTargetSelectorIds.SOURCE.value,
                StandardTargetSelectorIds.ALLY.value,
                StandardTargetSelectorIds.ALL_ALLIES.value,
                StandardTargetSelectorIds.FOE.value,
                StandardTargetSelectorIds.ALL_FOES.value,
                StandardTargetSelectorIds.SIDE.value,
                StandardTargetSelectorIds.FOE_SIDE.value,
                StandardTargetSelectorIds.FIELD.value,
                StandardTargetSelectorIds.ALL.value,
            )

        private val supportedOperators =
            setOf(
                ">",
                ">=",
                "<",
                "<=",
                "==",
                "!=",
            )
    }
}
