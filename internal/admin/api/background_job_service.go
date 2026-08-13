package api

import (
	"context"
	"errors"
	"fmt"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
	"strings"
	"time"

	kratoserrors "github.com/go-kratos/kratos/v3/errors"
	adminv1 "github.com/lishangbu/avalon/api/gen/go/avalon/admin/v1"
	"github.com/lishangbu/avalon/internal/admin"
	"github.com/lishangbu/avalon/internal/platform/idempotency"
	"github.com/lishangbu/avalon/internal/security/authentication"
	"google.golang.org/protobuf/types/known/timestamppb"
)

// BackgroundJobApplication 是管理员后台任务 API 依赖的最小应用服务边界。
type BackgroundJobApplication interface {
	// List 返回受约束查询对应的当前页和精确总数。
	List(context.Context, admin.BackgroundJobQuery) (admin.BackgroundJobPage, error)
	// Get 返回一个后台任务的脱敏运维视图。
	Get(context.Context, snowflake.ID) (admin.BackgroundJob, error)
	// Retry 重试任务并持久化审计、幂等响应。
	Retry(context.Context, admin.BackgroundJobOperation) (admin.BackgroundJob, error)
	// Cancel 取消任务并持久化审计、幂等响应。
	Cancel(context.Context, admin.BackgroundJobOperation) (admin.BackgroundJob, error)
	// EnqueueBattleReplayVerification 创建持久 Battle 回放校验任务并持久化审计、幂等响应。
	EnqueueBattleReplayVerification(context.Context, admin.VerificationJobOperation) (admin.BackgroundJob, error)
	// EnqueueAuditHashVerification 创建人工审计哈希链校验任务并持久化审计、幂等响应。
	EnqueueAuditHashVerification(context.Context, admin.VerificationJobOperation) (admin.BackgroundJob, error)
}

// BackgroundScheduleApplication 是动态调度管理 API 使用的应用服务边界。
type BackgroundScheduleApplication interface {
	ListSchedules(context.Context, admin.BackgroundScheduleQuery) (admin.BackgroundSchedulePage, error)
	CreateSchedule(context.Context, admin.BackgroundScheduleInput, admin.BackgroundScheduleMutation) (admin.BackgroundSchedule, error)
	UpdateSchedule(context.Context, snowflake.ID, int64, admin.BackgroundScheduleInput, admin.BackgroundScheduleMutation) (admin.BackgroundSchedule, error)
	SetScheduleEnabled(context.Context, snowflake.ID, int64, bool, admin.BackgroundScheduleMutation) (admin.BackgroundSchedule, error)
}

// BackgroundJobService 实现管理员后台任务查询与人工处置 HTTP 契约。
type BackgroundJobService struct {
	// application 承载任务状态转换、幂等与同事务审计逻辑。
	application BackgroundJobApplication
	// battleOperations 提供与后台任务写模型隔离的 Battle 运维只读查询。
	battleOperations admin.BattleOperationsReader
}

// NewBackgroundJobService 使用显式应用服务创建管理员后台任务 HTTP 服务。
func NewBackgroundJobService(application BackgroundJobApplication) *BackgroundJobService {
	return &BackgroundJobService{application: application}
}

// WithBattleOperations 为 Admin Operations 服务注入 Battle 运维只读查询。
func (s *BackgroundJobService) WithBattleOperations(reader admin.BattleOperationsReader) *BackgroundJobService {
	s.battleOperations = reader
	return s
}

// ListBattles 按页返回 Battle 生命周期运维摘要。
func (s *BackgroundJobService) ListBattles(ctx context.Context, request *adminv1.ListBattlesRequest) (*adminv1.ListBattlesResponse, error) {
	if s == nil || s.battleOperations == nil || request == nil {
		return nil, battleOperationsError(admin.ErrInvalidBattleOperationsQuery)
	}
	query := admin.BattleOperationsQuery{Page: int(request.GetPage()), PageSize: int(request.GetPageSize()), Mode: strings.TrimSpace(request.GetMode()), SourceType: strings.TrimSpace(request.GetSourceType()), Status: strings.TrimSpace(request.GetStatus())}
	page, err := s.battleOperations.ListBattles(ctx, query)
	if err != nil {
		return nil, battleOperationsError(err)
	}
	items := make([]*adminv1.BattleOperationsItem, 0, len(page.Items))
	for _, item := range page.Items {
		items = append(items, battleOperationsItemMessage(item))
	}
	return &adminv1.ListBattlesResponse{Items: items, Total: page.Total, Page: request.GetPage(), PageSize: request.GetPageSize()}, nil
}

