// Package battleformat 定义游戏资料中 BattleFormat 与战斗规则组件的管理边界。
package battleformat

import (
	"context"
	"errors"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/lishangbu/avalon/internal/gamedata/administration"
	"github.com/lishangbu/avalon/internal/gamedata/effect"
)

var (
	// ErrInvalidClause 表示 Battle Clause 定义无效。
	ErrInvalidClause = errors.New("战斗条款无效")
	// ErrClauseNotFound 表示实时资料中不存在指定 Battle Clause。
	ErrClauseNotFound = errors.New("战斗条款不存在")
	// ErrClauseConflict 表示 Battle Clause 版本、Stable Code 或引用状态冲突。
	ErrClauseConflict = errors.New("战斗条款冲突")
	// ErrInvalidRestriction 表示 Battle Restriction 定义无效。
	ErrInvalidRestriction = errors.New("战斗限制无效")
	// ErrRestrictionNotFound 表示实时资料中不存在指定 Battle Restriction。
	ErrRestrictionNotFound = errors.New("战斗限制不存在")
	// ErrRestrictionConflict 表示 Battle Restriction 版本、Stable Code 或引用状态冲突。
	ErrRestrictionConflict = errors.New("战斗限制冲突")
	// ErrInvalidMechanic 表示 Special Mechanic 定义无效。
	ErrInvalidMechanic = errors.New("特殊机制无效")
	// ErrMechanicNotFound 表示实时资料中不存在指定 Special Mechanic。
	ErrMechanicNotFound = errors.New("特殊机制不存在")
	// ErrMechanicConflict 表示 Special Mechanic 版本、Stable Code 或引用状态冲突。
	ErrMechanicConflict = errors.New("特殊机制冲突")
	// ErrInvalidFormat 表示 BattleFormat 字段或规则组合无效。
	ErrInvalidFormat = errors.New("战斗赛制无效")
	// ErrFormatNotFound 表示实时资料中不存在指定 BattleFormat。
	ErrFormatNotFound = errors.New("战斗赛制不存在")
	// ErrFormatConflict 表示 BattleFormat 版本或 Stable Code 冲突。
	ErrFormatConflict = errors.New("战斗赛制冲突")
)

// ComponentType 区分 BattleFormat 可组合的三类代码注册规则。
type ComponentType string

const (
	// ComponentTypeClause 表示对整场成立的战斗条款。
	ComponentTypeClause ComponentType = "clause"
	// ComponentTypeRestriction 表示队伍或资料选择限制。
	ComponentTypeRestriction ComponentType = "restriction"
	// ComponentTypeMechanic 表示赛制启用的特殊运行时机制。
	ComponentTypeMechanic ComponentType = "mechanic"
)

// Component 是 BattleFormat 跨资源校验和运行时编译阶段使用的统一只读投影。
//
// 管理端 Clause、Restriction 与 Mechanic 各自拥有独立领域形状和持久化边界；这里只在不可变
// BattleFormat 合并三者，以便按稳定类型完成跨资源引用校验和运行时规则编译。
type Component struct {
	// ID 是规则组件不可修改的稳定 Identifier。
	ID snowflake.ID `json:"id"`
	// Type 区分 Clause、Restriction 和 Mechanic 三类独立资料。
	Type ComponentType `json:"type"`
	// Code 是当前实时资料中全局唯一的英文机器标识。
	Code string `json:"code"`
	// Name 是管理端和玩家端使用的简体中文名称。
	Name string `json:"name"`
	// Description 是组件用途与约束的简体中文说明。
	Description string `json:"description"`
	// Definition 保存由代码注册表解释的效果标识、版本和参数。
	Definition effect.Definition `json:"definition"`
	// Enabled 表示组件是否允许被启用的 BattleFormat 引用。
	Enabled bool `json:"enabled"`
	// Version 是管理写入使用的乐观锁版本。
	Version int64 `json:"version"`
}

// RuntimeComponent 是已经完成参数规范化、可供玩家流程消费的战斗规则组件。
//
// 它保留玩家展示和规则解析需要的稳定资料字段，但 Definition 必须来自代码注册表的编译结果，
// 不能直接复用实时资料中尚未规范化的原始参数。
type RuntimeComponent struct {
	// ID 是规则组件不可修改的稳定 Identifier。
	ID snowflake.ID `json:"id"`
	// Type 区分 Clause、Restriction 和 Mechanic 三类独立资料。
	Type ComponentType `json:"type"`
	// Code 是当前实时资料中全局唯一的英文机器标识。
	Code string `json:"code"`
	// Name 是玩家流程使用的简体中文名称。
	Name string `json:"name"`
	// Description 是组件用途与约束的简体中文说明。
	Description string `json:"description"`
	// Definition 是已经过注册表校验和规范化的运行时效果定义。
	Definition effect.CompiledDefinition `json:"definition"`
	// Enabled 表示组件是否允许进入当前玩家资料目录。
	Enabled bool `json:"enabled"`
	// Version 是作为不透明十进制字符串传输的资料版本。
	Version string `json:"version"`
}

