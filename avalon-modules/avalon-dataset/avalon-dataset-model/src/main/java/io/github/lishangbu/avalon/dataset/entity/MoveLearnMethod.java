package io.github.lishangbu.avalon.dataset.entity;

import java.io.Serial;
import java.io.Serializable;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 宝可梦可以学习招式的方法
 *
 * @author lishangbu
 * @since 2025/6/9
 */
@Table
public class MoveLearnMethod implements Serializable {

  @Serial private static final long serialVersionUID = 1L;

  /** ID */
  @Id private Integer id;

  /** 内部名称 */
  private String internalName;

  /** 目标名称 */
  private String name;

  /** 说明 */
  private String description;

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

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }
}