// GetBattleOperationsDetail 返回单场 Battle 的 Participant、Lease、Recovery 与 Outbox 状态。
func (s *BackgroundJobService) GetBattleOperationsDetail(ctx context.Context, request *adminv1.GetBattleOperationsDetailRequest) (*adminv1.GetBattleOperationsDetailResponse, error) {
	if s == nil || s.battleOperations == nil || request == nil {
		return nil, battleOperationsError(admin.ErrInvalidBattleOperationsQuery)
	}
	battleID, err := snowflake.Parse(request.GetBattleId())
	if err != nil || battleID == 0 {
		return nil, battleOperationsError(admin.ErrInvalidBattleOperationsQuery)
	}
	detail, err := s.battleOperations.GetBattleOperationsDetail(ctx, battleID)
	if err != nil {
		return nil, battleOperationsError(err)
	}
	participants := make([]*adminv1.BattleOperationsParticipant, 0, len(detail.Participants))
	for _, participant := range detail.Participants {
		message := &adminv1.BattleOperationsParticipant{Side: int32(participant.Side), ParticipantType: participant.ParticipantType, InputType: participant.InputType, DisplayName: participant.DisplayName, BotCode: participant.BotCode}
		if participant.PlayerCharacterID != nil {
			message.PlayerCharacterId = participant.PlayerCharacterID.String()
		}
		participants = append(participants, message)
	}
	recoveryAttempts := make([]*adminv1.BattleRecoveryAttemptView, 0, len(detail.RecoveryAttempts))
	for _, attempt := range detail.RecoveryAttempts {
		recoveryAttempts = append(recoveryAttempts, &adminv1.BattleRecoveryAttemptView{Id: attempt.ID.String(), AttemptNumber: attempt.AttemptNumber, State: attempt.State, AvailableAt: timestamppb.New(attempt.AvailableAt), ClaimedBy: attempt.ClaimedBy, FailureReason: attempt.FailureReason})
	}
	message := &adminv1.BattleOperationsDetail{Battle: battleOperationsItemMessage(detail.Battle), Participants: participants, RecoveryAttempts: recoveryAttempts, PendingOutboxCount: int32(detail.PendingOutboxCount)}
	if detail.RuntimeLease != nil {
		message.RuntimeLease = &adminv1.BattleRuntimeLeaseView{HolderId: detail.RuntimeLease.HolderID, FencingToken: fmt.Sprint(detail.RuntimeLease.FencingToken), LeaseExpiresAt: timestamppb.New(detail.RuntimeLease.ExpiresAt), RenewedAt: timestamppb.New(detail.RuntimeLease.RenewedAt)}
	}
	return &adminv1.GetBattleOperationsDetailResponse{Detail: message}, nil
}

func battleOperationsItemMessage(item admin.BattleOperationsItem) *adminv1.BattleOperationsItem {
	return &adminv1.BattleOperationsItem{BattleId: item.ID.String(), Mode: item.Mode, SourceType: item.SourceType, Status: item.Status, StateVersion: fmt.Sprint(item.StateVersion), TerminalReason: item.TerminalReason, CreatedAt: timestamppb.New(item.CreatedAt), UpdatedAt: timestamppb.New(item.UpdatedAt), StartedAt: timestampOrNil(item.StartedAt), CompletedAt: timestampOrNil(item.CompletedAt)}
}

func battleOperationsError(err error) error {
	switch {
	case errors.Is(err, admin.ErrInvalidBattleOperationsQuery):
		return kratoserrors.BadRequest("INVALID_BATTLE_OPERATIONS_QUERY", "Battle 运维查询参数无效")
	case errors.Is(err, admin.ErrBattleOperationsNotFound):
		return kratoserrors.NotFound("BATTLE_NOT_FOUND", "Battle 不存在")
	default:
		return kratoserrors.InternalServer("BATTLE_OPERATIONS_QUERY_FAILED", "服务端无法读取 Battle 运维状态")
	}
}

