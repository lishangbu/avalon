// Package item 定义 实时游戏资料 中道具资料的独立命令与查询边界。
package item

import (
	"context"
	"errors"
	"strings"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/lishangbu/avalon/internal/gamedata/administration"
	"github.com/lishangbu/avalon/internal/gamedata/itemrules"
	"github.com/lishangbu/avalon/internal/gamedata/stablecode"
)

var (
	// ErrInvalidItem 表示道具资料或管理写入上下文未通过边界校验。
	ErrInvalidItem = errors.New("道具资料无效")
	// ErrItemNotFound 表示实时资料中不存在指定道具资料。
	ErrItemNotFound = errors.New("道具资料不存在")
	// ErrItemVersionConflict 表示道具资料已被其他管理写入修改。
	ErrItemVersionConflict = errors.New("道具资料版本冲突")
	// ErrItemCodeConflict 表示实时资料中已有资料使用相同稳定编码。
	ErrItemCodeConflict = errors.New("道具资料编码已存在")
	// ErrItemCategoryNotFound 表示指定分类不属于当前实时资料。
	ErrItemCategoryNotFound = errors.New("道具分类不存在")
	// ErrItemReferenced 表示其他 实时资料 资料仍然引用目标道具资料。
	ErrItemReferenced = errors.New("道具资料仍被引用")
)

// UsageType 是道具在玩家玩法中的稳定用途分类。
type UsageType string

const (
	// UsageHeld 表示可由参战成员携带并在对战规则中生效的道具。
	UsageHeld UsageType = "held"
	// UsageEquipment 表示每次获取都建立独立 Equipment Instance 的角色装备资料。
	UsageEquipment UsageType = "equipment"
	// UsageBattleConsumable 表示在对战中主动消耗的道具。
	UsageBattleConsumable UsageType = "battle_consumable"
	// UsageCapture 表示捕捉玩法使用的道具。
	UsageCapture UsageType = "capture"
	// UsageEvolution 表示触发进化流程的道具。
	UsageEvolution UsageType = "evolution"
	// UsageTraining 表示培养或训练玩法使用的道具。
	UsageTraining UsageType = "training"
	// UsageKey 表示不可作为普通物资消费的关键道具。
	UsageKey UsageType = "key"
	// UsageMaterial 表示经济或合成玩法中的普通材料。
	UsageMaterial UsageType = "material"
	// UsageCatalog 表示已从权威目录建档、但尚未声明可执行用途且不能进入玩家玩法的道具。
	UsageCatalog UsageType = "catalog"
)

// Item 是管理端读取和写入的一条 实时资料 道具资料。
type Item struct {
	ID            snowflake.ID
	Code          string
	Name          string
	Description   *string
	Effect        *string
	ShortEffect   *string
	FlingEffectID *snowflake.ID
	UsageType     UsageType
	CategoryID    *snowflake.ID
	Cost          int32
	FlingPower    *int32
	Enabled       bool
	Version       int64
	// AssetID 是目录图标对应的 Ready Asset 稳定 Identifier；尚未绑定图片时为空。
	AssetID *snowflake.ID
}

// Sort 声明管理端道具列表允许使用的稳定排序。
type Sort string

const (
	// SortCodeAscending 按稳定编码升序排列，并使用 ID 打破平局。
	SortCodeAscending Sort = "code_asc"
	// SortCodeDescending 按稳定编码降序排列，并使用 ID 打破平局。
	SortCodeDescending Sort = "code_desc"
	// SortNameAscending 按简体中文名称升序排列，并使用 ID 打破平局。
	SortNameAscending Sort = "name_asc"
	// SortNameDescending 按简体中文名称降序排列，并使用 ID 打破平局。
	SortNameDescending Sort = "name_desc"
	// SortCostAscending 按基础价格升序排列，并使用 ID 打破平局。
	SortCostAscending Sort = "cost_asc"
	// SortCostDescending 按基础价格降序排列，并使用 ID 打破平局。
	SortCostDescending Sort = "cost_desc"
)

