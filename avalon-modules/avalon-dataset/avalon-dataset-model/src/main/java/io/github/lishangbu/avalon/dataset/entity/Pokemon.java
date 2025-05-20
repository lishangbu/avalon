package io.github.lishangbu.avalon.dataset.entity;

import jakarta.persistence.*;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;

/**
 * 宝可梦
 *
 * @author lishangbu
 * @since 2025/4/21
 */
@Entity
@Table(
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_pokemon_internal_name",
          columnNames = {"internal_name"})
    })
public class Pokemon implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  /** ID */
  @Id
  @Comment("主键")
  private Integer id;

  /**
   * 名称
   *
   * <p>取百科中的中文名数据
   */
  @Column(nullable = false, length = 100)
  @ColumnDefault("''")
  @Comment("名称")
  private String name;

  /**
   * 内部名称
   *
   * <p>取百科中的英文名数据
   */
  @Column(nullable = false, length = 100)
  @ColumnDefault("''")
  @Comment("内部名称")
  private String internalName;

  /** 身高，数字每增加1，身高增加0.1m */
  @Comment("身高")
  @Column(nullable = false)
  @ColumnDefault("0")
  private Integer height;

  /** 身高，数字每增加1，体重增加0.1kg */
  @Comment("体重")
  @Column(nullable = false)
  @ColumnDefault("0")
  private Integer weight;

  @Comment("基础经验值")
  @Column(nullable = false)
  @ColumnDefault("0")
  private Integer baseExperience;

  @OneToMany(mappedBy = "pokemon")
  private List<PokemonType> pokemonTypes;

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getInternalName() {
    return internalName;
  }

  public void setInternalName(String internalName) {
    this.internalName = internalName;
  }

  public Integer getHeight() {
    return height;
  }

  public void setHeight(Integer height) {
    this.height = height;
  }

  public Integer getWeight() {
    return weight;
  }

  public void setWeight(Integer weight) {
    this.weight = weight;
  }

  public Integer getBaseExperience() {
    return baseExperience;
  }

  public void setBaseExperience(Integer baseExperience) {
    this.baseExperience = baseExperience;
  }

  public List<PokemonType> getPokemonTypes() {
    return pokemonTypes;
  }

  public void setPokemonTypes(List<PokemonType> pokemonTypes) {
    this.pokemonTypes = pokemonTypes;
  }
}
