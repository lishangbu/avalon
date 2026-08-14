package persistence

import (
	"context"
	"encoding/json"
	"encoding/json/jsontext"
	"errors"
	"fmt"
	"strings"
	"time"

	"github.com/lishangbu/avalon/ent"
	"github.com/lishangbu/avalon/ent/backgroundschedule"
	"github.com/lishangbu/avalon/internal/admin"
	"github.com/lishangbu/avalon/internal/backgroundtask"
	platformaudit "github.com/lishangbu/avalon/internal/platform/audit"
	"github.com/lishangbu/avalon/internal/platform/database"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
	"github.com/robfig/cron/v3"
)

// ListSchedules 按编码稳定排序返回动态调度页和精确总数。
func (repository *backgroundJobRepository) ListSchedules(ctx context.Context, query admin.BackgroundScheduleListQuery) (admin.BackgroundSchedulePage, error) {
	if repository == nil || repository.pool == nil {
		return admin.BackgroundSchedulePage{}, errors.New("后台调度存储未配置")
	}
	q := repository.pool.Client(ctx).BackgroundSchedule.Query()
	if query.Enabled != nil {
		q = q.Where(backgroundschedule.EnabledEQ(*query.Enabled))
	}
	total, err := q.Count(ctx)
	if err != nil {
		return admin.BackgroundSchedulePage{}, fmt.Errorf("统计后台调度: %w", err)
	}
	rows, err := q.Order(backgroundschedule.ByCode(), backgroundschedule.ByID()).Offset((query.PageNumber - 1) * query.PageSize).Limit(query.PageSize).All(ctx)
	if err != nil {
		return admin.BackgroundSchedulePage{}, fmt.Errorf("查询后台调度: %w", err)
	}
	values := make([]admin.BackgroundSchedule, 0, len(rows))
	for _, row := range rows {
		values = append(values, backgroundScheduleValue(row))
	}
	return admin.BackgroundSchedulePage{Schedules: values, TotalCount: int64(total)}, nil
}

// CreateSchedule 创建默认停用且尚无 next_run_at 的动态调度。
func (repository *backgroundJobRepository) CreateSchedule(ctx context.Context, input admin.BackgroundScheduleInput, mutation admin.BackgroundScheduleMutation) (admin.BackgroundSchedule, error) {
	if repository == nil || repository.pool == nil || !validBackgroundScheduleInput(input) {
		return admin.BackgroundSchedule{}, admin.ErrInvalidBackgroundSchedule
	}
	id, idErr := repository.newID.Next(ctx)
	if idErr != nil {
		return admin.BackgroundSchedule{}, idErr
	}
	var result admin.BackgroundSchedule
	err := repository.pool.WithinTransaction(ctx, func(transactionCtx context.Context) error {
		row, err := repository.pool.Client(transactionCtx).BackgroundSchedule.Create().
			SetID(id).SetCode(strings.TrimSpace(input.Code)).SetName(strings.TrimSpace(input.Name)).
			SetTaskKind(input.TaskKind).SetScheduleKind(input.ScheduleKind).
			SetNillableCronExpression(input.CronExpression).SetNillableIntervalSeconds(input.IntervalSeconds).
			SetMissedRunPolicy(input.MissedRunPolicy).SetParameters(jsontext.Value(input.Parameters)).
			SetEnabled(false).SetVersion(1).SetCreatedAt(mutation.OccurredAt.UTC()).SetUpdatedAt(mutation.OccurredAt.UTC()).Save(transactionCtx)
		if err != nil {
			return fmt.Errorf("创建后台调度: %w", err)
		}
		auditID, idErr := repository.newID.Next(transactionCtx)
		if idErr != nil {
			return idErr
		}
		if err := insertBackgroundScheduleAudit(transactionCtx, database.Executor(transactionCtx, nil), auditID, mutation, "admin.background_schedule.create", id, input, nil); err != nil {
			return err
		}
		result = backgroundScheduleValue(row)
		return nil
	})
	return result, err
}

