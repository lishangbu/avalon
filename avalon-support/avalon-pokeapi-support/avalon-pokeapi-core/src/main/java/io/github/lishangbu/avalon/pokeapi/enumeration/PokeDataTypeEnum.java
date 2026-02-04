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
import io.github.lishangbu.avalon.pokeapi.model.item.*;
import io.github.lishangbu.avalon.pokeapi.model.machine.Machine;
import io.github.lishangbu.avalon.pokeapi.model.move.*;
import io.github.lishangbu.avalon.pokeapi.model.pokemon.Type;

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

  /// 遭遇方式（玩家在野外遇到宝可梦的方式）
  ENCOUNTER_METHOD("encounter-method", EncounterMethod.class),

  /// 遭遇条件（影响野外出现宝可梦的条件，例如白天/夜晚）
  ENCOUNTER_CONDITION("encounter-condition", EncounterCondition.class),

  /// 遭遇条件的取值列表
  ENCOUNTER_CONDITION_VALUE("encounter-condition-value", EncounterConditionValue.class),

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

  /// 属性（例如水、火等），以及属性间的相互克制关系
  TYPE("type", Type.class);

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