// ListBackgroundJobs 按页返回 PostgreSQL 权威任务最小视图及精确总数。
func (s *BackgroundJobService) ListBackgroundJobs(
	ctx context.Context,
	request *adminv1.ListBackgroundJobsRequest,
) (*adminv1.ListBackgroundJobsResponse, error) {
	if s == nil || s.application == nil {
		return nil, kratoserrors.InternalServer("BACKGROUND_JOB_SERVICE_UNAVAILABLE", "服务端无法读取后台任务")
	}
	query, err := backgroundJobQuery(request)
	if err != nil {
		return nil, kratoserrors.BadRequest("INVALID_BACKGROUND_JOB_QUERY", "后台任务查询参数无效")
	}
	page, err := s.application.List(ctx, query)
	if err != nil {
		return nil, backgroundJobError(err)
	}
	jobs, err := backgroundJobMessages(page.Jobs)
	if err != nil {
		return nil, backgroundJobError(err)
	}
	return &adminv1.ListBackgroundJobsResponse{Jobs: jobs, TotalCount: page.TotalCount}, nil
}

// GetBackgroundJob 返回一个后台任务的受控运维视图。
func (s *BackgroundJobService) GetBackgroundJob(
	ctx context.Context,
	request *adminv1.GetBackgroundJobRequest,
) (*adminv1.GetBackgroundJobResponse, error) {
	if s == nil || s.application == nil {
		return nil, kratoserrors.InternalServer("BACKGROUND_JOB_SERVICE_UNAVAILABLE", "服务端无法读取后台任务")
	}
	jobID, parseErr := snowflake.Parse(request.GetJobId())
	if parseErr != nil || jobID == snowflake.ID(0) {
		return nil, kratoserrors.BadRequest("INVALID_BACKGROUND_JOB_ID", "后台任务标识无效")
	}
	job, err := s.application.Get(ctx, jobID)
	if err != nil {
		return nil, backgroundJobError(err)
	}
	message, err := backgroundJobMessage(job)
	if err != nil {
		return nil, backgroundJobError(err)
	}
	return &adminv1.GetBackgroundJobResponse{Job: message}, nil
}

// RetryBackgroundJob 以管理员身份重试一个任务，并使用 HTTP Header 幂等键保护重放。
func (s *BackgroundJobService) RetryBackgroundJob(
	ctx context.Context,
	request *adminv1.RetryBackgroundJobRequest,
) (*adminv1.RetryBackgroundJobResponse, error) {
	job, err := s.mutate(ctx, request.GetJobId(), request.GetHeaderIdempotencyKey(), true)
	if err != nil {
		return nil, err
	}
	return &adminv1.RetryBackgroundJobResponse{Job: job}, nil
}

// CancelBackgroundJob 以管理员身份请求取消一个任务，并使用 HTTP Header 幂等键保护重放。
func (s *BackgroundJobService) CancelBackgroundJob(
	ctx context.Context,
	request *adminv1.CancelBackgroundJobRequest,
) (*adminv1.CancelBackgroundJobResponse, error) {
	job, err := s.mutate(ctx, request.GetJobId(), request.GetHeaderIdempotencyKey(), false)
	if err != nil {
		return nil, err
	}
	return &adminv1.CancelBackgroundJobResponse{Job: job}, nil
}

// EnqueueBattleReplayVerification 以管理员身份创建指定持久 Battle 的严格回放校验任务。
func (s *BackgroundJobService) EnqueueBattleReplayVerification(
	ctx context.Context,
	request *adminv1.EnqueueBattleReplayVerificationRequest,
) (*adminv1.EnqueueBattleReplayVerificationResponse, error) {
	if s == nil || s.application == nil {
		return nil, kratoserrors.InternalServer("BACKGROUND_JOB_SERVICE_UNAVAILABLE", "服务端无法创建回放校验任务")
	}
	principal, ok := authentication.PrincipalFromContext(ctx)
	if !ok {
		return nil, kratoserrors.Unauthorized("SESSION_INVALID", "登录会话无效")
	}
	matchID, err := snowflake.Parse(request.GetBattleId())
	if err != nil || matchID == snowflake.ID(0) {
		return nil, kratoserrors.BadRequest("INVALID_BATTLE_ID", "Battle 标识无效")
	}
	job, err := s.application.EnqueueBattleReplayVerification(ctx, admin.VerificationJobOperation{
		BattleID: matchID, ActorAccountID: principal.AccountID,
		IdempotencyKey: request.GetHeaderIdempotencyKey(), RequestID: requestIDFromContext(ctx),
	})
	if err != nil {
		return nil, backgroundJobError(err)
	}
	message, err := backgroundJobMessage(job)
	if err != nil {
		return nil, backgroundJobError(err)
	}
	return &adminv1.EnqueueBattleReplayVerificationResponse{Job: message}, nil
}

