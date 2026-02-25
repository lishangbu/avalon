package io.github.lishangbu.avalon.authorization.model;

import io.github.lishangbu.avalon.authorization.entity.Role;
import io.github.lishangbu.avalon.authorization.entity.User;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Data;

/// 用户(包含角色信息)
///
/// @author lishangbu
/// @since 2025/9/19
public record UserWithRoles(Long id, String username, Set<PlainRole> roles) {

    public UserWithRoles(User user) {
        this(
                user.getId(),
                user.getUsername(),
                user.getRoles().stream().map(PlainRole::new).collect(Collectors.toSet()));
    }

    @Data
    static class PlainRole {

        /// 主键
        private Long id;

        /// 角色代码
        private String code;

        /// 角色名称
        private String name;

        /// 角色是否启用
        private Boolean enabled;

        public PlainRole(Role role) {
            this.id = role.getId();
            this.code = role.getCode();
            this.name = role.getName();
            this.enabled = role.getEnabled();
        }
    }
}
