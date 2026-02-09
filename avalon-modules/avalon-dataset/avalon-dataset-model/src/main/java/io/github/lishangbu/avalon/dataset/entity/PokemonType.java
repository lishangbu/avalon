package io.github.lishangbu.avalon.dataset.entity;

import io.github.lishangbu.avalon.hibernate.Flex;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
@Entity
@Table(comment = "宝可梦属性")
public class PokemonType implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  /// 主键
  @Id
  @Flex
  @Column(comment = "主键")
  private Long id;

  /// 宝可梦内部名称
  @Column(comment = "宝可梦内部名称", length = 100)
  private String pokemonInternalName;

  /// 属性内部名称
  @Column(comment = "属性内部名称", length = 100)
  private String typeInternalName;

  /// 属性内部排序
  @Column(comment = "属性内部排序")
  private Integer sortingOrder;
}
