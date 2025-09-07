package io.github.lishangbu.avalon.dataset.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;

/**
 * 宝可梦(Pokemon)实体类
 *
 * @author lishangbu
 * @since 2025/08/20
 */
@Entity
@Getter
@Setter
@ToString
@RequiredArgsConstructor
public class Pokemon implements Serializable {
  @Serial private static final long serialVersionUID = -16769441693768999L;

  /** 主键 */
  @Id private Long id;

  /** 宝可梦内部名称 */
  private String internalName;

  /** 宝可梦名称 */
  private String name;

  /** 身高，单位为分米 */
  private Integer height;

  /** 体重，数字每增加1，体重增加0.1kg */
  private Integer weight;

  /** 基础经验值 */
  private Integer baseExperience;

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
    Pokemon pokemon = (Pokemon) o;
    return getId() != null && Objects.equals(getId(), pokemon.getId());
  }

  @Override
  public final int hashCode() {
    return this instanceof HibernateProxy
        ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode()
        : getClass().hashCode();
  }
}
