package io.github.lishangbu.avalon.dataset.entity;

import java.io.Serial;
import java.io.Serializable;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Sequence;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 特性(Ability)实体类
 *
 * @author lishangbu
 * @since 2025/08/20
 */
@Data
@Table
public class Ability implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  /** 主键 */
  @Id
  @Sequence("ability_id_seq")
  private Integer id;

  /** 内部名称 */
  private String internalName;

  /** 名称 */
  private String name;

  /** 特性效果 */
  private String effect;

  /** 基本信息 */
  private String info;

  /** 文字介绍 */
  private String text;
}
