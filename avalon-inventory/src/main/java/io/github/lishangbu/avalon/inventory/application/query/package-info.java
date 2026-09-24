/**
 * 只读用例层。使用普通查询构建不可变结果，不调用命令服务、不签发奖励、不取得悲观写锁。服务与命令共用 PostgreSQL 和同一套表，不引入独立读库。单条 SQL
 * 读取已提交数据；多字段进度需从同一查询结果派生，不承诺多个独立请求共享快照。
 */
package io.github.lishangbu.avalon.inventory.application.query;
