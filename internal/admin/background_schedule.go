package admin

import (
	"context"
	"encoding/json"
	"errors"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

var (
	// ErrBackgroundScheduleNotFound 表示调度不存在或乐观版本已经变化。
	ErrBackgroundScheduleNotFound = errors.New("后台调度不存在或版本冲突")
	// ErrInvalidBackgroundSchedule 表示表达式、参数或任务类型不满足代码约束。
	ErrInvalidBackgroundSchedule = errors.New("后台调度参数无效")
)

// BackgroundSchedule 描述由管理端维护、由 Worker 生成任务的动态调度实例。
type BackgroundSchedule struct {
	// ID 是动态调度的稳定 Snowflake Identifier。
	ID snowflake.ID
	// Code 是全局唯一稳定机器编码。
	Code string
	// Name 是简体中文显示名称。
	Name string
	// TaskKind 是代码白名单声明的任务类型。
	TaskKind string
	// ScheduleKind 是 cron 或 interval。
	ScheduleKind string
	// CronExpression 是 Asia/Shanghai 时区的五段 Cron；固定间隔调度为空。
	CronExpression *string
	// IntervalSeconds 是固定间隔秒数；Cron 调度为空。
	IntervalSeconds *int32
	// MissedRunPolicy 是 skip、coalesce 或 catch_up。
	MissedRunPolicy string
	// Parameters 是具体任务类型解释的 JSON 对象。
	Parameters json.RawMessage
	// Enabled 表示调度是否继续生成未来任务。
	Enabled bool
	// NextRunAt 是启用调度的下一次理论触发时间。
	NextRunAt *time.Time
	// LastScheduledAt 是最近一次成功生成任务的理论触发时间。
	LastScheduledAt *time.Time
	// Version 是管理写入使用的乐观并发版本。
	Version int64
	// CreatedAt 是调度创建时间。
	CreatedAt time.Time
	// UpdatedAt 是调度最近修改时间。
	UpdatedAt time.Time
}

// BackgroundScheduleInput 保存创建和修改动态调度的全部可编辑字段。
type BackgroundScheduleInput struct {
	// Code 是全局唯一稳定机器编码。
	Code string
	// Name 是简体中文显示名称。
	Name string
	// TaskKind 是代码白名单声明的任务类型。
	TaskKind string
	// ScheduleKind 是 cron 或 interval。
	ScheduleKind string
	// CronExpression 是可选五段 Cron。
	CronExpression *string
	// IntervalSeconds 是可选固定间隔秒数。
	IntervalSeconds *int32
	// MissedRunPolicy 是错过周期处理策略。
	MissedRunPolicy string
	// Parameters 是已经确认编码 JSON 对象的任务参数。
	Parameters json.RawMessage
}

// BackgroundScheduleMutation 保存一次动态调度管理写入的可信审计上下文。
type BackgroundScheduleMutation struct {
	// ActorAccountID 是发起写入的已认证管理员 Identifier。
	ActorAccountID snowflake.ID
	// RequestID 关联入口 HTTP 日志和管理员审计记录。
	RequestID string
	// OccurredAt 是应用层确认的 UTC 操作时间。
	OccurredAt time.Time
}

// BackgroundScheduleQuery 指定动态调度页码和可选启停筛选。
type BackgroundScheduleQuery struct {
	// PageNumber 从一开始计数。
	PageNumber int
	// PageSize 是不超过一百的单页条数。
	PageSize int
	// Enabled 存在时只读取对应启停状态。
	Enabled *bool
}

// BackgroundSchedulePage 返回动态调度页和精确总数。
type BackgroundSchedulePage struct {
	// Schedules 是按稳定编码排序的当前页。
	Schedules []BackgroundSchedule
	// TotalCount 是筛选后的精确总数。
	TotalCount int64
}

// BackgroundScheduleRepository 定义动态调度管理所需的 PostgreSQL 边界。
type BackgroundScheduleRepository interface {
	// ListSchedules 按页读取调度。
	ListSchedules(context.Context, BackgroundScheduleQuery) (BackgroundSchedulePage, error)
	// CreateSchedule 创建默认停用的调度。
	CreateSchedule(context.Context, BackgroundScheduleInput, BackgroundScheduleMutation) (BackgroundSchedule, error)
	// UpdateSchedule 替换指定版本调度的可编辑字段。
	UpdateSchedule(context.Context, snowflake.ID, int64, BackgroundScheduleInput, BackgroundScheduleMutation) (BackgroundSchedule, error)
	// SetScheduleEnabled 切换指定版本调度的启停状态。
	SetScheduleEnabled(context.Context, snowflake.ID, int64, bool, BackgroundScheduleMutation) (BackgroundSchedule, error)
}
