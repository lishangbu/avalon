# 命名与列规则

## 1. 表命名

- 使用小写 `snake_case` 表名
- 优先使用单数或项目内一致的命名风格，但不要在同一个 schema 中混用

## 2. 约束与索引命名

- 主键：`pk_<table>`
- 唯一约束：`uk_<table>__<column>` 或 `uk_<table>__<col1>__<col2>`
- 外键：`fk_<table>__<column>`
- 索引：`idx_<table>__<column>` 或 `idx_<table>__<col1>__<col2>`

## 3. 列基线

对于可变聚合表，通常要评估是否需要：

- 主键
- 乐观锁版本列，在并发更新重要时启用
- `created_at`
- `updated_at`

### 3.1 通用描述字段语义

对参考数据、管理端可维护字典、枚举扩展表和其他需要稳定展示的聚合，优先使用以下字段语义：

- `code`：稳定业务编码，用于系统内部引用、跨上下文契约、管理端选择项和导入导出匹配；独立参考数据表通常应添加唯一约束。
- `name`：短展示名称，面向用户、运营或管理端展示，不承载内部路由、权限表达式或复杂说明。
- `description`：通用短说明，可为空；适合列表、详情页摘要或管理端备注。
- `long_description`：长正文说明，可为空；只有存在明显长文本需求时才添加，Kotlin/JSON 字段对应 `longDescription`。

不要为了套用通用字段而掩盖具体语义：

- 枚举型或分类型字段使用 `xxx_code`，例如 `category_code`、`trigger_code`、`slot_code`，不要改成普通 `code`。
- 描述特定业务概念时优先带上限定词，例如 `effect_description`、`short_effect_description`，不要使用含义过泛的 `effect` 或
  `text`。
- 前端路由、导航和 UI 树可以保留更贴近语义的命名，例如 `menu_key`、`title`；这类例外应由上下文语义解释，而不是机械统一为
  `code`、`name`。
- 不要因为某张表拥有 `code`、`name`，就机械添加 `description` 或 `long_description`；字段必须服务于真实展示、检索或编辑场景。

### 3.2 排序字段语义

排序字段必须表达稳定的业务展示顺序，而不是临时查询参数。

- `sorting_order`：统一排序字段名，适用于参考数据、管理端可维护字典、图鉴/列表展示项、菜单树、权限树等需要稳定业务顺序的结构；Kotlin/JSON
  字段统一对应 `sortingOrder`。
- 查询入参中的 `sort`、`orderBy`、`direction` 不应落为表字段，除非它本身就是业务状态。
- 不要引入 `sort_order`、`order_index` 等近似排序字段；需要持久化业务排序时统一使用 `sorting_order`。
- 排序字段默认使用 `INTEGER NOT NULL DEFAULT 0`；是否增加 `(sorting_order, id)` 或 `(parent_id, sorting_order, id)`
  索引必须由真实列表、树查询或热路径驱动。
- 稳定排序查询应补充确定性兜底列，例如 `ORDER BY sorting_order, id`，避免同排序值记录在分页或重复查询中顺序漂移。

对于 join、projection 和 ledger 风格表：

- 只保留访问路径真正需要的列
- 不要机械继承审计列

## 4. 外键规则

- 同一上下文内部的外键，只要有助于完整性且不扭曲归属关系，就允许使用
- 跨上下文强外键一律禁止

## 5. 评审问题

1. 这张表是写模型、projection、ledger，还是 join 结构
2. 索引是否由真实读路径驱动，而不是习惯性补齐
3. 是否有任何外键破坏了上下文归属
4. `code`、`name`、`description`、`long_description` 是否语义清晰，是否存在应改为 `xxx_code` 或 `xxx_description` 的字段
5. 是否存在 `text`、`effect`、`label`、`title`、`key` 等近似字段；如果保留，是否能用上下文语义解释
6. 排序字段是否统一使用 `sorting_order` / `sortingOrder`，查询是否有确定性兜底排序
