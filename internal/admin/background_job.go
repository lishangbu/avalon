package admin

import (
	"context"
	"errors"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

const (
	// MaximumBackgroundJobPageSize 限制单次管理查询的任务数量，避免运维页面耗尽数据库连接。
	MaximumBackgroundJobPageSize = 100
	// MaximumBackgroundJobPageNumber 限制后台任务页码查询的最大深度。
	MaximumBackgroundJobPageNumber = 1000
)

var (
	// ErrBackgroundJobNotFound 表示指定 Asynq 任务不存在或已被 Asynq 保留策略清理。
	ErrBackgroundJobNotFound = errors.New("后台任务不存在")
	// ErrInvalidBackgroundJobQuery 表示页码、页大小或筛选条件超出受控查询边界。
	ErrInvalidBackgroundJobQuery = errors.New("后台任务查询参数无效")
	// ErrInvalidBackgroundJobOperation 表示人工任务处置缺少可信管理员、幂等键或请求标识。
	ErrInvalidBackgroundJobOperation = errors.New("后台任务操作参数无效")
)

// BackgroundJobState 是 PostgreSQL 权威后台任务的稳定生命周期状态。
type BackgroundJobState string

const (
	// BackgroundJobStatePending 表示任务等待 Outbox 投递。
	BackgroundJobStatePending BackgroundJobState = "pending"
	// BackgroundJobStateQueued 表示任务已经可靠写入 Asynq。
	BackgroundJobStateQueued BackgroundJobState = "queued"
	// BackgroundJobStateScheduled 表示任务尚未到达计划执行时间。
	BackgroundJobStateScheduled BackgroundJobState = "scheduled"
	// BackgroundJobStateRunning 表示任务正在由 Worker 执行。
	BackgroundJobStateRunning BackgroundJobState = "running"
	// BackgroundJobStateRetryWait 表示任务等待下一次自动执行。
	BackgroundJobStateRetryWait BackgroundJobState = "retry_wait"
	// BackgroundJobStateCancellationRequested 表示管理员已经请求取消在途任务。
	BackgroundJobStateCancellationRequested BackgroundJobState = "cancellation_requested"
	// BackgroundJobStateRetryRequested 表示管理员已经请求重新执行终态任务。
	BackgroundJobStateRetryRequested BackgroundJobState = "retry_requested"
	// BackgroundJobStateCancelled 表示任务已被取消。
	BackgroundJobStateCancelled BackgroundJobState = "cancelled"
	// BackgroundJobStateCompleted 表示任务已成功完成。
	BackgroundJobStateCompleted BackgroundJobState = "completed"
	// BackgroundJobStateFailed 表示任务已耗尽最大尝试次数。
	BackgroundJobStateFailed BackgroundJobState = "failed"
)

// BackgroundJob 是管理员可查看的 PostgreSQL 权威任务最小视图。
//
// 参数、输出、堆栈与元数据均可能携带敏感业务信息，因此不属于该视图；失败原因由持久化适配器进行
// 长度限制和控制字符清理后才会返回。
type BackgroundJob struct {
	// ID 是 API、PostgreSQL、Outbox 与 Asynq 共用的稳定 Snowflake Identifier。
	ID snowflake.ID
	// Kind 是 Worker 注册的稳定任务类型。
	Kind string
	// Queue 是 Asynq 调度任务使用的逻辑队列。
	Queue string
	// State 是当前受控生命周期状态。
	State BackgroundJobState
	// Attempt 是已经实际执行过的次数。
	Attempt int
	// MaxAttempts 是后台任务自动执行允许的最大次数。
	MaxAttempts int
	// FailureReason 是最后一次失败的脱敏摘要；没有失败时为空。
	FailureReason string
	// VerificationPassed 是受控校验任务的确定结论；普通任务或尚无输出时为 nil。
	VerificationPassed *bool
	// ResultSummary 是受控校验任务输出的脱敏简体中文摘要；普通任务或尚无输出时为空。
	ResultSummary string
	// CreatedAt 是任务首次入队时间。
	CreatedAt time.Time
	// ScheduledAt 是任务下一次允许执行的时间。
	ScheduledAt time.Time
	// AttemptedAt 是最近一次实际执行开始时间；从未执行时为 nil。
	AttemptedAt *time.Time
	// FinalizedAt 是最终完成、取消或丢弃的时间；未最终结束时为 nil。
	FinalizedAt *time.Time
}

// BackgroundJobQuery 指定受约束的后台任务页码查询。
type BackgroundJobListQuery struct {
	// PageNumber 从一开始计数，不能作为长期保存的稳定游标。
	PageNumber int
	// PageSize 是单页任务条数，必须不大于 MaximumBackgroundJobPageSize。
	PageSize int
	// State 为空时不筛选任务状态；非空时必须是声明的稳定状态之一。
	State BackgroundJobState
	// Kind 为空时不筛选任务类型；非空时精确匹配 Asynq Worker 类型。
	Kind string
}

// BackgroundJobPage 是当前筛选条件下的一页任务和精确总数。
type BackgroundJobPage struct {
	// Jobs 是按创建时间和 Identifier 倒序排列的当前页。
	Jobs []BackgroundJob
	// TotalCount 是当前筛选条件下的精确任务总数。
	TotalCount int64
}

// BackgroundJobOperation 是一次人工重试或取消需要审计和幂等的最小事实。
type BackgroundJobOperation struct {
	// JobID 是目标后台任务的稳定 Snowflake Identifier。
	JobID snowflake.ID
	// ActorAccountID 是发起操作的已认证管理员 Identifier。
	ActorAccountID snowflake.ID
	// IdempotencyKey 是作用域限于管理员、操作和任务的客户端幂等键。
	IdempotencyKey string
	// RequestID 关联 HTTP 日志、审计和本次受控运维操作。
	RequestID string
	// OccurredAt 是应用层观察到本次操作的 UTC 时间。
	OccurredAt time.Time
}

// VerificationJobOperation 是人工创建受控校验任务需要审计和幂等的最小事实。
type VerificationJobOperation struct {
	// BattleID 是回放校验的目标 Battle Identifier；审计哈希链校验时必须为空。
	BattleID snowflake.ID
	// ActorAccountID 是发起校验的已认证管理员 Identifier。
	ActorAccountID snowflake.ID
	// IdempotencyKey 是作用域限于管理员、校验类型和目标对象的客户端幂等键。
	IdempotencyKey string
	// RequestID 关联管理 HTTP 日志、Asynq 参数和管理员审计记录。
	RequestID string
	// OccurredAt 是应用层观察到本次命令的 UTC 时间。
	OccurredAt time.Time
}

// BackgroundJobQuery 返回后台任务分页管理投影。
type BackgroundJobQuery interface {
	// List 返回受限查询条件下的一页任务及精确总数。
	List(context.Context, BackgroundJobListQuery) (BackgroundJobPage, error)
}

// BackgroundJobReader 返回单个后台任务领域对象。
type BackgroundJobReader interface {
	// Get 返回一个尚未被保留策略清理的任务。
	Get(context.Context, snowflake.ID) (BackgroundJob, error)
}

// BackgroundJobRepository 隔离后台任务命令与 Asynq、PostgreSQL 及管理员审计实现。
type BackgroundJobRepository interface {
	// Retry 在同一数据库事务内重试任务、写管理员审计并保存幂等响应。
	Retry(context.Context, BackgroundJobOperation) (BackgroundJob, error)
	// Cancel 在同一数据库事务内取消任务、写管理员审计并保存幂等响应。
	Cancel(context.Context, BackgroundJobOperation) (BackgroundJob, error)
	// EnqueueBattleReplayVerification 在同一事务内创建回放校验任务、审计和幂等响应。
	EnqueueBattleReplayVerification(context.Context, VerificationJobOperation) (BackgroundJob, error)
	// EnqueueAuditHashVerification 在同一事务内创建审计校验任务、审计和幂等响应。
	EnqueueAuditHashVerification(context.Context, VerificationJobOperation) (BackgroundJob, error)
}

// BackgroundJobService 是管理员后台任务查询与处置的应用服务。
type BackgroundJobService struct {
	// query 负责后台任务分页管理投影。
	query BackgroundJobQuery
	// reader 负责单个后台任务读取。
	reader BackgroundJobReader
	// repository 负责 Asynq 状态转换与同事务审计持久化。
	repository BackgroundJobRepository
	// scheduleQuery 负责动态调度分页管理投影。
	scheduleQuery BackgroundScheduleQuery
	// scheduleRepository 负责动态调度写命令。
	scheduleRepository BackgroundScheduleRepository
	// now 为命令写入提供可测试的统一时间来源。
	now func() time.Time
}

// NewBackgroundJobService 使用按职责拆分的显式持久化端口创建后台任务应用服务。
func NewBackgroundJobService(
	query BackgroundJobQuery,
	reader BackgroundJobReader,
	repository BackgroundJobRepository,
	scheduleQuery BackgroundScheduleQuery,
	scheduleRepository BackgroundScheduleRepository,
	now func() time.Time,
) *BackgroundJobService {
	if now == nil {
		now = time.Now
	}
	return &BackgroundJobService{
		query: query, reader: reader, repository: repository,
		scheduleQuery: scheduleQuery, scheduleRepository: scheduleRepository, now: now,
	}
}

// List 返回当前筛选条件下的受限后台任务页。
func (s *BackgroundJobService) List(ctx context.Context, query BackgroundJobListQuery) (BackgroundJobPage, error) {
	if s == nil || s.query == nil || !validBackgroundJobQuery(query) {
		return BackgroundJobPage{}, ErrInvalidBackgroundJobQuery
	}
	return s.query.List(ctx, query)
}

// Get 返回一个后台任务的脱敏运维视图。
func (s *BackgroundJobService) Get(ctx context.Context, jobID snowflake.ID) (BackgroundJob, error) {
	if s == nil || s.reader == nil || jobID == snowflake.ID(0) {
		return BackgroundJob{}, ErrBackgroundJobNotFound
	}
	return s.reader.Get(ctx, jobID)
}

// Retry 重新安排一个任务，并保证重放相同命令不会再次改变 Asynq 状态或重复写审计。
func (s *BackgroundJobService) Retry(ctx context.Context, operation BackgroundJobOperation) (BackgroundJob, error) {
	if s == nil || s.repository == nil || !validBackgroundJobOperation(operation) {
		return BackgroundJob{}, ErrInvalidBackgroundJobOperation
	}
	operation.OccurredAt = s.now().UTC()
	return s.repository.Retry(ctx, operation)
}

// Cancel 请求取消一个任务，并保证重放相同命令不会再次改变 Asynq 状态或重复写审计。
func (s *BackgroundJobService) Cancel(ctx context.Context, operation BackgroundJobOperation) (BackgroundJob, error) {
	if s == nil || s.repository == nil || !validBackgroundJobOperation(operation) {
		return BackgroundJob{}, ErrInvalidBackgroundJobOperation
	}
	operation.OccurredAt = s.now().UTC()
	return s.repository.Cancel(ctx, operation)
}

// EnqueueBattleReplayVerification 创建一个读取指定 Battle 冻结档案的受控 Asynq 校验任务。
func (s *BackgroundJobService) EnqueueBattleReplayVerification(
	ctx context.Context,
	operation VerificationJobOperation,
) (BackgroundJob, error) {
	if s == nil || s.repository == nil || !validVerificationJobOperation(operation, true) {
		return BackgroundJob{}, ErrInvalidBackgroundJobOperation
	}
	operation.OccurredAt = s.now().UTC()
	return s.repository.EnqueueBattleReplayVerification(ctx, operation)
}

// EnqueueAuditHashVerification 创建一个与每日周期任务共用实现的人工审计哈希链校验任务。
func (s *BackgroundJobService) EnqueueAuditHashVerification(
	ctx context.Context,
	operation VerificationJobOperation,
) (BackgroundJob, error) {
	if s == nil || s.repository == nil || !validVerificationJobOperation(operation, false) {
		return BackgroundJob{}, ErrInvalidBackgroundJobOperation
	}
	operation.OccurredAt = s.now().UTC()
	return s.repository.EnqueueAuditHashVerification(ctx, operation)
}

// ListSchedules 按页读取动态调度实例。
func (s *BackgroundJobService) ListSchedules(ctx context.Context, query BackgroundScheduleListQuery) (BackgroundSchedulePage, error) {
	if s == nil || s.scheduleQuery == nil || query.PageNumber < 1 || query.PageSize < 1 || query.PageSize > 100 {
		return BackgroundSchedulePage{}, ErrInvalidBackgroundSchedule
	}
	return s.scheduleQuery.ListSchedules(ctx, query)
}

// CreateSchedule 创建默认停用的动态调度实例。
func (s *BackgroundJobService) CreateSchedule(ctx context.Context, input BackgroundScheduleInput, mutation BackgroundScheduleMutation) (BackgroundSchedule, error) {
	if s == nil || s.scheduleRepository == nil || !validBackgroundScheduleMutation(mutation) {
		return BackgroundSchedule{}, ErrInvalidBackgroundSchedule
	}
	mutation.OccurredAt = s.now().UTC()
	return s.scheduleRepository.CreateSchedule(ctx, input, mutation)
}

// UpdateSchedule 替换指定版本调度的可编辑字段。
func (s *BackgroundJobService) UpdateSchedule(ctx context.Context, id snowflake.ID, expectedVersion int64, input BackgroundScheduleInput, mutation BackgroundScheduleMutation) (BackgroundSchedule, error) {
	if s == nil || s.scheduleRepository == nil || id == snowflake.ID(0) || expectedVersion < 1 || !validBackgroundScheduleMutation(mutation) {
		return BackgroundSchedule{}, ErrInvalidBackgroundSchedule
	}
	mutation.OccurredAt = s.now().UTC()
	return s.scheduleRepository.UpdateSchedule(ctx, id, expectedVersion, input, mutation)
}

// SetScheduleEnabled 切换指定版本调度的启停状态，只影响未来任务。
func (s *BackgroundJobService) SetScheduleEnabled(ctx context.Context, id snowflake.ID, expectedVersion int64, enabled bool, mutation BackgroundScheduleMutation) (BackgroundSchedule, error) {
	if s == nil || s.scheduleRepository == nil || id == snowflake.ID(0) || expectedVersion < 1 || !validBackgroundScheduleMutation(mutation) {
		return BackgroundSchedule{}, ErrInvalidBackgroundSchedule
	}
	mutation.OccurredAt = s.now().UTC()
	return s.scheduleRepository.SetScheduleEnabled(ctx, id, expectedVersion, enabled, mutation)
}

func validBackgroundScheduleMutation(mutation BackgroundScheduleMutation) bool {
	return mutation.ActorAccountID != snowflake.ID(0) && mutation.RequestID != ""
}

func validBackgroundJobQuery(query BackgroundJobListQuery) bool {
	return query.PageNumber >= 1 && query.PageNumber <= MaximumBackgroundJobPageNumber &&
		query.PageSize >= 1 && query.PageSize <= MaximumBackgroundJobPageSize && validBackgroundJobState(query.State)
}

func validBackgroundJobState(state BackgroundJobState) bool {
	switch state {
	case "", BackgroundJobStatePending, BackgroundJobStateQueued, BackgroundJobStateScheduled, BackgroundJobStateRunning,
		BackgroundJobStateRetryWait, BackgroundJobStateCancellationRequested, BackgroundJobStateRetryRequested,
		BackgroundJobStateCancelled, BackgroundJobStateCompleted, BackgroundJobStateFailed:
		return true
	default:
		return false
	}
}

func validBackgroundJobOperation(operation BackgroundJobOperation) bool {
	return operation.JobID != snowflake.ID(0) && operation.ActorAccountID != snowflake.ID(0) && operation.IdempotencyKey != "" &&
		operation.RequestID != ""
}

func validVerificationJobOperation(operation VerificationJobOperation, requiresBattle bool) bool {
	validBattle := operation.BattleID == snowflake.ID(0)
	if requiresBattle {
		validBattle = operation.BattleID != snowflake.ID(0)
	}
	return validBattle && operation.ActorAccountID != snowflake.ID(0) && operation.IdempotencyKey != "" &&
		operation.RequestID != ""
}
