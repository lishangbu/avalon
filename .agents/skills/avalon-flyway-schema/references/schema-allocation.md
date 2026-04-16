# Schema 分配

## 1. 默认 schema

默认使用下面这些 schema：

- `iam`
- `catalog`
- `player`
- `battle`
- `integration`

## 2. 归属规则

- `iam`：用户、角色、权限、菜单、验证码工件、认证审计
- `catalog`：参考数据和规则事实
- `player`：玩家档案、背包、已拥有生物、存储结构
- `battle`：战斗会话数据、快照、projection、ledger
- `integration`：outbox 和其他跨上下文集成支撑表

## 3. 硬规则

- 一张表只能有一个归属上下文
- 不要把一个上下文的写模型放进另一个上下文的 schema
- 不要用跨上下文强外键来模拟归属关系
