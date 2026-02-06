package io.github.lishangbu.avalon.dataset.entity;

import io.github.lishangbu.avalon.hibernate.Flex;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/// 蛋组(EggGroup)实体类
///
/// 表示宝可梦蛋的分类信息，用于蛋孵化与交配规则
///
/// @author lishangbu
/// @since 2025/08/20
@Data
@Entity
public class EggGroup implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  /// 主键
  @Id @Flex private Long id;

  /// 内部名称
  private String internalName;

  /// 蛋组名称
  private String name;

  /// 描述文本
  private String text;

  /// 蛋群整体特征
  private String characteristics;
}
