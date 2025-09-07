package io.github.lishangbu.avalon.dataset.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;

/**
 * 道具类别(ItemCategory)实体类
 *
 * @author lishangbu
 * @since 2025/08/20
 */
@Entity
@Getter
@Setter
@ToString
@RequiredArgsConstructor
public class ItemCategory implements Serializable {
  @Serial private static final long serialVersionUID = 698222055482015322L;

  /** 主键 */
  @Id private Long id;

  /** 内部名称 */
  private String internalName;

  /** 道具类别名称 */
  private String name;

  /** 该类别道具所属的口袋 */
  private String itemPocketInternalName;

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
    ItemCategory that = (ItemCategory) o;
    return getId() != null && Objects.equals(getId(), that.getId());
  }

  @Override
  public final int hashCode() {
    return this instanceof HibernateProxy
        ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode()
        : getClass().hashCode();
  }
}
