package persistence

import (
	"context"
	"database/sql"
	"errors"
	"fmt"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// InsertBackgroundJobOccurrence 原子写入周期任务 occurrence。
// PostgreSQL 唯一索引负责并发幂等；该函数只封装数据库专用 ON CONFLICT 语义，
// 不承载后台任务的业务状态转换。
func InsertBackgroundJobOccurrence(ctx context.Context, executor SQLExecutor, id snowflake.ID, kind string, parameters []byte, scheduleID snowflake.ID, scheduledFor, now time.Time) (bool, error) {
	if executor == nil {
		return false, errors.New("调度任务创建必须运行在事务中")
	}
	var insertedID snowflake.ID
	err := executor.QueryRowContext(ctx, `
		INSERT INTO background_job
			(id, kind, queue, state, parameters, attempt_count, max_attempts, schedule_id,
			 scheduled_for, next_attempt_at, version, created_at, updated_at)
		VALUES ($1, $2, 'default', 'scheduled', $3, 0, 10, $4, $5, $5, 1, $6, $6)
		ON CONFLICT (schedule_id, scheduled_for) WHERE schedule_id IS NOT NULL DO NOTHING
		RETURNING id`, id, kind, parameters, scheduleID, scheduledFor.UTC(), now.UTC()).Scan(&insertedID)
	if errors.Is(err, sql.ErrNoRows) {
		return false, nil
	}
	if err != nil {
		return false, fmt.Errorf("创建调度后台任务: %w", err)
	}
	return insertedID.IsValid(), nil
}