// ListQuery 是实时资料 道具列表的显式分页、筛选和排序条件。
type ListQuery struct {
	Page       int32
	PageSize   int32
	Q          string
	Code       string
	Name       string
	UsageType  *UsageType
	CategoryID *snowflake.ID
	Cost       *int32
	Enabled    *bool
	Sort       Sort
}

// Page 是管理端道具资料的有界分页结果。
type Page struct {
	Items    []Item
	Total    int64
	Page     int32
	PageSize int32
}

// CreateCommand 包含创建道具资料所需的业务字段和管理写入上下文。
type CreateCommand struct {
	administration.GameDataWriteContext
	Code          string
	Name          string
	Description   *string
	Effect        *string
	ShortEffect   *string
	FlingEffectID *snowflake.ID
	UsageType     UsageType
	CategoryID    *snowflake.ID
	Cost          int32
	FlingPower    *int32
	Enabled       bool
}

// CreateRecord 是存储层原子创建资料、审计和幂等响应所需的完整事实。
type CreateRecord struct {
	administration.GameDataWriteContext
	Item      Item
	CreatedAt time.Time
}

// UpdateCommand 使用完整资料表示和预期版本更新 实时资料中的一条道具资料。
type UpdateCommand struct {
	administration.GameDataWriteContext
	ItemID          snowflake.ID
	ExpectedVersion int64
	Code            string
	Name            string
	Description     *string
	Effect          *string
	ShortEffect     *string
	FlingEffectID   *snowflake.ID
	UsageType       UsageType
	CategoryID      *snowflake.ID
	Cost            int32
	FlingPower      *int32
	Enabled         bool
}

// UpdateRecord 是存储层原子更新资料、审计记录和幂等响应所需的完整事实。
type UpdateRecord struct {
	administration.GameDataWriteContext
	Item            Item
	ExpectedVersion int64
	UpdatedAt       time.Time
}

// DisableCommand 使用预期版本禁用 实时资料中未被引用的一条道具资料。
type DisableCommand struct {
	administration.GameDataWriteContext
	ItemID          snowflake.ID
	ExpectedVersion int64
}

// DisableRecord 是存储层原子禁用资料、审计记录和幂等响应所需的完整事实。
type DisableRecord struct {
	administration.GameDataWriteContext
	ItemID          snowflake.ID
	ExpectedVersion int64
	DisabledAt      time.Time
}

// Rules 是一个道具及其规范化战斗规则的管理聚合。
type Rules struct {
	// ItemID 是规则所属道具的稳定 Identifier。
	ItemID snowflake.ID
	// Version 复用道具主体版本，保证全部关系表按同一乐观版本替换。
	Version int64
	// Rules 保存由规范化关系表表达的完整规则值。
	Rules itemrules.Detail
}

// ReplaceRulesCommand 使用道具预期版本整体替换全部规范化规则关系。
type ReplaceRulesCommand struct {
	administration.GameDataWriteContext
	// ItemID 是规则所属道具的稳定 Identifier。
	ItemID snowflake.ID
	// ExpectedVersion 是提交表单时读取的道具主体版本。
	ExpectedVersion int64
	// Rules 是替换后的完整规则集合；零值字段表示删除对应规则事实。
	Rules itemrules.Detail
}

// ReplaceRulesRecord 是事务适配器持久化规则、审计和幂等结果所需事实。
type ReplaceRulesRecord struct {
	administration.GameDataWriteContext
	// ItemID 是规则所属道具的稳定 Identifier。
	ItemID snowflake.ID
	// ExpectedVersion 是事务必须匹配的道具主体版本。
	ExpectedVersion int64
	// Rules 是替换后的完整规则集合。
	Rules itemrules.Detail
	// UpdatedAt 是规则聚合的统一修改时间。
	UpdatedAt time.Time
}

// Writer 是一次道具资料管理事务内使用的最小写入边界。
type Writer interface {
	Create(context.Context, CreateRecord) (Item, error)
	Update(context.Context, UpdateRecord) (Item, error)
	Disable(context.Context, DisableRecord) error
}

// Store 提供由应用服务划定范围的道具资料事务执行边界。
type Store interface {
	GetItem(context.Context, snowflake.ID) (Item, error)
	ListItems(context.Context, ListQuery) (Page, error)
	GetManagedItemRules(context.Context, snowflake.ID) (Rules, error)
	ReplaceItemRules(context.Context, ReplaceRulesRecord) (Rules, error)
	WithinItem(context.Context, func(Writer) error) error
}

