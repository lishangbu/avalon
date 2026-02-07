package io.github.lishangbu.avalon.dataset.entity;

import io.github.lishangbu.avalon.hibernate.Flex;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/// 道具属性关系(ItemAttributeRelation)实体类
///
/// 表示道具与属性之间的多对多关系实体
///
/// @author lishangbu
/// @since 2025/08/20
@Data
@Entity
public class ItemAttributeRelation implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  /// 主键
  @Id
  @Flex
  @Column(comment = "主键")
  private Long id;

  /// 道具ID
  @Column(comment = "道具ID")
  private Long itemId;

  /// 道具属性ID
  @Column(comment = "道具属性ID")
  private Long itemAttributeId;
}