// UpdateSchedule 替换指定版本调度字段；已启用调度会从修改时间重新计算下一次触发。
func (repository *backgroundJobRepository) UpdateSchedule(ctx context.Context, id snowflake.ID, expectedVersion int64, input admin.BackgroundScheduleInput, mutation admin.BackgroundScheduleMutation) (admin.BackgroundSchedule, error) {
	if repository == nil || repository.pool == nil || id == snowflake.ID(0) || expectedVersion < 1 || !validBackgroundScheduleInput(input) {
		return admin.BackgroundSchedule{}, admin.ErrInvalidBackgroundSchedule
	}
	nextRunAt, err := nextScheduleRun(input.ScheduleKind, input.CronExpression, input.IntervalSeconds, mutation.OccurredAt)
	if err != nil {
		return admin.BackgroundSchedule{}, err
	}
	var result admin.BackgroundSchedule
	err = repository.pool.WithinTransaction(ctx, func(transactionCtx context.Context) error {
		client := repository.pool.Client(transactionCtx)
		builder := client.BackgroundSchedule.Update().Where(backgroundschedule.IDEQ(id), backgroundschedule.VersionEQ(expectedVersion)).
			SetCode(strings.TrimSpace(input.Code)).SetName(strings.TrimSpace(input.Name)).SetTaskKind(input.TaskKind).
			SetScheduleKind(input.ScheduleKind).SetMissedRunPolicy(input.MissedRunPolicy).
			SetParameters(jsontext.Value(input.Parameters)).AddVersion(1).SetUpdatedAt(mutation.OccurredAt.UTC())
		if input.CronExpression == nil {
			builder.ClearCronExpression()
		} else {
			builder.SetCronExpression(*input.CronExpression)
		}
		if input.IntervalSeconds == nil {
			builder.ClearIntervalSeconds()
		} else {
			builder.SetIntervalSeconds(*input.IntervalSeconds)
		}
		current, findErr := client.BackgroundSchedule.Query().Where(backgroundschedule.IDEQ(id), backgroundschedule.VersionEQ(expectedVersion)).Only(transactionCtx)
		if findErr != nil {
			if ent.IsNotFound(findErr) {
				return admin.ErrBackgroundScheduleNotFound
			}
			return findErr
		}
		if current.Enabled {
			builder.SetNextRunAt(nextRunAt)
		} else {
			builder.ClearNextRunAt()
		}
		if _, err := builder.Save(transactionCtx); err != nil {
			if ent.IsNotFound(err) {
				return admin.ErrBackgroundScheduleNotFound
			}
			return fmt.Errorf("修改后台调度: %w", err)
		}
		auditID, idErr := repository.newID.Next(transactionCtx)
		if idErr != nil {
			return idErr
		}
		if err := insertBackgroundScheduleAudit(transactionCtx, database.Executor(transactionCtx, nil), auditID, mutation, "admin.background_schedule.update", id, input, nil); err != nil {
			return err
		}
		row, err := client.BackgroundSchedule.Query().Where(backgroundschedule.IDEQ(id)).Only(transactionCtx)
		if err != nil {
			return err
		}
		result = backgroundScheduleValue(row)
		return nil
	})
	return result, err
}

// SetScheduleEnabled 切换调度启停状态；启用时从当前时间计算首个未来触发点。
func (repository *backgroundJobRepository) SetScheduleEnabled(ctx context.Context, id snowflake.ID, expectedVersion int64, enabled bool, mutation admin.BackgroundScheduleMutation) (admin.BackgroundSchedule, error) {
	if repository == nil || repository.pool == nil || id == snowflake.ID(0) || expectedVersion < 1 {
		return admin.BackgroundSchedule{}, admin.ErrInvalidBackgroundSchedule
	}
	var result admin.BackgroundSchedule
	err := repository.pool.WithinTransaction(ctx, func(transactionCtx context.Context) error {
		client := repository.pool.Client(transactionCtx)
		row, err := client.BackgroundSchedule.Query().Where(backgroundschedule.IDEQ(id), backgroundschedule.VersionEQ(expectedVersion)).Only(transactionCtx)
		if ent.IsNotFound(err) {
			return admin.ErrBackgroundScheduleNotFound
		}
		if err != nil {
			return err
		}
		nextRunAt, err := nextScheduleRun(row.ScheduleKind, row.CronExpression, row.IntervalSeconds, mutation.OccurredAt)
		if err != nil {
			return err
		}
		builder := client.BackgroundSchedule.UpdateOneID(id).Where(backgroundschedule.VersionEQ(expectedVersion)).SetEnabled(enabled).AddVersion(1).SetUpdatedAt(mutation.OccurredAt.UTC())
		if enabled {
			builder.SetNextRunAt(nextRunAt)
		} else {
			builder.ClearNextRunAt()
		}
		if _, err := builder.Save(transactionCtx); err != nil {
			return admin.ErrBackgroundScheduleNotFound
		}
		input := admin.BackgroundScheduleInput{Code: row.Code, Name: row.Name, TaskKind: row.TaskKind, ScheduleKind: row.ScheduleKind, CronExpression: row.CronExpression, IntervalSeconds: row.IntervalSeconds, MissedRunPolicy: row.MissedRunPolicy, Parameters: json.RawMessage(row.Parameters)}
		auditID, idErr := repository.newID.Next(transactionCtx)
		if idErr != nil {
			return idErr
		}
		if err := insertBackgroundScheduleAudit(transactionCtx, database.Executor(transactionCtx, nil), auditID, mutation, "admin.background_schedule.set_enabled", id, input, &enabled); err != nil {
			return err
		}
		updated, err := client.BackgroundSchedule.Query().Where(backgroundschedule.IDEQ(id)).Only(transactionCtx)
		if err != nil {
			return err
		}
		result = backgroundScheduleValue(updated)
		return nil
	})
	return result, err
}

