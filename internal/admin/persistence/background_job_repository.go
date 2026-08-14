package persistence

import (
	"context"
	"encoding/json"
	"encoding/json/jsontext"
	"errors"
	"fmt"
	"strings"
	"time"
	"unicode/utf8"

	entsql "entgo.io/ent/dialect/sql"
	"github.com/lishangbu/avalon/ent"
	"github.com/lishangbu/avalon/ent/adminidempotencyrecord"
	"github.com/lishangbu/avalon/ent/backgroundjob"
	"github.com/lishangbu/avalon/ent/backgroundjobattempt"
	"github.com/lishangbu/avalon/ent/outboxmessage"
	"github.com/lishangbu/avalon/internal/admin"
	platformaudit "github.com/lishangbu/avalon/internal/platform/audit"
	"github.com/lishangbu/avalon/internal/platform/database"
	"github.com/lishangbu/avalon/internal/platform/idempotency"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
	"github.com/lishangbu/avalon/internal/verification"
	"github.com/lishangbu/avalon/internal/worker"
)

const (
	maximumFailureReasonLength = 512
	maximumResultSummaryLength = 256
	backgroundJobTopic         = "background.job.execute.v1"
)

// backgroundJobRepository 将 PostgreSQL 权威任务、Outbox、管理员审计和幂等响应放入同一 Ent 事务。
type backgroundJobRepository struct {
	// pool 提供共享的 Ent Client、连接池和事务上下文。
	pool *database.Pool
	// newID 为任务、Outbox 和审计事实生成 Snowflake Identifier。
	newID snowflake.Source
}

// NewBackgroundJobRepository 创建不直接连接 Valkey 的管理端后台任务持久化适配器。
func NewBackgroundJobRepository(pool *database.Pool, newID snowflake.Source) *backgroundJobRepository {
	return &backgroundJobRepository{pool: pool, newID: newID}
}

// List 按页返回 PostgreSQL 权威任务及精确总数。
func (repository *backgroundJobRepository) List(ctx context.Context, query admin.BackgroundJobListQuery) (admin.BackgroundJobPage, error) {
	if repository == nil || repository.pool == nil {
		return admin.BackgroundJobPage{}, errors.New("后台任务存储未配置")
	}
	q := repository.pool.Client(ctx).BackgroundJob.Query()
	if query.State != "" {
		q = q.Where(backgroundjob.StateEQ(string(query.State)))
	}
	if query.Kind != "" {
		q = q.Where(backgroundjob.KindEQ(query.Kind))
	}
	total, err := q.Count(ctx)
	if err != nil {
		return admin.BackgroundJobPage{}, fmt.Errorf("统计后台任务: %w", err)
	}
	rows, err := q.WithAttempts(func(aq *ent.BackgroundJobAttemptQuery) {
		aq.Order(backgroundjobattempt.ByStartedAt(entsql.OrderDesc()))
	}).Order(backgroundjob.ByCreatedAt(entsql.OrderDesc()), backgroundjob.ByID(entsql.OrderDesc())).Limit(query.PageSize).Offset((query.PageNumber - 1) * query.PageSize).All(ctx)
	if err != nil {
		return admin.BackgroundJobPage{}, fmt.Errorf("查询后台任务: %w", err)
	}
	jobs := make([]admin.BackgroundJob, 0, len(rows))
	for _, row := range rows {
		jobs = append(jobs, backgroundJobValue(row))
	}
	return admin.BackgroundJobPage{Jobs: jobs, TotalCount: int64(total)}, nil
}

// Get 返回指定 Snowflake Identifier 后台任务的受控管理视图。
func (repository *backgroundJobRepository) Get(ctx context.Context, jobID snowflake.ID) (admin.BackgroundJob, error) {
	if repository == nil || repository.pool == nil || jobID == snowflake.ID(0) {
		return admin.BackgroundJob{}, admin.ErrBackgroundJobNotFound
	}
	row, err := repository.pool.Client(ctx).BackgroundJob.Query().Where(backgroundjob.IDEQ(jobID)).WithAttempts(func(aq *ent.BackgroundJobAttemptQuery) {
		aq.Order(backgroundjobattempt.ByStartedAt(entsql.OrderDesc()))
	}).Only(ctx)
	if ent.IsNotFound(err) {
		return admin.BackgroundJob{}, admin.ErrBackgroundJobNotFound
	}
	if err != nil {
		return admin.BackgroundJob{}, err
	}
	return backgroundJobValue(row), nil
}

// Retry 在同一事务中请求重试、恢复 Outbox 并追加管理员审计。
func (repository *backgroundJobRepository) Retry(ctx context.Context, operation admin.BackgroundJobOperation) (admin.BackgroundJob, error) {
	return repository.mutate(ctx, operation, "admin.background_job.retry", "retry_requested")
}

