package io.github.lishangbu.avalon.dataset.entity;

import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/**
 * 技能学习机器(Machine)实体类
 *
 * @author lishangbu
 * @since 2025/08/20
 */
@Data
public class Machine implements Serializable {
  @Serial private static final long serialVersionUID = 225851657308520018L;

  /** 主键 */
  private Long id;

  /** 道具ID */
  private Long itemId;

  /** 招式ID */
  private Long moveId;
}
