package io.github.lishangbu.avalon.dataset.entity;

import io.github.lishangbu.avalon.data.jdbc.id.AutoLongIdGenerator;
import java.io.Serial;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 道具类别
 *
 * @author lishangbu
 * @since 2025/6/8
 */
@Table
public class ItemAttribute implements AutoLongIdGenerator {
  @Serial private static final long serialVersionUID = 1L;

  /** ID */
  @Id private Long id;

  /**
   * 内部名称
   *
   * <p>取百科中的分类数据
   */
  private String internalName;

  /** 名称 */
  private String name;

  /** 道具属性描述 */
  private String description;

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

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }
}
