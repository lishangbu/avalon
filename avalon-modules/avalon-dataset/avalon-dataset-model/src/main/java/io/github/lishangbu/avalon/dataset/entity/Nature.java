package io.github.lishangbu.avalon.dataset.entity;

import io.github.lishangbu.avalon.hibernate.Flex;
import jakarta.persistence.*;
import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/// 性格(Nature)实体类
///
/// 表示宝可梦的性格信息，影响属性成长方式
///
/// @author lishangbu
/// @since 2026/2/12
@Data
@Entity
@Table(comment = "性格")
public class Nature implements Serializable {
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

  /// 降低 10% 的属性
  @ManyToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "decreased_stat_id", comment = "降低 10% 的属性")
  private Stat decreasedStat;

  /// 增加 10% 的属性
  @ManyToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "increased_stat_id", comment = "增加 10% 的属性")
  private Stat increasedStat;

  /// 讨厌的口味
  @ManyToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "hates_berry_flavor_id", comment = "讨厌的口味")
  private BerryFlavor hatesFlavor;

  /// 喜欢的口味
  @ManyToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "likes_berry_flavor_id", comment = "喜欢的口味")
  private BerryFlavor likesFlavor;
}