// EnqueueAuditHashChainVerification 以管理员身份创建与每日周期任务共用实现的审计校验任务。
func (s *BackgroundJobService) EnqueueAuditHashChainVerification(
	ctx context.Context,
	request *adminv1.EnqueueAuditHashChainVerificationRequest,
) (*adminv1.EnqueueAuditHashChainVerificationResponse, error) {
	if s == nil || s.application == nil {
		return nil, kratoserrors.InternalServer("BACKGROUND_JOB_SERVICE_UNAVAILABLE", "服务端无法创建审计校验任务")
	}
	principal, ok := authentication.PrincipalFromContext(ctx)
	if !ok {
		return nil, kratoserrors.Unauthorized("SESSION_INVALID", "登录会话无效")
	}
	job, err := s.application.EnqueueAuditHashVerification(ctx, admin.VerificationJobOperation{
		ActorAccountID: principal.AccountID, IdempotencyKey: request.GetHeaderIdempotencyKey(),
		RequestID: requestIDFromContext(ctx),
	})
	if err != nil {
		return nil, backgroundJobError(err)
	}
	message, err := backgroundJobMessage(job)
	if err != nil {
		return nil, backgroundJobError(err)
	}
	return &adminv1.EnqueueAuditHashChainVerificationResponse{Job: message}, nil
}

// ListBackgroundSchedules 按页返回动态调度和精确总数。
func (s *BackgroundJobService) ListBackgroundSchedules(ctx context.Context, request *adminv1.ListBackgroundSchedulesRequest) (*adminv1.ListBackgroundSchedulesResponse, error) {
	application, ok := s.application.(BackgroundScheduleApplication)
	if !ok {
		return nil, backgroundJobError(admin.ErrInvalidBackgroundSchedule)
	}
	page, err := application.ListSchedules(ctx, admin.BackgroundScheduleQuery{
		PageNumber: int(request.GetPageNumber()), PageSize: int(request.GetPageSize()), Enabled: request.Enabled,
	})
	if err != nil {
		return nil, backgroundJobError(err)
	}
	messages := make([]*adminv1.BackgroundSchedule, 0, len(page.Schedules))
	for _, schedule := range page.Schedules {
		messages = append(messages, backgroundScheduleMessage(schedule))
	}
	return &adminv1.ListBackgroundSchedulesResponse{Schedules: messages, TotalCount: page.TotalCount}, nil
}

// CreateBackgroundSchedule 创建默认停用的动态调度。
func (s *BackgroundJobService) CreateBackgroundSchedule(ctx context.Context, request *adminv1.CreateBackgroundScheduleRequest) (*adminv1.CreateBackgroundScheduleResponse, error) {
	application, ok := s.application.(BackgroundScheduleApplication)
	if !ok {
		return nil, backgroundJobError(admin.ErrInvalidBackgroundSchedule)
	}
	input, err := backgroundScheduleInput(request.GetInput())
	if err != nil {
		return nil, backgroundJobError(err)
	}
	mutation, err := backgroundScheduleMutation(ctx)
	if err != nil {
		return nil, err
	}
	schedule, err := application.CreateSchedule(ctx, input, mutation)
	if err != nil {
		return nil, backgroundJobError(err)
	}
	return &adminv1.CreateBackgroundScheduleResponse{Schedule: backgroundScheduleMessage(schedule)}, nil
}

