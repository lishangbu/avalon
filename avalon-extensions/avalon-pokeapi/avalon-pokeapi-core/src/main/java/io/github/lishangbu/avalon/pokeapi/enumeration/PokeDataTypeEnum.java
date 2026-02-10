package io.github.lishangbu.avalon.pokeapi.enumeration;

import io.github.lishangbu.avalon.pokeapi.model.berry.Berry;
import io.github.lishangbu.avalon.pokeapi.model.berry.BerryFirmness;
import io.github.lishangbu.avalon.pokeapi.model.berry.BerryFlavor;
import io.github.lishangbu.avalon.pokeapi.model.contest.ContestEffect;
import io.github.lishangbu.avalon.pokeapi.model.contest.ContestType;
import io.github.lishangbu.avalon.pokeapi.model.contest.SuperContestEffect;
import io.github.lishangbu.avalon.pokeapi.model.encounter.EncounterCondition;
import io.github.lishangbu.avalon.pokeapi.model.encounter.EncounterConditionValue;
import io.github.lishangbu.avalon.pokeapi.model.encounter.EncounterMethod;
import io.github.lishangbu.avalon.pokeapi.model.evolution.EvolutionChain;
import io.github.lishangbu.avalon.pokeapi.model.evolution.EvolutionTrigger;
import io.github.lishangbu.avalon.pokeapi.model.item.*;
import io.github.lishangbu.avalon.pokeapi.model.location.Location;
import io.github.lishangbu.avalon.pokeapi.model.location.LocationArea;
import io.github.lishangbu.avalon.pokeapi.model.location.Region;
import io.github.lishangbu.avalon.pokeapi.model.machine.Machine;
import io.github.lishangbu.avalon.pokeapi.model.move.*;
import io.github.lishangbu.avalon.pokeapi.model.pokemon.*;
import io.github.lishangbu.avalon.pokeapi.model.pokemon.GrowthRate;

/// Poke API 端点枚举
///
/// 定义 PokeAPI 提供的资源类型及其对应的响应映射类型
///
/// @author lishangbu
/// @since 2025/5/22
public enum PokeDataTypeEnum {
  /// 树果：可为宝可梦恢复 HP、异常状态或提供其他效果的水果
  BERRY("berry", Berry.class),

  /// 树果的硬度分类
  BERRY_FIRMNESS("berry-firmness", BerryFirmness.class),

  /// 树果的风味信息
  BERRY_FLAVOR("berry-flavor", BerryFlavor.class),

  /// 华丽大赛中招式的效果
  CONTEST_EFFECT("contest-effect", ContestEffect.class),

  /// 超级华丽大赛中招式的效果
  SUPER_CONTEST_EFFECT("super-contest-effect", SuperContestEffect.class),

  /// 竞赛类型（华丽大赛评判的类别）
  CONTEST_TYPE("contest-type", ContestType.class),

  /// 蛋组 决定哪些宝可梦能够互相繁殖
  EGG_GROUP("egg-group", EggGroup.class),

  /// 遭遇方式（玩家在野外遇到宝可梦的方式）
  ENCOUNTER_METHOD("encounter-method", EncounterMethod.class),

  /// 遭遇条件（影响野外出现宝可梦的条件，例如白天/夜晚）
  ENCOUNTER_CONDITION("encounter-condition", EncounterCondition.class),

  /// 遭遇条件的取值列表
  ENCOUNTER_CONDITION_VALUE("encounter-condition-value", EncounterConditionValue.class),

  /// 性别 主要用于宝可梦繁殖，并可能影响外观或进化，详情参考 [Bulbapedia](http://bulbapedia.bulbagarden.net/wiki/Gender)
  GENDER("gender", Gender.class),

  /// 宝可梦颜色 用于在图鉴中根据宝可梦的主色对其进行分类
  POKEMON_COLOR("pokemon-color", PokemonColor.class),

  /// 宝可梦形态 表示宝可梦的视觉形态，适用于纯粹的外观变化
  POKEMON_FORM("pokemon-form", PokemonForm.class),

  /// 宝可梦栖息地 表示宝可梦可能被发现的地形或区域
  POKEMON_HABITAT("pokemon-habitat", PokemonHabitat.class),

