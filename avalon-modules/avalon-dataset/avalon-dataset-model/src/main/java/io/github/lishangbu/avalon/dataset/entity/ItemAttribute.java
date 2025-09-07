package io.github.lishangbu.avalon.dataset.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;

/**
 * 道具属性(ItemAttribute)实体类
 *
 * @author lishangbu
 * @since 2025/08/20
 */
@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Entity
public class ItemAttribute implements Serializable {
  @Serial private static final long serialVersionUID = -45782438342388356L;

  /** 主键 */
  @Id private Long id;

  /** 内部名称 */
  private String internalName;

  /** 道具属性名称 */
  private String name;

  /** 道具属性描述 */
  private String description;

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
    ItemAttribute that = (ItemAttribute) o;
    return getId() != null && Objects.equals(getId(), that.getId());
  }

  @Override
  public final int hashCode() {
    return this instanceof HibernateProxy
        ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode()
        : getClass().hashCode();
  }
}
