package io.github.lishangbu.avalon.dataset.entity;

import java.io.Serial;
import java.io.Serializable;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Sequence;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 宝可梦属性(PokemonType)实体类
 *
 * @author lishangbu
 * @since 2025/08/20
 */
@Data
@Table
public class PokemonType implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  /** 主键 */
  @Id
  @Sequence("pokemon_type_id_seq")
  private Integer id;

  /** 宝可梦内部名称 */
  private String pokemonInternalName;

  /** 属性内部名称 */
  private String typeInternalName;

  /** 属性内部排序 */
  private Integer sortingOrder;
}
