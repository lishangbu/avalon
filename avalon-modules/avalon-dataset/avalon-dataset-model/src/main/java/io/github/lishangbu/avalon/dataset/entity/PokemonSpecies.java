package io.github.lishangbu.avalon.dataset.entity;

import io.github.lishangbu.avalon.hibernate.Flex;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serial;
import java.io.Serializable;
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

  /// 成长速率 ID
  @Column(comment = "成长速率ID")
  private Long growthRateId;

  /// 颜色 ID
  @Column(comment = "颜色ID")
  private Long colorId;

  /// 形状 ID
  @Column(comment = "形状ID")
  private Long shapeId;

  /// 进化来源种类 ID
  @Column(comment = "进化来源种类ID")
  private Long evolvesFromSpeciesId;

  /// 进化链 ID
  @Column(comment = "进化链ID")
  private Long evolutionChainId;

  /// 栖息地 ID
  @Column(comment = "栖息地ID")
  private Long habitatId;

  /// 世代 ID
  @Column(comment = "世代ID")
  private Long generationId;
}
