/**
 * 写用例层。事务入口负责将校验、状态变化与幂等凭证原子提交；跨领域任务效果使用 MANDATORY 加入同一事务。需要角色或设备锁的操作在命令代码中主动取得锁，不能由前端
 * busy 状态替代。不得在这里读取浏览器 DOM、拼接 HTTP 响应或引入异步消息最终一致性。
 */
package io.github.lishangbu.avalon.world.application.command;
