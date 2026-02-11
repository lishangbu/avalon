package io.github.lishangbu.avalon.dataset.entity;

import io.github.lishangbu.avalon.hibernate.Flex;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/// 成长速率模型
///
/// 表示宝可梦通过经验获得等级的速度，详情参考 [Bulbapedia](http://bulbapedia.bulbagarden.net/wiki/Experience)
///
/// @author lishangbu
/// @since 2026/2/10
@Data
@Entity
@Table(comment = "成长速率")
public class GrowthRate implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  /// 主键
  @Id
  @Flex
  @Column(comment = "主键")
  private Long id;

  /// 内部名称
  @Column(comment = "内部名称", length = 100)
  private String internalName;

  ///  显示名称
  @Column(comment = "显示名称", length = 100)
  private String name;

  /// 描述
  @Column(comment = "描述", length = 200)
  private String description;
}
