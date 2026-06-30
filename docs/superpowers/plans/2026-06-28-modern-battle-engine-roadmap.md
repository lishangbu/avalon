# 现代主系列战斗引擎路线图

**目标：** 在现有 `game-data` 静态资料基础上，新增现代主系列回合制战斗规则、不同对战格式、确定性战斗引擎和管理端维护能力。实现代码必须自研，规则正确性通过成熟公开对战引擎或公开规则用例进行对照验证。

## 硬性约束

- 只支持现代主系列回合制规则，不做历史版本兼容。
- 支持不同对战规则和赛制，但不引入游戏版本、世代或版本组维度。
- 代码、包名、模块名、表名、API 路径和权限 code 继续使用中性命名，不使用源品牌术语。
- 资料数据内容可以保留实际条目文本，例如现有生物、技能、特性、道具的中文名称。
- 战斗规则实现目标是现代规则正确，不追求复刻历史漏洞或旧版本差异。
- 实现逻辑不直接复制外部项目代码；允许参考公开规则说明、成熟公开引擎行为和公开测试用例。
- 实现类必须提供非常完整的 KDoc，不能采用普通 CRUD 类的简短注释标准。
- 战斗引擎必须可复盘：同一初始状态、规则快照、行动序列和随机序列应得到一致事件流。
- 数据库设计继续遵守三范式，不保存 raw JSON 作为业务事实来源。

## 非目标

- 不实现即时战斗或动作类战斗系统。
- 不实现 GO、HOME、跨世代传送、真实线上服务或外部平台集成。
- 不实现历史规则切换。
- 不在第一阶段实现 52poke 游戏系统分类下所有非战斗系统。
- 不把文本效果说明当作可执行规则。

## 领域拆分

| 子域 | 分类 | 说明 |
| --- | --- | --- |
| 战斗引擎 | 核心域 | 负责战斗状态、行动结算、事件流、胜负判定和确定性随机数。 |
| 战斗规则 | 核心域 | 负责技能、特性、道具、状态、天气、地形和对战格式的可执行规则建模。 |
| 游戏资料 | 支撑域 | 继续提供生物、种类、技能、特性、道具、属性、数值项等静态资料。 |
| 管理端 | 支撑域 | 维护规则资料、对战格式和测试 fixture。 |
| 系统管理 | 通用域 | 继续提供 RBAC、OAuth client、token、JWK 和调度管理。 |

## 限界上下文

### `game-data`

- 已存在。
- 保存静态资料，不参与战斗过程计算。
- 输出给战斗规则模块的内容是稳定引用对象：技能、特性、道具、属性、数值项、生物和种类。
- 不新增版本维度。

### `battle-rules`

- 新增模块。
- 保存现代主系列规则的结构化描述。
- 管理不同对战格式，例如单打、双打、官方竞技、自定义规则。
- 将数据库规则转换为引擎可执行的规则快照。
- 不保存战斗运行态。

### `battle-engine`

- 新增模块。
- 纯 Kotlin 规则计算模块，尽量不直接依赖 Spring、Jimmer 或数据库。
- 输入为 `BattleInitialState`、`BattleFormatSnapshot`、`BattleRuleSnapshot`、行动序列和随机源。
- 输出为 `BattleEvent` 流、最终 `BattleState` 和 `BattleResult`。

### `battle-admin`

- 后端可归入 `battle-rules` 管理 API，前端归入 `avalon-admin-ui`。
- 每类规则独立页面维护，不做一个万能大页面。
- 页面用于 CRUD、规则预览、fixture 管理和对照测试结果查看。

## 统一术语

| 术语 | 含义 |
| --- | --- |
| 生物 `creature` | 可参与战斗的具体资料条目，对应现有 `game_creature`。 |
| 种类 `species` | 生物所属的种类资料，对应现有 `game_species`。 |
| 技能 `skill` | 战斗中可选择的行动能力，对应现有 `game_skill`。 |
| 特性 `ability` | 生物被动能力，对应现有 `game_ability`。 |
| 道具 `item` | 战斗中可携带、使用或触发的道具，对应现有 `game_item`。 |
| 对战格式 `battle_format` | 单打、双打、官方竞技、自定义规则等赛制配置。 |
| 规则快照 `rule_snapshot` | 某次战斗开始时冻结的一组可执行规则。 |
| 行动 `battle_action` | 本回合一方选择的技能、替换、使用道具等动作。 |
| 事件 `battle_event` | 引擎结算产生的状态变化记录，是回放和调试的事实来源。 |
| Hook | 特性、道具、状态等规则插入战斗流程的触发点。 |
| Fixture | 用于验证规则的固定输入、随机序列、期望事件和期望状态。 |

