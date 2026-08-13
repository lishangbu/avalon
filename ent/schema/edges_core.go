package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/schema/edge"
)

// Edges 返回 player_character 与 Account 的持久化关系。
func (PlayerCharacter) Edges() []ent.Edge {
	return []ent.Edge{
		edge.To("account", Account.Type).
			Field("account_id").Unique().Required().
			StorageKey(edge.Symbol("fk_player_character_account_id_id")),
	}
}

// Edges 返回 Account 的玩家角色关系。
func (Account) Edges() []ent.Edge {
	return []ent.Edge{
		edge.From("player_characters", PlayerCharacter.Type).
			Ref("account"),
	}
}

// Edges 返回 AdminAccount 的会话关系。
func (AdminAccount) Edges() []ent.Edge {
	return []ent.Edge{
		edge.From("assets", Asset.Type).
			Ref("owner_account"),
	}
}

// Edges 返回 asset 与 AdminAccount 的持久化关系。
func (Asset) Edges() []ent.Edge {
	return []ent.Edge{
		edge.To("owner_account", AdminAccount.Type).
			Field("owner_account_id").Unique().Required().
			StorageKey(edge.Symbol("fk_asset_owner_account_id_id")),
	}
}

// Edges 返回 game_creature 与 Species 及自身继承关系。
func (GameCreature) Edges() []ent.Edge {
	return []ent.Edge{
		edge.To("species", GameSpecies.Type).Field("species_id").Unique().Required().
			StorageKey(edge.Symbol("fk_game_creature_species_id_id")),
		edge.To("inherits_from", GameCreature.Type).Field("inherits_from_creature_id").Unique().
			StorageKey(edge.Symbol("fk_game_creature_inherits_from_creature_id_id")),
		edge.From("inheriting_creatures", GameCreature.Type).Ref("inherits_from"),
		edge.From("abilities", GameCreatureAbility.Type).Ref("creature"),
		edge.From("forms", GameCreatureForm.Type).Ref("creature"),
		edge.From("held_items", GameCreatureHeldItem.Type).Ref("creature"),
		edge.From("skills", GameCreatureSkillLearn.Type).Ref("creature"),
		edge.From("skins", GameCreatureSkin.Type).Ref("creature"),
		edge.From("stats", GameCreatureStat.Type).Ref("creature"),
		edge.From("evolutions_from", GameCreatureEvolution.Type).Ref("from_creature"),
		edge.From("evolutions_to", GameCreatureEvolution.Type).Ref("to_creature"),
	}
}

// Edges 返回 GameSpecies 的 Creature 关系。
func (GameSpecies) Edges() []ent.Edge {
	return []ent.Edge{
		edge.From("creatures", GameCreature.Type).Ref("species"),
		edge.To("color", GameSpeciesColor.Type).Field("color_id").Unique().StorageKey(edge.Symbol("fk_game_species_color_id_id")),
		edge.To("growth_rate", GameGrowthRate.Type).Field("growth_rate_id").Unique().StorageKey(edge.Symbol("fk_game_species_growth_rate_id_id")),
		edge.To("habitat", GameHabitat.Type).Field("habitat_id").Unique().StorageKey(edge.Symbol("fk_game_species_habitat_id_id")),
		edge.To("shape", GameSpeciesShape.Type).Field("shape_id").Unique().StorageKey(edge.Symbol("fk_game_species_shape_id_id")),
	}
}

// Edges 返回 game_creature_ability 的资料关系。
func (GameCreatureAbility) Edges() []ent.Edge {
	return []ent.Edge{
		edge.To("creature", GameCreature.Type).Field("creature_id").Unique().Required().
			StorageKey(edge.Symbol("fk_game_creature_ability_creature_id_id")),
		edge.To("ability", GameAbility.Type).Field("ability_id").Unique().Required().
			StorageKey(edge.Symbol("fk_game_creature_ability_ability_id_id")),
	}
}

// Edges 返回 game_creature_form 与 Creature 的持久化关系。
func (GameCreatureForm) Edges() []ent.Edge {
	return []ent.Edge{
		edge.To("creature", GameCreature.Type).Field("creature_id").Unique().Required().
			StorageKey(edge.Symbol("fk_game_creature_form_creature_id_id")),
	}
}

// Edges 返回 game_creature_form_element 的形态与属性关系。
func (GameCreatureFormElement) Edges() []ent.Edge {
	return []ent.Edge{
		edge.To("form", GameCreatureForm.Type).Field("form_id").Unique().Required().
			StorageKey(edge.Symbol("fk_game_creature_form_element_form_id_id")),
		edge.To("element", GameElement.Type).Field("element_id").Unique().Required().
			StorageKey(edge.Symbol("fk_game_creature_form_element_element_id_id")),
	}
}

// Edges 返回 game_creature_held_item 的 Creature 与 Item 关系。
func (GameCreatureHeldItem) Edges() []ent.Edge {
	return []ent.Edge{
		edge.To("creature", GameCreature.Type).Field("creature_id").Unique().Required().
			StorageKey(edge.Symbol("fk_game_creature_held_item_creature_id_id")),
		edge.To("item", GameItem.Type).Field("item_id").Unique().Required().
			StorageKey(edge.Symbol("fk_game_creature_held_item_item_id_id")),
	}
}

// Edges 返回 game_creature_stat 的 Creature 与 Stat 关系。
func (GameCreatureStat) Edges() []ent.Edge {
	return []ent.Edge{
		edge.To("creature", GameCreature.Type).Field("creature_id").Unique().Required().
			StorageKey(edge.Symbol("fk_game_creature_stat_creature_id_id")),
		edge.To("stat", GameStat.Type).Field("stat_id").Unique().Required().
			StorageKey(edge.Symbol("fk_game_creature_stat_stat_id_id")),
	}
}

// Edges 返回 game_element_effectiveness 的攻击与防御属性关系。
func (GameElementEffectiveness) Edges() []ent.Edge {
	return []ent.Edge{
		edge.To("attack_element", GameElement.Type).Field("attack_element_id").Unique().Required().
			StorageKey(edge.Symbol("fk_game_element_effectiveness_attack_element_id_id")),
		edge.To("defense_element", GameElement.Type).Field("defense_element_id").Unique().Required().
			StorageKey(edge.Symbol("fk_game_element_effectiveness_defense_element_id_id")),
	}
}

// Edges 返回 Element 被资料关系引用的反向关系。
func (GameElement) Edges() []ent.Edge {
	return []ent.Edge{
		edge.From("form_elements", GameCreatureFormElement.Type).Ref("element"),
		edge.From("effectiveness_as_attack", GameElementEffectiveness.Type).Ref("attack_element"),
		edge.From("effectiveness_as_defense", GameElementEffectiveness.Type).Ref("defense_element"),
		edge.From("skills", GameSkill.Type).Ref("element"),
	}
}

