package io.github.lishangbu.avalon.dataset.entity;

import java.io.Serial;
import java.io.Serializable;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Sequence;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 招式指向目标(MoveTarget)实体类
 *
 * @author lishangbu
 * @since 2025/08/20
 */
@Data
@Table
public class MoveTarget implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  /** 主键 */
  @Id
  @Sequence("move_target_id_seq")
  private Integer id;

  /** 招式指向目标内部名称 */
  private String internalName;

  /** 招式指向目标名称 */
  private String name;

  /** 招式指向目标描述 */
  private String description;
}