// UpdateBackgroundSchedule 替换指定版本调度的可编辑字段。
func (s *BackgroundJobService) UpdateBackgroundSchedule(ctx context.Context, request *adminv1.UpdateBackgroundScheduleRequest) (*adminv1.UpdateBackgroundScheduleResponse, error) {
	application, ok := s.application.(BackgroundScheduleApplication)
	if !ok {
		return nil, backgroundJobError(admin.ErrInvalidBackgroundSchedule)
	}
	id, err := snowflake.Parse(request.GetScheduleId())
	if err != nil || id == snowflake.ID(0) {
		return nil, backgroundJobError(admin.ErrInvalidBackgroundSchedule)
	}
	input, err := backgroundScheduleInput(request.GetInput())
	if err != nil {
		return nil, backgroundJobError(err)
	}
	mutation, err := backgroundScheduleMutation(ctx)
	if err != nil {
		return nil, err
	}
	schedule, err := application.UpdateSchedule(ctx, id, request.GetExpectedVersion(), input, mutation)
	if err != nil {
		return nil, backgroundJobError(err)
	}
	return &adminv1.UpdateBackgroundScheduleResponse{Schedule: backgroundScheduleMessage(schedule)}, nil
}

// SetBackgroundScheduleEnabled 切换指定版本调度的启停状态。
func (s *BackgroundJobService) SetBackgroundScheduleEnabled(ctx context.Context, request *adminv1.SetBackgroundScheduleEnabledRequest) (*adminv1.SetBackgroundScheduleEnabledResponse, error) {
	application, ok := s.application.(BackgroundScheduleApplication)
	if !ok {
		return nil, backgroundJobError(admin.ErrInvalidBackgroundSchedule)
	}
	id, err := snowflake.Parse(request.GetScheduleId())
	if err != nil || id == snowflake.ID(0) {
		return nil, backgroundJobError(admin.ErrInvalidBackgroundSchedule)
	}
	mutation, err := backgroundScheduleMutation(ctx)
	if err != nil {
		return nil, err
	}
	schedule, err := application.SetScheduleEnabled(ctx, id, request.GetExpectedVersion(), request.GetEnabled(), mutation)
	if err != nil {
		return nil, backgroundJobError(err)
	}
	return &adminv1.SetBackgroundScheduleEnabledResponse{Schedule: backgroundScheduleMessage(schedule)}, nil
}

func backgroundScheduleInput(input *adminv1.BackgroundScheduleInput) (admin.BackgroundScheduleInput, error) {
	if input == nil {
		return admin.BackgroundScheduleInput{}, admin.ErrInvalidBackgroundSchedule
	}
	return admin.BackgroundScheduleInput{
		Code: input.GetCode(), Name: input.GetName(), TaskKind: input.GetTaskKind(),
		ScheduleKind: input.GetScheduleKind(), CronExpression: input.CronExpression,
		IntervalSeconds: input.IntervalSeconds, MissedRunPolicy: input.GetMissedRunPolicy(),
		Parameters: []byte(input.GetParametersJson()),
	}, nil
}

func backgroundScheduleMutation(ctx context.Context) (admin.BackgroundScheduleMutation, error) {
	principal, ok := authentication.PrincipalFromContext(ctx)
	if !ok {
		return admin.BackgroundScheduleMutation{}, kratoserrors.Unauthorized("SESSION_INVALID", "登录会话无效")
	}
	return admin.BackgroundScheduleMutation{
		ActorAccountID: principal.AccountID,
		RequestID:      requestIDFromContext(ctx),
	}, nil
}

func backgroundScheduleMessage(schedule admin.BackgroundSchedule) *adminv1.BackgroundSchedule {
	return &adminv1.BackgroundSchedule{
		Id: schedule.ID.String(), Code: schedule.Code, Name: schedule.Name, TaskKind: schedule.TaskKind,
		ScheduleKind: schedule.ScheduleKind, CronExpression: schedule.CronExpression,
		IntervalSeconds: schedule.IntervalSeconds, MissedRunPolicy: schedule.MissedRunPolicy,
		ParametersJson: string(schedule.Parameters), Enabled: schedule.Enabled,
		NextRunAt: timestampOrNil(schedule.NextRunAt), LastScheduledAt: timestampOrNil(schedule.LastScheduledAt),
		Version: schedule.Version, CreatedAt: timestamppb.New(schedule.CreatedAt), UpdatedAt: timestamppb.New(schedule.UpdatedAt),
	}
}

