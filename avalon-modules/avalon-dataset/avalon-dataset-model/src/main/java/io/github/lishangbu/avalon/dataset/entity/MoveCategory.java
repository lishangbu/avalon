package io.github.lishangbu.avalon.dataset.entity;

import java.io.Serial;
import java.io.Serializable;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Sequence;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 招式分类(MoveCategory)实体类
 *
 * @author lishangbu
 * @since 2025/08/20
 */
@Table
@Data
public class MoveCategory implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  /** 主键 */
  @Id
  @Sequence("move_category_id_seq")
  private Integer id;

  /** 招式分类内部名称 */
  private String internalName;

  /** 招式分类名称 */
  private String name;

  /** 招式分类描述 */
  private String description;
}
