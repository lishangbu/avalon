package io.github.lishangbu.avalon.dataset.entity;

import io.github.lishangbu.avalon.jpa.Flex;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;

/**
 * 属性(Type)实体类
 *
 * @author lishangbu
 * @since 2025/08/20
 */
@Entity
@Getter
@Setter
@ToString
@RequiredArgsConstructor
public class Type implements Serializable {
  @Serial private static final long serialVersionUID = -68547309302497308L;

  /** 主键 */
  @Id @Flex private Long id;

  /** 属性内部名称 */
  private String internalName;

  /** 属性名称 */
  private String name;

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
    Type type = (Type) o;
    return getId() != null && Objects.equals(getId(), type.getId());
  }

  @Override
  public final int hashCode() {
    return this instanceof HibernateProxy
        ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode()
        : getClass().hashCode();
  }
}
