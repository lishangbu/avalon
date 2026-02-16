package io.github.lishangbu.avalon.dataset.entity;

import jakarta.persistence.*;
import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/// 宝可梦能力值(PokemonStat)实体类
///
/// 表示宝可梦的各项能力值，包括基础能力值和努力值
///
/// @author lishangbu
/// @since 2026/2/16
@Data
@Entity
@Table(comment = "宝可梦能力值")
public class PokemonStat implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  /// 宝可梦能力值主键
  ///
  /// 由宝可梦ID和属性ID组成的联合主键
  ///
  /// @author lishangbu
  /// @since 2026/2/16
  @Embeddable
  @Data
  public static class PokemonStatId implements Serializable {
    @Serial private static final long serialVersionUID = 1L;

    /// 宝可梦
    @ManyToOne
    @JoinColumn(name = "pokemon_id")
    private Pokemon pokemon;

    /// 属性
    @ManyToOne
    @JoinColumn(name = "stat_id")
    private Stat stat;
  }

  /// 联合主键
  @EmbeddedId private PokemonStatId id;

  /// 基础能力值
  @Column(comment = "基础能力值")
  private Integer baseStat;

  /// 努力值
  @Column(comment = "努力值")
  private Integer effort;
}
