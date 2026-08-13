package worker

import (
	"context"
	"encoding/json"
	"encoding/json/jsontext"
	"errors"
	"fmt"
	"log/slog"
	"math"
	"strings"
	"sync"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/hibiken/asynq"
	"github.com/lishangbu/avalon/ent"
	"github.com/lishangbu/avalon/ent/backgroundjob"
	"github.com/lishangbu/avalon/ent/backgroundschedule"
	"github.com/lishangbu/avalon/ent/outboxmessage"
	"github.com/lishangbu/avalon/internal/platform/persistence"
	"github.com/lishangbu/avalon/internal/verification"
	"github.com/robfig/cron/v3"
)

const (
	backgroundJobTaskType = "background.job.execute.v1"
	outboxBatchSize       = 100
	outboxLeaseDuration   = 30 * time.Second
	outboxMaximumAttempts = 20
	outboxMaximumBackoff  = 5 * time.Minute
	completedRetention    = 7 * 24 * time.Hour
	maximumTaskTimeout    = 15 * time.Minute
)

// TaskHandler 执行一个由 PostgreSQL 保存参数的白名单任务并返回安全结果。
type TaskHandler func(context.Context, json.RawMessage) (TaskResult, error)

// TaskResult 是允许写入后台任务结果列并展示给管理员的受控结果。
type TaskResult struct {
	// Summary 是不包含任务参数、堆栈或敏感业务内容的简体中文摘要。
	Summary string `json:"summary"`
	// Value 是由具体任务类型解释的安全结构化结果；普通任务可以为空。
	Value any `json:"value,omitempty"`
}

// TaskDefinition 声明一个代码白名单任务的处理器和调度并发语义。
type TaskDefinition struct {
	// Kind 是 PostgreSQL 中保存的稳定任务类型。
	Kind string
	// Singleton 表示同一时刻最多允许一个该类型的非终态任务。
	Singleton bool
	// Handler 执行任务业务逻辑，不直接修改通用任务生命周期。
	Handler TaskHandler
}

// Registry 保存当前二进制明确支持的全部后台任务类型。
type Registry struct {
	definitions map[string]TaskDefinition
}

// NewRegistry 校验并创建不可变任务类型白名单。
func NewRegistry(definitions ...TaskDefinition) (*Registry, error) {
	values := make(map[string]TaskDefinition, len(definitions))
	for _, definition := range definitions {
		if strings.TrimSpace(definition.Kind) == "" || definition.Handler == nil {
			return nil, errors.New("后台任务定义缺少类型或处理器")
		}
		if _, duplicate := values[definition.Kind]; duplicate {
			return nil, fmt.Errorf("后台任务类型 %s 重复注册", definition.Kind)
		}
		values[definition.Kind] = definition
	}
	return &Registry{definitions: values}, nil
}

// Definition 返回稳定任务类型的代码定义。
func (registry *Registry) Definition(kind string) (TaskDefinition, bool) {
	if registry == nil {
		return TaskDefinition{}, false
	}
	definition, exists := registry.definitions[kind]
	return definition, exists
}

// NewDefaultRegistry 将现有 Battle 与校验应用服务组装为后台任务白名单。
func NewDefaultRegistry(
	lifecycle *BattleLifecycleWorker,
	analytics *BattleAnalyticsWorker,
	replay *BattleReplayVerificationWorker,
	audit *AuditHashVerificationWorker,
) (*Registry, error) {
	return NewRegistry(
		TaskDefinition{Kind: BattleLifecycleTaskKind, Singleton: true, Handler: func(ctx context.Context, _ json.RawMessage) (TaskResult, error) {
			if err := lifecycle.Run(ctx); err != nil {
				return TaskResult{}, err
			}
			return TaskResult{Summary: "Battle 生命周期扫描完成"}, nil
		}},
		TaskDefinition{Kind: BattleAnalyticsTaskKind, Singleton: true, Handler: func(ctx context.Context, _ json.RawMessage) (TaskResult, error) {
			if err := analytics.Run(ctx); err != nil {
				return TaskResult{}, err
			}
			return TaskResult{Summary: "Battle 分析投影消费完成"}, nil
		}},
		TaskDefinition{Kind: BattleReplayVerificationTaskKind, Handler: func(ctx context.Context, payload json.RawMessage) (TaskResult, error) {
			var args BattleReplayVerificationArgs
			if err := json.Unmarshal(payload, &args); err != nil {
				return TaskResult{}, fmt.Errorf("解码 Battle 回放校验参数: %w", err)
			}
			result, err := replay.Run(ctx, args)
			return verificationTaskResult(result), err
		}},
		TaskDefinition{Kind: AuditHashVerificationTaskKind, Singleton: true, Handler: func(ctx context.Context, payload json.RawMessage) (TaskResult, error) {
			var args AuditHashVerificationArgs
			if err := json.Unmarshal(payload, &args); err != nil {
				return TaskResult{}, fmt.Errorf("解码审计哈希校验参数: %w", err)
			}
			result, err := audit.Run(ctx, args)
			return verificationTaskResult(result), err
		}},
	)
}

