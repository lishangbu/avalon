package io.github.lishangbu.avalon.dataset.entity;

import java.io.Serial;
import java.io.Serializable;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Sequence;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 学习招式的方法(MoveLearnMethod)实体类
 *
 * @author lishangbu
 * @since 2025/08/20
 */
@Data
@Table
public class MoveLearnMethod implements Serializable {
  @Serial private static final long serialVersionUID = 240573508164457984L;

  /** 主键 */
  @Id
  @Sequence("move_learn_method_id_seq")
  private Integer id;

  /** 学习招式的方法内部名称 */
  private String internalName;

  /** 学习招式的方法名称 */
  private String name;

  /** 学习招式的方法的描述 */
  private String description;
}
