# Separate battle execution from player matches

Avalon 将 Battle Session 定义为服务端单节点持有的确定性战斗执行边界，由管理端或 Match 提交完整 Turn Command，且不把账户、Trainer 归属或持久业务状态写入会话模型。Match 单独拥有 Trainer、阵营映射、分别提交、隐藏信息、超时和持久生命周期，在收齐行动后调用 Battle Session；玩家 API 只暴露 `matchId`，底层 `battleSessionId` 仅供 Match Runtime 与管理员诊断使用，玩家不能绕过 Match 直接操作 Session。
