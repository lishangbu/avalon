package rpg

import (
	"errors"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

var (
	// ErrQuestUnavailable 表示任务不存在、未启用、地点或前置条件不满足，或当前生命周期不允许执行命令。
	ErrQuestUnavailable = errors.New("Quest 当前不可用")
	// ErrQuestProgressConflict 表示客户端提交的任务进度版本已经过期。
	ErrQuestProgressConflict = errors.New("Quest Progress 版本冲突")
	// ErrQuestObjectivesIncomplete 表示任务仍有尚未达到要求数量的目标。
	ErrQuestObjectivesIncomplete = errors.New("Quest Objective 尚未全部完成")
)

// AvailableQuest 是当前活动角色在当前位置和前置条件下可以开始的任务定义。
type AvailableQuest struct {
	QuestID     snowflake.ID
	Code, Name  string
	Description string
	QuestType   string
	Repeatable  bool
}

// QuestObjectiveProgress 是任务定义目标及当前角色累计值的只读投影。
type QuestObjectiveProgress struct {
	ObjectiveID                 snowflake.ID
	Code, ObjectiveType         string
	CurrentCount, RequiredCount int32
	Description                 string
	CompletedAt                 *time.Time
}

// QuestProgress 是一个任务在当前活动角色上的版本化生命周期投影。
type QuestProgress struct {
	QuestID         snowflake.ID
	Code, Name      string
	Description     string
	Status          string
	CompletionCount int32
	Version         int64
	StartedAt       time.Time
	CompletedAt     *time.Time
	Objectives      []QuestObjectiveProgress
}

// StartQuestCommand 开始首轮任务或已领取上一轮奖励的可重复任务。
type StartQuestCommand struct {
	AccountID      snowflake.ID
	QuestID        snowflake.ID
	IdempotencyKey string
	Now            time.Time
}

// CompleteQuestCommand 在权威交付地点完成全部目标已经达成的一轮任务。
type CompleteQuestCommand struct {
	AccountID       snowflake.ID
	QuestID         snowflake.ID
	ExpectedVersion int64
	IdempotencyKey  string
	Now             time.Time
}
