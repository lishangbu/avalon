package io.github.lishangbu.avalon.authorization.entity;

import io.github.lishangbu.avalon.hibernate.Flex;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import lombok.Data;

@Data
@Entity
@Table(comment = "认证日志")
public class AuthenticationLog implements Serializable {
    @Serial private static final long serialVersionUID = 1L;

    @Flex
    @Id
    @Column(comment = "主键")
    private Long id;

    @Column(comment = "用户名", length = 20)
    private String username;

    @Column(comment = "客户端ID", length = 100)
    private String clientId;

    @Column(comment = "授权方式", length = 50)
    private String grantType;

    @Column(comment = "客户端IP", length = 512)
    private String ip;

    @Column(comment = "用户代理", length = 512)
    private String userAgent;

    @Column(comment = "是否成功")
    private Boolean success;

    @Column(comment = "错误信息", length = 2000)
    private String errorMessage;

    @Column(comment = "发生时间")
    private Instant occurredAt;
}
