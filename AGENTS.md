# AGENTS.md

## Instructions

- 如果当前环境提供 `rtk`，优先用 `rtk` 包装 shell 命令
- 全程优先使用中文沟通；新增注释和项目内说明优先使用中文，专有名词保留英文

使用项目内技能：

- `./.agents/skills/avalon-guardrails`
- `./.agents/skills/avalon-code-commenting`
- `./.agents/skills/avalon-module-scaffold`
- `./.agents/skills/avalon-flyway-schema`
- `./.agents/skills/avalon-transaction-outbox`
- `./.agents/skills/kotlin-jvm-api-guidelines`

详细项目约束以：

- `./.agents/skills/avalon-guardrails/references/foundation-constraints.md`

为准。`AGENTS.md` 只保留仓库级入口规则，不重复展开完整细则。

## 核心目标

- 优先交付结构清晰、性能合理、便于长期演进的实现
- 优先减少重复代码、重复 SQL、重复映射、重复事务编排和重复事件处理
- 避免业务规则散落在 `interfaces`、`application`、`infrastructure` 多层重复出现

## 最小硬约束

- 先识别主归属 `bounded context`，再改动代码
- 不允许跨上下文 `repository` 直接调用
- 不允许跨上下文强外键
- 不允许写库加发消息的裸双写；跨上下文通知优先 `local transaction + outbox`
- 不主动加入无关兼容分支、额外兜底参数或 hack 式容错
- 模块入口文件、`shared-kernel` 下对外公开的核心抽象、跨上下文公共契约、复杂单元都必须补充中文说明
- 复杂单元的注释优先解释设计原因、事务边界、数据流和性能取舍；缺失时视为实现未完成
- 当前改动触及 `deprecated` API 时，默认优先替换为当前基线支持的非过期用法；如果暂时无法替换，必须明确说明原因与收口计划

## 默认工作方式

- 先理解现有实现，再改动
- 先复用，再扩展，再新增
- 先识别热路径，再实现
- 清理只发生在当前改动直接相关的范围内
- 改动后执行与范围匹配的最小验证，并明确说明结果