// Edges 返回 Creature Evolution 的来源、目标及触发资料关系。
func (GameCreatureEvolution) Edges() []ent.Edge {
	return []ent.Edge{
		edge.To("from_creature", GameCreature.Type).Field("from_creature_id").Unique().Required().
			StorageKey(edge.Symbol("fk_game_creature_evolution_from_creature_id_id")),
		edge.To("to_creature", GameCreature.Type).Field("to_creature_id").Unique().Required().
			StorageKey(edge.Symbol("fk_game_creature_evolution_to_creature_id_id")),
		edge.To("trigger_item", GameItem.Type).Field("trigger_item_id").Unique().
			StorageKey(edge.Symbol("fk_game_creature_evolution_trigger_item_id_id")),
		edge.To("required_skill", GameSkill.Type).Field("required_skill_id").Unique().
			StorageKey(edge.Symbol("fk_game_creature_evolution_required_skill_id_id")),
	}
}

// Edges 返回 GameSkill 与可选属性、伤害分类的关系。
func (GameSkill) Edges() []ent.Edge {
	return []ent.Edge{
		edge.To("element", GameElement.Type).Field("element_id").Unique().
			StorageKey(edge.Symbol("fk_game_skill_element_id_id")),
		edge.To("damage_class", GameSkillDamageClass.Type).Field("damage_class_id").Unique().
			StorageKey(edge.Symbol("fk_game_skill_damage_class_id_id")),
	}
}

// Edges 返回 Species 与 Egg Group 的关联关系。
func (GameSpeciesEggGroup) Edges() []ent.Edge {
	return []ent.Edge{
		edge.To("species", GameSpecies.Type).Field("species_id").Unique().Required().
			StorageKey(edge.Symbol("fk_game_species_egg_group_species_id_id")),
		edge.To("egg_group", GameEggGroup.Type).Field("egg_group_id").Unique().Required().
			StorageKey(edge.Symbol("fk_game_species_egg_group_egg_group_id_id")),
	}
}

// Edges 返回 GameItem 与资源、分类资料的关系。
func (GameItem) Edges() []ent.Edge {
	return []ent.Edge{
		edge.To("asset", Asset.Type).Field("asset_id").Unique().
			StorageKey(edge.Symbol("fk_game_item_asset_id_id")),
		edge.To("category", GameItemCategory.Type).Field("category_id").Unique().
			StorageKey(edge.Symbol("fk_game_item_category_id_id")),
		edge.To("fling_effect", GameItemFlingEffect.Type).Field("fling_effect_id").Unique().
			StorageKey(edge.Symbol("fk_game_item_fling_effect_id_id")),
		edge.From("held_by_creatures", GameCreatureHeldItem.Type).Ref("item"),
		edge.From("evolution_triggers", GameCreatureEvolution.Type).Ref("trigger_item"),
		edge.From("catalog_bindings", GameItemCatalogCategoryBinding.Type).Ref("item"),
		edge.From("inventory_items", PlayerCharacterInventoryItem.Type).Ref("item"),
		edge.From("inventory_transactions", PlayerCharacterInventoryTransaction.Type).Ref("item"),
		edge.From("quest_objective_targets", RpgQuestObjective.Type).Ref("target_item"),
		edge.From("quest_rewards", RpgQuestReward.Type).Ref("item"),
		edge.From("recipe_ingredients", RpgRecipeIngredient.Type).Ref("item"),
		edge.From("recipe_outputs", RpgRecipeOutput.Type).Ref("item"),
		edge.From("shop_items", RpgShopItem.Type).Ref("item"),
		edge.From("attribute_bindings", GameItemAttributeBinding.Type).Ref("item"),
		edge.From("status_rules", GameItemStatusRule.Type).Ref("item"),
		edge.From("damage_rules", GameItemDamageRule.Type).Ref("item"),
		edge.From("stat_booster_abilities", GameItemStatBoosterAbility.Type).Ref("item"),
		edge.From("weather_rules", GameItemWeatherRule.Type).Ref("item"),
		edge.From("switch_rules", GameItemSwitchRule.Type).Ref("item"), edge.From("contact_rules", GameItemContactRule.Type).Ref("item"), edge.From("recovery_rules", GameItemRecoveryRule.Type).Ref("item"), edge.From("stat_rules", GameItemStatRule.Type).Ref("item"), edge.From("action_rules", GameItemActionRule.Type).Ref("item"), edge.From("multi_hit_rules", GameItemMultiHitRule.Type).Ref("item"), edge.From("weight_rules", GameItemWeightRule.Type).Ref("item"),
	}
}

// Edges 返回道具状态规则与 Item 的关系。
func (GameItemStatusRule) Edges() []ent.Edge {
	return []ent.Edge{edge.To("item", GameItem.Type).Field("item_id").Unique().Required().StorageKey(edge.Symbol("fk_game_item_status_rule_item_id_id"))}
}

// Edges 返回道具伤害规则与 Item 的关系。
func (GameItemDamageRule) Edges() []ent.Edge {
	return []ent.Edge{
		edge.To("item", GameItem.Type).Field("item_id").Unique().Required().StorageKey(edge.Symbol("fk_game_item_damage_rule_item_id_id")),
		edge.To("element_boost_element", GameElement.Type).Field("element_boost_element_id").Unique().StorageKey(edge.Symbol("fk_game_item_damage_rule_element_boost_element_id_id")),
		edge.To("consumable_boost_element", GameElement.Type).Field("consumable_boost_element_id").Unique().StorageKey(edge.Symbol("fk_game_item_damage_rule_consumable_boost_element_id_id")),
		edge.To("reduction_element", GameElement.Type).Field("reduction_element_id").Unique().StorageKey(edge.Symbol("fk_game_item_damage_rule_reduction_element_id_id")),
	}
}

// Edges 返回最高能力强化规则的 Item 与 Ability 引用。
func (GameItemStatBoosterAbility) Edges() []ent.Edge {
	return []ent.Edge{
		edge.To("item", GameItem.Type).Field("item_id").Unique().Required().StorageKey(edge.Symbol("fk_game_item_stat_booster_ability_item_id_id")),
		edge.To("ability", GameAbility.Type).Field("ability_id").Unique().Required().StorageKey(edge.Symbol("fk_game_item_stat_booster_ability_ability_id_id")),
	}
}

// Edges 返回道具天气规则与 Item 的关系。
func (GameItemWeatherRule) Edges() []ent.Edge {
	return []ent.Edge{edge.To("item", GameItem.Type).Field("item_id").Unique().Required().StorageKey(edge.Symbol("fk_game_item_weather_rule_item_id_id"))}
}