## 命名反例

- 不使用源品牌词作为模块名、包名、表名、类名或 API 路径。
- 不使用 `pokemon`、`poke`、`generation`、`version_group` 等命名表达业务边界。
- 不使用历史版本命名表达规则差异。

## 注释标准

### 类级 KDoc 必须包含

- 类的职责和不负责的边界。
- 输入和输出模型。
- 在战斗生命周期中的位置。
- 状态不变量。
- 与规则资料或其他引擎组件的关系。
- 对照测试期望覆盖的关键场景。

### 方法级 KDoc 必须包含

- 方法处理的规则阶段。
- 参数含义，尤其是战斗状态、行动、随机源和规则快照。
- 返回结果和事件产生方式。
- 失败条件或不处理条件。
- 涉及公式时说明取整、截断、倍率叠加和随机数消费规则。

### 私有方法注释

- 承载战斗规则细节的私有方法也必须注释。
- 纯粹的字段转换、小型 getter、简单集合操作可以不写。

### 测试注释

- 每个 fixture 要说明规则场景、参考来源类型、固定随机序列意图和验证重点。
- 对照测试失败时要能从注释看出是公式差异、触发时机差异还是资料缺失。

## 数据模型规划

### 对战格式

- `battle_format`
  - `id`
  - `code`
  - `name`
  - `battle_type`
  - `team_size`
  - `select_size`
  - `active_slots_per_side`
  - `max_turns`
  - `team_preview`
  - `level_cap`
  - `level_flattened`
  - `enabled`

- `battle_format_clause`
  - `id`
  - `format_id`
  - `clause_type`
  - `clause_code`
  - `name`
  - `enabled`

- `battle_format_restriction`
  - `id`
  - `format_id`
  - `target_type`
  - `target_id`
  - `restriction_type`
  - `reason`
  - `enabled`

### 特殊机制

- `battle_special_mechanic`
  - `id`
  - `code`
  - `name`
  - `description`
  - `enabled`

- `battle_format_special_mechanic`
  - `id`
  - `format_id`
  - `mechanic_id`
  - `allowed`

### 状态、天气和场地

- `battle_status_rule`
  - `id`
  - `code`
  - `name`
  - `status_kind`
  - `stacking_policy`
  - `duration_policy`
  - `turn_start_hook`
  - `turn_end_hook`
  - `enabled`

- `battle_weather_rule`
  - `id`
  - `code`
  - `name`
  - `default_duration`
  - `damage_modifier_policy`
  - `element_modifier_policy`
  - `end_turn_effect_policy`
  - `enabled`

- `battle_terrain_rule`
  - `id`
  - `code`
  - `name`
  - `default_duration`
  - `target_filter_policy`
  - `element_modifier_policy`
  - `status_modifier_policy`
  - `enabled`

- `battle_field_rule`
  - `id`
  - `code`
  - `name`
  - `side_scope`
  - `duration_policy`
  - `effect_policy`
  - `enabled`

### 技能规则

- `battle_skill_rule`
  - `id`
  - `skill_id`
  - `rule_code`
  - `category`
  - `target_policy`
  - `accuracy_policy`
  - `damage_policy`
  - `priority_policy`
  - `contact_policy`
  - `protect_policy`
  - `reflectable`
  - `snatchable`
  - `sound_based`
  - `punch_based`
  - `bite_based`
  - `pulse_based`
  - `ballistics_based`
  - `powder_based`
  - `dance_based`
  - `enabled`

- `battle_skill_effect`
  - `id`
  - `skill_rule_id`
  - `effect_order`
  - `trigger_hook`
  - `condition_policy`
  - `effect_policy`
  - `chance`
  - `target_policy`
  - `enabled`

- `battle_skill_status_effect`
  - `id`
  - `skill_effect_id`
  - `status_rule_id`
  - `application_policy`
  - `duration_policy`

- `battle_skill_stat_effect`
  - `id`
  - `skill_effect_id`
  - `stat_id`
  - `stage_delta`
  - `target_policy`

