// Package team 定义 PlayerCharacter 在 Battle 外维护的版本化命名阵容。
package team

import (
	"context"
	"errors"
	"reflect"
	"strings"
	"time"
	"unicode/utf8"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/lishangbu/avalon/internal/platform/idempotency"
	"golang.org/x/text/unicode/norm"
)

// MaximumPerPlayerCharacter 是每个未归档 PlayerCharacter 可以保存的 Team 上限。
const MaximumPerPlayerCharacter int64 = 20

const (
	maximumMembers         = 6
	maximumSkillsPerMember = 4
	maximumStatsPerMember  = 32
)

var (
	// ErrInvalidTeam 表示队名、阵容数量、引用形状或培养数值无效。
	ErrInvalidTeam = errors.New("Team 无效")
	// ErrTeamLimitExceeded 表示角色已经保存二十支 Team。
	ErrTeamLimitExceeded = errors.New("PlayerCharacter Team 数量已达上限")
	// ErrTeamConflict 表示 Team 名称、版本、状态或所有权冲突。
	ErrTeamConflict = errors.New("Team 状态或版本冲突")
	// ErrTeamNotFound 表示调用角色不拥有指定 Team。
	ErrTeamNotFound = errors.New("Team 不存在")
	// ErrPlayerCharacterUnavailable 表示角色不存在、已经归档或不能再维护 Team。
	ErrPlayerCharacterUnavailable = errors.New("PlayerCharacter 不可用于 Team")
)

// Team 是 PlayerCharacter 拥有的版本化命名阵容，参战前按当前实时资料重新校验。
type Team struct {
	// ID 是 Team 的稳定 Identifier。
	ID snowflake.ID
	// PlayerCharacterID 是拥有该 Team 的 PlayerCharacter 稳定 Identifier。
	PlayerCharacterID snowflake.ID
	// Name 是规范化后展示给拥有者的 Team 名称。
	Name string
	// NameKey 是用于同一 PlayerCharacter 内唯一性比较的规范化名称键。
	NameKey string
	// Active 表示该 Team 是否为 PlayerCharacter 当前默认 Team。
	Active bool
	// Version 是完整替换 Team 时递增的乐观并发控制版本。
	Version int64
	// Members 是按固定 Position 顺序保存的完整阵容。
	Members []Member
	// CreatedAt 是 Team 首次创建的 UTC 时间。
	CreatedAt time.Time
	// UpdatedAt 是 Team 最近一次完整替换的 UTC 时间。
	UpdatedAt time.Time
}

// Member 是 Team 中固定位置的一名参战成员。
type Member struct {
	// Position 是成员在 Team 中从一开始的固定位置。
	Position int32 `json:"position"`
	// CreatureID 是成员选择的生物实时资料稳定 Identifier。
	CreatureID snowflake.ID `json:"creatureId"`
	// FormID 是可选形态实时资料稳定 Identifier。
	FormID *snowflake.ID `json:"formId,omitempty"`
	// GenderID 是可选性别实时资料稳定 Identifier。
	GenderID *snowflake.ID `json:"genderId,omitempty"`
	// SkinID 是可选皮肤实时资料稳定 Identifier。
	SkinID *snowflake.ID `json:"skinId,omitempty"`
	// AbilityID 是成员选择的特性实时资料稳定 Identifier。
	AbilityID snowflake.ID `json:"abilityId"`
	// ItemID 是可选持有道具实时资料稳定 Identifier。
	ItemID *snowflake.ID `json:"itemId,omitempty"`
	// TeraElementID 是成员选择的太晶属性实时资料稳定 Identifier。
	TeraElementID snowflake.ID `json:"teraElementId"`
	// NatureID 是成员选择的 Nature 实时资料稳定 Identifier。
	NatureID snowflake.ID `json:"natureId"`
	// Level 是成员在保留等级赛制下使用的固定等级，范围为 1 至 100。
	Level int32 `json:"level"`
	// Skills 是成员按固定 Position 保存的技能栏。
	Skills []MemberSkill `json:"skills"`
	// Stats 是成员按稳定数值 Identifier 保存的个体值与努力值。
	Stats []MemberStat `json:"stats"`
}

