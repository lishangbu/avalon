package io.github.lishangbu.avalon.auth.entity;

import java.io.Serial;
import java.io.Serializable;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table
public class Role implements Serializable {

  @Serial private static final long serialVersionUID = 1L;

  /** 主键 */
  @Id private Integer id;

  /** 角色代码 */
  private String code;

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }
}
