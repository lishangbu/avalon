package io.github.lishangbu.avalon.dataset.entity;

import io.github.lishangbu.avalon.hibernate.Flex;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/// 道具(Item)实体类
///
/// 表示游戏中的道具信息，包含标识、内部名称、价格、投掷效果及文本描述
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

  /// 公式
  @Column(comment = "公式", length = 1000)
  private String formula;

  /// 描述
  @Column(comment = "描述", length = 200)
  private String description;
}
