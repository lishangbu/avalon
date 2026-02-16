package io.github.lishangbu.avalon.dataset.entity;

import io.github.lishangbu.avalon.hibernate.Flex;
import jakarta.persistence.*;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import lombok.Data;

/// 宝可梦种类(PokemonSpecies)实体类
///
/// 表示宝可梦种类的基本信息，包括性别比例、捕获率、进化等属性
///
/// @author lishangbu
/// @since 2026/2/12
@Data
@Entity
@Table(comment = "宝可梦种类")
public class PokemonSpecies implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  /// 主键
  @Id
  @Flex
  @Column(comment = "主键")
  private Long id;

  /// 内部名称
  @Column(comment = "内部名称", length = 100)
  private String internalName;

  /// 显示名称
  @Column(comment = "显示名称", length = 100)
  private String name;

  /// 排序顺序
  @Column(comment = "排序顺序")
  private Integer sortingOrder;

  /// 性别比例
  @Column(comment = "性别比例")
  private Integer genderRate;

  /// 捕获率
  @Column(comment = "捕获率")
  private Integer captureRate;

  /// 基础幸福度
  @Column(comment = "基础幸福度")
  private Integer baseHappiness;

  /// 是否为幼年形态
  @Column(comment = "是否为幼年形态")
  private Boolean isBaby;

  /// 是否为传说宝可梦
  @Column(comment = "是否为传说宝可梦")
  private Boolean isLegendary;

  /// 是否为神话宝可梦
  @Column(comment = "是否为神话宝可梦")
  private Boolean isMythical;

  /// 孵化周期
  @Column(comment = "孵化周期")
  private Integer hatchCounter;

  /// 是否有性别差异
  @Column(comment = "是否有性别差异")
  private Boolean hasGenderDifferences;

  /// 形态是否可切换
  @Column(comment = "形态是否可切换")
  private Boolean formsSwitchable;

  /// 成长速率
  @ManyToOne
  @JoinColumn(name = "growth_rate_id", comment = "成长速率")
  private GrowthRate growthRate;

  /// 颜色
  @ManyToOne
  @JoinColumn(name = "pokemon_color_id", comment = "颜色")
  private PokemonColor pokemonColor;

  /// 形状
  @ManyToOne
  @JoinColumn(name = "pokemon_shape_id", comment = "形状")
  private PokemonShape pokemonShape;

  /// 进化来源种类 ID
  @Column(name = "evolves_from_species_id", comment = "进化来源种类ID")
  private Long evolvesFromSpeciesId;

  /// 进化链 ID
  @Column(name = "evolution_chain_id", comment = "进化链ID")
  private Long evolutionChainId;

  /// 栖息地
  @ManyToOne
  @JoinColumn(name = "pokemon_habitat_id", comment = "栖息地")
  private PokemonHabitat pokemonHabitat;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    PokemonSpecies that = (PokemonSpecies) o;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
