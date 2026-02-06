package io.github.lishangbu.avalon.dataset.entity;

import io.github.lishangbu.avalon.hibernate.Flex;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/// 招式伤害类别(MoveDamageClass)实体类
///
/// 表示招式的伤害分类（例如物理、特殊、非伤害），包含名称与描述
///
/// @author lishangbu
/// @since 2025/08/20
@Data
@Entity
public class MoveDamageClass implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  /// 主键
  @Id @Flex private Long id;

  /// 招式伤害类别内部名称
  private String internalName;

  /// 招式伤害类别名称
  private String name;

  /// 招式伤害类别描述
  private String description;
}