func (GameItemSwitchRule) Edges() []ent.Edge {
	return []ent.Edge{edge.To("item", GameItem.Type).Field("item_id").Unique().Required().StorageKey(edge.Symbol("fk_game_item_switch_rule_item_id_id"))}
}
func (GameItemContactRule) Edges() []ent.Edge {
	return []ent.Edge{edge.To("item", GameItem.Type).Field("item_id").Unique().Required().StorageKey(edge.Symbol("fk_game_item_contact_rule_item_id_id"))}
}
func (GameItemRecoveryRule) Edges() []ent.Edge {
	return []ent.Edge{edge.To("item", GameItem.Type).Field("item_id").Unique().Required().StorageKey(edge.Symbol("fk_game_item_recovery_rule_item_id_id"))}
}

// Edges 返回能力规则引用的 Item 与触发 Element。
func (GameItemStatRule) Edges() []ent.Edge {
	return []ent.Edge{
		edge.To("item", GameItem.Type).Field("item_id").Unique().Required().StorageKey(edge.Symbol("fk_game_item_stat_rule_item_id_id")),
		edge.To("water_damage_special_attack_boost_element", GameElement.Type).Field("water_spa_element_id").Unique().StorageKey(edge.Symbol("fk_game_item_stat_rule_water_spa_element_id_id")),
		edge.To("electric_damage_attack_boost_element", GameElement.Type).Field("electric_atk_element_id").Unique().StorageKey(edge.Symbol("fk_game_item_stat_rule_electric_atk_element_id_id")),
		edge.To("water_damage_special_defense_boost_element", GameElement.Type).Field("water_spd_element_id").Unique().StorageKey(edge.Symbol("fk_game_item_stat_rule_water_spd_element_id_id")),
		edge.To("ice_damage_attack_boost_element", GameElement.Type).Field("ice_atk_element_id").Unique().StorageKey(edge.Symbol("fk_game_item_stat_rule_ice_atk_element_id_id")),
	}
}
func (GameItemActionRule) Edges() []ent.Edge {
	return []ent.Edge{edge.To("item", GameItem.Type).Field("item_id").Unique().Required().StorageKey(edge.Symbol("fk_game_item_action_rule_item_id_id"))}
}
func (GameItemMultiHitRule) Edges() []ent.Edge {
	return []ent.Edge{edge.To("item", GameItem.Type).Field("item_id").Unique().Required().StorageKey(edge.Symbol("fk_game_item_multi_hit_rule_item_id_id"))}
}
func (GameItemWeightRule) Edges() []ent.Edge {
	return []ent.Edge{edge.To("item", GameItem.Type).Field("item_id").Unique().Required().StorageKey(edge.Symbol("fk_game_item_weight_rule_item_id_id"))}
}

// Edges 返回道具分类与 Pocket 的关系。
func (GameItemCategory) Edges() []ent.Edge {
	return []ent.Edge{
		edge.To("pocket", GameItemPocket.Type).Field("pocket_id").Unique().Required().
			StorageKey(edge.Symbol("fk_game_item_category_pocket_id_id")),
		edge.From("items", GameItem.Type).Ref("category"),
	}
}

// Edges 返回道具 Pocket 的反向分类关系。
func (GameItemPocket) Edges() []ent.Edge {
	return []ent.Edge{edge.From("categories", GameItemCategory.Type).Ref("pocket")}
}

// Edges 返回道具投掷效果的反向 Item 关系。
func (GameItemFlingEffect) Edges() []ent.Edge {
	return []ent.Edge{edge.From("items", GameItem.Type).Ref("fling_effect")}
}

// Edges 返回道具 Attribute 关系的两端引用。
func (GameItemAttributeBinding) Edges() []ent.Edge {
	return []ent.Edge{
		edge.To("item", GameItem.Type).Field("item_id").Unique().Required().
			StorageKey(edge.Symbol("fk_game_item_attribute_binding_item_id_id")),
		edge.To("attribute", GameItemAttribute.Type).Field("attribute_id").Unique().Required().
			StorageKey(edge.Symbol("fk_game_item_attribute_binding_attribute_id_id")),
	}
}

// Edges 返回道具 Attribute 的反向关系集合。
func (GameItemAttribute) Edges() []ent.Edge {
	return []ent.Edge{edge.From("bindings", GameItemAttributeBinding.Type).Ref("attribute")}
}

// Edges 返回 RPG 对话与 NPC 的关系。
func (RpgDialogue) Edges() []ent.Edge {
	return []ent.Edge{edge.To("npc", RpgNpc.Type).Field("npc_id").Unique().Required().StorageKey(edge.Symbol("fk_rpg_dialogue_npc_id_id"))}
}

// Edges 返回 RPG 对话行与对话的关系。
func (RpgDialogueLine) Edges() []ent.Edge {
	return []ent.Edge{edge.To("dialogue", RpgDialogue.Type).Field("dialogue_id").Unique().Required().StorageKey(edge.Symbol("fk_rpg_dialogue_line_dialogue_id_id"))}
}

// Edges 返回 RPG 遭遇条目与遭遇表、Creature、Form 的关系。
func (RpgEncounterEntry) Edges() []ent.Edge {
	return []ent.Edge{
		edge.To("encounter_table", RpgEncounterTable.Type).Field("encounter_table_id").Unique().Required().StorageKey(edge.Symbol("fk_rpg_encounter_entry_encounter_table_id_id")),
		edge.To("creature", GameCreature.Type).Field("creature_id").Unique().Required().StorageKey(edge.Symbol("fk_rpg_encounter_entry_creature_id_id")),
		edge.To("form", GameCreatureForm.Type).Field("form_id").Unique().StorageKey(edge.Symbol("fk_rpg_encounter_entry_form_id_id")),
	}
}

// Edges 返回 RPG 遭遇表与地点的关系。
func (RpgEncounterTable) Edges() []ent.Edge {
	return []ent.Edge{edge.To("location", RpgLocation.Type).Field("location_id").Unique().Required().StorageKey(edge.Symbol("fk_rpg_encounter_table_location_id_id"))}
}

// Edges 返回 RPG 地点的父级和区域关系。
func (RpgLocation) Edges() []ent.Edge {
	return []ent.Edge{
		edge.To("parent", RpgLocation.Type).Field("parent_id").Unique().StorageKey(edge.Symbol("fk_rpg_location_parent_id_id")),
		edge.From("children", RpgLocation.Type).Ref("parent"),
		edge.To("region", RpgRegion.Type).Field("region_id").Unique().Required().StorageKey(edge.Symbol("fk_rpg_location_region_id_id")),
	}
}

// Edges 返回有向出口的来源地点、目标地点和发现关系。
func (RpgLocationExit) Edges() []ent.Edge {
	return []ent.Edge{
		edge.To("source_location", RpgLocation.Type).Field("source_location_id").Unique().Required().StorageKey(edge.Symbol("fk_rpg_location_exit_source_location_id_id")),
		edge.To("target_location", RpgLocation.Type).Field("target_location_id").Unique().Required().StorageKey(edge.Symbol("fk_rpg_location_exit_target_location_id_id")),
		edge.From("discovered_by", PlayerCharacterDiscoveredExit.Type).Ref("location_exit"),
	}
}

