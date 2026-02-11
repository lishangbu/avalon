package io.github.lishangbu.avalon.dataset.entity;

import io.github.lishangbu.avalon.hibernate.Flex;
import jakarta.persistence.*;
import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/// 属性(Stat)实体类
///
/// 表示宝可梦的属性信息，如HP、攻击、防御等
///
/// @author lishangbu
/// @since 2026/2/11
@Data
@Entity
@Table(comment = "属性")
public class Stat implements Serializable {
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

  /// 游戏侧用于此属性的 ID
  @Column(comment = "游戏索引")
  private Integer gameIndex;

  /// 此属性是否仅在战斗中存在
  @Column(comment = "是否仅战斗中存在")
  private Boolean isBattleOnly;

  /// 与此属性相关的伤害类别
  @ManyToOne(cascade = CascadeType.ALL)
  @JoinColumn(
      name = "move_damage_class_id",
      referencedColumnName = "id",
      comment = "招式属性",
      foreignKey = @ForeignKey(name = "fk_stat_move_damage_class_id"))
  private MoveDamageClass moveDamageClass;
}
