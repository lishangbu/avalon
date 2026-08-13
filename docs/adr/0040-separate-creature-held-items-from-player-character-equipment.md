# 0040：分离 Creature 携带物与 PlayerCharacter 装备

状态：已接受

## 决策

系统把 Creature 的 Held Item 与 PlayerCharacter 的多槽 Equipment 建模为两个独立领域概念。

Held Item 仍是 Owned Creature 上至多一件的战斗道具。它来自 PlayerCharacter 的聚合 Inventory Stack，并在替换、卸下、Creature 离开角色和 Battle 终局消费时，通过同一 PostgreSQL 事务完成扣除、归还或清空。Held Item 不占用 PlayerCharacter 的 Equipment Loadout，也不能作为 Equipment Instance 穿戴。

Equipment Catalog Entry 在 Item Catalog Entry 之上声明角色装备资料；玩家每次获取装备时建立独立 Equipment Instance。Equipment Instance 不同时计入聚合 Inventory Stack，PlayerCharacter 通过单一版本化 Equipment Loadout 原子替换整套槽位关系。该选择为每件资产提供稳定身份，使穿戴、出售、幂等重放和资产流水不依赖可变的聚合数量。

Equipment Catalog Entry 同时声明出售使用的 Currency 与基础价格。出售命令只接受未穿戴实例身份和版本，在同一 PostgreSQL 事务中写入实例出售终态、Equipment Transaction、PlayerCharacter Wallet、Currency Transaction、幂等响应与 Outbox；客户端不能提交货币或价格。

Battle 创建时冻结所有影响该场战斗的 Held Item 或 Equipment 来源事实及编译结果。已开始的 Battle 不回查当前 Inventory Stack、Owned Creature、Equipment Loadout 或实时资料；Battle Reservation 存在期间禁止替换相关携带物或装备。

Shop、Quest 和 Loot 仍以 Item Catalog Entry 作为商品或奖励身份。事务在权威来源内部解析该 Item 是否对应 Equipment Catalog Entry：普通 Item 增加 Inventory Stack，Equipment Item 为每个数量单位建立独立 Equipment Instance。玩家端不存在可提交 `source_type` 的通用发放命令；Shop Purchase 冻结支付事实，Quest Reward Claim 绑定任务完成轮次，Loot Settlement 由 Battle 或世界交互预先建立。

PlayerCharacter 可以保留多项职业成长，但只有 Active Profession Set 参与装备资格。切换激活集合时必须在同一事务锁定当前 Equipment Loadout 并重新校验；不允许通过隐式卸装掩盖职业与装备冲突。

## 后果

- Held Item 与 Equipment 使用不同的资格、生命周期、事务和错误语义，不提供互相转换或兼容路径。
- 可堆叠普通道具继续使用 Inventory Stack；Equipment Instance 以独立资产存在。
- 当前 Creature Battle 只接入 Held Item。PlayerCharacter Equipment 只影响未来人物 PvE/PvP，在执行器接入前不得启用具有非空被动规则的装备资料。
- Team 是玩家按实时 Creature 资料配置的竞技阵容，不是 Owned Creature 集合；PvP 与 Training 不伪造 Owned Creature 身份。只有 Encounter Party Snapshot 保存 Owned Creature Identifier 并允许终局写回或 Held Item 消费。
- Encounter、Training 与 PvP 的真人 Participant 都冻结版本化 Equipment Snapshot；当前 Creature Battle Engine 不把人物装备修正错误套用到 Creature 属性。
- 管理端维护 Equipment Catalog Entry，并只读诊断实例、Loadout 和流水；玩家换装契约只由玩家服务暴露。
