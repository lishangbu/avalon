package io.github.lishangbu.avalon.dataset.entity;

import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/// 技能学习机器(Machine)实体类
///
/// 表示教授招式的机器信息（例如 TM/HM），包含关联的道具 ID 与招式 ID
///
/// @author lishangbu
/// @since 2025/08/20
@Data
public class Machine implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  /// 主键
  private Long id;

  /// 道具ID
  private Long itemId;

  /// 招式ID
  private Long moveId;
}
