# Treat account disablement as disconnection

管理员禁用账户或撤销其 Sa-Token token 时，使对应 Trainer Session 失效并立即移除 Trainer Presence，但不直接裁决 Match。Pending Challenge 按原五分钟期限自然过期，Active Match 继续等待行动并由九十秒期限决定是否超时判负；系统不把管理操作伪装成 Forfeit 或 Interrupted，Trainer 与 Match History 继续保留。

