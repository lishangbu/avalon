# 0027：Ent 关系与 PostgreSQL 约束例外边界

状态：已采用

## 背景

业务外键统一由 Ent Schema 的 edge 表达。少数 PostgreSQL 约束不能由 Ent 0.14 的单字段 edge 表达，继续由持久层扩展目录创建和校验。

## 规则

- 普通单字段外键必须在 `ent/schema` 中通过 `edge.To(...).Field(...)` 表达。
- 能由 Ent 表达的唯一索引、检查约束和删除动作不得只存在于扩展 SQL。
- 复合外键、共享主键外键以及依赖多列业务键的约束列入本 ADR 的例外清单，并保留 PostgreSQL 集成测试。
- 例外 SQL 只能通过 `internal/platform/persistence` 的受控边界执行，业务持久化适配器不得自行打开 SQL 连接。

## 当前例外

- `battle_turn_submission(battle_id, side)` → `battle_participant(battle_id, side)`；
- `battle_turn_submission(battle_id, state_version)` → `battle_turn_record(battle_id, state_version)`；
- `player_character_profession_skill(player_character_id, profession_id)` → `player_character_profession(player_character_id, profession_id)`；
- `player_character_quest_objective(player_character_id, quest_id)` → `player_character_quest(player_character_id, quest_id)`；
- `player_character_team_member_skill/team_member_stat(team_id, member_position)` → `player_character_team_member(team_id, position)`；
- 使用主键复用外键列名的 `active_player_character`、`battle_participant_reservation`、`battle_authoritative_summary`、`player_character_checkpoint` 关系。

新增例外必须先更新本文件和对应集成测试。
