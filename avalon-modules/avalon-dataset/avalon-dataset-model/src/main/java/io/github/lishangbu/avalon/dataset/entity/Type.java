package io.github.lishangbu.avalon.dataset.entity;

import java.io.Serial;
import java.io.Serializable;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 属性
 *
 * @author lishangbu
 * @since 2025/4/14
 */
@Table
public class Type implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  /** ID */
  @Id private Integer id;

  /**
   * 内部名称
   *
   * <p>取百科中的属性英文名称
   */
  private String internalName;

  /** 属性名称 */
  private String name;

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
}
