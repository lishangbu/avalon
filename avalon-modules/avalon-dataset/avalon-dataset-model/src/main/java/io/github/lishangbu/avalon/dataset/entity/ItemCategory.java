package io.github.lishangbu.avalon.dataset.entity;

import io.github.lishangbu.avalon.hibernate.Flex;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/// 道具类别(ItemCategory)实体类
///
/// 表示道具的分类信息，包括所属口袋等元数据
///
/// @author lishangbu
/// @since 2025/08/20
@Data
@Entity
public class ItemCategory implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  /// 主键
  @Id @Flex private Long id;

  /// 内部名称
  private String internalName;

  /// 道具类别名称
  private String name;

  /// 该类别道具所属的口袋（内部名称）
  private String itemPocketInternalName;
}