// Edges 返回地图展示布局与地点投影关系。
func (RpgMapProjection) Edges() []ent.Edge {
	return []ent.Edge{edge.From("locations", RpgMapProjectionLocation.Type).Ref("projection")}
}

// Edges 返回地点投影与地图布局、Location 的关系。
func (RpgMapProjectionLocation) Edges() []ent.Edge {
	return []ent.Edge{
		edge.To("projection", RpgMapProjection.Type).Field("projection_id").Unique().Required().StorageKey(edge.Symbol("fk_rpg_map_projection_location_projection_id_id")),
		edge.To("location", RpgLocation.Type).Field("location_id").Unique().Required().StorageKey(edge.Symbol("fk_rpg_map_projection_location_location_id_id")),
	}
}

// Edges 返回 RPG 掉落条目与掉落表、道具的关系。
func (RpgLootEntry) Edges() []ent.Edge {
	return []ent.Edge{
		edge.To("loot_table", RpgLootTable.Type).Field("loot_table_id").Unique().Required().StorageKey(edge.Symbol("fk_rpg_loot_entry_loot_table_id_id")),
		edge.To("item", GameItem.Type).Field("item_id").Unique().Required().StorageKey(edge.Symbol("fk_rpg_loot_entry_item_id_id")),
	}
}

// Edges 返回 RPG NPC 与地点的关系。
func (RpgNpc) Edges() []ent.Edge {
	return []ent.Edge{edge.To("location", RpgLocation.Type).Field("location_id").Unique().Required().StorageKey(edge.Symbol("fk_rpg_npc_location_id_id"))}
}

// Edges 返回 RPG 职业技能与职业的关系。
func (RpgProfessionSkill) Edges() []ent.Edge {
	return []ent.Edge{
		edge.To("profession", RpgProfession.Type).Field("profession_id").Unique().Required().StorageKey(edge.Symbol("fk_rpg_profession_skill_profession_id_id")),
		edge.From("player_character_unlocks", PlayerCharacterProfessionSkill.Type).Ref("profession_skill"),
	}
}

// Edges 返回 RPG 任务与前置任务、起始 NPC、交付 NPC 的关系。
func (RpgQuest) Edges() []ent.Edge {
	return []ent.Edge{
		edge.To("prerequisite", RpgQuest.Type).Field("prerequisite_quest_id").Unique().StorageKey(edge.Symbol("fk_rpg_quest_prerequisite_quest_id_id")),
		edge.To("start_npc", RpgNpc.Type).Field("start_npc_id").Unique().StorageKey(edge.Symbol("fk_rpg_quest_start_npc_id_id")),
		edge.To("turn_in_npc", RpgNpc.Type).Field("turn_in_npc_id").Unique().StorageKey(edge.Symbol("fk_rpg_quest_turn_in_npc_id_id")),
	}
}

// Edges 返回 RPG 任务目标与任务及目标资料的关系。
func (RpgQuestObjective) Edges() []ent.Edge {
	return []ent.Edge{
		edge.To("quest", RpgQuest.Type).Field("quest_id").Unique().Required().StorageKey(edge.Symbol("fk_rpg_quest_objective_quest_id_id")),
		edge.From("player_character_progress", PlayerCharacterQuestObjective.Type).Ref("objective"),
		edge.To("target_creature", GameCreature.Type).Field("target_creature_id").Unique().StorageKey(edge.Symbol("fk_rpg_quest_objective_target_creature_id_id")),
		edge.To("target_item", GameItem.Type).Field("target_item_id").Unique().StorageKey(edge.Symbol("fk_rpg_quest_objective_target_item_id_id")),
		edge.To("target_location", RpgLocation.Type).Field("target_location_id").Unique().StorageKey(edge.Symbol("fk_rpg_quest_objective_target_location_id_id")),
		edge.To("target_npc", RpgNpc.Type).Field("target_npc_id").Unique().StorageKey(edge.Symbol("fk_rpg_quest_objective_target_npc_id_id")),
	}
}

// Edges 返回 RPG 任务奖励与任务及奖励资料的关系。
func (RpgQuestReward) Edges() []ent.Edge {
	return []ent.Edge{
		edge.To("quest", RpgQuest.Type).Field("quest_id").Unique().Required().StorageKey(edge.Symbol("fk_rpg_quest_reward_quest_id_id")),
		edge.To("creature", GameCreature.Type).Field("creature_id").Unique().StorageKey(edge.Symbol("fk_rpg_quest_reward_creature_id_id")),
		edge.To("currency", GameCurrency.Type).Field("currency_id").Unique().StorageKey(edge.Symbol("fk_rpg_quest_reward_currency_id_id")),
		edge.To("item", GameItem.Type).Field("item_id").Unique().StorageKey(edge.Symbol("fk_rpg_quest_reward_item_id_id")),
	}
}

// Edges 返回 PlayerCharacter 的 RPG 检查点关系。
func (PlayerCharacterCheckpoint) Edges() []ent.Edge {
	return []ent.Edge{
		edge.To("player_character", PlayerCharacter.Type).Field("player_character_id").Unique().Required().StorageKey(edge.Symbol("fk_player_character_checkpoint_player_character_id_id")),
		edge.To("checkpoint", RpgCheckpoint.Type).Field("checkpoint_id").Unique().Required().StorageKey(edge.Symbol("fk_player_character_checkpoint_checkpoint_id_id")),
	}
}

// Edges 返回 PlayerCharacter 已解锁职业技能及其资料关系。
func (PlayerCharacterProfessionSkill) Edges() []ent.Edge {
	return []ent.Edge{
		edge.To("profession_skill", RpgProfessionSkill.Type).Field("profession_skill_id").Unique().Required().StorageKey(edge.Symbol("fk_player_character_profession_skill_profession_skill_id_id")),
	}
}

// Edges 返回 PlayerCharacter 任务目标进度及其目标资料关系。
func (PlayerCharacterQuestObjective) Edges() []ent.Edge {
	return []ent.Edge{
		edge.To("objective", RpgQuestObjective.Type).Field("objective_id").Unique().Required().StorageKey(edge.Symbol("fk_player_character_quest_objective_objective_id_id")),
	}
}

