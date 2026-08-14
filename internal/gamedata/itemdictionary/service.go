// Package itemdictionary 提供 Pocket、Attribute 与 Fling Effect 三类独立道具字典的共享命令编排。
package itemdictionary

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
	// ErrInvalid 表示字典类型或字段无效。
	ErrInvalid = errors.New("道具字典字段无效")
	// ErrNotFound 表示指定字典记录不存在。
	ErrNotFound = errors.New("道具字典记录不存在")
	// ErrConflict 表示稳定编码或乐观版本冲突。
	ErrConflict = errors.New("道具字典记录冲突")
)

// Kind 标识独立的规范化字典资源。
type Kind string

const (
	// KindPocket 是道具分类所属口袋。
	KindPocket Kind = "pocket"
	// KindAttribute 是道具可绑定属性。
	KindAttribute Kind = "attribute"
	// KindFlingEffect 是投掷道具产生的效果。
	KindFlingEffect Kind = "fling-effect"
)

// Entry 是三个字典共享的记录形状；Description 仅供 Attribute/Fling Effect 使用。
type Entry struct {
	ID          snowflake.ID
	Kind        Kind
	Code        string
	Name        string
	Description *string
	SortOrder   int32
	Enabled     bool
	Version     int64
}

// CreateCommand 包含创建字典记录的全部事实。
type CreateCommand struct {
	administration.GameDataWriteContext
	Kind        Kind
	Code        string
	Name        string
	Description *string
	SortOrder   int32
	Enabled     bool
}

// UpdateCommand 包含更新字典记录的全部事实。
type UpdateCommand struct {
	administration.GameDataWriteContext
	Entry
	ExpectedVersion int64
}

// ItemDictionaryQuery 返回规范化道具字典的管理投影。
type ItemDictionaryQuery interface {
	ListItemDictionary(context.Context, Kind) ([]Entry, error)
}

// ItemDictionaryRepository 提供规范化道具字典的记录级写入能力。
type ItemDictionaryRepository interface {
	CreateItemDictionary(context.Context, Entry, administration.GameDataWriteContext, time.Time) (Entry, error)
	UpdateItemDictionary(context.Context, Entry, int64, administration.GameDataWriteContext, time.Time) (Entry, error)
}

// Service 统一复用三类字典完全相同的校验和身份生成流程。
type Service struct {
	query      ItemDictionaryQuery
	repository ItemDictionaryRepository
	newID      snowflake.Source
	now        func() time.Time
}

// NewService 创建规范化道具字典服务。
func NewService(query ItemDictionaryQuery, repository ItemDictionaryRepository, newID snowflake.Source, now func() time.Time) *Service {
	return &Service{query: query, repository: repository, newID: newID, now: now}
}

// List 读取一种字典的全部记录。
func (s *Service) List(ctx context.Context, kind Kind) ([]Entry, error) {
	if !kind.Valid() {
		return nil, ErrInvalid
	}
	return s.query.ListItemDictionary(ctx, kind)
}

// Create 创建版本为一的字典记录。
func (s *Service) Create(ctx context.Context, command CreateCommand) (Entry, error) {
	command.GameDataWriteContext = command.Normalize()
	entry := Entry{Kind: command.Kind, Code: strings.TrimSpace(command.Code), Name: strings.TrimSpace(command.Name), Description: trimText(command.Description), SortOrder: command.SortOrder, Enabled: command.Enabled, Version: 1}
	if !command.GameDataWriteContext.Valid() || !entry.Valid(false) {
		return Entry{}, ErrInvalid
	}
	id, err := s.newID.Next(ctx)
	if err != nil {
		return Entry{}, err
	}
	entry.ID = id
	return s.repository.CreateItemDictionary(ctx, entry, command.GameDataWriteContext, s.now().UTC())
}

// Update 使用预期版本完整替换一条字典记录。
func (s *Service) Update(ctx context.Context, command UpdateCommand) (Entry, error) {
	command.GameDataWriteContext = command.Normalize()
	entry := command.Entry
	entry.Code = strings.TrimSpace(entry.Code)
	entry.Name = strings.TrimSpace(entry.Name)
	entry.Description = trimText(entry.Description)
	entry.Version = command.ExpectedVersion + 1
	if !command.GameDataWriteContext.Valid() || command.ExpectedVersion < 1 || !entry.Valid(true) {
		return Entry{}, ErrInvalid
	}
	return s.repository.UpdateItemDictionary(ctx, entry, command.ExpectedVersion, command.GameDataWriteContext, s.now().UTC())
}

// Valid 判断资源类型和公共字段是否合法。
func (entry Entry) Valid(requireID bool) bool {
	return (!requireID || entry.ID != 0) && entry.Kind.Valid() && stablecode.Valid(entry.Code) && entry.Name != "" && len([]rune(entry.Name)) <= 120 && (entry.Description == nil || len([]rune(*entry.Description)) <= 4000)
}

// Valid 判断是否为受支持的字典资源。
func (kind Kind) Valid() bool {
	return kind == KindPocket || kind == KindAttribute || kind == KindFlingEffect
}

func trimText(value *string) *string {
	if value == nil {
		return nil
	}
	result := strings.TrimSpace(*value)
	if result == "" {
		return nil
	}
	return &result
}
