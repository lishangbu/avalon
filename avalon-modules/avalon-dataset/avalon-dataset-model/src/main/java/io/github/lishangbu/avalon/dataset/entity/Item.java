package io.github.lishangbu.avalon.dataset.entity;

import io.github.lishangbu.avalon.hibernate.Flex;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/// 道具(Item)实体类
///
/// 表示游戏中的道具信息，包含标识、内部名称、价格、投掷效果及文本描述
///
/// @author lishangbu
/// @since 2025/08/20
@Data
@Entity
public class Item implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  /// 主键
  @Id @Flex private Long id;

  /// 内部名称
  private String internalName;

  /// 道具名称
  private String name;

  /// 在商店中的价格
  private Integer cost;

  /// 使用此道具进行投掷行动时的威力
  private Integer flingPower;

  /// 使用此道具进行投掷行动时的效果(内部名称)
  private String flingEffectInternalName;

  /// 此道具所属的类别(内部名称)
  private String categoryInternalName;

  /// 简要效果描述
  private String shortEffect;

  /// 详细效果描述
  private String effect;

  /// 道具文本
  private String text;
}
