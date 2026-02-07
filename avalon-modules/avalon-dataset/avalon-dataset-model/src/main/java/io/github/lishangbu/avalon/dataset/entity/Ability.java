package io.github.lishangbu.avalon.dataset.entity;

import io.github.lishangbu.avalon.hibernate.Flex;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.io.Serial;
import java.io.Serializable;
import lombok.*;

/// 特性(Ability)实体类
///
/// 表示宝可梦特性的元数据，包括效果、介绍与文本描述
///
/// @author lishangbu
/// @since 2025/08/20
@Data
@Entity
public class Ability implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  /// 主键
  @Id
  @Flex
  @Column(comment = "主键")
  private Long id;

  /// 内部名称
  @Column(comment = "内部名称", length = 100)
  private String internalName;

  /// 名称
  @Column(comment = "名称", length = 100)
  private String name;

  /// 特性效果
  @Column(comment = "特性效果", length = 1000)
  private String effect;

  /// 基本信息
  @Column(comment = "基本信息", length = 500)
  private String info;

  /// 文字介绍
  @Column(comment = "文字介绍", length = 500)
  private String text;
}