// MemberSkill 是成员技能栏中的固定位置引用。
type MemberSkill struct {
	// Position 是技能在成员技能栏中从一开始的固定位置。
	Position int32 `json:"position"`
	// SkillID 是成员选择的技能实时资料稳定 Identifier。
	SkillID snowflake.ID `json:"skillId"`
}

// MemberStat 保存一个稳定数值项对应的个体值和努力值。
type MemberStat struct {
	// StatID 是培养数值对应的实时资料稳定 Identifier。
	StatID snowflake.ID `json:"statId"`
	// IndividualValue 是零至三十一范围内的个体值。
	IndividualValue int32 `json:"individualValue"`
	// EffortValue 是零至二百五十二范围内的努力值。
	EffortValue int32 `json:"effortValue"`
}

// MemberInput 是完整替换命令中的成员载荷；位置由数组顺序决定。
type MemberInput struct {
	// CreatureID 是待创建成员选择的生物实时资料稳定 Identifier。
	CreatureID snowflake.ID
	// FormID 是待创建成员可选的形态实时资料稳定 Identifier。
	FormID *snowflake.ID
	// GenderID 是待创建成员可选的性别实时资料稳定 Identifier。
	GenderID *snowflake.ID
	// SkinID 是待创建成员可选的皮肤实时资料稳定 Identifier。
	SkinID *snowflake.ID
	// AbilityID 是待创建成员选择的特性实时资料稳定 Identifier。
	AbilityID snowflake.ID
	// ItemID 是待创建成员可选的持有道具实时资料稳定 Identifier。
	ItemID *snowflake.ID
	// TeraElementID 是待创建成员选择的太晶属性实时资料稳定 Identifier。
	TeraElementID snowflake.ID
	// NatureID 是待创建成员选择的 Nature 实时资料稳定 Identifier。
	NatureID snowflake.ID
	// Level 是创建或完整替换 Team 时设置的成员等级。
	Level int32
	// SkillIDs 按输入顺序定义成员技能栏的固定位置。
	SkillIDs []snowflake.ID
	// Stats 是待创建成员的个体值与努力值输入集合。
	Stats []MemberStatInput
}

// MemberStatInput 是完整替换命令中的培养数值载荷。
type MemberStatInput struct {
	// StatID 是培养数值对应的实时资料稳定 Identifier。
	StatID snowflake.ID
	// IndividualValue 是零至三十一范围内的个体值。
	IndividualValue int32
	// EffortValue 是零至二百五十二范围内的努力值。
	EffortValue int32
}

// CreateCommand 包含创建 Team 所需的角色、完整阵容和幂等上下文。
type CreateCommand struct {
	// AccountID 是发起创建命令的已认证账号稳定 Identifier。
	AccountID snowflake.ID
	// PlayerCharacterID 是拥有新 Team 的活动 PlayerCharacter 稳定 Identifier。
	PlayerCharacterID snowflake.ID
	// Name 是客户端提供的 Team 展示名称。
	Name string
	// Members 是客户端提供的完整成员输入，数组顺序决定成员位置。
	Members []MemberInput
	// IdempotencyKey 是本次创建的稳定幂等键。
	IdempotencyKey string
	// RequestID 是贯穿日志和审计的请求关联标识。
	RequestID string
}

// CreateRecord 是存储层原子检查角色、上限、名称并创建完整 Team 所需的事实。
type CreateRecord struct {
	// Team 是已经规范化且等待原子写入的完整 Team。
	Team Team
	// ActorAccountID 是执行此写操作的账号稳定 Identifier。
	ActorAccountID snowflake.ID
	// IdempotencyKey 是与请求载荷绑定的稳定幂等键。
	IdempotencyKey string
	// RequestID 是写入审计记录的请求关联标识。
	RequestID string
	// currentMemberValidator 只能由 Team 应用服务在可用资料事务内注入；存储 adapter 据此拒绝直接构造
	// Record 绕过 Current Game Data 校验，并在幂等认领确认首次执行后调用它。
	currentMemberValidator CurrentMemberValidator
}

