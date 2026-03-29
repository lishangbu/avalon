# Database Changelog Conventions

本参考文件用于兜底说明 Liquibase 变更文件的常用规则。

如果目标仓库存在 `db/changelog/CONVENTIONS.md`，应优先读取仓库内文件，并把它视为最终规则来源。

## 1. 外键声明

- 被引用表已经在当前 `changeSet` 前面创建时，优先将外键内联到 `createTable.columns[].constraints`
- 被引用表在当前 `changeSet` 后面创建时，使用 `addForeignKeyConstraint`
- 自关联外键默认使用 `addForeignKeyConstraint`
- 需要 `deferrable`、`initiallyDeferred`、特殊 `onDelete`、特殊 `onUpdate` 时，必须使用 `addForeignKeyConstraint`

示例：

```yaml
- createTable:
    tableName: encounter_condition_value
    columns:
      - column:
          name: encounter_condition_id
          type: int8
          constraints:
            nullable: false
            foreignKeyName: fk_encounter_condition_value__encounter_condition_id
            referencedTableName: encounter_condition
            referencedColumnNames: id
```

```yaml
- addForeignKeyConstraint:
    baseTableName: menu
    baseColumnNames: parent_id
    referencedTableName: menu
    referencedColumnNames: id
    constraintName: fk_menu__parent_id
    onDelete: NO ACTION
    onUpdate: NO ACTION
```

## 2. 索引

- 不能因为已经有外键就默认不建索引，所有外键列都要单独评估索引
- 多对多关联表，除了复合主键外，还要为常见反向查询列补单列索引
- 树结构表必须为 `parent_id` 建索引
- 明确存在稳定排序查询时，补对应复合索引，例如 `sorting_order, id`

示例：

```yaml
- createIndex:
    tableName: menu
    indexName: idx_menu__parent_id
    columns:
      - column:
          name: parent_id
```

```yaml
- createIndex:
    tableName: menu
    indexName: idx_menu__sorting_order__id
    columns:
      - column:
          name: sorting_order
      - column:
          name: id
```

## 3. 唯一约束

- 业务自然键优先加唯一约束，例如 `code`、`key`、稳定的 `internal_name`
- 加唯一约束前必须先核对原始 seed 数据，确认不存在重复
- 对可能为空、历史上可能重复、或兼容性不明确的列，不强加唯一

示例：

```yaml
- addUniqueConstraint:
    tableName: move_damage_class
    columnNames: internal_name
    constraintName: uk_move_damage_class__internal_name
```

## 4. 初始化数据

- 原始 seed 数据必须能通过当前全部唯一约束与外键约束校验
- 若 seed 装载顺序会触发自关联校验问题，使用延迟外键
- 若当前并没有完整 seed 数据，不为了“理论完整”强行添加外键

示例：

```yaml
- addForeignKeyConstraint:
    baseTableName: pokemon_species
    baseColumnNames: evolves_from_species_id
    referencedTableName: pokemon_species
    referencedColumnNames: id
    constraintName: fk_pokemon_species__evolves_from_species_id
    deferrable: true
    initiallyDeferred: true
    onDelete: NO ACTION
    onUpdate: NO ACTION
```

## 5. 命名

- 索引命名：`idx_<table>__<column>` 或 `idx_<table>__<col1>__<col2>`
- 唯一约束命名：`uk_<table>__<column>`
- 外键命名：`fk_<table>__<column>`

## 6. 历史文件修改策略

- 初始化阶段允许直接整理原始 schema 文件
- 正式发版后，结构变更只新增 changelog，不回写历史版本文件
- 若变更会影响已有 seed，需要同时更新 seed，并重新验证建库流程

## 7. 当前仓库建议

- `db/changelog/versions/0.0.1/0.0.1.yaml` 可作为初始化 schema 的参考实现
- 新增表时优先对照同类表的索引、唯一约束和外键命名方式
- 提交数据库结构变更前，至少验证一次 Liquibase 起库与关键仓储测试
