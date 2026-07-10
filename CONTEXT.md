# Avalon Backend

定义 Avalon 后端在游戏资料与可执行战斗规则中使用的稳定领域语言。

## Language

### Game Data

**Current Game Data**:
与 NationalDex 当前状态一致的唯一权威资料快照；版本、世代和历史不属于该模型。
_Avoid_: Versioned Data, Historical Dataset, Generation Data

**Internal Code**:
来源于外部页面 slug、但由 Avalon 自己拥有的稳定机器标识；它不是来源追踪信息。
_Avoid_: Source Slug, Source URL

**Form Inheritance**:
同一物种的形态复用其基准生物未变化资料的关系。
_Avoid_: Version Inheritance, Generation Inheritance

**Identifier**:
后端持有的长整型记录身份，在 JSON 契约中作为不透明十进制字符串表达。
_Avoid_: Numeric JSON ID, Sequence Number

**Support Data**:
因进化或战斗规则必须可执行而保留的资料，即使它不属于当前公开资料；它不被视为历史数据。
_Avoid_: Historical Data, Legacy Data

### API Contract

**Admin API Contract**:
后端为管理端消费者提供的权威机器可读接口描述，包含字段存在性、可空性和 Identifier 形态。
_Avoid_: Frontend DTO, Local API Shape

### Battle Rules

**Executable Battle Rule**:
运行时能够执行、且所有资料引用都必须解析到 Current Game Data 或明确 Support Data 的战斗规则。
_Avoid_: Battle Metadata, Historical Rule
