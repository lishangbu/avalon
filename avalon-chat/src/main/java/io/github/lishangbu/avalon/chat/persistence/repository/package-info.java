/**
 * 聊天集合存储。使用应用共享JdbcClient和事务连接，SQL显式包含本人参与条件；已读逐消息防重，历史分页和公开过期过滤由数据库完成。
 */
package io.github.lishangbu.avalon.chat.persistence.repository;
