package io.github.lishangbu.avalon.dataset.entity;

import java.io.Serial;
import java.io.Serializable;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Sequence;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 树果硬度(BerryFirmness)实体类
 *
 * @author lishangbu
 * @since 2025/08/20
 */
@Data
@Table
public class BerryFirmness implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  /** 主键 */
  @Id
  @Sequence("berry_firmness_id_seq")
  private Integer id;

  /** 内部名称 */
  private String internalName;

  /** 树果硬度名称 */
  private String name;
}
