/**
 * HTTP 适配层。解析请求、取得认证身份并调用应用用例；不得持有 EntityManager、Repository
 * 或实体。请求字段只表达玩家意图，奖励、归属和等级等权威值由服务端计算。同步 JDBC 的 Resource 使用虚拟线程，不能在 Vert.x 事件循环内执行阻塞操作。
 */
package io.github.lishangbu.avalon.identity.api;
