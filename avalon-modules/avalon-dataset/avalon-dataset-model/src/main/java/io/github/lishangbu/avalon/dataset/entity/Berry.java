package io.github.lishangbu.avalon.dataset.entity;

import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/**
 * 树果(Berry)实体类
 *
 * @author lishangbu
 * @since 2025/08/20
 */
@Data
public class Berry implements Serializable {
  @Serial private static final long serialVersionUID = 382523524754343414L;

  /** 主键 */
  private Long id;

  /** 内部名称 */
  private String internalName;

  /** 名称 */
  private String name;

  /** 生长到下一个阶段所需的时间(小时) */
  private Integer growthTime;

  /** 最大结果数 */
  private Integer maxHarvest;

  /** 大小（毫米） */
  private Integer size;

  /** 光滑度 */
  private Integer smoothness;

  /** 生长时使土壤干燥的速度，数值越高土壤干燥越快 */
  private Integer soilDryness;

  /** 树果的坚硬度(内部名称) */
  private String firmnessInternalName;

  /** 搭配该树果使用“自然之恩”招式时继承的属性类型 */
  private String naturalGiftTypeInternalName;

  /** 搭配该树果使用“自然之恩”招式时的威力 */
  private Integer naturalGiftPower;
}
