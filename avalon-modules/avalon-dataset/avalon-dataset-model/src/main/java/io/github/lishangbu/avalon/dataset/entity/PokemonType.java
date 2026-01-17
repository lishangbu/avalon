package io.github.lishangbu.avalon.dataset.entity;

import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/// 宝可梦属性(PokemonType)实体类
///
/// 表示宝可梦与属性的关联信息，包含属性的内部名称与排序信息
///
/// @author lishangbu
/// @since 2025/08/20
@Data
public class PokemonType implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  /// 主键
  private Long id;

  /// 宝可梦内部名称
  private String pokemonInternalName;

  /// 属性内部名称
  private String typeInternalName;

  /// 属性内部排序
  private Integer sortingOrder;
}
