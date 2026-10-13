# 任务框架实现约定

本文件记录已确认的需求、模块接口和验证入口。任务核心位于 `avalon-quest`，只依赖 JDK；Avalon 内容、条件、奖励、持久化和事务位于 `avalon-world` 的 `quest` 包。

## 已确认行为

已有目标、条件和奖励通过配置组合；新能力注册处理器，不要求任意脚本。主线默认不可放弃，普通支线放弃后保留进度。每项任务同角色同时只有一个运行实例。

计数事件只统计接取后或恢复后的真实行为，可同时推进多个任务。收集分别支持累计获得和当前持有，扣除物品是独立效果。步骤支持自动结算和手动交付，全部资产与状态、回执在持角色锁的同一事务提交。

每日和每周按服务器配置时区统一重置；默认每周期一次，可配置次数。未达成的旧周期实例过期，已达成待交付保留资格。旧周期完成计入其接取周期。

委托十次一环，每次独立结算，整环额外奖励。按每日开启次数限制，放弃不返还开启次数；放弃后新实例从第一轮开始，已发奖励保留。

实例接取、步骤开放和固定截止时间均可计时，下线继续；超时失败，允许重试时生成新实例并清空进度。限时任务主动放弃同样结束本次实例。已达成目标的周期任务保留跨周期交付资格，但显式限时约束仍须遵守；周期任务的新周期不受旧周期失败的重试限制。

分支为明确互斥选择，确认后同轮不可更改，未选路线跳过；所选路线与公共步骤全部完成才完成整项。恢复保留选择，重开或下一轮重新选择。

不保存任务定义版本或运行快照，实例使用当前定义。调整目标数量和奖励影响未结算任务，已经结算的效果不变。任务、步骤、目标身份、依赖、分支及生命周期不兼容变化必须用新任务编号，旧定义保留。最近一次部署的单份结构基线只用于部署校验，不参与运行。第一版部署加载，无热更新。

## Java 类型组织

公开类型组织遵循 [开发约定](development-constraints.md#工程与数据)，任务包职责如下：

| 包 | 职责 |
| --- | --- |
| `avalon.quest` | 核心引擎、宿主接口和聚合状态 |
| `avalon.quest.model` | 目标、步骤、条件、效果、事件及状态枚举 |
| `avalon.content.quest` | 配置目录加载与校验 |
| `avalon.content.quest.model` | 游戏步骤、规则、生命周期策略及对白配置 |
| `avalon.world.quest` | Avalon 宿主、扩展注册及事务编排 |
| `avalon.world.quest.model` | 实例命令、结算结果和实例视图 |
| `avalon.world.application.model` | 接取、放弃、追踪请求及旧接口展示数据 |

调用方直接引用 `QuestStep`、`QuestContentStep`、`QuestAcceptRequest` 等顶层类型；Java 引用路径改变，HTTP 路由、JSON 字段、数据库结构和业务规则不变。

## 配置接口

旧 `quests.json` 保持兼容。步骤可增加 `runtime`，声明 `objectives`、`conditions`、`effects`、`delivery`、`branch`、`timeout`。`quest-rules.json` 的 `repeatPolicy` 支持 `ONCE`、`DAILY`、`WEEKLY`、`RING`，可增加 `policy` 声明时区、限制、环长度、限时、重试与整环奖励。

核心 `QuestDefinition` 在构造时校验结构。宿主 `QuestEffectHandler`、`QuestConditionHandler` 同时声明类型、校验和执行；重复注册、未知类型、无效资产引用拒绝启动。

## 事务与接口

`GET /api/quests/instances` 查询实例历史，不发奖。`POST /api/quests/instances/commands` 使用 UUID 请求号、实例编号和轮次，操作为 `REFRESH`、`DELIVER`、`CHOOSE`、`ABANDON`。实例和任务主体由登录账号解析，不接受客户端奖励或行为计数。

旧接取入口继续负责位置修订检查、NPC和补给，宿主建立或恢复实例。旧参与状态和一次性步骤回执作为地图、剧情及现有日志的兼容投影；实例是重复任务进度与次数统计的依据。

## 校验入口

`./scripts/validate-quests.sh > /tmp/quest-definitions.json` 离线检查并导出当前定义清单。部署前可传上次清单路径做兼容检查。此命令不连接数据库；应用启动还会与数据库中最近一次成功部署的结构基线检查。

规则测试位于 `QuestEngineTest`，真实PostgreSQL事务测试位于 `QuestRuntimeIntegrationTest`，旧接取和剧情回归继续执行 `QuestAcceptanceIntegrationTest`、`OpeningQuestTest` 等。

## 配置示例

计数、当前持有与消耗分开声明。以下字段放入步骤的 `runtime`，既有 `location`、`acceptance` 和 `npcId` 仍负责接取与交付入口：

```json
{
  "objectives": [
    {"id": "kills", "measure": "EVENT", "code": "KILL:CLAW_CAT", "amount": 10},
    {"id": "held", "measure": "HOLDING", "code": "ITEM:HEALTH_POTION", "amount": 2}
  ],
  "effects": [{"kind": "TAKE_ITEM", "reference": "HEALTH_POTION", "amount": 2}],
  "delivery": "MANUAL"
}
```

累计获得物品用 `measure: EVENT`、`code: ITEM:<物品编号>`；永久知识用 `HISTORY`。普通战斗和副本的合法实际参战者产生 `KILL:<物种编号>`；捕获不计入击杀。`VICTORY:<遭遇或关卡编号>` 可用于战斗胜利次数。

每日规则示例：

```json
{
  "repeatPolicy": "DAILY",
  "policy": {"timezone": "Asia/Hong_Kong", "limit": 1, "ringSize": 10, "retryable": true}
}
```

周任务改为 `WEEKLY`；委托环用 `RING`，`limit` 表示每日开启环数，`ringRewards` 配置整环效果。实例限时放入 `policy.timeout`，步骤限时放入 `runtime.timeout`，使用 `relativeSeconds` 或 `deadline`，两者互斥。分支步骤声明 `branch: {"group": "route", "option": "A"}`；公共汇合步骤的 `requires` 可同时引用各互斥路线的末端，选择后未选路线自动跳过。