func verificationTaskResult(result verification.Result) TaskResult {
	return TaskResult{Summary: result.Summary, Value: result}
}

// AsynqServerConfig 保存 Worker 进程显式提供的 Valkey 和执行边界。
type AsynqServerConfig struct {
	// Redis 是 Asynq 使用的 Valkey 连接配置。
	Redis asynq.RedisClientOpt
	// Concurrency 是单实例 Worker 同时执行任务的固定数量。
	Concurrency int
	// ShutdownTimeout 是等待在途任务结束的最长时间。
	ShutdownTimeout time.Duration
	// WorkerID 写入每次不可变任务尝试记录。
	WorkerID string
}

// AsynqServer 把 Outbox、动态调度和 Asynq 消费者组装为一个 Kratos 生命周期组件。
type AsynqServer struct {
	database    *persistence.Database
	registry    *Registry
	server      *asynq.Server
	mux         *asynq.ServeMux
	client      *asynq.Client
	inspector   *asynq.Inspector
	logger      *slog.Logger
	workerID    string
	identifiers snowflake.Source
	stopped     chan struct{}
	stopOnce    sync.Once
	cancel      context.CancelFunc
	background  sync.WaitGroup
}

// NewAsynqServer 创建固定队列权重、有限重试和 PostgreSQL 权威状态的 Worker。
func NewAsynqServer(database *persistence.Database, registry *Registry, identifiers snowflake.Source, config AsynqServerConfig, logger *slog.Logger) (*AsynqServer, error) {
	if database == nil || registry == nil || identifiers == nil || config.Concurrency < 1 || strings.TrimSpace(config.WorkerID) == "" {
		return nil, errors.New("Asynq Worker 配置无效")
	}
	server := &AsynqServer{
		database: database, registry: registry, client: asynq.NewClient(config.Redis),
		inspector: asynq.NewInspector(config.Redis), logger: logger, workerID: config.WorkerID, identifiers: identifiers,
		stopped: make(chan struct{}),
	}
	server.server = asynq.NewServer(config.Redis, asynq.Config{
		Concurrency: config.Concurrency,
		Queues:      map[string]int{"critical": 6, "default": 3, "low": 1},
		RetryDelayFunc: func(retryCount int, err error, task *asynq.Task) time.Duration {
			_ = err
			_ = task
			return boundedExponentialBackoff(retryCount, 15*time.Minute)
		},
		ShutdownTimeout: config.ShutdownTimeout,
	})
	server.mux = asynq.NewServeMux()
	server.mux.HandleFunc(backgroundJobTaskType, server.processTask)
	return server, nil
}

// Start 启动 Asynq 消费、Outbox 派发与动态调度，并阻塞到 Stop 完成。
func (server *AsynqServer) Start(ctx context.Context) error {
	if err := server.validateEnabledScheduleKinds(ctx); err != nil {
		return err
	}
	if err := server.server.Start(server.mux); err != nil {
		return fmt.Errorf("启动 Asynq Worker: %w", err)
	}
	backgroundCtx, cancel := context.WithCancel(context.Background())
	server.cancel = cancel
	server.background.Add(2)
	go func() {
		defer server.background.Done()
		server.runOutboxDispatcher(backgroundCtx)
	}()
	go func() {
		defer server.background.Done()
		server.runScheduler(backgroundCtx)
	}()
	if server.logger != nil {
		server.logger.InfoContext(ctx, "Avalon Asynq worker 已就绪")
	}
	<-server.stopped
	return nil
}