func (s *BackgroundJobService) mutate(
	ctx context.Context,
	jobID string,
	idempotencyKey string,
	retry bool,
) (*adminv1.BackgroundJob, error) {
	if s == nil || s.application == nil {
		return nil, kratoserrors.InternalServer("BACKGROUND_JOB_SERVICE_UNAVAILABLE", "服务端无法处理后台任务")
	}
	principal, ok := authentication.PrincipalFromContext(ctx)
	if !ok {
		return nil, kratoserrors.Unauthorized("SESSION_INVALID", "登录会话无效")
	}
	parsedJobID, err := snowflake.Parse(jobID)
	if err != nil || parsedJobID == snowflake.ID(0) {
		return nil, kratoserrors.BadRequest("INVALID_BACKGROUND_JOB_ID", "后台任务标识无效")
	}
	operation := admin.BackgroundJobOperation{
		JobID: parsedJobID, ActorAccountID: principal.AccountID, IdempotencyKey: idempotencyKey,
		RequestID: requestIDFromContext(ctx),
	}
	var (
		job          admin.BackgroundJob
		operationErr error
	)
	if retry {
		job, operationErr = s.application.Retry(ctx, operation)
	} else {
		job, operationErr = s.application.Cancel(ctx, operation)
	}
	if operationErr != nil {
		return nil, backgroundJobError(operationErr)
	}
	return backgroundJobMessage(job)
}

func backgroundJobQuery(request *adminv1.ListBackgroundJobsRequest) (admin.BackgroundJobQuery, error) {
	if request == nil {
		return admin.BackgroundJobQuery{}, admin.ErrInvalidBackgroundJobQuery
	}
	state, err := backgroundJobState(request.GetState())
	if err != nil {
		return admin.BackgroundJobQuery{}, err
	}
	return admin.BackgroundJobQuery{
		PageNumber: int(request.GetPageNumber()), PageSize: int(request.GetPageSize()), State: state,
		Kind: strings.TrimSpace(request.GetKind()),
	}, nil
}

func backgroundJobMessages(jobs []admin.BackgroundJob) ([]*adminv1.BackgroundJob, error) {
	result := make([]*adminv1.BackgroundJob, 0, len(jobs))
	for _, job := range jobs {
		message, err := backgroundJobMessage(job)
		if err != nil {
			return nil, err
		}
		result = append(result, message)
	}
	return result, nil
}

func backgroundJobMessage(job admin.BackgroundJob) (*adminv1.BackgroundJob, error) {
	state, err := backgroundJobStateMessage(job.State)
	if err != nil {
		return nil, err
	}
	return &adminv1.BackgroundJob{
		Id: job.ID.String(), Kind: job.Kind, Queue: job.Queue, State: state, Attempt: int32(job.Attempt),
		MaxAttempts: int32(job.MaxAttempts), FailureReason: job.FailureReason,
		CreatedAt: timestamppb.New(job.CreatedAt), ScheduledAt: timestamppb.New(job.ScheduledAt),
		AttemptedAt: timestampOrNil(job.AttemptedAt), FinalizedAt: timestampOrNil(job.FinalizedAt),
		VerificationPassed: job.VerificationPassed, ResultSummary: job.ResultSummary,
	}, nil
}

func backgroundJobState(state adminv1.BackgroundJobState) (admin.BackgroundJobState, error) {
	switch state {
	case adminv1.BackgroundJobState_BACKGROUND_JOB_STATE_UNSPECIFIED:
		return "", nil
	case adminv1.BackgroundJobState_BACKGROUND_JOB_STATE_PENDING:
		return admin.BackgroundJobStatePending, nil
	case adminv1.BackgroundJobState_BACKGROUND_JOB_STATE_QUEUED:
		return admin.BackgroundJobStateQueued, nil
	case adminv1.BackgroundJobState_BACKGROUND_JOB_STATE_SCHEDULED:
		return admin.BackgroundJobStateScheduled, nil
	case adminv1.BackgroundJobState_BACKGROUND_JOB_STATE_RUNNING:
		return admin.BackgroundJobStateRunning, nil
	case adminv1.BackgroundJobState_BACKGROUND_JOB_STATE_RETRY_WAIT:
		return admin.BackgroundJobStateRetryWait, nil
	case adminv1.BackgroundJobState_BACKGROUND_JOB_STATE_CANCELLATION_REQUESTED:
		return admin.BackgroundJobStateCancellationRequested, nil
	case adminv1.BackgroundJobState_BACKGROUND_JOB_STATE_RETRY_REQUESTED:
		return admin.BackgroundJobStateRetryRequested, nil
	case adminv1.BackgroundJobState_BACKGROUND_JOB_STATE_CANCELLED:
		return admin.BackgroundJobStateCancelled, nil
	case adminv1.BackgroundJobState_BACKGROUND_JOB_STATE_COMPLETED:
		return admin.BackgroundJobStateCompleted, nil
	case adminv1.BackgroundJobState_BACKGROUND_JOB_STATE_FAILED:
		return admin.BackgroundJobStateFailed, nil
	default:
		return "", admin.ErrInvalidBackgroundJobQuery
	}
}

