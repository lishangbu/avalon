package io.github.lishangbu.avalon.dataset.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;

/**
 * 招式导致的状态异常(MoveAilment)实体类
 *
 * @author lishangbu
 * @since 2025/08/20
 */
@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Entity
public class MoveAilment implements Serializable {
  @Serial private static final long serialVersionUID = -55400454442808113L;

  /** 主键 */
  @Id private Long id;

  /** 招式导致的状态异常内部名称 */
  private String internalName;

  /** 招式导致的状态异常名称 */
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
    MoveAilment that = (MoveAilment) o;
    return getId() != null && Objects.equals(getId(), that.getId());
  }

  @Override
  public final int hashCode() {
    return this instanceof HibernateProxy
        ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode()
        : getClass().hashCode();
  }
}
