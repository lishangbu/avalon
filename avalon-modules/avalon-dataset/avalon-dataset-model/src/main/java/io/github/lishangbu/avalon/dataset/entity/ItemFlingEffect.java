package io.github.lishangbu.avalon.dataset.entity;

import io.github.lishangbu.avalon.hibernate.Flex;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/// 道具"投掷"效果(ItemFlingEffect)实体类
///
/// 表示道具投掷时触发的效果描述
///
/// @author lishangbu
/// @since 2025/08/20
@Data
@Entity
public class ItemFlingEffect implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  /// 主键
  @Id
  @Flex
  @Column(comment = "主键")
  private Long id;

  /// 内部名称
  @Column(comment = "内部名称", length = 100)
  private String internalName;

  /// 道具投掷效果名称
  @Column(comment = "道具投掷效果名称", length = 100)
  private String name;

  /// 道具效果
  @Column(comment = "道具效果", length = 100)
  private String effect;
}
