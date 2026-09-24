/**
 * 纯业务规则层。根据显式输入检查约束或计算结果，不依赖 Repository、JPA EntityManager、HTTP Resource 或 Spring
 * 调度。业务拒绝通过明确异常表达，应用命令决定如何在事务中调用规则。没有实际复杂规则的模块不为目录完整性创建空 DomainService。
 */
package io.github.lishangbu.avalon.creature.domain;
