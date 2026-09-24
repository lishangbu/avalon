/**
 * 持久化查询层。使用具体实体的 Spring Data JPA Repository
 * 封装查询语句和锁定操作，不提供泛型万能业务服务。调用者决定事务边界，本层不自行启动独立事务打断奖励原子性。ID 和来源参数通过绑定传入，不能拼接用户输入构造查询。
 */
package io.github.lishangbu.avalon.creature.persistence.repository;
