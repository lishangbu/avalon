package io.github.lishangbu.avalon.authorization.entity;

import io.github.lishangbu.avalon.hibernate.Flex;
import jakarta.persistence.*;
import java.io.Serial;
import java.io.Serializable;
import java.util.Set;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/// 角色信息(Role)实体类
///
/// 包含角色标识、代码、名称与启用标志
///
/// @author lishangbu
/// @since 2025/08/20
@Data
@Entity
public class Role implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  /// 主键
  @Id
  @Flex
  @Column(comment = "主键")
  private Long id;

  /// 角色代码
  @Column(comment = "角色代码", length = 50)
  private String code;

  /// 角色名称
  @Column(comment = "角色名称", length = 50)
  private String name;

  /// 角色是否启用
  @Column(comment = "角色是否启用 true: 启用，false: 禁用")
  private Boolean enabled;

  /// 角色与权限多对多关系
  @ManyToMany
  @JoinTable(
      name = "role_menu_relation",
      joinColumns = @JoinColumn(name = "role_id"),
      inverseJoinColumns = @JoinColumn(name = "menu_id"))
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private Set<Menu> menus;
}