func backgroundJobStateMessage(state admin.BackgroundJobState) (adminv1.BackgroundJobState, error) {
	switch state {
	case admin.BackgroundJobStatePending:
		return adminv1.BackgroundJobState_BACKGROUND_JOB_STATE_PENDING, nil
	case admin.BackgroundJobStateQueued:
		return adminv1.BackgroundJobState_BACKGROUND_JOB_STATE_QUEUED, nil
	case admin.BackgroundJobStateScheduled:
		return adminv1.BackgroundJobState_BACKGROUND_JOB_STATE_SCHEDULED, nil
	case admin.BackgroundJobStateRunning:
		return adminv1.BackgroundJobState_BACKGROUND_JOB_STATE_RUNNING, nil
	case admin.BackgroundJobStateRetryWait:
		return adminv1.BackgroundJobState_BACKGROUND_JOB_STATE_RETRY_WAIT, nil
	case admin.BackgroundJobStateCancellationRequested:
		return adminv1.BackgroundJobState_BACKGROUND_JOB_STATE_CANCELLATION_REQUESTED, nil
	case admin.BackgroundJobStateRetryRequested:
		return adminv1.BackgroundJobState_BACKGROUND_JOB_STATE_RETRY_REQUESTED, nil
	case admin.BackgroundJobStateCancelled:
		return adminv1.BackgroundJobState_BACKGROUND_JOB_STATE_CANCELLED, nil
	case admin.BackgroundJobStateCompleted:
		return adminv1.BackgroundJobState_BACKGROUND_JOB_STATE_COMPLETED, nil
	case admin.BackgroundJobStateFailed:
		return adminv1.BackgroundJobState_BACKGROUND_JOB_STATE_FAILED, nil
	default:
		return adminv1.BackgroundJobState_BACKGROUND_JOB_STATE_UNSPECIFIED, fmt.Errorf("后台任务状态 %q 无法映射到 HTTP 契约", state)
	}
}

func timestampOrNil(value *time.Time) *timestamppb.Timestamp {
	if value == nil {
		return nil
	}
	return timestamppb.New(*value)
}

func requestIDFromContext(ctx context.Context) string {
	request, _, ok := httpBoundary(ctx)
	if ok {
		return requestID(request)
	}
	return "background-job-operation"
}

func backgroundJobError(err error) error {
	switch {
	case errors.Is(err, admin.ErrBackgroundJobNotFound):
		return kratoserrors.NotFound("BACKGROUND_JOB_NOT_FOUND", "后台任务不存在")
	case errors.Is(err, admin.ErrInvalidBackgroundJobQuery), errors.Is(err, admin.ErrInvalidBackgroundJobOperation):
		return kratoserrors.BadRequest("INVALID_BACKGROUND_JOB_REQUEST", "后台任务请求无效")
	case errors.Is(err, admin.ErrInvalidBackgroundSchedule):
		return kratoserrors.BadRequest("INVALID_BACKGROUND_SCHEDULE", "后台调度参数无效")
	case errors.Is(err, admin.ErrBackgroundScheduleNotFound):
		return kratoserrors.Conflict("BACKGROUND_SCHEDULE_CONFLICT", "后台调度不存在或版本已经变化")
	case errors.Is(err, idempotency.ErrInvalidKey):
		return kratoserrors.BadRequest("INVALID_IDEMPOTENCY_KEY", "幂等键无效")
	case errors.Is(err, idempotency.ErrConflict):
		return kratoserrors.Conflict("IDEMPOTENCY_CONFLICT", "幂等键已用于不同请求")
	default:
		return kratoserrors.InternalServer("BACKGROUND_JOB_OPERATION_FAILED", "服务端无法处理后台任务")
	}
}
