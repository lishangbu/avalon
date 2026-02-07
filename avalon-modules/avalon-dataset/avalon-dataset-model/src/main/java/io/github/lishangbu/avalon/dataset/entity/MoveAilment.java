package io.github.lishangbu.avalon.dataset.entity;

import io.github.lishangbu.avalon.hibernate.Flex;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/// 招式导致的状态异常(MoveAilment)实体类
///
/// 表示招式可能引发的状态异常类型，例如中毒、麻痹等
///
/// @author lishangbu
/// @since 2025/08/20
@Data
@Entity
public class MoveAilment implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  /// 主键
  @Id
  @Flex
  @Column(comment = "主键")
  private Long id;

  /// 招式导致的状态异常内部名称
  @Column(comment = "招式导致的状态异常内部名称", length = 100)
  private String internalName;

  /// 招式导致的状态异常名称
  @Column(comment = "招式导致的状态异常名称", length = 100)
  private String name;
}
