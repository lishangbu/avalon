# Bind game actions to a Trainer Session

一个账户可以拥有多个 Trainer，因此账户登录本身不足以确定游戏行为主体。账户进入游戏时选择自己拥有的 Trainer 并建立 Trainer Session，每个账户同一时刻最多一个有效 Trainer Session；之后的 REST/WebSocket 行为从该会话取得 Trainer 身份，而不是信任调用方逐请求提交的 `trainerId`。首版 Trainer Session 只保存在单节点内存中，不建数据库表或 Redis；采用可配置的三十分钟滑动空闲期限，合法游戏请求或已认证 WebSocket 心跳会刷新期限，退出、切换、新设备进入或进程重启立即失效，而会话丢失不改变持久 Match 的状态。HTTP 游戏请求同时携带 Sa-Token Bearer token 与 `X-Trainer-Session`，WebSocket 因浏览器不能设置自定义握手头而在首个应用消息中同时认证账户与 Trainer Session；Trainer 凭证不使用 Cookie、URL 或浏览器持久存储。