  /// 宝可梦形状 用于在图鉴中根据宝可梦的形状对其进行分类
  POKEMON_SHAPE("pokemon-shape", PokemonShape.class),

  /// 宝可梦种类 表示至少一种宝可梦的基础，其属性在该种类内的所有变种中共享
  POKEMON_SPECIES("pokemon-species", PokemonSpecies.class),

  /// 宝可梦 表示栖息在宝可梦游戏世界中的生物
  POKEMON("pokemon", Pokemon.class),

  /// 特征 表示哪个属性包含宝可梦的最高个体值 (IV)
  CHARACTERISTIC("characteristic", Characteristic.class),

  /// 特性 为宝可梦提供被动效果
  ABILITY("ability", Ability.class),

  /// 成长速率 表示宝可梦通过经验获得等级的速度
  GROWTH_RATE("growth-rate", GrowthRate.class),

  /// 属性 决定了战斗的某些方面；可随等级增长并受战斗效果临时改变
  STAT("stat", Stat.class),

  /// 性格 影响宝可梦的属性成长方式
  NATURE("nature", Nature.class),

  /// 道具（可收集和使用的物品）
  ITEM("item", Item.class),

  /// 道具属性（例如是否可在战斗中使用）
  ITEM_ATTRIBUTE("item-attribute", ItemAttribute.class),

  /// 道具类别（背包中的存放分类）
  ITEM_CATEGORY("item-category", ItemCategory.class),

  /// 道具投掷时的效果
  ITEM_FLING_EFFECT("item-fling-effect", ItemFlingEffect.class),

  /// 背包中按类别存放道具的口袋
  ITEM_POCKET("item-pocket", ItemPocket.class),

  /// 教授招式的机器（例如 TM / HM 等）
  MACHINE("machine", Machine.class),

  /// 招式（宝可梦在战斗中的技能）
  MOVE("move", Move.class),

  /// 招式造成的异常状态或副作用
  MOVE_AILMENT("move-ailment", MoveAilment.class),

  /// 招式的类别（宽泛分类，用于分组效果）
  MOVE_CATEGORY("move-category", MoveCategory.class),

  /// 招式的伤害类别（物理 / 特殊 / 非伤害）
  MOVE_DAMAGE_CLASS("move-damage-class", MoveDamageClass.class),

  /// 招式学习方式
  MOVE_LEARN_METHOD("move-learn-method", MoveLearnMethod.class),

  /// 招式目标（战斗中招式可指向的对象）
  MOVE_TARGET("move-target", MoveTarget.class),

  /// 招式战斗风格（对战开拓区中使用的招式风格）
  MOVE_BATTLE_STYLE("move-battle-style", MoveBattleStyle.class),

  /// 进化触发器 表示导致宝可梦进化的事件与条件
  EVOLUTION_TRIGGER("evolution-trigger", EvolutionTrigger.class),

  /// 进化链 表示宝可梦进化的全过程，包括所有中间阶段
  EVOLUTION_CHAIN("evolution-chain", EvolutionChain.class),

  /// 属性（例如水、火等），以及属性间的相互克制关系
  TYPE("type", Type.class),

  /// 地区（例如关卡、城镇等），宝可梦的栖息地或出现区域
  REGION("region", Region.class),

  /// 位置（更具体的地理区域，通常是宝可梦的具体栖息地）
  LOCATION("location", Location.class),

  /// 位置区域（位置内的具体区域或地点）
  LOCATION_AREA("location-area", LocationArea.class);

  /// Poke API 数据类型标识
  private final String type;

  /// 响应映射到的 Java 类型
  private final Class<?> responseType;

  PokeDataTypeEnum(String type, Class<?> responseType) {
    this.type = type;
    this.responseType = responseType;
  }

  public String getType() {
    return type;
  }

  public Class<?> getResponseType() {
    return responseType;
  }

  public static PokeDataTypeEnum getDataTypeByTypeName(String typename) {
    for (PokeDataTypeEnum endpoint : PokeDataTypeEnum.values()) {
      if (endpoint.getType().equals(typename)) {
        return endpoint;
      }
    }
    throw new IllegalArgumentException("No poke-api data type found for typename: " + typename);
  }
}
