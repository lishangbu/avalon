package io.github.lishangbu.avalon.dataset.entity;

import io.github.lishangbu.avalon.hibernate.Flex;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
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
@Entity
public class Machine implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  /// 主键
  @Id
  @Flex
  @Column(comment = "主键")
  private Long id;

  /// 道具ID
  @Column(comment = "道具ID")
  private Long itemId;

  /// 招式ID
  @Column(comment = "招式ID")
  private Long moveId;
}
