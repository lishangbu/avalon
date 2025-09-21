package io.github.lishangbu.avalon.authorization.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.jpa.Flex;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
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
 * 菜单
 *
 * @author lishangbu
 * @since 2025/9/17
 */
@Entity
@Getter
@Setter
@ToString
@RequiredArgsConstructor
public class Menu implements Serializable {

  @Serial private static final long serialVersionUID = SpringSecurityCoreVersion.SERIAL_VERSION_UID;

  /** 主键 */
  @Id @Flex private Long id;

  /** 父权限ID */
  private Long parentId;

  // region Naive UI Menu 属性

  /** 是否禁用菜单项 */
  private Boolean disabled;

  /** 菜单项的额外部分 */
  private String extra;

  /** 菜单项的图标 */
  private String icon;

  /** 菜单项的标识符 */
  private String key;

  /** 菜单项的内容 */
  private String label;

  /** 是否显示菜单项 */
  private Boolean show;

  // endregion

  // region Vue Router 属性

  /** 路径 */
  private String path;

  /** 名称 */
  private String name;

  /** 重定向路径 */
  private String redirect;

  /** 组件路径 */
  private String component;

  /** 排序顺序 */
  private Integer sortOrder;

  // endregion

  // region 其他的 router metadata

  /** 固定标签页 */
  private Boolean pinned;

  /** 显示标签页 */
  private Boolean showTab;

  /** 多标签页显示 */
  private Boolean enableMultiTab;

  // endregion

  /** 权限与角色多对多关系 */
  @ManyToMany(mappedBy = "menus")
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
    Menu menu = (Menu) o;
    return getId() != null && Objects.equals(getId(), menu.getId());
  }

  @Override
  public final int hashCode() {
    return this instanceof HibernateProxy
        ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode()
        : getClass().hashCode();
  }
}
