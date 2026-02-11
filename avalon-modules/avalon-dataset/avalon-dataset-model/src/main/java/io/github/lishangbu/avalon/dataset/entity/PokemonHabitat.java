package io.github.lishangbu.avalon.dataset.entity;

import io.github.lishangbu.avalon.hibernate.Flex;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/// 宝可梦栖息地(PokemonHabitat)实体类
///
/// 表示宝可梦可能被发现的地形或区域
///
/// @author lishangbu
/// @since 2026/2/12
@Data
@Entity
@Table(comment = "宝可梦栖息地")
public class PokemonHabitat implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  /// 主键
  @Id
  @Flex
  @Column(comment = "主键")
  private Long id;

  /// 内部名称
  @Column(comment = "内部名称", length = 100)
  private String internalName;

  /// 显示名称
  @Column(comment = "显示名称", length = 100)
  private String name;
}
