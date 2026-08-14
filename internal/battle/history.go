package battle

import (
	"encoding/json"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// HistoryEntry 是某一 Participant 可查询到的一条已终局 Battle 历史摘要。
type HistoryEntry struct {
	// BattleID 是历史 Battle 稳定 Identifier。
	BattleID snowflake.ID
	// Mode 是 pvp 或 pve 参与关系类型。
	Mode BattleMode
	// SourceType 是 challenge、training 或 encounter 来源。
	SourceType BattleSourceType
	// Side 是当前查询 PlayerCharacter 在本场中的固定位置。
	Side ParticipantSide
	// DisplayName 是参赛时冻结的当前 Participant 展示名称。
	DisplayName string
	// WinnerSide 是正常获胜方；平局、No Contest 与 Interrupted 为零值。
	WinnerSide ParticipantSide
	// TerminalReason 是稳定终局原因。
	TerminalReason string
	// TurnCount 是已提交权威回合记录数量。
	TurnCount int32
	// Summary 是可供历史页面显示的权威摘要 JSON。
	Summary json.RawMessage
	// CompletedAt 是终局 UTC 时间。
	CompletedAt time.Time
}

// HistoryPage 是某一 PlayerCharacter 已终局 Battle 历史的统一页码查询结果。
type HistoryPage struct {
	// Items 是按完成时间与 Battle Identifier 倒序排列的当前页历史摘要。
	Items []HistoryEntry
	// Page 是从 1 开始的当前页码。
	Page int32
	// PageSize 是单页最多返回的历史摘要数。
	PageSize int32
	// Total 是查询时精确匹配该 PlayerCharacter 的终局 Battle 总数。
	Total int64
}
