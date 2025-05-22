package io.github.lishangbu.avalon.dataset.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Comment;

/**
 * 宝可梦类型
 *
 * @author lishangbu
 * @since 2025/5/20
 */
@Entity
public class PokemonType {
  /** ID */
  @Id
  @Comment("主键")
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "pokemon_type_seq_gen")
  @TableGenerator(
      name = "pokemon_type_seq_gen",
      table = "hibernate_sequences",
      pkColumnValue = "pokemon_type")
  private Integer id;

  @ManyToOne
  @JoinColumn(
      name = "type_id",
      nullable = false,
      foreignKey = @ForeignKey(name = "fk_pokemon_type_type_id"))
  @Comment("属性")
  private Type type;

  @ManyToOne
  @JoinColumn(
      name = "pokemon_id",
      nullable = false,
      foreignKey = @ForeignKey(name = "fk_pokemon_type_pokemon_id"))
  @Comment("宝可梦")
  private Pokemon pokemon;

  @Column(nullable = false)
  @Comment("是否是主属性")
  private Boolean primaryType;

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public Type getType() {
    return type;
  }

  public void setType(Type type) {
    this.type = type;
  }

  public Pokemon getPokemon() {
    return pokemon;
  }

  public void setPokemon(Pokemon pokemon) {
    this.pokemon = pokemon;
  }

  public Boolean getPrimaryType() {
    return primaryType;
  }

  public void setPrimaryType(Boolean primaryType) {
    this.primaryType = primaryType;
  }
}
