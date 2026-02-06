package io.github.lishangbu.avalon.dataset.entity;

import io.github.lishangbu.avalon.hibernate.Flex;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/// 树果硬度(BerryFirmness)实体类
///
/// 表示树果的硬度分类，用于描述树果的质地
///
/// @author lishangbu
/// @since 2025/08/20
@Data
@Entity
public class BerryFirmness implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  /// 主键
  @Id @Flex private Long id;

  /// 内部名称
  private String internalName;

  /// 树果硬度名称
  private String name;
}
