package io.github.lishangbu.avalon.dataset.entity;

import java.io.Serial;
import java.io.Serializable;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Sequence;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 宝可梦(Pokemon)实体类
 *
 * @author lishangbu
 * @since 2025/08/20
 */
@Data
@Table
public class Pokemon implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  /** 主键 */
  @Id
  @Sequence("pokemon_id_seq")
  private Integer id;

  /** 宝可梦内部名称 */
  private String internalName;

  /** 宝可梦名称 */
  private String name;

  /** 身高，单位为分米 */
  private Integer height;

  /** 体重，数字每增加1，体重增加0.1kg */
  private Integer weight;

  /** 基础经验值 */
  private Integer baseExperience;
}