// HasCurrentGameDataValidator 表示该创建事实携带只能由 Team 应用服务注入的可信当前资料校验器。
//
// 返回值只供持久化 adapter 在访问数据库前拒绝绕过应用服务的写入；它不表示校验已经执行。adapter 必须
// 先认领幂等响应，再通过 ValidateCurrentMembers 执行首次命令的真实资料校验。
func (record CreateRecord) HasCurrentGameDataValidator() bool {
	return !dependencyIsNil(record.currentMemberValidator)
}

// ValidateCurrentMembers 按锁定的 Current Game Data 校验待创建 Team 的完整成员集合。
//
// 此方法只能使用由应用服务注入的私有校验器；外部包即使可以构造 CreateRecord，也无法伪造该依赖。持久化
// adapter 必须只在幂等认领确认本次为首次执行后调用，以保证已成功的同键请求确定性重放。
func (record CreateRecord) ValidateCurrentMembers(ctx context.Context) error {
	if !record.HasCurrentGameDataValidator() {
		return ErrTeamCatalogUnavailable
	}
	return record.currentMemberValidator.ValidateCurrent(ctx, record.Team.Members)
}

// UpdateCommand 使用乐观版本完整替换 Team 名称与阵容。
type UpdateCommand struct {
	// AccountID 是发起完整替换的已认证账号稳定 Identifier。
	AccountID snowflake.ID
	// PlayerCharacterID 是拥有待更新 Team 的稳定 Identifier。
	PlayerCharacterID snowflake.ID
	// TeamID 是待完整替换的 Team 稳定 Identifier。
	TeamID snowflake.ID
	// ExpectedVersion 是客户端读取后携带的 Team 乐观版本。
	ExpectedVersion int64
	// Name 是客户端提供的新 Team 展示名称。
	Name string
	// Members 是客户端提供的完整新成员输入。
	Members []MemberInput
	// IdempotencyKey 是本次替换的稳定幂等键。
	IdempotencyKey string
	// RequestID 是贯穿日志和审计的请求关联标识。
	RequestID string
}

// UpdateRecord 是存储层原子替换 Team 与保存幂等结果所需的完整事实。
type UpdateRecord struct {
	// Team 是已经规范化并携带新内容的完整 Team。
	Team Team
	// ExpectedVersion 是用于比较并推进 Team 版本的乐观版本。
	ExpectedVersion int64
	// ActorAccountID 是执行此写操作的账号稳定 Identifier。
	ActorAccountID snowflake.ID
	// IdempotencyKey 是与请求载荷绑定的稳定幂等键。
	IdempotencyKey string
	// RequestID 是写入审计记录的请求关联标识。
	RequestID string
	// currentMemberValidator 只能由 Team 应用服务在可用资料事务内注入；存储 adapter 据此拒绝直接构造
	// Record 绕过 Current Game Data 校验，并在幂等认领确认首次执行后调用它。
	currentMemberValidator CurrentMemberValidator
}

// HasCurrentGameDataValidator 表示该替换事实携带只能由 Team 应用服务注入的可信当前资料校验器。
//
// 返回值只供持久化 adapter 在访问数据库前拒绝绕过应用服务的写入；它不表示校验已经执行。adapter 必须
// 先认领幂等响应，再通过 ValidateCurrentMembers 执行首次命令的真实资料校验。
func (record UpdateRecord) HasCurrentGameDataValidator() bool {
	return !dependencyIsNil(record.currentMemberValidator)
}

// ValidateCurrentMembers 按锁定的 Current Game Data 校验待完整替换 Team 的成员集合。
//
// 此方法只能使用由应用服务注入的私有校验器；外部包即使可以构造 UpdateRecord，也无法伪造该依赖。持久化
// adapter 必须只在幂等认领确认本次为首次执行后调用，以保证 Team 后续删除或资料禁用不会破坏成功响应重放。
func (record UpdateRecord) ValidateCurrentMembers(ctx context.Context) error {
	if !record.HasCurrentGameDataValidator() {
		return ErrTeamCatalogUnavailable
	}
	return record.currentMemberValidator.ValidateCurrent(ctx, record.Team.Members)
}

