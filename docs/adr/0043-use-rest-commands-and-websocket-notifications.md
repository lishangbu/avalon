# Use REST commands and WebSocket notifications

首版玩家写命令统一使用 REST：Trainer、Trainer Team、Challenge、Trainer Turn Submission 与 Forfeit 都保留单一的幂等、revision 和错误语义；WebSocket 只在首帧认证 Sa-Token 与 Trainer Session 后承担心跳和最小失效通知，消息只包含类型、资源 ID 与最新 revision，不携带 Team Snapshot、行动或 Battle Session 原始事件。客户端收到通知、发现 revision 跳号或断线重连时，都通过 REST 重新获取当前 Trainer 的权威视图；系统不维护消息补发日志、第二套业务 DTO 或 WebSocket 命令入口。

