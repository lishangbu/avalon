# Authenticate player APIs without RBAC

普通玩家能力只要求账户认证，不检查角色、access node、额外 OAuth scope 或业务权限：Trainer 管理与进入游戏只需有效 OAuth 登录，Trainer Team、Trainer 查询、Challenge、Match 与有效 Trainer 的 Match History 同时要求 OAuth 登录和 `X-Trainer-Session`。归档 Trainer 无法建立会话，但所属账户可通过 OAuth 账户级只读入口查询其 Match History；没有任何角色的已登录账户也能使用玩家功能，现有 RBAC 仅保护管理端基础数据操作。
