package io.github.lishangbu.avalon.dataset.entity;

import jakarta.persistence.*;
import java.io.Serial;
import java.io.Serializable;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;

/**
 * 蛋群
 *
 * <p>和属性类似，一种宝可梦可能只属于一个蛋群，也可能同时属于两个蛋群。
 *
 * <p>蛋群通过宝可梦的生物特征分类。蛋群无法在《宝可梦》系列中查看，一般记载在攻略本或部分旁支游戏中。
 *
 * @author lishangbu
 * @since 2025/4/15
 */
@Entity
@Table(
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_egg_group_internal_name",
          columnNames = {"internal_name"})
    })
public class EggGroup implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  /** ID */
  @Id
  @Comment("主键")
  private Integer id;

  /**
   * 内部名称
   *
   * <p>取百科中的蛋群分类数据
   */
  @Column(nullable = false, length = 100)
  @ColumnDefault("''")
  @Comment("内部名称")
  private String internalName;

  /** 蛋群名称 */
  @ColumnDefault("''")
  @Comment("名称")
  @Column(nullable = false, length = 10)
  private String name;

  /** 描述文本 */
  @ColumnDefault("''")
  @Comment("描述文本")
  @Column(nullable = false, length = 200)
  private String text;

  /** 蛋群整体特征 */
  @ColumnDefault("''")
  @Comment("特征")
  @Column(nullable = false, length = 500)
  private String characteristics;

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getInternalName() {
    return internalName;
  }

  public void setInternalName(String internalName) {
    this.internalName = internalName;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }

  public String getCharacteristics() {
    return characteristics;
  }

  public void setCharacteristics(String characteristics) {
    this.characteristics = characteristics;
  }
}
