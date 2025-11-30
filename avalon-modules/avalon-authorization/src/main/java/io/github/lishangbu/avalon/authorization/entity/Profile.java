package io.github.lishangbu.avalon.authorization.entity;

import java.io.Serial;
import java.io.Serializable;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 用户个人资料表(Profile)实体类
 *
 * @author lishangbu
 * @since 2025/08/30
 */
@Data
@Table
public class Profile implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  @Id private Long id;

  private Integer gender;

  private String avatar;

  private String address;

  private String email;

  private Long userId;

  private String nickname;
}
