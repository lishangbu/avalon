package io.github.lishangbu.avalon.dataset.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;

/**
 * 树果(Berry)实体类
 *
 * @author lishangbu
 * @since 2025/08/20
 */
@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Entity
public class Berry implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  /** 主键 */
  @Id private Long id;

  /** 内部名称 */
  private String internalName;

  /** 名称 */
  private String name;

  /** 生长到下一个阶段所需的时间(小时) */
  private Integer growthTime;

  /** 最大结果数 */
  private Integer maxHarvest;

  /** 大小（毫米） */
  private Integer size;

  /** 光滑度 */
  private Integer smoothness;

  /** 生长时使土壤干燥的速度，数值越高土壤干燥越快 */
  private Integer soilDryness;

  /** 树果的坚硬度(内部名称) */
  private String firmnessInternalName;

  /** 搭配该树果使用“自然之恩”招式时继承的属性类型 */
  private String naturalGiftTypeInternalName;

  /** 搭配该树果使用“自然之恩”招式时的威力 */
  private Integer naturalGiftPower;

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
    Berry berry = (Berry) o;
    return getId() != null && Objects.equals(getId(), berry.getId());
  }

  @Override
  public final int hashCode() {
    return this instanceof HibernateProxy
        ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode()
        : getClass().hashCode();
  }
}
