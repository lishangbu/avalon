package io.github.lishangbu.avalon.auth.model;

/**
 * 用户信息
 *
 * @param id 用户标识
 * @param username 用户名
 * @param password 密码
 * @param roleCodes 角色代码
 * @author lishangbu
 * @since 2025/8/9
 */
public record UserDTO(Long id, String username, String password, String roleCodes) {}
