// Package referencedictionary 管理生物引用字典与货币资料的记录级维护命令。
package referencedictionary

import (
	"context"
	"errors"
	"strings"
	"time"

	"github.com/lishangbu/avalon/internal/gamedata/administration"
	"github.com/lishangbu/avalon/internal/gamedata/stablecode"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

var (
	// ErrInvalid 表示资源类型或字段无效。
	ErrInvalid = errors.New("引用字典字段无效")
	// ErrNotFound 表示指定记录不存在。
	ErrNotFound = errors.New("引用字典记录不存在")
	// ErrConflict 表示稳定编码、乐观版本或幂等请求冲突。
	ErrConflict = errors.New("引用字典记录冲突")
)

// Kind 标识一种具有独立管理契约的引用资料。
type Kind string

const (
	// KindGrowthRate 是 Creature Species 引用的成长速率。
	KindGrowthRate Kind = "growth-rate"
	// KindHabitat 是 Creature Species 引用的栖息地。
	KindHabitat Kind = "habitat"
	// KindSpeciesColor 是 Creature Species 引用的颜色。
	KindSpeciesColor Kind = "species-color"
	// KindSpeciesShape 是 Creature Species 引用的外形。
	KindSpeciesShape Kind = "species-shape"
	// KindEggGroup 是 Creature Species 引用的蛋组。
	KindEggGroup Kind = "egg-group"
	// KindCurrency 是 RPG 经济系统使用的货币。
	KindCurrency Kind = "currency"
)

// Entry 是内部持久化复用的完整记录；对外 RPC 仍按资源提供专属消息。
type Entry struct {
	ID          snowflake.ID
	Kind        Kind
	Code        string
	Name        string
	Formula     *string
	Description *string
	Symbol      *string
	SortOrder   int32
	Enabled     bool
	Version     int64
}

// CreateCommand 包含创建一条引用资料所需的全部事实。
type CreateCommand struct {
	administration.GameDataWriteContext
	Entry
}

// UpdateCommand 包含完整替换一条引用资料所需的全部事实。
type UpdateCommand struct {
	administration.GameDataWriteContext
	Entry
	ExpectedVersion int64
}

// ReferenceDictionaryQuery 返回六种引用资料的管理投影。
type ReferenceDictionaryQuery interface {
	ListReferenceDictionary(context.Context, Kind) ([]Entry, error)
}

// ReferenceDictionaryRepository 提供六种引用资料共享的事务写入端口。
type ReferenceDictionaryRepository interface {
	CreateReferenceDictionary(context.Context, Entry, administration.GameDataWriteContext, time.Time) (Entry, error)
	UpdateReferenceDictionary(context.Context, Entry, int64, administration.GameDataWriteContext, time.Time) (Entry, error)
}

// Service 复用身份生成、规范化和并发控制流程。
type Service struct {
	query      ReferenceDictionaryQuery
	repository ReferenceDictionaryRepository
	newID      snowflake.Source
	now        func() time.Time
}

// NewService 创建引用资料管理服务。
func NewService(query ReferenceDictionaryQuery, repository ReferenceDictionaryRepository, newID snowflake.Source, now func() time.Time) *Service {
	return &Service{query: query, repository: repository, newID: newID, now: now}
}

// List 读取指定资源的全部记录。
func (s *Service) List(ctx context.Context, kind Kind) ([]Entry, error) {
	if !kind.Valid() {
		return nil, ErrInvalid
	}
	return s.query.ListReferenceDictionary(ctx, kind)
}

// Create 创建版本为一的引用资料。
func (s *Service) Create(ctx context.Context, command CreateCommand) (Entry, error) {
	command.GameDataWriteContext = command.Normalize()
	entry := normalize(command.Entry)
	entry.Version = 1
	if !command.GameDataWriteContext.Valid() || !entry.Valid(false) {
		return Entry{}, ErrInvalid
	}
	id, err := s.newID.Next(ctx)
	if err != nil {
		return Entry{}, err
	}
	entry.ID = id
	return s.repository.CreateReferenceDictionary(ctx, entry, command.GameDataWriteContext, s.now().UTC())
}

// Update 使用预期版本完整更新引用资料。
func (s *Service) Update(ctx context.Context, command UpdateCommand) (Entry, error) {
	command.GameDataWriteContext = command.Normalize()
	entry := normalize(command.Entry)
	entry.Version = command.ExpectedVersion + 1
	if !command.GameDataWriteContext.Valid() || command.ExpectedVersion < 1 || !entry.Valid(true) {
		return Entry{}, ErrInvalid
	}
	return s.repository.UpdateReferenceDictionary(ctx, entry, command.ExpectedVersion, command.GameDataWriteContext, s.now().UTC())
}

// Valid 判断记录是否符合对应资源的字段边界。
func (entry Entry) Valid(requireID bool) bool {
	if (requireID && entry.ID == 0) || !entry.Kind.Valid() || !stablecode.Valid(entry.Code) || entry.Name == "" || len([]rune(entry.Name)) > 80 {
		return false
	}
	switch entry.Kind {
	case KindGrowthRate:
		return validText(entry.Formula, 4000) && validText(entry.Description, 4000) && entry.Symbol == nil
	case KindCurrency:
		return validText(entry.Symbol, 16) && entry.Formula == nil && entry.Description == nil
	default:
		return entry.Formula == nil && entry.Description == nil && entry.Symbol == nil
	}
}

// Valid 判断资源类型是否受支持。
func (kind Kind) Valid() bool {
	return kind == KindGrowthRate || kind == KindHabitat || kind == KindSpeciesColor || kind == KindSpeciesShape || kind == KindEggGroup || kind == KindCurrency
}

func normalize(entry Entry) Entry {
	entry.Code = strings.TrimSpace(entry.Code)
	entry.Name = strings.TrimSpace(entry.Name)
	entry.Formula = trimText(entry.Formula)
	entry.Description = trimText(entry.Description)
	entry.Symbol = trimText(entry.Symbol)
	return entry
}

func trimText(value *string) *string {
	if value == nil {
		return nil
	}
	trimmed := strings.TrimSpace(*value)
	if trimmed == "" {
		return nil
	}
	return &trimmed
}

func validText(value *string, maximum int) bool {
	return value == nil || len([]rune(*value)) <= maximum
}
