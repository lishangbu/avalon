package io.github.lishangbu.avalon.dataset.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;

/**
 * 学习招式的方法(MoveLearnMethod)实体类
 *
 * @author lishangbu
 * @since 2025/08/20
 */
@Entity
@Getter
@Setter
@ToString
@RequiredArgsConstructor
public class MoveLearnMethod implements Serializable {
  @Serial private static final long serialVersionUID = 240573508164457984L;

  /** 主键 */
  @Id private Long id;

  /** 学习招式的方法内部名称 */
  private String internalName;

  /** 学习招式的方法名称 */
  private String name;

  /** 学习招式的方法的描述 */
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
    MoveLearnMethod that = (MoveLearnMethod) o;
    return getId() != null && Objects.equals(getId(), that.getId());
  }

  @Override
  public final int hashCode() {
    return this instanceof HibernateProxy
        ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode()
        : getClass().hashCode();
  }
}
