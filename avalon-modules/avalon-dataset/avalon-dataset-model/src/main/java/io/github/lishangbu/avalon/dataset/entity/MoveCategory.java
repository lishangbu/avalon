package io.github.lishangbu.avalon.dataset.entity;

import io.github.lishangbu.avalon.data.jdbc.id.AutoLongIdGenerator;
import java.io.Serial;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 招式分类
 *
 * @author lishangbu
 * @since 2025/4/15
 */
@Table
public class MoveCategory implements AutoLongIdGenerator {

  @Serial private static final long serialVersionUID = 1L;

  /** ID */
  @Id private Long id;

  /** 内部名称 */
  private String internalName;

  /** 属性说明 */
  private String description;

  /** 名称 */
  private String name;

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

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}