// Cancel 在同一事务中请求取消并追加管理员审计。
func (repository *backgroundJobRepository) Cancel(ctx context.Context, operation admin.BackgroundJobOperation) (admin.BackgroundJob, error) {
	return repository.mutate(ctx, operation, "admin.background_job.cancel", "cancellation_requested")
}

// EnqueueBattleReplayVerification 创建严格回放校验任务及其可靠 Outbox。
func (repository *backgroundJobRepository) EnqueueBattleReplayVerification(ctx context.Context, operation admin.VerificationJobOperation) (admin.BackgroundJob, error) {
	parameters := worker.BattleReplayVerificationArgs{BattleID: operation.BattleID, ActorAccountID: operation.ActorAccountID, RequestID: operation.RequestID}
	return repository.enqueueVerification(ctx, operation, "admin.verification.battle_replay.enqueue", "verification.battle-replay.v1", parameters)
}

// EnqueueAuditHashVerification 创建审计哈希链校验任务及其可靠 Outbox。
func (repository *backgroundJobRepository) EnqueueAuditHashVerification(ctx context.Context, operation admin.VerificationJobOperation) (admin.BackgroundJob, error) {
	parameters := worker.AuditHashVerificationArgs{Trigger: "manual", ActorAccountID: &operation.ActorAccountID, RequestID: operation.RequestID}
	return repository.enqueueVerification(ctx, operation, "admin.verification.audit_hash_chain.enqueue", "verification.audit-hash-chain.v1", parameters)
}

func (repository *backgroundJobRepository) enqueueVerification(ctx context.Context, operation admin.VerificationJobOperation, operationID, kind string, parameters any) (admin.BackgroundJob, error) {
	if repository == nil || repository.pool == nil || !idempotency.ValidKey(operation.IdempotencyKey) {
		return admin.BackgroundJob{}, admin.ErrInvalidBackgroundJobOperation
	}
	parameterJSON, err := json.Marshal(parameters)
	if err != nil {
		return admin.BackgroundJob{}, fmt.Errorf("编码后台任务参数: %w", err)
	}
	digest, err := idempotency.Digest(parameters)
	if err != nil {
		return admin.BackgroundJob{}, fmt.Errorf("计算后台任务幂等摘要: %w", err)
	}
	jobID, idErr := repository.newID.Next(ctx)
	if idErr != nil {
		return admin.BackgroundJob{}, idErr
	}
	var result admin.BackgroundJob
	err = repository.pool.WithinTransaction(ctx, func(txctx context.Context) error {
		replayed, claimErr := claimBackgroundJobResponse(txctx, repository.pool.Client(txctx), repository.newID, operation.ActorAccountID, operationID, operation.IdempotencyKey, digest, operation.OccurredAt, &result)
		if claimErr != nil || replayed {
			return claimErr
		}
		row, createErr := repository.pool.Client(txctx).BackgroundJob.Create().SetID(jobID).SetKind(kind).SetQueue("default").SetState("pending").SetParameters(jsontext.Value(parameterJSON)).SetAttemptCount(0).SetMaxAttempts(10).SetVersion(1).SetCreatedAt(operation.OccurredAt.UTC()).SetUpdatedAt(operation.OccurredAt.UTC()).Save(txctx)
		if createErr != nil {
			return fmt.Errorf("创建 PostgreSQL 后台任务: %w", createErr)
		}
		outboxID, idErr := repository.newID.Next(txctx)
		if idErr != nil {
			return idErr
		}
		if err := insertBackgroundJobOutbox(txctx, repository.pool.Client(txctx), outboxID, jobID, operation.OccurredAt.UTC()); err != nil {
			return err
		}
		auditID, idErr := repository.newID.Next(txctx)
		if idErr != nil {
			return idErr
		}
		if err := insertBackgroundJobAudit(txctx, database.Executor(txctx, nil), auditID, operation.ActorAccountID, operationID, jobID, operation.RequestID, operation.OccurredAt); err != nil {
			return err
		}
		result = backgroundJobValue(row)
		return completeBackgroundJobResponse(txctx, repository.pool.Client(txctx), operation.ActorAccountID, operationID, operation.IdempotencyKey, digest, result)
	})
	return result, err
}

