package io.github.lishangbu.avalon.dataset.entity;

import java.io.Serial;
import java.io.Serializable;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 招式导致的状态异常
 *
 * @author lishangbu
 * @since 2025/6/9
 */
@Table
public class MoveAilment implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  /** ID */
  @Id private Integer id;

  /** 内部名称 */
  private String internalName;

  /** 名称 */
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
