package io.github.lishangbu.avalon.dataset.entity;

import io.github.lishangbu.avalon.data.jdbc.id.AutoLongIdGenerator;
import java.io.Serial;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

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
@Table
public class EggGroup implements AutoLongIdGenerator {
  @Serial private static final long serialVersionUID = 1L;

  /** ID */
  @Id private Long id;

  /**
   * 内部名称
   *
   * <p>取百科中的蛋群分类数据
   */
  private String internalName;

  /** 蛋群名称 */
  private String name;

  /** 描述文本 */
  private String text;

  /** 蛋群整体特征 */
  private String characteristics;

  @Override
  public Long getId() {
    return id;
  }

  @Override
  public void setId(Long id) {
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