// Stop 停止生成和投递新任务，再等待 Asynq 完成在途任务。
func (server *AsynqServer) Stop(ctx context.Context) error {
	if server == nil {
		return nil
	}
	if server.cancel != nil {
		server.cancel()
	}
	server.server.Shutdown()
	done := make(chan struct{})
	go func() {
		server.background.Wait()
		close(done)
	}()
	select {
	case <-ctx.Done():
		return context.Cause(ctx)
	case <-done:
	}
	_ = server.client.Close()
	_ = server.inspector.Close()
	server.stopOnce.Do(func() { close(server.stopped) })
	return nil
}

func (server *AsynqServer) runOutboxDispatcher(ctx context.Context) {
	delay := 500 * time.Millisecond
	for ctx.Err() == nil {
		count, err := server.dispatchOutboxBatch(ctx)
		if err != nil && server.logger != nil {
			server.logger.ErrorContext(ctx, "派发后台任务 Outbox 失败", "error", err.Error())
		}
		if count > 0 {
			delay = 500 * time.Millisecond
			continue
		}
		delay *= 2
		if delay > 5*time.Second {
			delay = 5 * time.Second
		}
		if !waitContext(ctx, delay) {
			return
		}
	}
}

type claimedOutbox struct {
	id           snowflake.ID
	jobID        snowflake.ID
	payload      []byte
	queue        string
	maxAttempts  int
	jobState     string
	attemptCount int
}

func (server *AsynqServer) dispatchOutboxBatch(ctx context.Context) (int, error) {
	var messages []claimedOutbox
	err := server.database.WithinTransaction(ctx, persistence.TransactionOptions{Isolation: persistence.IsolationReadCommitted}, func(transactionCtx context.Context) error {
		client := server.database.Client(transactionCtx)
		now := time.Now().UTC()
		rows, err := client.OutboxMessage.Query().Where(
			outboxmessage.TopicEQ(backgroundJobTaskType),
			outboxmessage.Or(
				outboxmessage.And(outboxmessage.StateEQ("pending"), outboxmessage.AvailableAtLTE(now)),
				outboxmessage.And(outboxmessage.StateEQ("processing"), outboxmessage.LeaseExpiresAtLTE(now)),
			),
		).Order(outboxmessage.ByAvailableAt(), outboxmessage.ByID()).Limit(outboxBatchSize).All(transactionCtx)
		if err != nil {
			return err
		}
		for _, row := range rows {
			leaseUntil := now.Add(outboxLeaseDuration)
			if _, err := client.OutboxMessage.UpdateOneID(row.ID).Where(outboxmessage.StateIn("pending", "processing")).SetState("processing").SetLeaseExpiresAt(leaseUntil).SetUpdatedAt(now).Save(transactionCtx); err != nil {
				return err
			}
			job, err := client.BackgroundJob.Query().Where(backgroundjob.IDEQ(row.AggregateID)).Only(transactionCtx)
			if ent.IsNotFound(err) {
				continue
			}
			if err != nil {
				return err
			}
			messages = append(messages, claimedOutbox{id: row.ID, jobID: row.AggregateID, payload: append([]byte(nil), row.Payload...), queue: job.Queue, maxAttempts: int(job.MaxAttempts), jobState: job.State, attemptCount: int(row.AttemptCount)})
		}
		return nil
	})
	if err != nil {
		return 0, err
	}
	for _, message := range messages {
		if message.jobState == "retry_requested" {
			if err := server.inspector.DeleteTask(message.queue, message.jobID.String()); err != nil && !errors.Is(err, asynq.ErrTaskNotFound) {
				if markErr := server.markOutboxFailed(ctx, message, err); markErr != nil {
					return len(messages), errors.Join(err, markErr)
				}
				continue
			}
		}
		task := asynq.NewTask(backgroundJobTaskType, message.payload)
		_, enqueueErr := server.client.EnqueueContext(ctx, task,
			asynq.TaskID(message.jobID.String()), asynq.Queue(message.queue),
			asynq.MaxRetry(max(message.maxAttempts-1, 0)), asynq.Retention(completedRetention),
			asynq.Timeout(maximumTaskTimeout),
		)
		if enqueueErr == nil || errors.Is(enqueueErr, asynq.ErrTaskIDConflict) {
			if err := server.markOutboxDelivered(ctx, message); err != nil {
				return len(messages), err
			}
			continue
		}
		if err := server.markOutboxFailed(ctx, message, enqueueErr); err != nil {
			return len(messages), err
		}
	}
	return len(messages), nil
}

