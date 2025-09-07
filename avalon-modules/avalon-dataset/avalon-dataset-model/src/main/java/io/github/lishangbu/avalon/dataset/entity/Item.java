package io.github.lishangbu.avalon.dataset.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;

/**
 * 道具(Item)实体类
 *
 * @author lishangbu
 * @since 2025/08/20
 */
@Entity
@Getter
@Setter
@ToString
@RequiredArgsConstructor
public class Item implements Serializable {
  @Serial private static final long serialVersionUID = -99781643102904453L;

  /** 主键 */
  @Id private Long id;

  /** 内部名称 */
  private String internalName;

  /** 道具名称 */
  private String name;

  /** 在商店中的价格 */
  private Integer cost;

  /** 使用此道具进行投掷行动时的威力 */
  private Integer flingPower;

  /** 使用此道具进行投掷行动时的效果(内部名称) */
  private String flingEffectInternalName;

  /** 此道具所属的类别(内部名称) */
  private String categoryInternalName;

  /** 简要效果描述 */
  private String shortEffect;

  /** 简要效果描述 */
  private String effect;

  /** 道具文本 */
  private String text;

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
    Item item = (Item) o;
    return getId() != null && Objects.equals(getId(), item.getId());
  }

  @Override
  public final int hashCode() {
    return this instanceof HibernateProxy
        ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode()
        : getClass().hashCode();
  }
}
