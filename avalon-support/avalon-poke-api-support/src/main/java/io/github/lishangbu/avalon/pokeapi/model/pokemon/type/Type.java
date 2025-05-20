package io.github.lishangbu.avalon.pokeapi.model.pokemon.type;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.GenerationGameIndex;
import io.github.lishangbu.avalon.pokeapi.model.common.Name;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import java.util.List;

/**
 * 属性是宝可梦及其招式的属性。每种类型有三个特性：对哪些类型的宝可梦效果绝佳、对哪些类型的宝可梦效果不佳，以及对哪些类型的宝可梦完全无效。
 *
 * <p>参考<a href="https://pokeapi.co/docs/v2">官网Pokémon/Types/Type</a>
 *
 * @author lishangbu
 * @since 2025/5/20
 */
public record Type(
    Integer id,
    String name,
    @JsonProperty("damage_relations") TypeRelations damageRelations,
    @JsonProperty("past_damage_relations") List<TypeRelationsPast<?>> pastDamageRelations,
    @JsonProperty("game_indices") List<GenerationGameIndex> gameIndices,
    NamedApiResource<?> generation,
    @JsonProperty("move_damage_class") NamedApiResource<?> moveDamageClass,
    List<Name> names,
    List<TypePokemon> pokemon,
    List<NamedApiResource<?>> moves) {
  /** 获取此资源的标识符 */
  public Integer id() {
    return id;
  }

  /** 获取此资源的名称 */
  public String name() {
    return name;
  }

  /** 获取此属性与其他属性之间的有效性关系 */
  public TypeRelations damageRelations() {
    return damageRelations;
  }

  /** 获取此属性在之前世代中的有效性关系 */
  public List<TypeRelationsPast<?>> pastDamageRelations() {
    return pastDamageRelations;
  }

  /** 获取与此项相关的各代游戏索引 */
  public List<GenerationGameIndex> gameIndices() {
    return gameIndices;
  }

  /** 获取此属性首次出现的世代 */
  public NamedApiResource<?> generation() {
    return generation;
  }

  /** 获取此属性的伤害类别 */
  public NamedApiResource<?> moveDamageClass() {
    return moveDamageClass;
  }

  /** 获取此资源在不同语言中的名称 */
  @Override
  public List<Name> names() {
    return names;
  }

  /** 获取拥有此属性的宝可梦列表 */
  public List<TypePokemon> pokemon() {
    return pokemon;
  }

  /** 获取具有此属性的招式列表 */
  public List<NamedApiResource<?>> moves() {
    return moves;
  }
}
