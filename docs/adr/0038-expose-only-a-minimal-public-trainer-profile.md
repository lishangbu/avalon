# Expose only a minimal Public Trainer Profile

通过规范化后的完整 displayName 精确查找时只返回 `displayName`、`online` 与 `challengeable`，不暴露内部 Trainer Identifier、账户 ID、所属账户、Trainer Team、Match 历史、战绩、当前对手或不可挑战的具体原因。归档 Trainer 按不存在处理；同账户、离线或已有活跃 Match 等情况只通过 `challengeable = false` 收敛表达，不提供前缀、模糊搜索或候选列表。