// Edges 返回 Owned Creature 与其所有资料引用的关系。
func (PlayerCharacterCreature) Edges() []ent.Edge {
	return []ent.Edge{
		edge.To("ability", GameAbility.Type).Field("ability_id").Unique().Required().StorageKey(edge.Symbol("fk_player_character_creature_ability_id_id")),
		edge.To("captured_with_item", GameItem.Type).Field("captured_with_item_id").Unique().StorageKey(edge.Symbol("fk_player_character_creature_captured_with_item_id_id")),
		edge.To("creature", GameCreature.Type).Field("creature_id").Unique().Required().StorageKey(edge.Symbol("fk_player_character_creature_creature_id_id")),
		edge.To("form", GameCreatureForm.Type).Field("form_id").Unique().StorageKey(edge.Symbol("fk_player_character_creature_form_id_id")),
		edge.To("gender", GameGender.Type).Field("gender_id").Unique().StorageKey(edge.Symbol("fk_player_character_creature_gender_id_id")),
		edge.To("held_item", GameItem.Type).Field("held_item_id").Unique().StorageKey(edge.Symbol("fk_player_character_creature_held_item_id_id")),
		edge.To("nature", GameNature.Type).Field("nature_id").Unique().Required().StorageKey(edge.Symbol("fk_player_character_creature_nature_id_id")),
		edge.To("origin_location", RpgLocation.Type).Field("origin_location_id").Unique().StorageKey(edge.Symbol("fk_player_character_creature_origin_location_id_id")),
		edge.To("player_character", PlayerCharacter.Type).Field("player_character_id").Unique().Required().StorageKey(edge.Symbol("fk_player_character_creature_player_character_id_id")),
		edge.To("skin", GameCreatureSkin.Type).Field("skin_id").Unique().StorageKey(edge.Symbol("fk_player_character_creature_skin_id_id")),
	}
}

// Edges 返回 Owned Creature 技能关系。
func (PlayerCharacterCreatureSkill) Edges() []ent.Edge {
	return []ent.Edge{
		edge.To("player_character_creature", PlayerCharacterCreature.Type).Field("player_character_creature_id").Unique().Required().StorageKey(edge.Symbol("fk_player_character_creature_ski_player_character_crea_21bd7ea6")),
		edge.To("skill", GameSkill.Type).Field("skill_id").Unique().Required().StorageKey(edge.Symbol("fk_player_character_creature_skill_skill_id_id")),
	}
}

// Edges 返回 Owned Creature 培养属性关系。
func (PlayerCharacterCreatureStat) Edges() []ent.Edge {
	return []ent.Edge{
		edge.To("player_character_creature", PlayerCharacterCreature.Type).Field("player_character_creature_id").Unique().Required().StorageKey(edge.Symbol("fk_player_character_creature_sta_player_character_crea_aef1bf2f")),
		edge.To("stat", GameStat.Type).Field("stat_id").Unique().Required().StorageKey(edge.Symbol("fk_player_character_creature_stat_stat_id_id")),
	}
}

// Edges 返回货币流水与 Currency、PlayerCharacter 的关系。
func (PlayerCharacterCurrencyTransaction) Edges() []ent.Edge {
	return []ent.Edge{
		edge.To("currency", GameCurrency.Type).Field("currency_id").Unique().Required().StorageKey(edge.Symbol("fk_player_character_currency_transaction_currency_id_id")),
		edge.To("player_character", PlayerCharacter.Type).Field("player_character_id").Unique().Required().StorageKey(edge.Symbol("fk_player_character_currency_transaction_player_character_id_id")),
	}
}

// Edges 返回展示名称历史与 PlayerCharacter 的关系。
func (PlayerCharacterDisplayNameHistory) Edges() []ent.Edge {
	return []ent.Edge{edge.To("player_character", PlayerCharacter.Type).Field("player_character_id").Unique().Required().StorageKey(edge.Symbol("fk_player_character_display_name_history_player_character_id_id"))}
}

// Edges 返回背包当前物品及其流水关系。
func (PlayerCharacterInventoryItem) Edges() []ent.Edge {
	return []ent.Edge{
		edge.To("item", GameItem.Type).Field("item_id").Unique().Required().StorageKey(edge.Symbol("fk_player_character_inventory_item_item_id_id")),
		edge.To("player_character", PlayerCharacter.Type).Field("player_character_id").Unique().Required().StorageKey(edge.Symbol("fk_player_character_inventory_item_player_character_id_id")),
	}
}

// Edges 返回背包流水与道具、PlayerCharacter 的关系。
func (PlayerCharacterInventoryTransaction) Edges() []ent.Edge {
	return []ent.Edge{
		edge.To("item", GameItem.Type).Field("item_id").Unique().Required().StorageKey(edge.Symbol("fk_player_character_inventory_transaction_item_id_id")),
		edge.To("player_character", PlayerCharacter.Type).Field("player_character_id").Unique().Required().StorageKey(edge.Symbol("fk_player_character_inventory_transaction_player_chara_2adebd14")),
	}
}

// Edges 返回 RPG Party 与其成员的关系。
func (PlayerCharacterParty) Edges() []ent.Edge {
	return []ent.Edge{
		edge.To("player_character", PlayerCharacter.Type).Field("player_character_id").Unique().Required().StorageKey(edge.Symbol("fk_player_character_party_player_character_id_id")),
		edge.From("members", PlayerCharacterPartyMember.Type).Ref("party"),
	}
}

// Edges 返回 Party 成员与 Party、Owned Creature 的关系。
func (PlayerCharacterPartyMember) Edges() []ent.Edge {
	return []ent.Edge{
		edge.To("party", PlayerCharacterParty.Type).Field("party_id").Unique().Required().StorageKey(edge.Symbol("fk_player_character_party_member_party_id_id")),
		edge.To("player_character_creature", PlayerCharacterCreature.Type).Field("player_character_creature_id").Unique().Required().StorageKey(edge.Symbol("fk_player_character_party_member_player_character_crea_53a3f01c")),
	}
}

// Edges 返回当前位置与 PlayerCharacter、Location 的关系。
func (PlayerCharacterPosition) Edges() []ent.Edge {
	return []ent.Edge{
		edge.To("player_character", PlayerCharacter.Type).Field("player_character_id").Unique().Required().StorageKey(edge.Symbol("fk_player_character_position_player_character_id_id")),
		edge.To("location", RpgLocation.Type).Field("location_id").Unique().Required().StorageKey(edge.Symbol("fk_player_character_position_location_id_id")),
	}
}

// Edges 返回地点发现与 PlayerCharacter、Location 的关系。
func (PlayerCharacterDiscoveredLocation) Edges() []ent.Edge {
	return []ent.Edge{
		edge.To("player_character", PlayerCharacter.Type).Field("player_character_id").Unique().Required().StorageKey(edge.Symbol("fk_player_character_discovered_location_player_character_id_id")),
		edge.To("location", RpgLocation.Type).Field("location_id").Unique().Required().StorageKey(edge.Symbol("fk_player_character_discovered_location_location_id_id")),
	}
}

// Edges 返回 RPG Checkpoint 与 Location 的关系。
func (RpgCheckpoint) Edges() []ent.Edge {
	return []ent.Edge{edge.To("location", RpgLocation.Type).Field("location_id").Unique().Required().StorageKey(edge.Symbol("fk_rpg_checkpoint_location_id_id"))}
}