### 特性规则

- `battle_ability_rule`
  - `id`
  - `ability_id`
  - `rule_code`
  - `trigger_hook`
  - `priority`
  - `condition_policy`
  - `effect_policy`
  - `suppressible`
  - `enabled`

### 道具规则

- `battle_item_rule`
  - `id`
  - `item_id`
  - `rule_code`
  - `item_rule_kind`
  - `trigger_hook`
  - `priority`
  - `condition_policy`
  - `effect_policy`
  - `consumable`
  - `enabled`

## 规则表达方式

第一版不直接做自由脚本语言，采用“枚举策略 + 结构化参数”的方式：

- `condition_policy`：固定枚举条件，例如生命低于比例、受到指定属性攻击、处于指定状态。
- `effect_policy`：固定枚举效果，例如修改能力等级、附加状态、改变天气、修改伤害倍率。
- `target_policy`：固定枚举目标，例如自身、攻击者、防御者、己方全场、对方全场、全部相邻目标。
- `damage_policy`：固定枚举伤害模型，例如普通物理、普通特殊、固定伤害、比例伤害、自损伤害。

后续如果策略枚举无法表达复杂规则，再增加受限 DSL。受限 DSL 必须：

- 有 schema。
- 有静态校验。
- 有可视化预览。
- 不能直接执行任意 Kotlin、SQL 或 JavaScript。
- 不能绕过引擎事件流。

## 战斗引擎状态机

### 初始化阶段

- 加载规则快照。
- 校验对战格式。
- 校验队伍合法性。
- 计算战斗初始能力。
- 初始化双方场上位置。
- 初始化随机源。
- 产出 `BattleStarted` 事件。

### 回合开始阶段

- 递增回合。
- 处理回合开始状态。
- 处理必须先结算的特性、天气、场地或道具 hook。
- 请求行动。

### 行动选择阶段

- 校验技能是否可用。
- 校验 PP。
- 校验替换目标。
- 校验格式条款。
- 锁定本回合行动。

### 行动排序阶段

- 计算优先度。
- 计算速度修正。
- 处理同速随机。
- 处理强制行动、失控、锁定技能、先制和后制。

### 行动执行阶段

- 检查行动者是否仍可行动。
- 检查目标是否仍有效。
- 消耗 PP。
- 处理命中判定。
- 处理免疫、保护、替身、无效化等前置规则。
- 计算伤害。
- 结算伤害事件。
- 结算附加效果。
- 触发特性和道具 hook。

### 回合结束阶段

- 处理天气、地形、场地、状态持续伤害或恢复。
- 处理持续回合递减。
- 处理濒死和替换请求。
- 判断胜负。
- 产出 `TurnEnded` 或 `BattleEnded` 事件。

## 核心类型规划

- `BattleInitialState`
- `BattleFormatSnapshot`
- `BattleRuleSnapshot`
- `BattleState`
- `BattleSide`
- `BattleSlot`
- `BattleActor`
- `BattleMoveSet`
- `BattleAction`
- `BattleActionOrder`
- `BattleEvent`
- `BattleResult`
- `BattleRandom`
- `BattleReplay`

## 对照测试策略

### 原则

- 实现自研，验证不闭门。
- 优先收集公开规则用例和成熟公开引擎可复现结果。
- 每个复杂规则都需要至少一个 fixture。
- fixture 不保存外部项目代码，只保存输入、行动、随机序列、期望事件和期望最终状态。

### Fixture 结构

- `name`
- `description`
- `initial_state`
- `format_code`
- `rule_snapshot_code`
- `random_sequence`
- `actions`
- `expected_events`
- `expected_final_state`

### 第一批 fixture

- 普通物理伤害。
- 普通特殊伤害。
- 属性克制。
- 属性一致加成。
- 无效属性。
- 命中和闪避等级。
- 击中要害。
- 能力等级提升和下降。
- 灼伤对物理伤害影响。
- 麻痹对速度影响。
- 睡眠回合。
- 中毒和剧毒回合末伤害。
- 天气对伤害影响。
- 地形对技能威力和状态免疫影响。
- 双打范围技能伤害修正。
- 替换后能力等级和临时状态处理。
- 携带道具触发和消耗。
- 特性在出场、受击、攻击前后的触发顺序。

## 管理端页面规划

