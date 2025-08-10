package io.github.lishangbu.avalon.dataset.entity;

import java.io.Serial;
import java.io.Serializable;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 宝可梦
 *
 * @author lishangbu
 * @since 2025/4/21
 */
@Table
public class Pokemon implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  /** ID */
  @Id private Integer id;

  /**
   * 名称
   *
   * <p>取百科中的中文名数据
   */
  private String name;

  /**
   * 内部名称
   *
   * <p>取百科中的英文名数据
   */
  private String internalName;

  /** 身高，数字每增加1，身高增加0.1m */
  private Integer height;

  /** 体重，数字每增加1，体重增加0.1kg */
  private Integer weight;

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getInternalName() {
    return internalName;
  }

  public void setInternalName(String internalName) {
    this.internalName = internalName;
  }

  public Integer getHeight() {
    return height;
  }

  public void setHeight(Integer height) {
    this.height = height;
  }

  public Integer getWeight() {
    return weight;
  }

  public void setWeight(Integer weight) {
    this.weight = weight;
  }
}