// Edges 返回出口发现与 PlayerCharacter、Location Exit 的关系。
func (PlayerCharacterDiscoveredExit) Edges() []ent.Edge {
	return []ent.Edge{
		edge.To("player_character", PlayerCharacter.Type).Field("player_character_id").Unique().Required().StorageKey(edge.Symbol("fk_player_character_discovered_exit_player_character_id_id")),
		edge.To("location_exit", RpgLocationExit.Type).Field("location_exit_id").Unique().Required().StorageKey(edge.Symbol("fk_player_character_discovered_exit_location_exit_id_id")),
	}
}

// Edges 返回遭遇使用与 PlayerCharacter、Encounter Table 的关系。
func (PlayerCharacterEncounterUsage) Edges() []ent.Edge {
	return []ent.Edge{
		edge.To("player_character", PlayerCharacter.Type).Field("player_character_id").Unique().Required().StorageKey(edge.Symbol("fk_player_character_encounter_usage_player_character_id_id")),
		edge.To("encounter_table", RpgEncounterTable.Type).Field("encounter_table_id").Unique().Required().StorageKey(edge.Symbol("fk_player_character_encounter_usage_encounter_table_id_id")),
	}
}

// Edges 返回待处理遭遇与 PlayerCharacter、遭遇资料的关系。
func (PlayerCharacterPendingEncounter) Edges() []ent.Edge {
	return []ent.Edge{
		edge.To("player_character", PlayerCharacter.Type).Field("player_character_id").Unique().Required().StorageKey(edge.Symbol("fk_player_character_pending_encounter_player_character_id_id")),
		edge.To("traversal", PlayerCharacterTraversal.Type).Field("traversal_id").Unique().Required().StorageKey(edge.Symbol("fk_player_character_pending_encounter_traversal_id_id")),
		edge.To("encounter_table", RpgEncounterTable.Type).Field("encounter_table_id").Unique().Required().StorageKey(edge.Symbol("fk_player_character_pending_encounter_encounter_table_id_id")),
		edge.To("encounter_entry", RpgEncounterEntry.Type).Field("encounter_entry_id").Unique().Required().StorageKey(edge.Symbol("fk_player_character_pending_encounter_encounter_entry_id_id")),
		edge.To("battle", Battle.Type).Field("battle_id").Unique().StorageKey(edge.Symbol("fk_player_character_pending_encounter_battle_id_id")),
	}
}

// Edges 返回 Traversal 与角色、出口及两端地点的关系。
func (PlayerCharacterTraversal) Edges() []ent.Edge {
	return []ent.Edge{
		edge.To("player_character", PlayerCharacter.Type).Field("player_character_id").Unique().Required().StorageKey(edge.Symbol("fk_player_character_traversal_player_character_id_id")),
		edge.To("location_exit", RpgLocationExit.Type).Field("location_exit_id").Unique().Required().StorageKey(edge.Symbol("fk_player_character_traversal_location_exit_id_id")),
		edge.To("source_location", RpgLocation.Type).Field("source_location_id").Unique().Required().StorageKey(edge.Symbol("fk_player_character_traversal_source_location_id_id")),
		edge.To("target_location", RpgLocation.Type).Field("target_location_id").Unique().Required().StorageKey(edge.Symbol("fk_player_character_traversal_target_location_id_id")),
		edge.From("pending_encounter", PlayerCharacterPendingEncounter.Type).Ref("traversal"),
	}
}

// Edges 返回玩家幂等记录与 PlayerCharacter 的关系。
func (PlayerCharacterIdempotencyRecord) Edges() []ent.Edge {
	return []ent.Edge{edge.To("player_character", PlayerCharacter.Type).Field("player_character_id").Unique().Required().StorageKey(edge.Symbol("fk_player_character_idempotency_record_player_character_id_id"))}
}

// Edges 返回拓扑校验报告与问题明细的关系。
func (RpgTopologyIntegrityReport) Edges() []ent.Edge {
	return []ent.Edge{edge.From("issues", RpgTopologyIntegrityIssue.Type).Ref("report")}
}

// Edges 返回拓扑校验问题所属报告。
func (RpgTopologyIntegrityIssue) Edges() []ent.Edge {
	return []ent.Edge{edge.To("report", RpgTopologyIntegrityReport.Type).Field("report_id").Unique().Required().StorageKey(edge.Symbol("fk_rpg_topology_integrity_issue_report_id_id"))}
}

// Edges 返回 PlayerCharacter 职业关系。
func (PlayerCharacterProfession) Edges() []ent.Edge {
	return []ent.Edge{
		edge.To("player_character", PlayerCharacter.Type).Field("player_character_id").Unique().Required().StorageKey(edge.Symbol("fk_player_character_profession_player_character_id_id")),
		edge.To("profession", RpgProfession.Type).Field("profession_id").Unique().Required().StorageKey(edge.Symbol("fk_player_character_profession_profession_id_id")),
	}
}

// Edges 返回 PlayerCharacter 任务进度关系。
func (PlayerCharacterQuest) Edges() []ent.Edge {
	return []ent.Edge{
		edge.To("player_character", PlayerCharacter.Type).Field("player_character_id").Unique().Required().StorageKey(edge.Symbol("fk_player_character_quest_player_character_id_id")),
		edge.To("quest", RpgQuest.Type).Field("quest_id").Unique().Required().StorageKey(edge.Symbol("fk_player_character_quest_quest_id_id")),
	}
}

// Edges 返回 PlayerCharacter 队伍关系。
func (PlayerCharacterTeam) Edges() []ent.Edge {
	return []ent.Edge{edge.To("player_character", PlayerCharacter.Type).Field("player_character_id").Unique().Required().StorageKey(edge.Symbol("fk_player_character_team_player_character_id_id"))}
}

// Edges 返回队伍成员与队伍、Nature 的关系。
func (PlayerCharacterTeamMember) Edges() []ent.Edge {
	return []ent.Edge{
		edge.To("team", PlayerCharacterTeam.Type).Field("team_id").Unique().Required().StorageKey(edge.Symbol("fk_player_character_team_member_team_id_id")),
		edge.To("nature", GameNature.Type).Field("nature_id").Unique().Required().StorageKey(edge.Symbol("fk_player_character_team_member_nature_id_id")),
	}
}

// Edges 返回队伍分享与所有者角色的关系。
func (PlayerCharacterTeamShare) Edges() []ent.Edge {
	return []ent.Edge{edge.To("owner_player_character", PlayerCharacter.Type).Field("owner_player_character_id").Unique().Required().StorageKey(edge.Symbol("fk_player_character_team_share_owner_player_character_id_id"))}
}

// Edges 返回玩家钱包与 Currency、PlayerCharacter 的关系。
func (PlayerCharacterWallet) Edges() []ent.Edge {
	return []ent.Edge{
		edge.To("currency", GameCurrency.Type).Field("currency_id").Unique().Required().StorageKey(edge.Symbol("fk_player_character_wallet_currency_id_id")),
		edge.To("player_character", PlayerCharacter.Type).Field("player_character_id").Unique().Required().StorageKey(edge.Symbol("fk_player_character_wallet_player_character_id_id")),
	}
}

