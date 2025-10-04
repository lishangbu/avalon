package io.github.lishangbu.avalon.authorization.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.hibernate.Flex;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
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
@Table(name = "[permission]")
public class Permission implements Serializable {
  @Serial private static final long serialVersionUID = SpringSecurityCoreVersion.SERIAL_VERSION_UID;

  /** 主键 */
  @Id @Flex private Long id;

  /** 权限名称 */
  private String name;

  /** 权限编码 */
  private String code;

  /** 父权限ID */
  private Long parentId;

  /** 请求方法 */
  private String method;

  /** 描述 */
  private String description;

  /** 是否启用 */
  private Boolean enabled;

  /** 排序顺序 */
  private Integer sortOrder;

  /** 权限与角色多对多关系 */
  @ManyToMany(mappedBy = "permissions")
  @ToString.Exclude
  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
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
