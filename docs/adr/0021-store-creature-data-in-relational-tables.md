# 0021：使用关系表保存 Creature 资料并按需组装运行时投影

## 状态

已接受。

## 背景

Creature 资料需要支持有界分页、记录级并发控制和数据库关系约束，因此 Species、Creature、Form、能力、特性、技能学习、携带物和皮肤必须独立维护。

## 决定

- 使用独立关系表保存 Species 字典、Species、Creature、Form、属性、能力、技能学习、携带物与皮肤资料。
- 首套资料通过单一资料基线导入。
- 管理 API 按 Species、Creature 和各关系资源提供有界查询与记录级写入。
- Creature Metadata Reader 从关系表组装 `creaturemetadata.Data` 运行时投影，供 Team 校验与 Battle 创建读取。该投影不是持久化模型。
- 管理资料写入在停机维护期间执行，使用记录级乐观版本，并写入幂等响应和审计事实。

## 结果

数据库可以直接执行外键、唯一性和范围约束，技能学习关系可以独立分页。运行时读取只在完整 Team/Battle 校验边界组装当前投影；管理列表不会触发该成本。