// DeleteCommand 使用乐观版本永久删除可变 Team；已冻结的对局快照不依赖该记录。
type DeleteCommand struct {
	// AccountID 是发起删除命令的已认证账号稳定 Identifier。
	AccountID snowflake.ID
	// PlayerCharacterID 是拥有待删除 Team 的稳定 Identifier。
	PlayerCharacterID snowflake.ID
	// TeamID 是待删除可变 Team 的稳定 Identifier。
	TeamID snowflake.ID
	// ExpectedVersion 是客户端读取后携带的 Team 乐观版本。
	ExpectedVersion int64
	// IdempotencyKey 是本次删除的稳定幂等键。
	IdempotencyKey string
	// RequestID 是贯穿日志和审计的请求关联标识。
	RequestID string
}

// DeleteRecord 是存储层删除 Team、修复活动绑定和保存幂等结果所需的事实。
type DeleteRecord struct {
	// AccountID 是执行删除操作的账号稳定 Identifier。
	AccountID snowflake.ID
	// PlayerCharacterID 是拥有待删除 Team 的稳定 Identifier。
	PlayerCharacterID snowflake.ID
	// TeamID 是待删除可变 Team 的稳定 Identifier。
	TeamID snowflake.ID
	// ExpectedVersion 是用于比较并推进 Team 版本的乐观版本。
	ExpectedVersion int64
	// IdempotencyKey 是与请求载荷绑定的稳定幂等键。
	IdempotencyKey string
	// RequestID 是写入审计记录的请求关联标识。
	RequestID string
	// DeletedAt 是服务端确认删除的 UTC 时间。
	DeletedAt time.Time
}

// DeleteResult 返回被删除的稳定 Team ID，以及删除活动 Team 后可能产生的新绑定。
type DeleteResult struct {
	// DeletedTeamID 是成功删除的可变 Team 稳定 Identifier。
	DeletedTeamID snowflake.ID
	// Active 是删除默认 Team 后自动选出的新默认绑定；没有剩余 Team 时为空。
	Active *ActiveBinding
}

// ActiveBinding 是 PlayerCharacter 默认 Team 的持久化乐观版本绑定。
type ActiveBinding struct {
	// PlayerCharacterID 是拥有默认 Team 绑定的稳定 Identifier。
	PlayerCharacterID snowflake.ID
	// TeamID 是当前被选为默认 Team 的稳定 Identifier。
	TeamID snowflake.ID
	// Version 是默认绑定切换时递增的乐观并发控制版本。
	Version int64
	// UpdatedAt 是默认 Team 最近一次切换的 UTC 时间。
	UpdatedAt time.Time
}

// SwitchActiveCommand 使用绑定版本切换默认 Team，不影响 Challenge 和 Training 的显式选择。
type SwitchActiveCommand struct {
	// AccountID 是发起默认 Team 切换的已认证账号稳定 Identifier。
	AccountID snowflake.ID
	// PlayerCharacterID 是拥有默认绑定的稳定 Identifier。
	PlayerCharacterID snowflake.ID
	// TeamID 是将被选为默认 Team 的稳定 Identifier。
	TeamID snowflake.ID
	// ExpectedVersion 是客户端读取后携带的默认绑定乐观版本。
	ExpectedVersion int64
	// IdempotencyKey 是本次切换的稳定幂等键。
	IdempotencyKey string
	// RequestID 是贯穿日志和审计的请求关联标识。
	RequestID string
}

// SwitchActiveRecord 是存储层原子切换活动 Team、审计和幂等所需的事实。
type SwitchActiveRecord struct {
	// AccountID 是执行默认 Team 切换的账号稳定 Identifier。
	AccountID snowflake.ID
	// PlayerCharacterID 是拥有默认绑定的稳定 Identifier。
	PlayerCharacterID snowflake.ID
	// TeamID 是将被选为默认 Team 的稳定 Identifier。
	TeamID snowflake.ID
	// ExpectedVersion 是用于比较并推进默认绑定版本的乐观版本。
	ExpectedVersion int64
	// IdempotencyKey 是与请求载荷绑定的稳定幂等键。
	IdempotencyKey string
	// RequestID 是写入审计记录的请求关联标识。
	RequestID string
	// UpdatedAt 是服务端确认切换的 UTC 时间。
	UpdatedAt time.Time
}

// Repository 是 Team 完整替换、删除和活动绑定的关系型写入端口。
type Repository interface {
	Create(context.Context, CreateRecord) (Team, error)
	Update(context.Context, UpdateRecord) (Team, error)
	Delete(context.Context, DeleteRecord) (DeleteResult, error)
	SwitchActive(context.Context, SwitchActiveRecord) (ActiveBinding, error)
}

