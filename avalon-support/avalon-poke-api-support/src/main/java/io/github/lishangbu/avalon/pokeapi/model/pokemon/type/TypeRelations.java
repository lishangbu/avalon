package io.github.lishangbu.avalon.pokeapi.model.pokemon.type;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import java.util.List;

/**
 * 参考<a href="https://pokeapi.co/docs/v2">官网Pokémon/Types/TypeRelations</a>
 *
 * @author lishangbu
 * @since 2025/5/20
 */
public record TypeRelations(
    @JsonProperty("no_damage_to") List<NamedApiResource<Type>> noDamageTo,
    @JsonProperty("half_damage_to") List<NamedApiResource<Type>> halfDamageTo,
    @JsonProperty("double_damage_to") List<NamedApiResource<Type>> doubleDamageTo,
    @JsonProperty("no_damage_from") List<NamedApiResource<Type>> noDamageFrom,
    @JsonProperty("half_damage_from") List<NamedApiResource<Type>> halfDamageFrom,
    @JsonProperty("double_damage_from") List<NamedApiResource<Type>> doubleDamageFrom) {
  /** 获取此属性无效的属性列表 */
  public List<NamedApiResource<Type>> noDamageTo() {
    return noDamageTo;
  }

  /** 获取此属性效果较弱的属性列表 */
  public List<NamedApiResource<Type>> halfDamageTo() {
    return halfDamageTo;
  }

  /** 获取此属性效果强的属性列表 */
  public List<NamedApiResource<Type>> doubleDamageTo() {
    return doubleDamageTo;
  }

  /** 获取对该属性无效的属性列表 */
  public List<NamedApiResource<Type>> noDamageFrom() {
    return noDamageFrom;
  }

  /** 获取对该属性效果较弱的属性列表 */
  public List<NamedApiResource<Type>> halfDamageFrom() {
    return halfDamageFrom;
  }

  /** 获取对该属性效果强的属性列表 */
  public List<NamedApiResource<Type>> doubleDamageFrom() {
    return doubleDamageFrom;
  }
}
