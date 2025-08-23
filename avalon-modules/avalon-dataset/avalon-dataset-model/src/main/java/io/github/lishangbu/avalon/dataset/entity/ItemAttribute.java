package io.github.lishangbu.avalon.dataset.entity;

import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/**
 * 道具属性(ItemAttribute)实体类
 *
 * @author lishangbu
 * @since 2025/08/20
 */
@Data
public class ItemAttribute implements Serializable {
  @Serial private static final long serialVersionUID = -45782438342388356L;

  /** 主键 */
  private Long id;

  /** 内部名称 */
  private String internalName;

  /** 道具属性名称 */
  private String name;

  /** 道具属性描述 */
  private String description;
}