// TransactionRunner 由应用服务调用，用同一 Context 向多个 Repository 操作传播事务。
type TransactionRunner interface {
	WithinTransaction(context.Context, func(context.Context) error) error
}

// CurrentGameDataGate 在同一 PostgreSQL 事务中校验 Current Game Data，并向 Team 工作传播事务 Context。
//
// 调用方只能在回调内执行当前资料校验和相关 Team 写入；实现必须让两者共享同一事务 Context。
type CurrentGameDataGate interface {
	WithinAvailable(context.Context, func(context.Context) error) error
}

// Service 校验并编排版本化 Team 命令。
type Service struct {
	// repository 是 Team 及其幂等、审计事实的唯一关系型持久化端口。
	repository Repository
	// validator 按锁定的 Current Game Data 校验完整 Team 成员引用。
	validator CurrentMemberValidator
	// currentGameData 串行化实时资料校验、Team 写入与全局维护状态转换。
	currentGameData CurrentGameDataGate
	// transactions 用于不依赖实时资料的删除与默认 Team 切换原子写入。
	transactions TransactionRunner
	// newID 为新 Team 生成稳定 Identifier；调用方必须提供不会返回零值的实现。
	newID snowflake.Source
	// now 提供可替换的 UTC 时间源，以保证领域测试可精确断言写入事实。
	now func() time.Time
}

// NewService 使用显式存储、当前实时资料校验器、Identifier、时钟和事务依赖创建 Team 服务。
//
// validator 是保存 Team 的强制依赖，不能为 nil（包括包装了 nil 指针的接口值）。构造期立即拒绝
// 无效装配，使 Create 和 Update 不存在可绕过 Current Game Data 校验的路径。
func NewService(
	repository Repository, validator CurrentMemberValidator,
	currentGameData CurrentGameDataGate,
	newID snowflake.Source,
	now func() time.Time,
	transactions TransactionRunner,
) *Service {
	if dependencyIsNil(validator) {
		panic("team: CurrentMemberValidator 不能为空")
	}
	if dependencyIsNil(currentGameData) {
		panic("team: CurrentGameDataGate 不能为空")
	}
	return &Service{
		repository: repository, validator: validator, currentGameData: currentGameData,
		transactions: transactions, newID: newID, now: now,
	}
}

// dependencyIsNil 同时识别空接口和被接口包装的 nil 引用。
//
// Go 接口可以携带动态类型为指针但动态值为 nil 的情况；若只比较接口值是否为空，调用方法时才会以非
// 业务方式崩溃。这里将其归为构造错误，确保保存路径从启动时就同时拥有可调用的实时资料校验器与门禁。
func dependencyIsNil(dependency any) bool {
	if dependency == nil {
		return true
	}
	value := reflect.ValueOf(dependency)
	switch value.Kind() {
	case reflect.Chan, reflect.Func, reflect.Interface, reflect.Map, reflect.Pointer, reflect.Slice:
		return value.IsNil()
	default:
		return false
	}
}

// withinTransaction 在提供事务运行器时让领域与持久层操作共享一个事务 Context。
//
// 纯领域单元测试可显式传入 nil，此时直接执行操作；构造器不接受可变数量依赖，避免调用方误以为多个
// TransactionRunner 会被组合使用。
func withinTransaction(
	ctx context.Context,
	transactions TransactionRunner,
	operation func(context.Context) error,
) error {
	if transactions == nil {
		return operation(ctx)
	}
	return transactions.WithinTransaction(ctx, operation)
}