// Edges 返回玩家世界状态与 PlayerCharacter 的关系。
func (PlayerCharacterWorldState) Edges() []ent.Edge {
	return []ent.Edge{edge.To("player_character", PlayerCharacter.Type).Field("player_character_id").Unique().Required().StorageKey(edge.Symbol("fk_player_character_world_state_player_character_id_id"))}
}

// Edges 返回 Battle 账号占用关系。
func (BattleParticipantReservation) Edges() []ent.Edge {
	return []ent.Edge{
		edge.To("battle", Battle.Type).Field("battle_id").Unique().Required().StorageKey(edge.Symbol("fk_battle_participant_reservation_battle_id_id")),
	}
}

// Edges 返回对战权威摘要与 Battle 的关系。
func (BattleAuthoritativeSummary) Edges() []ent.Edge {
	return nil
}

// Edges 返回 Challenge 与赛制、双方身份和队伍的关系。
func (BattleChallenge) Edges() []ent.Edge {
	return []ent.Edge{
		edge.To("battle_format", GameBattleFormat.Type).Field("battle_format_id").Unique().Required().StorageKey(edge.Symbol("fk_battle_challenge_battle_format_id_id")),
		edge.To("challenger_account", Account.Type).Field("challenger_account_id").Unique().Required().StorageKey(edge.Symbol("fk_battle_challenge_challenger_account_id_id")),
		edge.To("challenger_player_character", PlayerCharacter.Type).Field("challenger_player_character_id").Unique().Required().StorageKey(edge.Symbol("fk_battle_challenge_challenger_player_character_id_id")),
		edge.To("challenger_team", PlayerCharacterTeam.Type).Field("challenger_team_id").Unique().Required().StorageKey(edge.Symbol("fk_battle_challenge_challenger_team_id_id")),
		edge.To("target_account", Account.Type).Field("target_account_id").Unique().Required().StorageKey(edge.Symbol("fk_battle_challenge_target_account_id_id")),
		edge.To("target_player_character", PlayerCharacter.Type).Field("target_player_character_id").Unique().Required().StorageKey(edge.Symbol("fk_battle_challenge_target_player_character_id_id")),
	}
}

// Edges 返回 Battle Disclosure Ledger 与 Battle 的关系。
func (BattleDisclosureLedger) Edges() []ent.Edge {
	return []ent.Edge{edge.To("battle", Battle.Type).Field("battle_id").Unique().Required().StorageKey(edge.Symbol("fk_battle_disclosure_ledger_battle_id_id"))}
}

// Edges 返回 Battle Outbox 与 Battle 的关系。
func (BattleOutbox) Edges() []ent.Edge {
	return []ent.Edge{edge.To("battle", Battle.Type).Field("battle_id").Unique().Required().StorageKey(edge.Symbol("fk_battle_outbox_battle_id_id"))}
}

// Edges 返回 Battle Participant 的账号、角色、队伍和 Battle 关系。
func (BattleParticipant) Edges() []ent.Edge {
	return []ent.Edge{
		edge.To("account", Account.Type).Field("account_id").Unique().StorageKey(edge.Symbol("fk_battle_participant_account_id_id")),
		edge.To("battle", Battle.Type).Field("battle_id").Unique().Required().StorageKey(edge.Symbol("fk_battle_participant_battle_id_id")),
		edge.To("player_character", PlayerCharacter.Type).Field("player_character_id").Unique().StorageKey(edge.Symbol("fk_battle_participant_player_character_id_id")),
		edge.To("source_team", PlayerCharacterTeam.Type).Field("source_team_id").Unique().StorageKey(edge.Symbol("fk_battle_participant_source_team_id_id")),
		edge.To("source_party", PlayerCharacterParty.Type).Field("source_party_id").Unique().StorageKey(edge.Symbol("fk_battle_participant_source_party_id_id")),
	}
}

// Edges 返回 Battle 预览提交与 Battle 的关系。
func (BattlePreviewSubmission) Edges() []ent.Edge {
	return []ent.Edge{edge.To("battle", Battle.Type).Field("battle_id").Unique().Required().StorageKey(edge.Symbol("fk_battle_preview_submission_battle_id_id"))}
}

// Edges 返回 Battle 与赛制、Challenge 和 Pending Encounter 的关系。
func (Battle) Edges() []ent.Edge {
	return []ent.Edge{
		edge.To("battle_format", GameBattleFormat.Type).Field("battle_format_id").Unique().Required().StorageKey(edge.Symbol("fk_battle_battle_format_id_id")),
		edge.To("challenge", BattleChallenge.Type).Field("challenge_id").Unique().StorageKey(edge.Symbol("fk_battle_challenge_id_id")),
		edge.To("pending_encounter", PlayerCharacterPendingEncounter.Type).Field("pending_encounter_id").Unique().StorageKey(edge.Symbol("fk_battle_pending_encounter_id_id")),
	}
}

// Edges 返回 Battle Recovery Attempt 与 Battle 的关系。
func (BattleRecoveryAttempt) Edges() []ent.Edge {
	return []ent.Edge{edge.To("battle", Battle.Type).Field("battle_id").Unique().Required().StorageKey(edge.Symbol("fk_battle_recovery_attempt_battle_id_id"))}
}

// Edges 返回 Battle Turn Record 与 Battle 的关系。
func (BattleTurnRecord) Edges() []ent.Edge {
	return []ent.Edge{edge.To("battle", Battle.Type).Field("battle_id").Unique().Required().StorageKey(edge.Symbol("fk_battle_turn_record_battle_id_id"))}
}

// Edges 返回 Battle Turn Submission 的角色关系。
func (BattleTurnSubmission) Edges() []ent.Edge {
	return []ent.Edge{edge.To("player_character", PlayerCharacter.Type).Field("player_character_id").Unique().StorageKey(edge.Symbol("fk_battle_turn_submission_player_character_id_id"))}
}

// Edges 返回活动角色及其当前队伍关系。
func (ActivePlayerCharacter) Edges() []ent.Edge {
	return []ent.Edge{
		edge.To("player_character", PlayerCharacter.Type).Field("player_character_id").Unique().Required().StorageKey(edge.Symbol("fk_active_player_character_player_character_id_id")),
	}
}

// Edges 返回活动角色队伍绑定关系。
func (ActivePlayerCharacterTeam) Edges() []ent.Edge {
	return []ent.Edge{
		edge.To("team", PlayerCharacterTeam.Type).Field("team_id").Unique().Required().StorageKey(edge.Symbol("fk_active_player_character_team_team_id_id")),
	}
}

// Edges 返回管理员审计与幂等记录的管理员关系。
func (AdminAuditLog) Edges() []ent.Edge {
	return []ent.Edge{edge.To("actor_account", AdminAccount.Type).Field("actor_account_id").Unique().StorageKey(edge.Symbol("fk_admin_audit_log_actor_account_id_id"))}
}

