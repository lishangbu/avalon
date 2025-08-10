package io.github.lishangbu.avalon.dataset.entity;

import io.github.lishangbu.avalon.data.jdbc.id.AutoLongIdGenerator;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 树果硬度
 *
 * @author lishangbu
 * @since 2025/5/22
 */
@Table
public class BerryFirmness implements AutoLongIdGenerator {
  /** ID */
  @Id private Long id;

  /** 树果硬度名称 */
  private String name;

  /** 内部名称 */
  private String internalName;

  @Override
  public Long getId() {
    return id;
  }

  @Override
  public void setId(Long id) {
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
