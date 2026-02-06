package io.github.lishangbu.avalon.dataset.entity;

import io.github.lishangbu.avalon.hibernate.Flex;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/// 招式指向目标(MoveTarget)实体类
///
/// 表示招式指向目标的元数据，包括内部名称、显示名称与描述
///
/// @author lishangbu
/// @since 2025/08/20
@Data
@Entity
public class MoveTarget implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  /// 主键
  @Id @Flex private Long id;

  /// 招式指向目标内部名称
  private String internalName;

  /// 招式指向目标名称
  private String name;

  /// 招式指向目标描述
  private String description;
}
