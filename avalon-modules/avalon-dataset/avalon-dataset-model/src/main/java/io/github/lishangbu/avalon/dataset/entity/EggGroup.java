package io.github.lishangbu.avalon.dataset.entity;

import io.github.lishangbu.avalon.hibernate.Flex;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
@Table(comment = "蛋组")
public class EggGroup implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  /// 主键
  @Id
  @Flex
  @Column(comment = "主键")
  private Long id;

  /// 内部名称
  @Column(comment = "内部名称", length = 100)
  private String internalName;

  /// 蛋组名称
  @Column(comment = "蛋组名称", length = 100)
  private String name;

  /// 描述文本
  @Column(comment = "描述文本", length = 200)
  private String text;

  /// 蛋群整体特征
  @Column(comment = "蛋群整体特征", length = 500)
  private String characteristics;
}
