---
status: accepted
---

# 全量建立道具目录与重 RPG 持久化基础

## 背景

道具目录需要覆盖背包、商店、掉落、制作、任务和世界交互。Team Member 是 Battle 外可编辑的对战配置，不是 PlayerCharacter 实际拥有且持续成长的 Creature；二者必须保持独立。

## 决策

- 经审查的固定导入结果全量建立规范 Item Catalog Entry。Stable Code、用途、价格、启用状态和可执行效果由 Avalon 决定。
- 同一个上游道具出现在多个目录位置时只创建一个主体，并通过多对多目录关系保留全部分类。英文名不能区分的三个身份组使用显式 Stable Code 映射，不使用中文名或数组位置充当持久身份。
- 尚无玩法语义的道具使用 `catalog` 用途并保持禁用；描述文本不得自动推断背包行为、经济价格或战斗效果。
- Owned Creature 独立于 Creature 实时资料、Team Member 和 Battle Participant 持久化。Party 只能引用同一 PlayerCharacter 实际拥有的 Creature。
- 背包与钱包分别保存当前余额和不可变增减流水。商店、掉落、任务奖励和制作配方只引用 Item Catalog Entry，不复制道具名称或效果。
- Region、Location、NPC、Encounter Table、Loot Table、Shop、Quest、Dialogue、Recipe、Profession、Checkpoint 与强类型 World State 建立独立关系表；不把不同 RPG 子域压缩为通用 JSON 聚合。
- 本决策只建立关系 Schema 与初始道具目录。具体命令事务、管理 API、玩家 API、效果执行器和首套世界内容按垂直领域后续实现。

## 后果

空数据库可以直接获得完整道具身份和重 RPG 所需的关系边界，Team 配置与玩家资产保持独立。数据库表数和初始化资料体积增加，新增 Item Catalog Entry 默认不可用，因此后续仍必须显式实现用途、获取方式与事务服务后才能进入玩家进度。

本决策确定 RPG 资料使用独立关系表和显式初始 SQL 基线；具体命令事务、管理 API、玩家 API、效果执行器和首套世界内容按垂直领域后续实现。