// Service 编排道具资料的独立校验、身份生成和持久化命令。
type Service struct {
	store Store
	newID snowflake.Source
	now   func() time.Time
}

// NewService 使用显式依赖创建道具资料应用服务。
func NewService(store Store, newID snowflake.Source, now func() time.Time) *Service {
	return &Service{store: store, newID: newID, now: now}
}

// Create 在当前实时资料中创建版本为 1 的道具资料。
func (s *Service) Create(ctx context.Context, command CreateCommand) (Item, error) {
	command.Code = strings.TrimSpace(command.Code)
	command.Name = strings.TrimSpace(command.Name)
	command.GameDataWriteContext = command.Normalize()
	if !command.Valid() || !stablecode.Valid(command.Code) || command.Name == "" || len([]rune(command.Name)) > 120 ||
		!validUsageType(command.UsageType) || command.Cost < 0 ||
		(command.CategoryID != nil && *command.CategoryID == snowflake.ID(0)) ||
		(command.FlingPower != nil && *command.FlingPower < 0) {
		return Item{}, ErrInvalidItem
	}
	id, idErr := s.newID.Next(ctx)
	if idErr != nil {
		return Item{}, idErr
	}
	value := Item{
		ID: id, Code: command.Code, Name: command.Name, UsageType: command.UsageType,
		Description: copyText(command.Description), Effect: copyText(command.Effect), ShortEffect: copyText(command.ShortEffect), FlingEffectID: copyIdentifier(command.FlingEffectID), CategoryID: copyIdentifier(command.CategoryID), Cost: command.Cost, FlingPower: copyInt32(command.FlingPower),
		Enabled: command.Enabled, Version: 1,
	}
	var created Item
	err := s.store.WithinItem(ctx, func(writer Writer) error {
		var createErr error
		created, createErr = writer.Create(ctx, CreateRecord{
			GameDataWriteContext: command.GameDataWriteContext, Item: value, CreatedAt: s.now().UTC(),
		})
		return createErr
	})
	if err != nil {
		return Item{}, err
	}
	return created, nil
}

// Update 使用乐观版本替换 实时资料中的完整道具字段并递增版本。
func (s *Service) Update(ctx context.Context, command UpdateCommand) (Item, error) {
	command.Code = strings.TrimSpace(command.Code)
	command.Name = strings.TrimSpace(command.Name)
	command.GameDataWriteContext = command.Normalize()
	if !command.Valid() || command.ItemID == snowflake.ID(0) || command.ExpectedVersion < 1 ||
		!stablecode.Valid(command.Code) || command.Name == "" || len([]rune(command.Name)) > 120 ||
		!validUsageType(command.UsageType) || command.Cost < 0 ||
		(command.CategoryID != nil && *command.CategoryID == snowflake.ID(0)) ||
		(command.FlingPower != nil && *command.FlingPower < 0) {
		return Item{}, ErrInvalidItem
	}
	value := Item{
		ID: command.ItemID, Code: command.Code, Name: command.Name, UsageType: command.UsageType,
		Description: copyText(command.Description), Effect: copyText(command.Effect), ShortEffect: copyText(command.ShortEffect), FlingEffectID: copyIdentifier(command.FlingEffectID), CategoryID: copyIdentifier(command.CategoryID), Cost: command.Cost, FlingPower: copyInt32(command.FlingPower),
		Enabled: command.Enabled, Version: command.ExpectedVersion + 1,
	}
	var updated Item
	err := s.store.WithinItem(ctx, func(writer Writer) error {
		var updateErr error
		updated, updateErr = writer.Update(ctx, UpdateRecord{
			GameDataWriteContext: command.GameDataWriteContext, Item: value,
			ExpectedVersion: command.ExpectedVersion, UpdatedAt: s.now().UTC(),
		})
		return updateErr
	})
	if err != nil {
		return Item{}, err
	}
	return updated, nil
}

