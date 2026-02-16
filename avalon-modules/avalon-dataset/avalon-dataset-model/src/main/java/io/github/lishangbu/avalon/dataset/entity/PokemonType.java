package io.github.lishangbu.avalon.dataset.entity;

import jakarta.persistence.*;
import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/// 宝可梦属性(PokemonType)实体类
///
/// 表示宝可梦与属性的关联信息，包括排序信息
///
/// @author lishangbu
/// @since 2025/08/20
@Data
@Entity
@Table(comment = "宝可梦属性")
public class PokemonType implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  /// 复合主键
  @EmbeddedId private PokemonTypeId id;

  /// 属性内部slot
  @Column(comment = "属性内部slot")
  private Integer slot;

  /// 宝可梦属性复合主键
  ///
  /// @author lishangbu
  /// @since 2026/2/16
  @Data
  @Embeddable
  public static class PokemonTypeId implements Serializable {
    @Serial private static final long serialVersionUID = 1L;

    /// 宝可梦引用
    @ManyToOne
    @JoinColumn(name = "pokemon_id", nullable = false)
    private Pokemon pokemon;

    /// 属性引用
    @ManyToOne
    @JoinColumn(name = "type_id", nullable = false)
    private Type type;
  }
}
