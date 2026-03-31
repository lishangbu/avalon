package io.github.lishangbu.avalon.game.service.effect

import io.github.lishangbu.avalon.game.battle.engine.dsl.ActionNode
import io.github.lishangbu.avalon.game.battle.engine.dsl.ConditionNode
import io.github.lishangbu.avalon.game.battle.engine.dsl.EffectDefinition
import io.github.lishangbu.avalon.game.battle.engine.dsl.HookRule
import io.github.lishangbu.avalon.game.battle.engine.dsl.action.AddStatusActionNode
import io.github.lishangbu.avalon.game.battle.engine.dsl.action.AddVolatileActionNode
import io.github.lishangbu.avalon.game.battle.engine.dsl.action.ApplyConditionActionNode
import io.github.lishangbu.avalon.game.battle.engine.dsl.action.BoostActionNode
import io.github.lishangbu.avalon.game.battle.engine.dsl.action.ConsumeItemActionNode
import io.github.lishangbu.avalon.game.battle.engine.dsl.action.HealActionNode
import io.github.lishangbu.avalon.game.battle.engine.dsl.condition.AllConditionNode
import io.github.lishangbu.avalon.game.battle.engine.dsl.condition.ChanceConditionNode
import io.github.lishangbu.avalon.game.battle.engine.dsl.condition.HasItemConditionNode
import io.github.lishangbu.avalon.game.battle.engine.dsl.condition.HpRatioConditionNode
import io.github.lishangbu.avalon.game.battle.engine.dsl.condition.TargetRelationConditionNode
import io.github.lishangbu.avalon.game.battle.engine.event.StandardHookNames
import io.github.lishangbu.avalon.game.battle.engine.type.ActorId
import io.github.lishangbu.avalon.game.battle.engine.type.StandardEffectKindIds
import io.github.lishangbu.avalon.game.battle.engine.type.StandardTargetSelectorIds
import io.github.lishangbu.avalon.game.battle.engine.type.TargetSelectorId

internal data class MoveImportRecord(
    val internalName: String,
    val name: String,
    val typeInternalName: String?,
    val damageClassInternalName: String?,
    val targetInternalName: String?,
    val accuracy: Int?,
    val effectChance: Int?,
    val pp: Int?,
    val priority: Int?,
    val power: Int?,
    val shortEffect: String?,
    val effect: String?,
    val ailmentInternalName: String?,
    val ailmentChance: Int?,
    val healing: Int?,
    val drain: Int?,
)

internal data class AbilityImportRecord(
    val internalName: String,
    val name: String,
    val effect: String?,
    val introduction: String?,
)

internal data class ItemImportRecord(
    val internalName: String,
    val name: String,
    val shortEffect: String?,
    val effect: String?,
    val text: String?,
    val attributeInternalNames: Set<String>,
    val flingEffectInternalName: String?,
)

internal object SmartBattleEffectAssembler {
    fun fromMove(record: MoveImportRecord): EffectDefinition {
        val hooks = linkedMapOf<io.github.lishangbu.avalon.game.battle.engine.type.HookName, MutableList<HookRule>>()
        val secondaryEffectAction = moveSecondaryEffectAction(record)
        val secondaryEffectChance = record.ailmentChance ?: record.effectChance

        if (record.healing != null && record.healing > 0) {
            hooks.getOrPut(StandardHookNames.ON_HIT, ::mutableListOf) +=
                HookRule(
                    thenActions =
                        listOf(
                            HealActionNode(
                                target = StandardTargetSelectorIds.SELF,
                                mode = "max_hp_ratio",
                                value = record.healing / 100.0,
                            ),
                        ),
                )
        }

        if (secondaryEffectAction != null) {
            hooks.getOrPut(StandardHookNames.ON_HIT, ::mutableListOf) +=
                HookRule(
                    condition = chanceCondition(secondaryEffectChance, guaranteedSecondaryEffect(record)),
                    thenActions = listOf(secondaryEffectAction),
                )
        }

        return EffectDefinition(
            id = record.internalName,
            kind = StandardEffectKindIds.MOVE,
            name = record.name,
            tags =
                buildSet {
                    add(if ((record.power ?: 0) > 0) "damaging" else "status")
                    record.damageClassInternalName?.let(::add)
                    record.typeInternalName?.let(::add)
                    if ((record.healing ?: 0) > 0) {
                        add("recovery")
                    }
                    normalizedAilmentTag(record.ailmentInternalName)?.let(::add)
                },
            data =
                buildMap {
                    put("source", "dataset")
                    put("entity", "move")
                    put("target", record.targetInternalName)
                    put("type", record.typeInternalName)
                    put("damageClass", record.damageClassInternalName)
                    put("accuracy", record.accuracy)
                    put("effectChance", record.effectChance)
                    put("pp", record.pp)
                    put("priority", record.priority)
                    put("basePower", record.power)
                    put("shortEffect", record.shortEffect)
                    put("effect", record.effect)
                    put("healing", record.healing)
                    put("drain", record.drain)
                }.filterValues { it != null },
            hooks = hooks.mapValues { (_, rules) -> rules.toList() },
        )
    }