// Get 读取当前实时资料中指定稳定身份的道具资料。
func (s *Service) Get(ctx context.Context, itemID snowflake.ID) (Item, error) {
	if itemID == snowflake.ID(0) {
		return Item{}, ErrInvalidItem
	}
	return s.store.GetItem(ctx, itemID)
}

// List 返回当前实时资料中经过显式筛选和稳定排序的道具资料页。
func (s *Service) List(ctx context.Context, query ListQuery) (Page, error) {
	if query.Page == 0 {
		query.Page = 1
	}
	if query.PageSize == 0 {
		query.PageSize = 20
	}
	if query.Sort == "" {
		query.Sort = SortCodeAscending
	}
	query.Q = strings.TrimSpace(query.Q)
	query.Code = strings.TrimSpace(query.Code)
	query.Name = strings.TrimSpace(query.Name)
	if query.Page < 1 || query.Page > 1_000_000 || query.PageSize < 1 || query.PageSize > 100 ||
		len([]rune(query.Q)) > 120 || len([]rune(query.Name)) > 120 || !validSort(query.Sort) ||
		(query.Code != "" && !stablecode.Valid(query.Code)) ||
		(query.UsageType != nil && !validUsageType(*query.UsageType)) ||
		(query.CategoryID != nil && *query.CategoryID == snowflake.ID(0)) ||
		(query.Cost != nil && *query.Cost < 0) {
		return Page{}, ErrInvalidItem
	}
	return s.store.ListItems(ctx, query)
}

// GetRules 读取一个道具的完整规范化规则聚合。
func (s *Service) GetRules(ctx context.Context, itemID snowflake.ID) (Rules, error) {
	if itemID == snowflake.ID(0) {
		return Rules{}, ErrInvalidItem
	}
	return s.store.GetManagedItemRules(ctx, itemID)
}

// ReplaceRules 在一个事务内整体替换规则关系，并递增作为聚合版本的道具版本。
func (s *Service) ReplaceRules(ctx context.Context, command ReplaceRulesCommand) (Rules, error) {
	command.GameDataWriteContext = command.Normalize()
	command.Rules.ItemID = command.ItemID
	if !command.Valid() || command.ItemID == snowflake.ID(0) || command.ExpectedVersion < 1 {
		return Rules{}, ErrInvalidItem
	}
	return s.store.ReplaceItemRules(ctx, ReplaceRulesRecord{
		GameDataWriteContext: command.GameDataWriteContext,
		ItemID:               command.ItemID, ExpectedVersion: command.ExpectedVersion,
		Rules: command.Rules, UpdatedAt: s.now().UTC(),
	})
}

func validSort(sort Sort) bool {
	switch sort {
	case SortCodeAscending, SortCodeDescending, SortNameAscending, SortNameDescending,
		SortCostAscending, SortCostDescending:
		return true
	default:
		return false
	}
}

// Delete 使用乐观版本禁用当前实时资料中未被引用的道具资料。
func (s *Service) Disable(ctx context.Context, command DisableCommand) error {
	command.GameDataWriteContext = command.Normalize()
	if !command.Valid() || command.ItemID == snowflake.ID(0) || command.ExpectedVersion < 1 {
		return ErrInvalidItem
	}
	return s.store.WithinItem(ctx, func(writer Writer) error {
		return writer.Disable(ctx, DisableRecord{
			GameDataWriteContext: command.GameDataWriteContext, ItemID: command.ItemID,
			ExpectedVersion: command.ExpectedVersion, DisabledAt: s.now().UTC(),
		})
	})
}

func validUsageType(value UsageType) bool {
	switch value {
	case UsageHeld, UsageEquipment, UsageBattleConsumable, UsageCapture, UsageEvolution, UsageTraining, UsageKey, UsageMaterial, UsageCatalog:
		return true
	default:
		return false
	}
}

func copyIdentifier(value *snowflake.ID) *snowflake.ID {
	if value == nil {
		return nil
	}
	copy := *value
	return &copy
}

func copyInt32(value *int32) *int32 {
	if value == nil {
		return nil
	}
	copy := *value
	return &copy
}

func copyText(value *string) *string {
	if value == nil {
		return nil
	}
	copy := *value
	return &copy
}
