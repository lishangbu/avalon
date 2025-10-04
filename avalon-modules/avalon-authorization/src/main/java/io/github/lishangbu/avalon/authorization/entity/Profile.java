package io.github.lishangbu.avalon.authorization.entity;

import io.github.lishangbu.avalon.hibernate.Flex;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.io.Serial;
import java.io.Serializable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.security.core.SpringSecurityCoreVersion;

/**
 * 用户个人资料表(Profile)实体类
 *
 * @author lishangbu
 * @since 2025/08/30
 */
@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Entity
public class Profile implements Serializable {
  @Serial private static final long serialVersionUID = SpringSecurityCoreVersion.SERIAL_VERSION_UID;

  @Id @Flex private Long id;

  private Integer gender;

  private String avatar;

  private String address;

  private String email;

  private Long userId;

  private String nickname;
}
