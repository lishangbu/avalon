# Separate Trainer Presence from Session validity

Trainer Session 使用三十分钟滑动空闲期限，但 Trainer Presence 由十五秒认证心跳或合法游戏请求刷新，超过四十五秒无活动即视为离线；离线不立即撤销会话，账户仍可重连。只有当前在线 Trainer 可以成为新 Challenge 的目标，Presence 丢失不结束 Match，仍由 Match 的九十秒行动期限裁决。
