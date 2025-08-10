package io.github.lishangbu.avalon.dataset.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 宝可梦类型
 *
 * @author lishangbu
 * @since 2025/5/20
 */
@Table
public class PokemonType {
  /** ID */
  @Id private Integer id;

  /** 属性内部名称 */
  private String typeInternalName;

  /** 宝可梦内部名称 */
  private String pokemonInternalName;

  /** 属性排序，第一个为主属性 */
  private Integer sortingOrder;

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getTypeInternalName() {
    return typeInternalName;
  }

  public void setTypeInternalName(String typeInternalName) {
    this.typeInternalName = typeInternalName;
  }

  public String getPokemonInternalName() {
    return pokemonInternalName;
  }

  public void setPokemonInternalName(String pokemonInternalName) {
    this.pokemonInternalName = pokemonInternalName;
  }

  public Integer getSortingOrder() {
    return sortingOrder;
  }

  public void setSortingOrder(Integer sortingOrder) {
    this.sortingOrder = sortingOrder;
  }
}