func insertBackgroundScheduleAudit(ctx context.Context, transaction database.Transaction, auditID snowflake.ID, mutation admin.BackgroundScheduleMutation, actionCode string, scheduleID snowflake.ID, input admin.BackgroundScheduleInput, enabled *bool) error {
	changes, _ := json.Marshal(struct {
		Code         string `json:"code,omitempty"`
		TaskKind     string `json:"taskKind,omitempty"`
		ScheduleKind string `json:"scheduleKind,omitempty"`
		Enabled      *bool  `json:"enabled,omitempty"`
	}{strings.TrimSpace(input.Code), input.TaskKind, input.ScheduleKind, enabled})
	objectID, reason := scheduleID.String(), "administrative_operation"
	return platformaudit.Append(ctx, transaction, platformaudit.AdminLedger, platformaudit.Entry{ID: auditID, ActorAccountID: &mutation.ActorAccountID, ActorKind: "admin", ActionCode: actionCode, ObjectType: "background_schedule", ObjectID: &objectID, RequestID: mutation.RequestID, Reason: &reason, Changes: changes, CreatedAt: mutation.OccurredAt.UTC()})
}

func backgroundScheduleValue(row *ent.BackgroundSchedule) admin.BackgroundSchedule {
	return admin.BackgroundSchedule{ID: snowflake.ID(row.ID), Code: row.Code, Name: row.Name, TaskKind: row.TaskKind, ScheduleKind: row.ScheduleKind, CronExpression: row.CronExpression, IntervalSeconds: row.IntervalSeconds, MissedRunPolicy: row.MissedRunPolicy, Parameters: json.RawMessage(row.Parameters), Enabled: row.Enabled, NextRunAt: row.NextRunAt, LastScheduledAt: row.LastScheduledAt, Version: row.Version, CreatedAt: row.CreatedAt.UTC(), UpdatedAt: row.UpdatedAt.UTC()}
}

func validBackgroundScheduleInput(input admin.BackgroundScheduleInput) bool {
	if strings.TrimSpace(input.Code) == "" || strings.TrimSpace(input.Name) == "" || !backgroundtask.IsKnown(input.TaskKind) || (input.MissedRunPolicy != "skip" && input.MissedRunPolicy != "coalesce" && input.MissedRunPolicy != "catch_up") {
		return false
	}
	var object map[string]json.RawMessage
	if json.Unmarshal(input.Parameters, &object) != nil || object == nil {
		return false
	}
	_, err := nextScheduleRun(input.ScheduleKind, input.CronExpression, input.IntervalSeconds, time.Now())
	return err == nil
}

func nextScheduleRun(kind string, expression *string, intervalSeconds *int32, observedAt time.Time) (time.Time, error) {
	if kind == "interval" && expression == nil && intervalSeconds != nil && *intervalSeconds >= 10 {
		return observedAt.UTC().Add(time.Duration(*intervalSeconds) * time.Second), nil
	}
	if kind == "cron" && expression != nil && strings.TrimSpace(*expression) != "" && intervalSeconds == nil {
		location, err := time.LoadLocation("Asia/Shanghai")
		if err != nil {
			return time.Time{}, err
		}
		parsed, err := cron.ParseStandard(strings.TrimSpace(*expression))
		if err != nil {
			return time.Time{}, admin.ErrInvalidBackgroundSchedule
		}
		return parsed.Next(observedAt.In(location)).UTC(), nil
	}
	return time.Time{}, admin.ErrInvalidBackgroundSchedule
}
