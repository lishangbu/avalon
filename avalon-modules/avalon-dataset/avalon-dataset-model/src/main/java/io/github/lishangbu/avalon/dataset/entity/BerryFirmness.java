package io.github.lishangbu.avalon.dataset.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 树果硬度
 *
 * @author lishangbu
 * @since 2025/5/22
 */
@Table
public class BerryFirmness {
  /** ID */
  @Id private Integer id;

  /** 树果硬度名称 */
  private String name;

  /** 内部名称 */
  private String internalName;

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
}