func (server *AsynqServer) markOutboxDelivered(ctx context.Context, message claimedOutbox) error {
	return server.database.WithinTransaction(ctx, persistence.TransactionOptions{Isolation: persistence.IsolationReadCommitted}, func(txctx context.Context) error {
		client := server.database.Client(txctx)
		if _, err := client.OutboxMessage.Update().Where(outboxmessage.IDEQ(message.id), outboxmessage.StateEQ("processing")).SetState("delivered").SetDeliveredAt(time.Now().UTC()).ClearLeaseExpiresAt().ClearLastError().SetUpdatedAt(time.Now().UTC()).Save(txctx); err != nil {
			return fmt.Errorf("确认 Outbox 已投递: %w", err)
		}
		if _, err := client.BackgroundJob.Update().Where(backgroundjob.IDEQ(message.jobID), backgroundjob.StateIn("pending", "retry_requested", "retry_wait")).SetState("queued").AddVersion(1).SetUpdatedAt(time.Now().UTC()).Save(txctx); err != nil {
			return fmt.Errorf("更新后台任务投递状态: %w", err)
		}
		return nil
	})
}

func (server *AsynqServer) markOutboxFailed(ctx context.Context, message claimedOutbox, cause error) error {
	return server.database.WithinTransaction(ctx, persistence.TransactionOptions{Isolation: persistence.IsolationReadCommitted}, func(txctx context.Context) error {
		client := server.database.Client(txctx)
		row, err := client.OutboxMessage.Query().Where(outboxmessage.IDEQ(message.id), outboxmessage.StateEQ("processing")).Only(txctx)
		if ent.IsNotFound(err) {
			return nil
		}
		if err != nil {
			return err
		}
		attempts := row.AttemptCount + 1
		builder := client.OutboxMessage.UpdateOneID(message.id).SetAttemptCount(attempts).SetAvailableAt(time.Now().UTC().Add(boundedExponentialBackoff(int(attempts), outboxMaximumBackoff))).ClearLeaseExpiresAt().SetLastError(sanitizeError(cause)).SetUpdatedAt(time.Now().UTC())
		if attempts >= outboxMaximumAttempts {
			builder.SetState("dead")
		} else {
			builder.SetState("pending")
		}
		if _, err := builder.Save(txctx); err != nil {
			return fmt.Errorf("记录 Outbox 投递失败: %w", err)
		}
		return nil
	})
}

type taskPayload struct {
	// JobID 是 PostgreSQL、Outbox 与 Asynq 共享的后台任务 Identifier。
	JobID snowflake.ID `json:"jobId"`
}

func (server *AsynqServer) processTask(ctx context.Context, task *asynq.Task) error {
	var payload taskPayload
	if err := json.Unmarshal(task.Payload(), &payload); err != nil {
		return fmt.Errorf("解码后台任务标识: %w", err)
	}
	if !payload.JobID.IsValid() {
		return errors.New("后台任务 Identifier 无效")
	}
	jobID := payload.JobID
	kind, parameters, attemptID, attemptNumber, executable, err := server.claimJob(ctx, jobID)
	if err != nil || !executable {
		return err
	}
	definition, exists := server.registry.Definition(kind)
	if !exists {
		err = fmt.Errorf("当前二进制未注册后台任务类型 %s", kind)
	} else {
		var result TaskResult
		result, err = definition.Handler(ctx, parameters)
		if err == nil {
			return server.completeJob(ctx, jobID, attemptID, attemptNumber, result)
		}
	}
	return server.failJob(ctx, jobID, attemptID, attemptNumber, err)
}

