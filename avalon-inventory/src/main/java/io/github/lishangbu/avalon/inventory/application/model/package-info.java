/**
 * 不可变用例数据模型。模型独立于 Service 内部类型与 JPA 实体，允许 API 直接序列化稳定查询结果，避免机械复制同构 DTO。内部上下文保留 Long
 * ID，浏览器展示结果保留原有字符串 ID 契约。模型不能承载数据库会话、懒加载代理或实体修改权限。
 */
package io.github.lishangbu.avalon.inventory.application.model;