// Create 规范化完整阵容，并把账号级并发不变量交给原子存储事务落实。
func (s *Service) Create(ctx context.Context, command CreateCommand) (Team, error) {
	name, nameKey, valid := normalizeName(command.Name)
	members, membersValid := normalizeMembers(command.Members)
	command.IdempotencyKey = strings.TrimSpace(command.IdempotencyKey)
	command.RequestID = strings.TrimSpace(command.RequestID)
	if !valid || !membersValid || command.AccountID == snowflake.ID(0) || command.PlayerCharacterID == snowflake.ID(0) ||
		!idempotency.ValidKey(command.IdempotencyKey) || command.RequestID == "" {
		return Team{}, ErrInvalidTeam
	}
	id, idErr := s.newID.Next(ctx)
	if idErr != nil {
		return Team{}, idErr
	}
	now := s.now().UTC()
	var created Team
	err := s.currentGameData.WithinAvailable(ctx, func(transactionContext context.Context) error {
		var createErr error
		created, createErr = s.repository.Create(transactionContext, CreateRecord{
			Team: Team{
				ID: id, PlayerCharacterID: command.PlayerCharacterID,
				Name: name, NameKey: nameKey, Version: 1, Members: members, CreatedAt: now, UpdatedAt: now,
			},
			ActorAccountID: command.AccountID,
			IdempotencyKey: command.IdempotencyKey, RequestID: command.RequestID,
			currentMemberValidator: s.validator,
		})
		return createErr
	})
	return created, err
}

// Update 按当前实时资料重新校验完整阵容，并以乐观版本替换 Team。
func (s *Service) Update(ctx context.Context, command UpdateCommand) (Team, error) {
	name, nameKey, valid := normalizeName(command.Name)
	members, membersValid := normalizeMembers(command.Members)
	command.IdempotencyKey = strings.TrimSpace(command.IdempotencyKey)
	command.RequestID = strings.TrimSpace(command.RequestID)
	if !valid || !membersValid || !validOwnedTeamCommand(
		command.AccountID, command.PlayerCharacterID, command.TeamID,
		command.ExpectedVersion, command.IdempotencyKey, command.RequestID,
	) {
		return Team{}, ErrInvalidTeam
	}
	var updated Team
	var operationErr error
	err := s.currentGameData.WithinAvailable(ctx, func(transactionContext context.Context) error {
		updated, operationErr = s.repository.Update(transactionContext, UpdateRecord{
			Team: Team{
				ID: command.TeamID, PlayerCharacterID: command.PlayerCharacterID,
				Name: name, NameKey: nameKey, Members: members, UpdatedAt: s.now().UTC(),
			},
			ExpectedVersion: command.ExpectedVersion, ActorAccountID: command.AccountID,
			IdempotencyKey: command.IdempotencyKey, RequestID: command.RequestID,
			currentMemberValidator: s.validator,
		})
		return operationErr
	})
	return updated, err
}

// Delete 删除可变 Team；删除活动 Team 时由存储事务确定性选择最早剩余 Team。
func (s *Service) Delete(ctx context.Context, command DeleteCommand) (DeleteResult, error) {
	command.IdempotencyKey = strings.TrimSpace(command.IdempotencyKey)
	command.RequestID = strings.TrimSpace(command.RequestID)
	if !validOwnedTeamCommand(
		command.AccountID, command.PlayerCharacterID, command.TeamID,
		command.ExpectedVersion, command.IdempotencyKey, command.RequestID,
	) {
		return DeleteResult{}, ErrInvalidTeam
	}
	var result DeleteResult
	err := withinTransaction(ctx, s.transactions, func(transactionContext context.Context) error {
		var operationErr error
		result, operationErr = s.repository.Delete(transactionContext, DeleteRecord{
			AccountID: command.AccountID, PlayerCharacterID: command.PlayerCharacterID, TeamID: command.TeamID,
			ExpectedVersion: command.ExpectedVersion, IdempotencyKey: command.IdempotencyKey,
			RequestID: command.RequestID, DeletedAt: s.now().UTC(),
		})
		return operationErr
	})
	return result, err
}

// SwitchActive 以绑定版本切换 PlayerCharacter 当前默认 Team。
func (s *Service) SwitchActive(ctx context.Context, command SwitchActiveCommand) (ActiveBinding, error) {
	command.IdempotencyKey = strings.TrimSpace(command.IdempotencyKey)
	command.RequestID = strings.TrimSpace(command.RequestID)
	if !validOwnedTeamCommand(
		command.AccountID, command.PlayerCharacterID, command.TeamID,
		command.ExpectedVersion, command.IdempotencyKey, command.RequestID,
	) {
		return ActiveBinding{}, ErrInvalidTeam
	}
	var result ActiveBinding
	err := withinTransaction(ctx, s.transactions, func(transactionContext context.Context) error {
		var operationErr error
		result, operationErr = s.repository.SwitchActive(transactionContext, SwitchActiveRecord{
			AccountID: command.AccountID, PlayerCharacterID: command.PlayerCharacterID, TeamID: command.TeamID,
			ExpectedVersion: command.ExpectedVersion, IdempotencyKey: command.IdempotencyKey,
			RequestID: command.RequestID, UpdatedAt: s.now().UTC(),
		})
		return operationErr
	})
	return result, err
}

