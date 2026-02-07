package io.github.lishangbu.avalon.dataset.entity;

import io.github.lishangbu.avalon.hibernate.Flex;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
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
@Entity
public class MoveCategory implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  /// 主键
  @Id
  @Flex
  @Column(comment = "主键")
  private Long id;

  /// 招式分类内部名称
  @Column(comment = "招式分类内部名称", length = 100)
  private String internalName;

  /// 招式分类名称
  @Column(comment = "招式分类名称", length = 100)
  private String name;

  /// 招式分类描述
  @Column(comment = "招式分类描述", length = 300)
  private String description;
}
