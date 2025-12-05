package io.github.lishangbu.avalon.dataset.entity;

import java.io.Serial;
import java.io.Serializable;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Sequence;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 蛋组(EggGroup)实体类
 *
 * @author lishangbu
 * @since 2025/08/20
 */
@Data
@Table
public class EggGroup implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  /** 主键 */
  @Id
  @Sequence("egg_group_id_seq")
  private Integer id;

  /** 内部名称 */
  private String internalName;

  /** 蛋组名称 */
  private String name;

  /** 描述文本 */
  private String text;

  /** 蛋群整体特征 */
  private String characteristics;
}