// RuntimeFormat 是供玩家流程读取的当前赛制投影。
//
// 管理领域使用 int64 做乐观锁，运行时契约把版本作为不透明十进制字符串传输，避免消费者
// 对数据库版本号执行数值运算。
type RuntimeFormat struct {
	// ID 是 BattleFormat 不可修改的稳定 Identifier。
	ID snowflake.ID `json:"id"`
	// Code 是当前实时资料中全局唯一的英文机器标识。
	Code string `json:"code"`
	// Name 是管理端和玩家端使用的简体中文名称。
	Name string `json:"name"`
	// Description 是赛制适用场景和规则的简体中文说明。
	Description string `json:"description"`
	// Mode 定义单打或双打等对战模式。
	Mode Mode `json:"mode"`
	// RosterCount 是每侧登记的 Team 成员数量。
	RosterCount int32 `json:"rosterCount"`
	// SelectCount 是 Team Preview 必须选择的参战成员数量。
	SelectCount int32 `json:"selectCount"`
	// ActiveParticipantsPerSide 是每侧同时在场的成员数量。
	ActiveParticipantsPerSide int32 `json:"activeParticipantsPerSide"`
	// LevelRule 定义保留或规范化参战成员等级的规则。
	LevelRule LevelRule `json:"levelRule"`
	// Deadlines 冻结预览、回合和整场对战的期限。
	Deadlines Deadlines `json:"deadlines"`
	// Availability 定义赛制允许进入的 Challenge、Training Battle 和管理预览入口。
	Availability Availability `json:"availability"`
	// ClauseIDs 保存赛制引用的启用 Battle Clause 稳定标识。
	ClauseIDs []snowflake.ID `json:"clauseIds"`
	// RestrictionIDs 保存赛制引用的启用 Battle Restriction 稳定标识。
	RestrictionIDs []snowflake.ID `json:"restrictionIds"`
	// MechanicIDs 保存赛制引用的启用 Special Mechanic 稳定标识。
	MechanicIDs []snowflake.ID `json:"mechanicIds"`
	// Default 表示该赛制是否是普通创建流程的默认选择。
	Default bool `json:"default"`
	// Enabled 表示该赛制是否允许进入玩家流程。
	Enabled bool `json:"enabled"`
	// Version 是作为不透明十进制字符串传输的资料版本。
	Version string `json:"version"`
}

// Mode 表示一方同时上场规模所使用的稳定战斗模式名称。
type Mode string

const (
	// ModeSingle 表示每方同时上场一个参战成员。
	ModeSingle Mode = "single"
	// ModeDouble 表示每方同时上场两个参战成员。
	ModeDouble Mode = "double"
)

// LevelRuleMode 表示赛制保留原等级或规范化到固定等级。
type LevelRuleMode string

const (
	// LevelRulePreserve 保留 Team Snapshot 中的成员等级。
	LevelRulePreserve LevelRuleMode = "preserve"
	// LevelRuleNormalize 把成员等级规范化到 Level 指定值。
	LevelRuleNormalize LevelRuleMode = "normalize"
)

// LevelRule 是 BattleFormat 冻结的等级解释规则。
type LevelRule struct {
	Mode  LevelRuleMode `json:"mode"`
	Level *int32        `json:"level"`
}

// Deadlines 是 Battle 创建后冻结的预览、回合和整场期限。
type Deadlines struct {
	PreviewSeconds int32 `json:"previewSeconds"`
	TurnSeconds    int32 `json:"turnSeconds"`
	BattleSeconds  int32 `json:"battleSeconds"`
}

// Availability 声明 BattleFormat 可以进入的产品流程。
type Availability struct {
	Challenge    bool `json:"challenge"`
	Training     bool `json:"training"`
	Encounter    bool `json:"encounter"`
	AdminPreview bool `json:"adminPreview"`
}

// Format 是实时资料中的一条完整 BattleFormat 修订。
type Format struct {
	ID                        snowflake.ID   `json:"id"`
	Code                      string         `json:"code"`
	Name                      string         `json:"name"`
	Description               string         `json:"description"`
	Mode                      Mode           `json:"mode"`
	RosterCount               int32          `json:"rosterCount"`
	SelectCount               int32          `json:"selectCount"`
	ActiveParticipantsPerSide int32          `json:"activeParticipantsPerSide"`
	LevelRule                 LevelRule      `json:"levelRule"`
	Deadlines                 Deadlines      `json:"deadlines"`
	Availability              Availability   `json:"availability"`
	ClauseIDs                 []snowflake.ID `json:"clauseIds"`
	RestrictionIDs            []snowflake.ID `json:"restrictionIds"`
	MechanicIDs               []snowflake.ID `json:"mechanicIds"`
	Default                   bool           `json:"default"`
	Enabled                   bool           `json:"enabled"`
	Version                   int64          `json:"version"`
}

