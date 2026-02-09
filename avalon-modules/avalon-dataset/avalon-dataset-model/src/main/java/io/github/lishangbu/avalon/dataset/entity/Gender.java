package io.github.lishangbu.avalon.dataset.entity;

import io.github.lishangbu.avalon.hibernate.Flex;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/// 性别
///
/// 性别主要用于宝可梦繁殖，并可能影响外观或进化，详情参考 [Bulbapedia](http://bulbapedia.bulbagarden.net/wiki/Gender)
///
/// @author lishangbu
/// @since 2025/08/20
@Data
@Entity
@Table(comment = "性别")
public class Gender implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  /// 主键
  @Id
  @Flex
  @Column(comment = "主键")
  private Long id;

  /// 内部名称
  @Column(comment = "内部名称", length = 100)
  private String internalName;
}
