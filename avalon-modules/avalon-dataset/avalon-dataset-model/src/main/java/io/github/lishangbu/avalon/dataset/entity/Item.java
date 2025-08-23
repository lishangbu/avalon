package io.github.lishangbu.avalon.dataset.entity;

import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/**
 * 道具(Item)实体类
 *
 * @author lishangbu
 * @since 2025/08/20
 */
@Data
public class Item implements Serializable {
  @Serial private static final long serialVersionUID = -99781643102904453L;

  /** 主键 */
  private Long id;

  /** 内部名称 */
  private String internalName;

  /** 道具名称 */
  private String name;

  /** 在商店中的价格 */
  private Integer cost;

  /** 使用此道具进行投掷行动时的威力 */
  private Integer flingPower;

  /** 使用此道具进行投掷行动时的效果(内部名称) */
  private String flingEffectInternalName;

  /** 此道具所属的类别(内部名称) */
  private String categoryInternalName;

  /** 简要效果描述 */
  private String shortEffect;

  /** 简要效果描述 */
  private String effect;

  /** 道具文本 */
  private String text;
}
