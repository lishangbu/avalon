package io.github.lishangbu.avalon.dataset.entity;

import io.github.lishangbu.avalon.hibernate.Flex;
import jakarta.persistence.*;
import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/// 遭遇条件值(EncounterConditionValue)实体类
///
/// 表示遭遇条件的具体取值，如时间为白天、晚上等
///
/// @author lishangbu
/// @since 2026/2/12
@Data
@Entity
@Table(comment = "遭遇条件值")
public class EncounterConditionValue implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  /// 主键
  @Id
  @Flex
  @Column(comment = "主键")
  private Long id;

  /// 内部名称
  @Column(comment = "内部名称", length = 100)
  private String internalName;

  /// 值名称
  @Column(comment = "值名称", length = 200)
  private String name;

  /// 遭遇条件 ID
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "encounter_condition_id", comment = "遭遇条件ID", nullable = false)
  private EncounterCondition encounterCondition;
}
