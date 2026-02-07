package io.github.lishangbu.avalon.dataset.entity;

import io.github.lishangbu.avalon.hibernate.Flex;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/// 宝可梦(Pokemon)实体类
///
/// 表示宝可梦的基本信息，包括内部名称、身高、体重与基础经验等
///
/// @author lishangbu
/// @since 2025/08/20
@Data
@Entity
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

  /// 身高，单位为分米
  @Column(comment = "身高，单位为分米")
  private Integer height;

  /// 体重，数字每增加1，体重增加0.1kg
  @Column(comment = "体重，数字每增加1，体重增加0.1kg")
  private Integer weight;

  /// 基础经验值
  @Column(comment = "基础经验值")
  private Integer baseExperience;
}