func (repository *backgroundJobRepository) mutate(ctx context.Context, operation admin.BackgroundJobOperation, operationID, requestedState string) (admin.BackgroundJob, error) {
	if repository == nil || repository.pool == nil || !idempotency.ValidKey(operation.IdempotencyKey) {
		return admin.BackgroundJob{}, admin.ErrInvalidBackgroundJobOperation
	}
	digest, err := idempotency.Digest(struct {
		JobID snowflake.ID `json:"jobId"`
	}{operation.JobID})
	if err != nil {
		return admin.BackgroundJob{}, fmt.Errorf("计算后台任务幂等摘要: %w", err)
	}
	var result admin.BackgroundJob
	err = repository.pool.WithinTransaction(ctx, func(txctx context.Context) error {
		client := repository.pool.Client(txctx)
		replayed, claimErr := claimBackgroundJobResponse(txctx, client, repository.newID, operation.ActorAccountID, operationID, operation.IdempotencyKey, digest, operation.OccurredAt, &result)
		if claimErr != nil || replayed {
			return claimErr
		}
		q := client.BackgroundJob.Update().Where(backgroundjob.IDEQ(operation.JobID))
		if requestedState == "retry_requested" {
			q = q.Where(backgroundjob.StateIn("failed", "cancelled")).SetState("retry_requested").SetNextAttemptAt(operation.OccurredAt.UTC()).ClearCompletedAt().ClearResultSummary().ClearResult().AddVersion(1).SetUpdatedAt(operation.OccurredAt.UTC())
		} else {
			row, findErr := client.BackgroundJob.Query().Where(backgroundjob.IDEQ(operation.JobID)).Only(txctx)
			if ent.IsNotFound(findErr) {
				return admin.ErrBackgroundJobNotFound
			}
			if findErr != nil {
				return findErr
			}
			q = q.Where(backgroundjob.StateNotIn("completed", "cancelled", "failed")).SetCancellationRequestedAt(operation.OccurredAt.UTC()).AddVersion(1).SetUpdatedAt(operation.OccurredAt.UTC())
			if row.State != "running" {
				q.SetState("cancelled").SetCompletedAt(operation.OccurredAt.UTC())
			} else {
				q.SetState("cancellation_requested")
			}
		}
		if affected, updateErr := q.Save(txctx); updateErr != nil {
			return updateErr
		} else if affected != 1 {
			return admin.ErrBackgroundJobNotFound
		}
		if requestedState == "retry_requested" {
			if _, err := client.OutboxMessage.Update().Where(outboxmessage.TopicEQ(backgroundJobTopic), outboxmessage.AggregateIDEQ(operation.JobID)).SetState("pending").SetAttemptCount(0).SetAvailableAt(operation.OccurredAt.UTC()).ClearLeaseExpiresAt().ClearDeliveredAt().ClearLastError().SetUpdatedAt(operation.OccurredAt.UTC()).Save(txctx); err != nil {
				return fmt.Errorf("恢复后台任务 Outbox: %w", err)
			}
		}
		auditID, idErr := repository.newID.Next(txctx)
		if idErr != nil {
			return idErr
		}
		if err := insertBackgroundJobAudit(txctx, database.Executor(txctx, nil), auditID, operation.ActorAccountID, operationID, operation.JobID, operation.RequestID, operation.OccurredAt); err != nil {
			return err
		}
		row, err := client.BackgroundJob.Query().Where(backgroundjob.IDEQ(operation.JobID)).WithAttempts(func(aq *ent.BackgroundJobAttemptQuery) {
			aq.Order(backgroundjobattempt.ByStartedAt(entsql.OrderDesc()))
		}).Only(txctx)
		if err != nil {
			return err
		}
		result = backgroundJobValue(row)
		return completeBackgroundJobResponse(txctx, client, operation.ActorAccountID, operationID, operation.IdempotencyKey, digest, result)
	})
	return result, err
}

func insertBackgroundJobOutbox(ctx context.Context, client *ent.Client, outboxID, jobID snowflake.ID, occurredAt time.Time) error {
	payload, _ := json.Marshal(struct {
		JobID snowflake.ID `json:"jobId"`
	}{jobID})
	if err := client.OutboxMessage.Create().SetID(outboxID).SetTopic(backgroundJobTopic).SetAggregateID(jobID).SetPayload(jsontext.Value(payload)).SetState("pending").SetAttemptCount(0).SetAvailableAt(occurredAt.UTC()).SetCreatedAt(occurredAt.UTC()).SetUpdatedAt(occurredAt.UTC()).Exec(ctx); err != nil {
		return fmt.Errorf("创建后台任务 Outbox: %w", err)
	}
	return nil
}

func insertBackgroundJobAudit(ctx context.Context, transaction database.Transaction, auditID, actorID snowflake.ID, actionCode string, jobID snowflake.ID, requestID string, occurredAt time.Time) error {
	changes, _ := json.Marshal(struct {
		JobID snowflake.ID `json:"jobId"`
	}{jobID})
	objectID, reason := jobID.String(), "administrative_operation"
	if err := platformaudit.Append(ctx, transaction, platformaudit.AdminLedger, platformaudit.Entry{ID: auditID, ActorAccountID: &actorID, ActorKind: "admin", ActionCode: actionCode, ObjectType: "background_job", ObjectID: &objectID, RequestID: requestID, Reason: &reason, Changes: changes, CreatedAt: occurredAt.UTC()}); err != nil {
		return fmt.Errorf("写入后台任务管理员审计: %w", err)
	}
	return nil
}

