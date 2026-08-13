---
status: accepted
supersedes:
  - 0029
  - 0033
---

# 在维护窗口提供 RPG 世界资料在线维护

管理员服务在停机维护窗口内提供 Region、Location、Location Exit 及后续 RPG 资料的强类型写入 RPC。玩家与管理员 Protobuf、认证域和可见字段继续严格分离；玩家服务仍只读取 Discovery 裁剪后的安全投影，管理员写入不能绕过服务端规则编译和拓扑完整性校验。

每种 RPG 资料保留专属消息和 RPC，不提供资源类型加任意字段的通用 CRUD，也不接受自由 JSON 或脚本。写入使用 Snowflake Identifier、幂等键、管理审计和乐观版本；稳定记录只允许停用，不物理删除。拓扑写入在事务内校验引用、层级无环、出口有向关系与出生点约束，失败时不发布无效状态。

本决定仅取代 ADR 0029 和 ADR 0033 中“管理员首期只读、不提供在线编辑 RPC”的限制。服务端权威拓扑、强类型 Exit Condition/Traversal Effect、玩家与管理员可见边界及 unary RPC 决定继续有效。