// FormatListQuery 是 BattleFormat 的有界分页条件。
type FormatListQuery struct {
	Page         int32
	PageSize     int32
	Q            string
	Mode         Mode
	Enabled      *bool
	Challenge    *bool
	Training     *bool
	Encounter    *bool
	AdminPreview *bool
}

// FormatPage 是 BattleFormat 管理列表的分页结果。
type FormatPage struct {
	Items    []Format
	Total    int64
	Page     int32
	PageSize int32
}

// CreateFormatCommand 包含创建 BattleFormat 所需的完整字段和管理写入上下文。
type CreateFormatCommand struct {
	administration.GameDataWriteContext
	Code                      string
	Name                      string
	Description               string
	Mode                      Mode
	RosterCount               int32
	SelectCount               int32
	ActiveParticipantsPerSide int32
	LevelRule                 LevelRule
	Deadlines                 Deadlines
	Availability              Availability
	ClauseIDs                 []snowflake.ID
	RestrictionIDs            []snowflake.ID
	MechanicIDs               []snowflake.ID
	Default                   bool
	Enabled                   bool
}

// UpdateFormatCommand 使用乐观版本完整替换一条 BattleFormat。
type UpdateFormatCommand struct {
	CreateFormatCommand
	FormatID        snowflake.ID
	ExpectedVersion int64
}

// DisableFormatCommand 禁用实时资料中的一条 BattleFormat。
type DisableFormatCommand struct {
	administration.GameDataWriteContext
	FormatID        snowflake.ID
	ExpectedVersion int64
}

// CreateFormatRecord 是存储层原子创建 BattleFormat 所需事实。
type CreateFormatRecord struct {
	administration.GameDataWriteContext
	Format    Format
	CreatedAt time.Time
}

// UpdateFormatRecord 是存储层原子更新 BattleFormat 所需事实。
type UpdateFormatRecord struct {
	administration.GameDataWriteContext
	Format          Format
	ExpectedVersion int64
	UpdatedAt       time.Time
}

// DisableFormatRecord 是存储层原子禁用 BattleFormat 所需事实。
type DisableFormatRecord struct {
	administration.GameDataWriteContext
	FormatID        snowflake.ID
	ExpectedVersion int64
	DisabledAt      time.Time
}

// Writer 是一次战斗规则管理事务内使用的最小写入边界。
type Writer interface {
	CreateClause(context.Context, CreateClauseRecord) (Clause, error)
	UpdateClause(context.Context, UpdateClauseRecord) (Clause, error)
	DisableClause(context.Context, DisableClauseRecord) error
	CreateRestriction(context.Context, CreateRestrictionRecord) (Restriction, error)
	UpdateRestriction(context.Context, UpdateRestrictionRecord) (Restriction, error)
	DisableRestriction(context.Context, DisableRestrictionRecord) error
	CreateMechanic(context.Context, CreateMechanicRecord) (Mechanic, error)
	UpdateMechanic(context.Context, UpdateMechanicRecord) (Mechanic, error)
	DisableMechanic(context.Context, DisableMechanicRecord) error
	CreateFormat(context.Context, CreateFormatRecord) (Format, error)
	UpdateFormat(context.Context, UpdateFormatRecord) (Format, error)
	DisableFormat(context.Context, DisableFormatRecord) error
}

// BattleRuleReader 返回战斗规则组件与 BattleFormat 领域对象。
type BattleRuleReader interface {
	GetClause(context.Context, snowflake.ID) (Clause, error)
	GetRestriction(context.Context, snowflake.ID) (Restriction, error)
	GetMechanic(context.Context, snowflake.ID) (Mechanic, error)
	GetFormat(context.Context, snowflake.ID) (Format, error)
}

// BattleRuleQuery 返回战斗规则组件与 BattleFormat 分页管理投影。
type BattleRuleQuery interface {
	ListClauses(context.Context, ClauseListQuery) (ClausePage, error)
	ListRestrictions(context.Context, RestrictionListQuery) (RestrictionPage, error)
	ListMechanics(context.Context, MechanicListQuery) (MechanicPage, error)
	ListFormats(context.Context, FormatListQuery) (FormatPage, error)
}

// BattleRuleRepository 提供由应用服务划定范围的战斗规则事务写入边界。
type BattleRuleRepository interface {
	WithinBattleRules(context.Context, func(Writer) error) error
}