func claimBackgroundJobResponse(ctx context.Context, client *ent.Client, identifiers snowflake.Source, actorID snowflake.ID, operationID, key string, digest []byte, occurredAt time.Time, result *admin.BackgroundJob) (bool, error) {
	// 使用 Ent upsert 的 ON CONFLICT DO NOTHING 认领幂等键，避免原生 SQL 绕过 Ent 事务。
	id, err := identifiers.Next(ctx)
	if err != nil {
		return false, fmt.Errorf("生成后台任务幂等记录标识: %w", err)
	}
	if err := client.AdminIdempotencyRecord.Create().
		SetID(id).
		SetActorAccountID(actorID).
		SetOperationID(operationID).
		SetIdempotencyKey(key).
		SetRequestDigest(digest).
		SetCreatedAt(occurredAt.UTC()).
		OnConflictColumns(adminidempotencyrecord.FieldActorAccountID, adminidempotencyrecord.FieldOperationID, adminidempotencyrecord.FieldIdempotencyKey).
		Ignore().
		Exec(ctx); err != nil {
		return false, fmt.Errorf("声明后台任务幂等请求: %w", err)
	}
	row, err := client.AdminIdempotencyRecord.Query().Where(adminidempotencyrecord.ActorAccountIDEQ(actorID), adminidempotencyrecord.OperationIDEQ(operationID), adminidempotencyrecord.IdempotencyKeyEQ(key)).Only(ctx)
	if err != nil {
		return false, fmt.Errorf("读取后台任务幂等请求: %w", err)
	}
	if string(row.RequestDigest) != string(digest) {
		return false, idempotency.ErrConflict
	}
	if len(row.Response) == 0 {
		return false, nil
	}
	if err := json.Unmarshal(row.Response, result); err != nil {
		return false, fmt.Errorf("解码后台任务幂等响应: %w", err)
	}
	return true, nil
}

func completeBackgroundJobResponse(ctx context.Context, client *ent.Client, actorID snowflake.ID, operationID, key string, digest []byte, result admin.BackgroundJob) error {
	response, err := json.Marshal(result)
	if err != nil {
		return fmt.Errorf("编码后台任务幂等响应: %w", err)
	}
	count, err := client.AdminIdempotencyRecord.Update().Where(adminidempotencyrecord.ActorAccountIDEQ(actorID), adminidempotencyrecord.OperationIDEQ(operationID), adminidempotencyrecord.IdempotencyKeyEQ(key), adminidempotencyrecord.RequestDigestEQ(digest)).SetResponse(jsontext.Value(response)).Save(ctx)
	if err != nil {
		return err
	}
	if count != 1 {
		return errors.New("后台任务幂等响应未保存")
	}
	return nil
}

func backgroundJobValue(row *ent.BackgroundJob) admin.BackgroundJob {
	value := admin.BackgroundJob{ID: snowflake.ID(row.ID), Kind: row.Kind, Queue: row.Queue, State: admin.BackgroundJobState(row.State), Attempt: int(row.AttemptCount), MaxAttempts: int(row.MaxAttempts), ResultSummary: sanitizedText(ptrString(row.ResultSummary), maximumResultSummaryLength), CreatedAt: row.CreatedAt.UTC()}
	if row.NextAttemptAt != nil {
		value.ScheduledAt = row.NextAttemptAt.UTC()
	} else {
		value.ScheduledAt = row.CreatedAt.UTC()
	}
	if row.CompletedAt != nil {
		t := row.CompletedAt.UTC()
		value.FinalizedAt = &t
	}
	if len(row.Result) > 0 {
		var verificationResult verification.Result
		if json.Unmarshal(row.Result, &verificationResult) == nil {
			value.VerificationPassed = &verificationResult.Passed
			value.FailureReason = sanitizedText(verificationResult.Summary, maximumFailureReasonLength)
			if value.ResultSummary == "" {
				value.ResultSummary = sanitizedText(verificationResult.Summary, maximumResultSummaryLength)
			}
		}
	}
	if len(row.Edges.Attempts) > 0 {
		t := row.Edges.Attempts[0].StartedAt.UTC()
		value.AttemptedAt = &t
	}
	return value
}

func ptrString(value *string) string {
	if value == nil {
		return ""
	}
	return *value
}

func sanitizedText(value string, maximumLength int) string {
	value = strings.Map(func(character rune) rune {
		if character < 0x20 && character != '\n' && character != '\t' {
			return -1
		}
		return character
	}, strings.TrimSpace(value))
	for utf8.RuneCountInString(value) > maximumLength {
		_, size := utf8.DecodeLastRuneInString(value)
		value = value[:len(value)-size]
	}
	return value
}