// Edges 返回管理员幂等记录的管理员关系。
func (AdminIdempotencyRecord) Edges() []ent.Edge {
	return []ent.Edge{edge.To("actor_account", AdminAccount.Type).Field("actor_account_id").Unique().Required().StorageKey(edge.Symbol("fk_admin_idempotency_record_actor_account_id_id"))}
}

// Edges 返回管理员登录尝试的可选管理员关系。
func (AdminLoginAttempt) Edges() []ent.Edge {
	return []ent.Edge{edge.To("account", AdminAccount.Type).Field("account_id").Unique().StorageKey(edge.Symbol("fk_admin_login_attempt_account_id_id"))}
}

// Edges 返回玩家审计与幂等记录的账号关系。
func (AdministrationAuditLog) Edges() []ent.Edge {
	return []ent.Edge{edge.To("actor_account", Account.Type).Field("actor_account_id").Unique().StorageKey(edge.Symbol("fk_security_audit_log_actor_account_id_id"))}
}

// Edges 返回玩家幂等记录的账号关系。
func (AdministrationIdempotencyRecord) Edges() []ent.Edge {
	return []ent.Edge{edge.To("actor_account", Account.Type).Field("actor_account_id").Unique().Required().StorageKey(edge.Symbol("fk_security_idempotency_record_actor_account_id_id"))}
}

// Edges 返回技能学习记录与 Creature、学习方式、Skill 的关系。
func (GameCreatureSkillLearn) Edges() []ent.Edge {
	return []ent.Edge{
		edge.To("creature", GameCreature.Type).Field("creature_id").Unique().Required().StorageKey(edge.Symbol("fk_game_creature_skill_learn_creature_id_id")),
		edge.To("learn_method", GameSkillLearnMethod.Type).Field("learn_method_id").Unique().Required().StorageKey(edge.Symbol("fk_game_creature_skill_learn_method_id_id")),
		edge.To("skill", GameSkill.Type).Field("skill_id").Unique().Required().StorageKey(edge.Symbol("fk_game_creature_skill_learn_skill_id_id")),
	}
}

// Edges 返回 Creature 皮肤与资源、Creature 的关系。
func (GameCreatureSkin) Edges() []ent.Edge {
	return []ent.Edge{
		edge.To("asset", Asset.Type).Field("asset_id").Unique().StorageKey(edge.Symbol("fk_game_creature_skin_asset_id_id")),
		edge.To("creature", GameCreature.Type).Field("creature_id").Unique().Required().StorageKey(edge.Symbol("fk_game_creature_skin_creature_id_id")),
	}
}

// Edges 返回物品目录分类的自引用父子关系。
func (GameItemCatalogCategory) Edges() []ent.Edge {
	return []ent.Edge{
		edge.To("parent", GameItemCatalogCategory.Type).Field("parent_id").Unique().StorageKey(edge.Symbol("fk_game_item_catalog_category_parent_id_id")),
		edge.From("children", GameItemCatalogCategory.Type).Ref("parent"),
	}
}

// Edges 返回物品目录分类绑定关系。
func (GameItemCatalogCategoryBinding) Edges() []ent.Edge {
	return []ent.Edge{
		edge.To("category", GameItemCatalogCategory.Type).Field("category_id").Unique().Required().StorageKey(edge.Symbol("fk_game_item_catalog_category_binding_category_id_id")),
		edge.To("item", GameItem.Type).Field("item_id").Unique().Required().StorageKey(edge.Symbol("fk_game_item_catalog_category_binding_item_id_id")),
	}
}

// Edges 返回 Nature 与增减属性的关系。
func (GameNature) Edges() []ent.Edge {
	return []ent.Edge{
		edge.To("increased_stat", GameStat.Type).Field("increased_stat_id").Unique().StorageKey(edge.Symbol("fk_game_nature_increased_stat_id_id")),
		edge.To("decreased_stat", GameStat.Type).Field("decreased_stat_id").Unique().StorageKey(edge.Symbol("fk_game_nature_decreased_stat_id_id")),
	}
}

// Edges 返回技能属性变化与 Skill、Stat 的关系。
func (GameSkillStatChange) Edges() []ent.Edge {
	return []ent.Edge{
		edge.To("skill", GameSkill.Type).Field("skill_id").Unique().Required().StorageKey(edge.Symbol("fk_game_skill_stat_change_skill_id_id")),
		edge.To("stat", GameStat.Type).Field("stat_id").Unique().Required().StorageKey(edge.Symbol("fk_game_skill_stat_change_stat_id_id")),
	}
}

// Edges 返回 RPG 配方原料与配方、道具的关系。
func (RpgRecipeIngredient) Edges() []ent.Edge {
	return []ent.Edge{
		edge.To("recipe", RpgRecipe.Type).Field("recipe_id").Unique().Required().StorageKey(edge.Symbol("fk_rpg_recipe_ingredient_recipe_id_id")),
		edge.To("item", GameItem.Type).Field("item_id").Unique().Required().StorageKey(edge.Symbol("fk_rpg_recipe_ingredient_item_id_id")),
	}
}

// Edges 返回 RPG 配方产出与配方、道具的关系。
func (RpgRecipeOutput) Edges() []ent.Edge {
	return []ent.Edge{
		edge.To("recipe", RpgRecipe.Type).Field("recipe_id").Unique().Required().StorageKey(edge.Symbol("fk_rpg_recipe_output_recipe_id_id")),
		edge.To("item", GameItem.Type).Field("item_id").Unique().Required().StorageKey(edge.Symbol("fk_rpg_recipe_output_item_id_id")),
	}
}

// Edges 返回 RPG 商店与地点、NPC 的关系。
func (RpgShop) Edges() []ent.Edge {
	return []ent.Edge{
		edge.To("location", RpgLocation.Type).Field("location_id").Unique().Required().StorageKey(edge.Symbol("fk_rpg_shop_location_id_id")),
		edge.To("npc", RpgNpc.Type).Field("npc_id").Unique().StorageKey(edge.Symbol("fk_rpg_shop_npc_id_id")),
	}
}

// Edges 返回商店商品与商店、道具、货币的关系。
func (RpgShopItem) Edges() []ent.Edge {
	return []ent.Edge{
		edge.To("currency", GameCurrency.Type).Field("currency_id").Unique().Required().StorageKey(edge.Symbol("fk_rpg_shop_item_currency_id_id")),
		edge.To("item", GameItem.Type).Field("item_id").Unique().Required().StorageKey(edge.Symbol("fk_rpg_shop_item_item_id_id")),
		edge.To("shop", RpgShop.Type).Field("shop_id").Unique().Required().StorageKey(edge.Symbol("fk_rpg_shop_item_shop_id_id")),
	}
}
