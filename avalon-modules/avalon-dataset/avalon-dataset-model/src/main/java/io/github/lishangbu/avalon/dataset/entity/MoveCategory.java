package io.github.lishangbu.avalon.dataset.entity;

import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/// 招式分类(MoveCategory)实体类
///
/// 表示招式的宽泛类别，用于对招式效果进行分组
///
/// @author lishangbu
/// @since 2025/08/20
@Data
public class MoveCategory implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  /// 主键
  private Long id;

  /// 招式分类内部名称
  private String internalName;

  /// 招式分类名称
  private String name;

  /// 招式分类描述
  private String description;
}
