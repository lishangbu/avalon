# 游戏资料管理设计

## 目标

新增独立游戏资料域，提供中性、无品牌术语的基础资料 CRUD。后端 API 使用独立权限 `game-data:admin`，路径为 `/api/game-data/**`，不挂到系统管理接口下。

## 范围

- 管理对象：生物、种类、属性、数值项、技能、技能分类、特性、道具、道具分类。
- 数据库：`002` 只建表，`003` 只写入内置中性数据和权限菜单数据。
- 备注：Liquibase 中所有新旧表和字段都必须写入详细 remarks/comment。
- 命名：代码、表名、API 和菜单使用中性命名，不使用源品牌词。
- 数据：表内资料详情保留参考数据的实际条目名称和结构化数值；显示名称使用简体中文。
- 结构：不保存 raw JSON，不保存多语言文本，不保存世代字段；关系表拆分，保持三范式。

## 权限

- `game-data:admin`：访问 `/api/game-data/**`。
- 菜单根节点：`game-data`，默认绑定到独立角色 `game-data-admin`。
- 默认管理员用户同时绑定系统管理员和游戏资料管理员角色，便于本地开发登录后直接看到菜单。

## 表结构

核心表：

- `game_element`
- `game_stat`
- `game_skill_damage_class`
- `game_skill`
- `game_ability`
- `game_item_category`
- `game_item`
- `game_species`
- `game_creature`

关系表：

- `game_creature_element`
- `game_creature_stat`
- `game_creature_ability`

## API

每张资料表提供独立 Controller 和 Service，支持普通分页列表、详情、创建、更新、删除接口。删除由数据库外键兜底保护正在被引用的数据。
