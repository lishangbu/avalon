package io.github.lishangbu.avalon.dataset.entity;

import io.github.lishangbu.avalon.hibernate.Flex;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/// 属性(Type)实体类
///
/// 表示宝可梦属性（如火、水、草等）的基础元数据
///
/// @author lishangbu
/// @since 2025/08/20
@Data
@Entity
public class Type implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  /// 主键
  @Id @Flex private Long id;

  /// 属性内部名称
  private String internalName;

  /// 属性名称
  private String name;
}