    fun fromAbility(record: AbilityImportRecord): EffectDefinition {
        val hooks =
            when (record.internalName) {
                "static" -> {
                    mapOf(
                        StandardHookNames.ON_HIT to
                            listOf(
                                HookRule(
                                    condition =
                                        AllConditionNode(
                                            conditions =
                                                listOf(
                                                    ChanceConditionNode(30),
                                                    TargetRelationConditionNode("foe"),
                                                ),
                                        ),
                                    thenActions =
                                        listOf(
                                            AddStatusActionNode(
                                                target = StandardTargetSelectorIds.SOURCE,
                                                value = "par",
                                            ),
                                        ),
                                ),
                            ),
                    )
                }

                "speed-boost" -> {
                    mapOf(
                        StandardHookNames.ON_RESIDUAL to
                            listOf(
                                HookRule(
                                    thenActions =
                                        listOf(
                                            BoostActionNode(
                                                target = StandardTargetSelectorIds.SELF,
                                                stats = mapOf("speed" to 1),
                                            ),
                                        ),
                                ),
                            ),
                    )
                }

                else -> {
                    emptyMap()
                }
            }

        return EffectDefinition(
            id = record.internalName,
            kind = StandardEffectKindIds.ABILITY,
            name = record.name,
            tags = inferTextTags(record.internalName, record.effect, record.introduction),
            data =
                buildMap {
                    put("source", "dataset")
                    put("entity", "ability")
                    put("effect", record.effect)
                    put("introduction", record.introduction)
                }.filterValues { it != null },
            hooks = hooks,
        )
    }

    fun fromItem(record: ItemImportRecord): EffectDefinition {
        val hooks =
            healingItemHook(record)
                ?.let { rule ->
                    mapOf(StandardHookNames.ON_RESIDUAL to listOf(rule))
                }.orEmpty()

        return EffectDefinition(
            id = record.internalName,
            kind = StandardEffectKindIds.ITEM,
            name = record.name,
            tags =
                buildSet {
                    addAll(record.attributeInternalNames)
                    addAll(inferTextTags(record.internalName, record.shortEffect, record.effect, record.text))
                },
            data =
                buildMap {
                    put("source", "dataset")
                    put("entity", "item")
                    put("shortEffect", record.shortEffect)
                    put("effect", record.effect)
                    put("text", record.text)
                    put("flingEffect", record.flingEffectInternalName)
                }.filterValues { it != null },
            hooks = hooks,
        )
    }

    private fun healingItemHook(record: ItemImportRecord): HookRule? {
        val recovery =
            when (record.internalName) {
                "sitrus-berry" -> HealingSpec(mode = "max_hp_ratio", value = 0.25)
                "oran-berry" -> HealingSpec(mode = null, value = 10.0)
                else -> null
            } ?: return null

        return HookRule(
            condition =
                AllConditionNode(
                    conditions =
                        listOf(
                            HasItemConditionNode(
                                actor = ActorId("self"),
                                value = record.internalName,
                            ),
                            HpRatioConditionNode(
                                actor = ActorId("self"),
                                operator = "<=",
                                value = 0.5,
                            ),
                        ),
                ),
            thenActions =
                listOf(
                    ConsumeItemActionNode(StandardTargetSelectorIds.SELF),
                    HealActionNode(
                        target = StandardTargetSelectorIds.SELF,
                        mode = recovery.mode,
                        value = recovery.value,
                    ),
                ),
        )
    }

    private fun moveSecondaryEffectAction(record: MoveImportRecord): ActionNode? {
        val ailment = normalizedAilmentTag(record.ailmentInternalName) ?: return null
        val target = targetSelectorOf(record.targetInternalName)
        return when (ailment) {
            "paralysis" -> AddStatusActionNode(target, "par")
            "burn" -> AddStatusActionNode(target, "brn")
            "poison" -> AddStatusActionNode(target, "psn")
            "sleep" -> AddStatusActionNode(target, "slp")
            "freeze" -> AddStatusActionNode(target, "frz")
            "confusion" -> AddVolatileActionNode(target, "confusion")
            else -> ApplyConditionActionNode(target, ailment)
        }
    }

    private fun targetSelectorOf(rawTarget: String?): TargetSelectorId =
        when (rawTarget) {
            "self", "user" -> StandardTargetSelectorIds.SELF
            "ally" -> StandardTargetSelectorIds.ALLY
            "users-field" -> StandardTargetSelectorIds.SIDE
            "user-or-ally", "user-and-allies", "all-allies" -> StandardTargetSelectorIds.ALL_ALLIES
            "opponents-field" -> StandardTargetSelectorIds.FOE_SIDE
            "all-opponents" -> StandardTargetSelectorIds.ALL_FOES
            "all-other-pokemon", "all-pokemon", "entire-field" -> StandardTargetSelectorIds.ALL
            else -> StandardTargetSelectorIds.TARGET
        }

    private fun guaranteedSecondaryEffect(record: MoveImportRecord): Boolean =
        record.ailmentInternalName != null &&
            ((record.power ?: 0) <= 0 || record.damageClassInternalName == "status") &&
            (record.ailmentChance == null && record.effectChance == null)

    private fun chanceCondition(
        chance: Int?,
        guaranteed: Boolean,
    ): ConditionNode? {
        if (guaranteed) {
            return null
        }
        if (chance == null || chance <= 0 || chance >= 100) {
            return null
        }
        return ChanceConditionNode(chance)
    }

    private fun normalizedAilmentTag(ailmentInternalName: String?): String? =
        when (ailmentInternalName) {
            null, "", "unknown", "none" -> null
            else -> ailmentInternalName
        }

    private fun inferTextTags(vararg values: String?): Set<String> {
        val text = values.filterNotNull().joinToString(" ").lowercase()
        return buildSet {
            if ("berry" in text) {
                add("berry")
            }
            if ("recover" in text || "restore" in text || "heal" in text) {
                add("recovery")
            }
            if ("contact" in text) {
                add("contact_reactive")
            }
            if ("paraly" in text) {
                add("paralysis")
            }
            if ("burn" in text) {
                add("burn")
            }
            if ("confus" in text) {
                add("confusion")
            }
        }
    }

    private data class HealingSpec(
        val mode: String?,
        val value: Double,
    )
}