func (server *AsynqServer) claimJob(ctx context.Context, jobID snowflake.ID) (string, json.RawMessage, snowflake.ID, int, bool, error) {
	attemptID, err := server.identifiers.Next(ctx)
	if err != nil {
		return "", nil, 0, 0, false, fmt.Errorf("生成后台任务尝试标识: %w", err)
	}
	var kind string
	var parameters []byte
	var attemptNumber int
	executable := false
	err = server.database.WithinTransaction(ctx, persistence.TransactionOptions{Isolation: persistence.IsolationReadCommitted}, func(transactionCtx context.Context) error {
		var state string
		client := server.database.Client(transactionCtx)
		row, err := client.BackgroundJob.Query().Where(backgroundjob.IDEQ(jobID)).Only(transactionCtx)
		if ent.IsNotFound(err) {
			return nil
		}
		if err != nil {
			return err
		}
		kind, parameters, state, attemptNumber = row.Kind, append([]byte(nil), row.Parameters...), row.State, int(row.AttemptCount)
		if state == "cancelled" || state == "completed" || state == "failed" || state == "cancellation_requested" {
			if state == "cancellation_requested" {
				_, err := client.BackgroundJob.UpdateOneID(jobID).SetState("cancelled").SetCompletedAt(time.Now().UTC()).AddVersion(1).SetUpdatedAt(time.Now().UTC()).Save(transactionCtx)
				return err
			}
			return nil
		}
		attemptNumber++
		if _, err := client.BackgroundJob.UpdateOneID(jobID).SetState("running").SetAttemptCount(int32(attemptNumber)).AddVersion(1).SetUpdatedAt(time.Now().UTC()).Save(transactionCtx); err != nil {
			return err
		}
		if _, err := client.BackgroundJobAttempt.Create().SetID(attemptID).SetJobID(jobID).SetAttemptNumber(int32(attemptNumber)).SetTrigger("automatic").SetState("running").SetWorkerID(server.workerID).SetStartedAt(time.Now().UTC()).SetCreatedAt(time.Now().UTC()).Save(transactionCtx); err != nil {
			return err
		}
		executable = true
		return nil
	})
	return kind, parameters, attemptID, attemptNumber, executable, err
}

func (server *AsynqServer) completeJob(ctx context.Context, jobID snowflake.ID, attemptID snowflake.ID, attemptNumber int, result TaskResult) error {
	var resultJSON []byte
	if result.Value != nil {
		var err error
		resultJSON, err = json.Marshal(result.Value)
		if err != nil {
			return fmt.Errorf("编码后台任务结果: %w", err)
		}
	}
	return server.database.WithinTransaction(ctx, persistence.TransactionOptions{Isolation: persistence.IsolationReadCommitted}, func(transactionCtx context.Context) error {
		client := server.database.Client(transactionCtx)
		row, err := client.BackgroundJob.Query().Where(backgroundjob.IDEQ(jobID), backgroundjob.AttemptCountEQ(int32(attemptNumber))).Only(transactionCtx)
		if err != nil {
			return err
		}
		state := "completed"
		if row.State == "cancellation_requested" {
			state = "cancelled"
		}
		builder := client.BackgroundJob.UpdateOneID(jobID).SetState(state).SetResultSummary(sanitizedText(result.Summary, 500)).SetResult(resultJSON).SetCompletedAt(time.Now().UTC()).AddVersion(1).SetUpdatedAt(time.Now().UTC())
		if _, err := builder.Save(transactionCtx); err != nil {
			return err
		}
		attemptState := "completed"
		if state == "cancelled" {
			attemptState = "cancelled"
		}
		_, err = client.BackgroundJobAttempt.UpdateOneID(attemptID).SetState(attemptState).SetFinishedAt(time.Now().UTC()).Save(transactionCtx)
		return err
	})
}

