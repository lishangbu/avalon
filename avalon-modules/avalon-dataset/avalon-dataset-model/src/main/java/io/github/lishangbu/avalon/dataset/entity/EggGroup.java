package io.github.lishangbu.avalon.dataset.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
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
public class EggGroup implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  /** 蛋群分类 */
  @Id
  @Comment("主键,标明蛋群的分类")
  @Column(name = "[group]")
  private String group;

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

  public String getGroup() {
    return group;
  }

  public void setGroup(String group) {
    this.group = group;
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
