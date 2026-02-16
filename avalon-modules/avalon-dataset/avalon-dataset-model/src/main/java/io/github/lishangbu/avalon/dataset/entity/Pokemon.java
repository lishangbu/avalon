package io.github.lishangbu.avalon.dataset.entity;

import io.github.lishangbu.avalon.hibernate.Flex;
import jakarta.persistence.*;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import lombok.Data;

/// 宝可梦(Pokemon)实体类
///
/// 表示宝可梦的基本信息，包括内部名称、身高、体重与基础经验等
///
/// @author lishangbu
/// @since 2025/08/20
@Data
@Entity
@Table(comment = "宝可梦")
public class Pokemon implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  /// 主键
  @Id
  @Flex
  @Column(comment = "主键")
  private Long id;

  /// 宝可梦内部名称
  @Column(comment = "宝可梦内部名称", length = 100)
  private String internalName;

  /// 宝可梦名称
  @Column(comment = "宝可梦名称", length = 100)
  private String name;

  /// 身高（以分米为单位）
  @Column(comment = "身高（以分米为单位）")
  private Integer height;

  /// 体重（以百克为单位）
  @Column(comment = "体重（以百克为单位）")
  private Integer weight;

  /// 基础经验值
  @Column(comment = "基础经验值")
  private Integer baseExperience;

  /// 用于排序的顺序
  @Column(comment = "用于排序的顺序")
  private Integer sortingOrder;

  /// 宝可梦种类
  @ManyToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "pokemon_species_id", comment = "宝可梦种类")
  private PokemonSpecies pokemonSpecies;

  /// 宝可梦特性列表
  @OneToMany(mappedBy = "id.pokemon", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<PokemonAbility> abilities;
}
