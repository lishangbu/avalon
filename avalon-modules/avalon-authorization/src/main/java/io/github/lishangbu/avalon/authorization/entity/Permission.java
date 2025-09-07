package io.github.lishangbu.avalon.authorization.entity;

import io.github.lishangbu.avalon.jpa.Flex;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.Set;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.security.core.SpringSecurityCoreVersion;

/**
 * 权限(Permission)实体类
 *
 * @author lishangbu
 * @since 2025/08/28
 */
@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Entity
public class Permission implements Serializable {
  @Serial private static final long serialVersionUID = SpringSecurityCoreVersion.SERIAL_VERSION_UID;

  /** 主键 */
  @Id @Flex private Long id;

  /** 权限名称 */
  private String name;

  /** 权限编码 */
  private String code;

  /** 权限类型,MENU-菜单,BUTTON-按钮 */
  private String type;

  /** 父权限ID */
  private Long parentId;

  /** 路径 */
  private String path;

  /** 重定向路径 */
  private String redirect;

  /** 图标 */
  private String icon;

  /** 组件路径 */
  private String component;

  /** 布局 */
  private String layout;

  /** 是否保持活动状态 */
  private Boolean keepAlive;

  /** 请求方法 */
  private String method;

  /** 描述 */
  private String description;

  /** 是否展示在页面菜单 */
  private Boolean show;

  /** 是否启用 */
  private Boolean enabled;

  /** 排序 */
  private Integer orderNum;

  /** 权限与角色多对多关系 */
  @ManyToMany(mappedBy = "permissions")
  @ToString.Exclude
  private Set<Role> roles;

  @Override
  public final boolean equals(Object o) {
    if (this == o) return true;
    if (o == null) return false;
    Class<?> oEffectiveClass =
        o instanceof HibernateProxy
            ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass()
            : o.getClass();
    Class<?> thisEffectiveClass =
        this instanceof HibernateProxy
            ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass()
            : this.getClass();
    if (thisEffectiveClass != oEffectiveClass) return false;
    Permission that = (Permission) o;
    return getId() != null && Objects.equals(getId(), that.getId());
  }

  @Override
  public final int hashCode() {
    return this instanceof HibernateProxy
        ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode()
        : getClass().hashCode();
  }
}
