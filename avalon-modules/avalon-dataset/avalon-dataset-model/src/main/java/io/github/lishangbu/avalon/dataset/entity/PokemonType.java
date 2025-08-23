package io.github.lishangbu.avalon.dataset.entity;

import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/**
 * 宝可梦属性(PokemonType)实体类
 *
 * @author lishangbu
 * @since 2025/08/20
 */
@Data
public class PokemonType implements Serializable {
  @Serial private static final long serialVersionUID = 540341561900293078L;

  /** 主键 */
  private Long id;

  /** 宝可梦内部名称 */
  private String pokemonInternalName;

  /** 属性内部名称 */
  private String typeInternalName;

  /** 属性内部排序 */
  private Integer sortingOrder;
}