func validOwnedTeamCommand(
	accountID, playerCharacterID, teamID snowflake.ID,
	expectedVersion int64,
	idempotencyKey, requestID string,
) bool {
	return accountID != snowflake.ID(0) && playerCharacterID != snowflake.ID(0) && teamID != snowflake.ID(0) &&
		expectedVersion >= 1 && idempotency.ValidKey(idempotencyKey) && requestID != ""
}

func normalizeName(raw string) (string, string, bool) {
	name := strings.Join(strings.Fields(norm.NFKC.String(raw)), " ")
	length := utf8.RuneCountInString(name)
	if length < 1 || length > 40 {
		return "", "", false
	}
	return name, strings.ToLower(name), true
}

func normalizeMembers(inputs []MemberInput) ([]Member, bool) {
	if len(inputs) < 1 || len(inputs) > maximumMembers {
		return nil, false
	}
	members := make([]Member, len(inputs))
	for index, input := range inputs {
		if input.CreatureID == snowflake.ID(0) || input.AbilityID == snowflake.ID(0) || input.TeraElementID == snowflake.ID(0) || input.NatureID == snowflake.ID(0) ||
			input.Level < 1 || input.Level > 100 ||
			!optionalIdentifierValid(input.FormID) || !optionalIdentifierValid(input.GenderID) ||
			!optionalIdentifierValid(input.SkinID) || !optionalIdentifierValid(input.ItemID) ||
			len(input.SkillIDs) < 1 || len(input.SkillIDs) > maximumSkillsPerMember ||
			len(input.Stats) > maximumStatsPerMember {
			return nil, false
		}
		skills := make([]MemberSkill, len(input.SkillIDs))
		seenSkills := make(map[snowflake.ID]struct{}, len(input.SkillIDs))
		for skillIndex, skillID := range input.SkillIDs {
			if skillID == snowflake.ID(0) {
				return nil, false
			}
			if _, duplicate := seenSkills[skillID]; duplicate {
				return nil, false
			}
			seenSkills[skillID] = struct{}{}
			skills[skillIndex] = MemberSkill{Position: int32(skillIndex + 1), SkillID: skillID}
		}
		stats := make([]MemberStat, len(input.Stats))
		seenStats := make(map[snowflake.ID]struct{}, len(input.Stats))
		var totalEffort int32
		for statIndex, stat := range input.Stats {
			if stat.StatID == snowflake.ID(0) || stat.IndividualValue < 0 || stat.IndividualValue > 31 ||
				stat.EffortValue < 0 || stat.EffortValue > 252 {
				return nil, false
			}
			if _, duplicate := seenStats[stat.StatID]; duplicate {
				return nil, false
			}
			seenStats[stat.StatID] = struct{}{}
			totalEffort += stat.EffortValue
			stats[statIndex] = MemberStat(stat)
		}
		if totalEffort > 510 {
			return nil, false
		}
		members[index] = Member{
			Position: int32(index + 1), CreatureID: input.CreatureID,
			FormID: cloneIdentifier(input.FormID), GenderID: cloneIdentifier(input.GenderID), SkinID: cloneIdentifier(input.SkinID),
			AbilityID: input.AbilityID, ItemID: cloneIdentifier(input.ItemID), TeraElementID: input.TeraElementID, NatureID: input.NatureID, Level: input.Level,
			Skills: skills, Stats: stats,
		}
	}
	return members, true
}

func optionalIdentifierValid(value *snowflake.ID) bool {
	return value == nil || *value != snowflake.ID(0)
}

func cloneIdentifier(value *snowflake.ID) *snowflake.ID {
	if value == nil {
		return nil
	}
	copy := *value
	return &copy
}
