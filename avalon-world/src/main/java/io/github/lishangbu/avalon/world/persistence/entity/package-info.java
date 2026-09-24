/**
 * 数据库映射层。JPA 实体只在所属业务模块内部使用，不能作为 API 响应或跨模块接口。Lombok 生成字段访问器但不生成实体
 * equals/toString，避免泄露密码或展开关联。唯一性、外键与数量边界仍由 Liquibase 定义的数据库约束兜底。
 */
package io.github.lishangbu.avalon.world.persistence.entity;
