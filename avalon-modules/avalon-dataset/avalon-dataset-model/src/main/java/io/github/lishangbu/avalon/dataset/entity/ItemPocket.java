package io.github.lishangbu.avalon.dataset.entity;

import io.github.lishangbu.avalon.hibernate.Flex;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/// 道具口袋(ItemPocket)实体类
///
/// 包含口袋 ID、内部名称与显示名称
///
/// @author lishangbu
/// @since 2025/08/20
@Data
@Entity
@Table(comment = "道具口袋")
public class ItemPocket implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  /// 主键
  @Id
  @Flex
  @Column(comment = "主键")
  private Long id;

  /// 内部名称
  @Column(comment = "内部名称", length = 100)
  private String internalName;

  /// 道具口袋名称
  @Column(comment = "道具口袋名称", length = 100)
  private String name;
}
