package io.github.lishangbu.avalon.pokeapi.enumeration;

import io.github.lishangbu.avalon.pokeapi.model.berry.Berry;
import io.github.lishangbu.avalon.pokeapi.model.berry.BerryFirmness;
import io.github.lishangbu.avalon.pokeapi.model.contest.ContestEffect;
import io.github.lishangbu.avalon.pokeapi.model.contest.ContestType;
import io.github.lishangbu.avalon.pokeapi.model.contest.SuperContestEffect;
import io.github.lishangbu.avalon.pokeapi.model.encounter.EncounterCondition;
import io.github.lishangbu.avalon.pokeapi.model.encounter.EncounterConditionValue;
import io.github.lishangbu.avalon.pokeapi.model.encounter.EncounterMethod;
import io.github.lishangbu.avalon.pokeapi.model.item.*;
import io.github.lishangbu.avalon.pokeapi.model.move.*;
import io.github.lishangbu.avalon.pokeapi.model.pokemon.Type;

/**
 * Poke Api 端点枚举
 *
 * @author lishangbu
 * @since 2025/5/22
 */
public enum PokeApiEndpointEnum {
  /**
   * 树果是可以为宝可梦恢复HP和异常状态、提升属性，甚至在食用时抵消伤害的小型水果。详情可参考<a
   * href="http://bulbapedia.bulbagarden.net/wiki/Category:Berries">Bulbapedia</a>
   */
  BERRY("berry", Berry.class),

  /**
   * 树果可以是软的或硬的。更多信息可参考 <a
   * href="http://bulbapedia.bulbagarden.net/wiki/Category:Berries_by_firmness">Bulbapedia</a>
   */
  BERRY_FIRMNESS("berry-firmness", BerryFirmness.class),

  /** 华丽大赛效果指的是招式在华丽大赛中使用时产生的效果 */
  CONTEST_EFFECT("contest-effect", ContestEffect.class),
  /** 超级华丽大赛效果指的是招式在超级华丽大赛中使用时产生的效果 */
  SUPER_CONTEST_EFFECT("super-contest-effect", SuperContestEffect.class),
  /**
   * 竞赛类型是评委在宝可梦华丽大赛中用于衡量宝可梦状态的类别。详情可参考<a
   * href="http://bulbapedia.bulbagarden.net/wiki/Contest_condition">Bulbapedia</a>
   */
  CONTEST_TYPE("contest-type", ContestType.class),

  /**
   * 玩家在野外遇到宝可梦的方式，例如在高草中行走。详情可参考<a
   * href="http://bulbapedia.bulbagarden.net/wiki/Category:Berries_by_firmness">Bulbapedia</a>
   */
  ENCOUNTER_METHOD("encounter-method", EncounterMethod.class),
  /**
   * 遭遇条件是影响野外出现宝可梦的条件，例如白天或夜晚，详情可参考<a
   * href="https://bulbapedia.bulbagarden.net/wiki/Wild_Pok%C3%A9mon">Bulbapedia</a>
   */
  ENCOUNTER_CONDITION("encounter-condition", EncounterCondition.class),
  /** 遭遇条件值是遭遇条件的所有可能取值列表 */
  ENCOUNTER_CONDITION_VALUE("encounter-condition-value", EncounterConditionValue.class),

  /** 道具是一种能够被收集和使用的对象，例如在宝可梦的世界中可以使用药剂、球，或者教授给宝可梦技能的技能机器等。 */
  ITEM("item", Item.class),
  /** 物品属性定义了物品的特定方面，例如"可在战斗中使用"或"可消耗" */
  ITEM_ATTRIBUTE("item-attribute", ItemAttribute.class),

  /** 道具类别决定了道具在玩家背包中的放置位置 */
  ITEM_CATEGORY("item-category", ItemCategory.class),

  /** 技能"投掷"与不同道具一起使用时的各种效果 */
  ITEM_FLING_EFFECT("item-fling-effect", ItemFlingEffect.class),
  /** 玩家背包中用于按类别存储道具的口袋 */
  ITEM_POCKET("item-pocket", ItemPocket.class),
  /** 招式导致的状态异常是战斗中使用招式造成的状态条件 */
  MOVE_AILMENT("move-ailment", MoveAilment.class),
  /** 非常宽泛的分类，松散地将招式效果分组 */
  MOVE_CATEGORY("move-category", MoveCategory.class),
  /** 招式可以拥有的伤害类别，例如物理、特殊或非伤害性 */
  MOVE_DAMAGE_CLASS("move-damage-class", MoveDamageClass.class),
  /** 宝可梦可以学习招式的方法 */
  MOVE_LEARN_METHOD("move-learn-method", MoveLearnMethod.class),
  /** 战斗中招式可以指向的目标。目标可以是宝可梦、环境甚至其他招式 */
  MOVE_TARGET("move-target", MoveTarget.class),

  /** 属性是宝可梦及其招式的特性。每种属性有三种特性：对哪些属性的宝可梦效果拔群、对哪些属性效果不佳、对哪些属性完全无效 */
  TYPE("type", Type.class);

  /** POKE API接口访问URI */
  private final String uri;

  /** 返回的资源映射到对应的Class上 */
  private final Class responseType;

  PokeApiEndpointEnum(String uri, Class responseType) {
    this.uri = uri;
    this.responseType = responseType;
  }

  public String getUri() {
    return uri;
  }

  public Class getResponseType() {
    return responseType;
  }
}
