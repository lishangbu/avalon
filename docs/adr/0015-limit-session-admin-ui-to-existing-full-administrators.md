# Limit Session admin UI to existing full administrators

首版 Battle Session 管理页面只面向现有全权限管理员。Avalon 新增 `battle-sessions:run` API 节点和 `battle-sessions` 页面节点，但不创建独立 battle-session-runner 角色；创建阵容继续复用受 `battle-rules:admin` 与 `game-data:admin` 保护的现有赛制和资料选项接口。系统不会为了让普通 Session 操作员使用表单而授予规则或资料写权限。未来真人 Match 或独立操作员需要资料发现时，再设计只读 Battle Catalog API 与权限，而不是扩大当前管理接口的授权范围。
