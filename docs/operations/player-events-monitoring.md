# 玩家实时通道监控

应用通过 Actuator 暴露 `/actuator/metrics`，实时通道使用以下低基数指标：

| 指标 | 标签 | 含义 |
| --- | --- | --- |
| `avalon.player.events.connections.active` | 无 | 当前节点打开的 WebSocket 连接数，包含等待首帧认证的连接 |
| `avalon.player.events.authentications` | `result`、`connection` | 首帧认证成功/失败；成功连接区分 `initial` 与 `reconnect` |
| `avalon.player.events.disconnections` | `reason` | 连接关闭原因；未知原因统一归为 `other` |
| `avalon.player.events.timeouts` | `phase` | `authentication` 或 `heartbeat` 超时次数 |
| `avalon.player.events.deliveries` | `event`、`result` | 各类最小失效通知发送成功/失败次数 |

指标标签禁止加入 account、Trainer、resource ID 或任意客户端文本，避免时序库产生高基数。

## 健康告警

`playerEvents` 健康组件观察最近 5 分钟的通知发送失败。窗口内达到 5 次时，`/actuator/health` 转为 `OUT_OF_SERVICE`；失败移出窗口后自动恢复为 `UP`。健康详情包含当前失败数、窗口秒数和阈值，部署平台可直接据此告警。

健康状态用于发现“领域命令已提交但实时失效提示持续无法送达”的故障。单次发送失败仍按 best-effort 处理，不会反向改变 REST 命令结果。