- 对战格式页面。
- 对战条款页面。
- 对战限制页面。
- 特殊机制页面。
- 状态规则页面。
- 天气规则页面。
- 地形规则页面。
- 场地规则页面。
- 技能规则页面。
- 技能效果页面。
- 特性规则页面。
- 道具规则页面。
- Fixture 页面。
- 对照测试结果页面。

每个页面独立维护，不使用一个通用万能页面替代全部业务语义。

## 后端 API 规划

- `/api/battle-rules/formats`
- `/api/battle-rules/format-clauses`
- `/api/battle-rules/format-restrictions`
- `/api/battle-rules/special-mechanics`
- `/api/battle-rules/status-rules`
- `/api/battle-rules/weather-rules`
- `/api/battle-rules/terrain-rules`
- `/api/battle-rules/field-rules`
- `/api/battle-rules/skill-rules`
- `/api/battle-rules/skill-effects`
- `/api/battle-rules/ability-rules`
- `/api/battle-rules/item-rules`
- `/api/battle-rules/fixtures`
- `/api/battle-rules/test-runs`

权限：

- `battle-rules:admin`：规则资料维护。
- `battle-engine:test`：执行 fixture 或模拟战斗。

## 实施阶段

### 阶段 1：规则资料基础

- 新增 `battle-rules` 模块。
- 新增 Liquibase 表和 remarks。
- 新增默认权限、菜单和管理员角色绑定。
- 新增对战格式、状态、天气、地形、场地、技能规则、特性规则、道具规则的基础 CRUD。
- 前端新增独立页面。
- 先导入少量现代主系列规则样例。

### 阶段 2：引擎骨架

- 新增 `battle-engine` 模块。
- 定义核心状态、行动、事件、随机源。
- 实现单打初始化、行动排序、技能执行、伤害和胜负判定。
- 增加 replay 输出。
- 增加第一批公式级测试。

### 阶段 3：单打 MVP

- 支持单打格式。
- 支持普通技能、变化技能、状态、天气、地形、基础特性、基础道具。
- 完成第一批 fixture 对照测试。
- 管理端支持 fixture 运行和结果查看。

### 阶段 4：双打 MVP

- 支持双打格式。
- 支持相邻目标、全体目标、己方目标和范围技能。
- 支持双打行动顺序和目标失效重定向。
- 增加双打 fixture。

### 阶段 5：对战格式扩展

- 支持官方竞技格式和自定义格式。
- 支持等级统一、队伍预览、重复种类限制、重复道具限制、禁用列表。
- 支持格式合法性校验 API。

### 阶段 6：规则覆盖扩展

- 扩展技能效果覆盖率。
- 扩展特性覆盖率。
- 扩展道具覆盖率。
- 增加规则覆盖报表。
- 逐步补齐公开用例。

## 分批提交建议

1. `docs`: 写入本路线图和规则注释标准。
2. `feat(battle-rules)`: 新增模块、权限、菜单和第一批表结构。
3. `feat(battle-rules)`: 新增规则资料 CRUD。
4. `feat(battle-rules-ui)`: 新增管理端页面。
5. `feat(battle-engine)`: 新增引擎核心模型和随机源。
6. `feat(battle-engine)`: 实现伤害、命中、属性和行动顺序。
7. `test(battle-engine)`: 引入第一批公开对照 fixture。
8. `feat(battle-engine)`: 实现状态、天气、地形。
9. `feat(battle-engine)`: 实现特性和道具 hook。
10. `feat(battle-engine)`: 支持双打。

## 验收标准

- 所有新表和字段都有 Liquibase remarks。
- 所有实现类都有完整 KDoc。
- 规则实现类有规则阶段、触发时机和边界说明。
- 引擎核心不直接读写数据库。
- fixture 能固定随机序列并稳定复现。
- 单打 MVP 通过公开对照用例。
- 双打 MVP 通过公开对照用例。
- 管理端能查看规则覆盖和测试结果。
- 代码、表名、API 和权限 code 不出现源品牌术语。

## 待确认但不阻塞阶段 1 的问题

- 第一批官方竞技格式的默认禁用列表如何维护：手动维护，还是只提供自定义格式能力。
- 特殊机制是否在第一阶段只建表不实现，还是单打 MVP 就实现一个代表机制。
- fixture 是否要落库管理，还是先以测试资源文件管理。
