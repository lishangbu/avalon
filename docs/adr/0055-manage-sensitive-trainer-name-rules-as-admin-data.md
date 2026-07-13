# Manage sensitive Trainer-name rules as admin data

首版把 Sensitive Name Rule 作为数据库持久化的管理端基础数据，由现有 RBAC 保护其 CRUD，并在创建不可修改的 Trainer displayName 时强制校验当前启用规则；支持 `EXACT` 与 `CONTAINS`，不支持正则。词条与名称先按 displayName 规则规范化，再额外移除空格、`_`、`-` 后匹配；规则变更不追溯处置已有 Trainer，基线只以 `EXACT` 预置少量系统保留词。
