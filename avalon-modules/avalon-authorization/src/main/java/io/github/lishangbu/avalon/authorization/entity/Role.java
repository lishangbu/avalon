package io.github.lishangbu.avalon.authorization.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.hibernate.Flex;
import jakarta.persistence.*;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.Set;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.security.core.SpringSecurityCoreVersion;

/**
 * 角色信息(Role)实体类
 *
 * @author lishangbu
 * @since 2025/08/20
 */
@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Entity
public class Role implements Serializable {
  @Serial private static final long serialVersionUID = SpringSecurityCoreVersion.SERIAL_VERSION_UID;

  /** 主键 */
  @Id @Flex private Long id;

  /** 角色代码 */
  private String code;

  /** 角色名称 */
  private String name;

  /** 角色是否启用 */
  private Boolean enabled;

  /** 角色与权限多对多关系 */
  @ManyToMany
  @JoinTable(
      name = "role_menu_relation",
      joinColumns = @JoinColumn(name = "role_id"),
      inverseJoinColumns = @JoinColumn(name = "menu_id"))
  @ToString.Exclude
  private Set<Menu> menus;

  /** 角色与权限多对多关系 */
  @ManyToMany
  @JoinTable(
      name = "role_permission_relation",
      joinColumns = @JoinColumn(name = "role_id"),
      inverseJoinColumns = @JoinColumn(name = "permission_id"))
  @ToString.Exclude
  private Set<Permission> permissions;

  /** 角色与用户多对多关系 */
  @ManyToMany(mappedBy = "roles")
  @ToString.Exclude
  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  private Set<User> users;

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
    Role role = (Role) o;
    return getId() != null && Objects.equals(getId(), role.getId());
  }

  @Override
  public final int hashCode() {
    return this instanceof HibernateProxy
        ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode()
        : getClass().hashCode();
  }
}
