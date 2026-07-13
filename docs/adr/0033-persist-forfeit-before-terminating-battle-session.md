# Persist Forfeit before terminating Battle Session

只有 Active Match 的当前 Trainer 可以认输；服务端先锁定并持久化 `COMPLETED / WIN / FORFEIT`，将对方 Trainer 记为胜者，再终止对应 Battle Session 释放运行时。认输、回合结算与超时并发时，首个成功取得 Match 终态锁的事件生效，重复认输返回同一结果，Session Termination 不反向决定 Match 胜负。
