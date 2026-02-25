/// 授权模块实体包包含授权模块的 JPA 实体类，用于持久化授权相关数据（例如角色、权限、用户、客户端等）
///
/// 说明与注意事项
/// - 实体应仅包含持久化相关字段与映射注解；业务逻辑与 DTO 请放在其他包中
/// - 避免在实体中放置大量非持久化方法；必要的转换可通过 Mapper/Converter 实现
///
/// @author lishangbu
/// @since 2026/2/25
package io.github.lishangbu.avalon.authorization.entity;