func (server *AsynqServer) failJob(ctx context.Context, jobID snowflake.ID, attemptID snowflake.ID, attemptNumber int, cause error) error {
	retryCount, _ := asynq.GetRetryCount(ctx)
	maxRetry, _ := asynq.GetMaxRetry(ctx)
	terminal := retryCount >= maxRetry
	nextAttempt := time.Now().UTC().Add(boundedExponentialBackoff(retryCount+1, 15*time.Minute))
	if err := server.database.WithinTransaction(ctx, persistence.TransactionOptions{Isolation: persistence.IsolationReadCommitted}, func(transactionCtx context.Context) error {
		state := "retry_wait"
		client := server.database.Client(transactionCtx)
		if terminal {
			state = "failed"
		}
		_, err := client.BackgroundJob.Query().Where(backgroundjob.IDEQ(jobID), backgroundjob.AttemptCountEQ(int32(attemptNumber))).Only(transactionCtx)
		if err != nil {
			return err
		}
		builder := client.BackgroundJob.UpdateOneID(jobID).SetState(state).SetResultSummary(sanitizeError(cause)).AddVersion(1).SetUpdatedAt(time.Now().UTC())
		if terminal {
			builder.SetCompletedAt(time.Now().UTC()).ClearNextAttemptAt()
		} else {
			builder.SetNextAttemptAt(nextAttempt).ClearCompletedAt()
		}
		if _, err := builder.Save(transactionCtx); err != nil {
			return err
		}
		_, err = client.BackgroundJobAttempt.UpdateOneID(attemptID).SetState("failed").SetErrorSummary(sanitizeError(cause)).SetFinishedAt(time.Now().UTC()).Save(transactionCtx)
		return err
	}); err != nil {
		return errors.Join(cause, err)
	}
	return cause
}

func (server *AsynqServer) validateEnabledScheduleKinds(ctx context.Context) error {
	rows, err := server.database.Client(ctx).BackgroundSchedule.Query().Where(backgroundschedule.EnabledEQ(true)).Order(backgroundschedule.ByCode()).All(ctx)
	if err != nil {
		return fmt.Errorf("读取启用的动态调度: %w", err)
	}
	for _, row := range rows {
		if _, exists := server.registry.Definition(row.TaskKind); !exists {
			return fmt.Errorf("启用的动态调度 %s 使用未知任务类型 %s", row.Code, row.TaskKind)
		}
	}
	return nil
}

func (server *AsynqServer) runScheduler(ctx context.Context) {
	ticker := time.NewTicker(time.Second)
	defer ticker.Stop()
	for {
		select {
		case <-ctx.Done():
			return
		case now := <-ticker.C:
			if err := server.scheduleDue(ctx, now.UTC()); err != nil && server.logger != nil {
				server.logger.ErrorContext(ctx, "生成动态调度任务失败", "error", err.Error())
			}
		}
	}
}

type scheduleRow struct {
	id              snowflake.ID
	kind            string
	scheduleKind    string
	cronExpression  *string
	intervalSeconds *int32
	missedPolicy    string
	parameters      []byte
	nextRunAt       time.Time
}

func (server *AsynqServer) scheduleDue(ctx context.Context, now time.Time) error {
	return server.database.WithinTransaction(ctx, persistence.TransactionOptions{Isolation: persistence.IsolationReadCommitted}, func(transactionCtx context.Context) error {
		client := server.database.Client(transactionCtx)
		rows, err := client.BackgroundSchedule.Query().Where(backgroundschedule.EnabledEQ(true), backgroundschedule.NextRunAtLTE(now)).Order(backgroundschedule.ByNextRunAt(), backgroundschedule.ByID()).Limit(100).All(transactionCtx)
		if err != nil {
			return err
		}
		var schedules []scheduleRow
		for _, row := range rows {
			if row.NextRunAt == nil {
				continue
			}
			value := scheduleRow{id: row.ID, kind: row.TaskKind, scheduleKind: row.ScheduleKind, missedPolicy: row.MissedRunPolicy, parameters: append([]byte(nil), row.Parameters...), nextRunAt: row.NextRunAt.UTC()}
			value.cronExpression = row.CronExpression
			value.intervalSeconds = row.IntervalSeconds
			schedules = append(schedules, value)
		}
		for _, schedule := range schedules {
			definition, exists := server.registry.Definition(schedule.kind)
			if !exists {
				return fmt.Errorf("启用的调度使用未知任务类型 %s", schedule.kind)
			}
			occurrences, nextRunAt, err := dueOccurrences(schedule, now)
			if err != nil {
				return err
			}
			for _, scheduledFor := range occurrences {
				if definition.Singleton {
					active, err := server.database.Client(transactionCtx).BackgroundJob.Query().Where(backgroundjob.KindEQ(schedule.kind), backgroundjob.StateNotIn("completed", "cancelled", "failed")).Exist(transactionCtx)
					if err != nil {
						return err
					}
					if active {
						continue
					}
				}
				jobID, idErr := server.identifiers.Next(transactionCtx)
				if idErr != nil {
					return idErr
				}
				inserted, err := persistence.InsertBackgroundJobOccurrence(transactionCtx, server.database.SQL(transactionCtx), jobID, schedule.kind, schedule.parameters, schedule.id, scheduledFor, now)
				if err != nil {
					return err
				}
				if inserted {
					payload, _ := json.Marshal(taskPayload{JobID: jobID})
					outboxID, idErr := server.identifiers.Next(transactionCtx)
					if idErr != nil {
						return idErr
					}
					if err := server.database.Client(transactionCtx).OutboxMessage.Create().SetID(outboxID).SetTopic(backgroundJobTaskType).SetAggregateID(jobID).SetPayload(jsontext.Value(payload)).SetState("pending").SetAttemptCount(0).SetAvailableAt(scheduledFor).SetCreatedAt(now.UTC()).SetUpdatedAt(now.UTC()).Exec(transactionCtx); err != nil {
						return err
					}
				}
			}
			builder := server.database.Client(transactionCtx).BackgroundSchedule.UpdateOneID(schedule.id).SetNextRunAt(nextRunAt).AddVersion(1).SetUpdatedAt(now.UTC())
			builder.SetNillableLastScheduledAt(nullableLastOccurrence(occurrences))
			if _, err := builder.Save(transactionCtx); err != nil {
				return err
			}
		}
		return nil
	})
}

