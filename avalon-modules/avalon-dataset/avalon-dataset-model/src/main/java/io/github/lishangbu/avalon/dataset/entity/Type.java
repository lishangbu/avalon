package io.github.lishangbu.avalon.dataset.entity;

import jakarta.persistence.*;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;

/**
 * 属性
 *
 * @author lishangbu
 * @since 2025/4/14
 */
@Comment("属性")
@Entity
@Table(
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_type_internal_name",
          columnNames = {"internal_name"})
    })
public class Type implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  /** ID */
  @Id
  @Comment("主键")
  private Integer id;

  /**
   * 内部名称
   *
   * <p>取百科中的属性英文名称
   */
  @Column(nullable = false, length = 20)
  @ColumnDefault("''")
  @Comment("内部名称")
  private String internalName;

  /** 属性名称 */
  @Comment("属性名称")
  @ColumnDefault("''")
  @Column(nullable = false, length = 20)
  private String name;

  /**
   * 一种属性有多个招式
   *
   * @see Move
   * @see Move#getType()
   */
  @OneToMany(mappedBy = "type")
  private List<Move> moves;

  @OneToMany(mappedBy = "type")
  private List<PokemonType> pokemonTypes;

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getInternalName() {
    return internalName;
  }

  public void setInternalName(String internalName) {
    this.internalName = internalName;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public List<Move> getMoves() {
    return moves;
  }

  public void setMoves(List<Move> moves) {
    this.moves = moves;
  }

  public List<PokemonType> getPokemonTypes() {
    return pokemonTypes;
  }

  public void setPokemonTypes(List<PokemonType> pokemonTypes) {
    this.pokemonTypes = pokemonTypes;
  }
}
