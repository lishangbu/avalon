# Separate battle execution from player matches

Avalon 将 Battle Session 定义为服务端单节点持有的确定性战斗执行边界，由管理端提交完整 Turn Command，且不把用户、玩家归属或持久业务状态写入会话模型。未来 Match 单独拥有玩家、阵营映射、分别提交、隐藏信息、超时和持久生命周期，在收齐行动后调用 Battle Session；这让当前 MVP 保持可交付，也避免以后为真人对局改写引擎或把高频运行态绑定到关系数据库。