func dueOccurrences(schedule scheduleRow, now time.Time) ([]time.Time, time.Time, error) {
	next := func(value time.Time) (time.Time, error) {
		if schedule.scheduleKind == "interval" && schedule.intervalSeconds != nil {
			return value.Add(time.Duration(*schedule.intervalSeconds) * time.Second), nil
		}
		if schedule.scheduleKind == "cron" && schedule.cronExpression != nil {
			location, err := time.LoadLocation("Asia/Shanghai")
			if err != nil {
				return time.Time{}, err
			}
			parsed, err := cron.ParseStandard(*schedule.cronExpression)
			if err != nil {
				return time.Time{}, fmt.Errorf("解析五段 Cron %q: %w", *schedule.cronExpression, err)
			}
			return parsed.Next(value.In(location)).UTC(), nil
		}
		return time.Time{}, errors.New("动态调度表达式无效")
	}
	var due []time.Time
	cursor := schedule.nextRunAt.UTC()
	for !cursor.After(now) && len(due) < 100 {
		due = append(due, cursor)
		value, err := next(cursor)
		if err != nil {
			return nil, time.Time{}, err
		}
		cursor = value
	}
	switch schedule.missedPolicy {
	case "skip":
		if len(due) > 1 {
			due = nil
		}
	case "coalesce":
		if len(due) > 1 {
			due = due[len(due)-1:]
		}
	case "catch_up":
	default:
		return nil, time.Time{}, fmt.Errorf("未知错过周期策略 %s", schedule.missedPolicy)
	}
	return due, cursor, nil
}

func nullableLastOccurrence(values []time.Time) *time.Time {
	if len(values) == 0 {
		return nil
	}
	value := values[len(values)-1]
	return &value
}

func boundedExponentialBackoff(attempt int, maximum time.Duration) time.Duration {
	if attempt < 1 {
		attempt = 1
	}
	seconds := math.Pow(2, float64(min(attempt-1, 20)))
	delay := time.Duration(seconds) * time.Second
	if delay > maximum {
		return maximum
	}
	return delay
}

func sanitizeError(err error) string {
	if err == nil {
		return ""
	}
	return sanitizedText(err.Error(), 1000)
}

func sanitizedText(value string, maximum int) string {
	value = strings.TrimSpace(strings.Map(func(character rune) rune {
		if character < 0x20 && character != '\n' && character != '\t' {
			return -1
		}
		return character
	}, value))
	characters := []rune(value)
	if len(characters) > maximum {
		characters = characters[:maximum]
	}
	return string(characters)
}

func waitContext(ctx context.Context, duration time.Duration) bool {
	timer := time.NewTimer(duration)
	defer timer.Stop()
	select {
	case <-ctx.Done():
		return false
	case <-timer.C:
		return true
	}
}
