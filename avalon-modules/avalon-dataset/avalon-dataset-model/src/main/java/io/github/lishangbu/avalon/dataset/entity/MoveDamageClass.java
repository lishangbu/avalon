package io.github.lishangbu.avalon.dataset.entity;

import java.io.Serializable;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 招式可以拥有的伤害类别，例如物理、特殊或非伤害性
 *
 * @author lishangbu
 * @since 2025/6/9
 */
@Table
public class MoveDamageClass implements Serializable {
  /** ID */
  @Id private Integer id;

  /** 名称 */
  private String name;

  /** 内部名称 */
  private String internalName;

  /** 描述 */
  private String description;

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

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }
}
