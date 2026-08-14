package persistence

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"math"
	"strings"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	avalonent "github.com/lishangbu/avalon/ent"
	"github.com/lishangbu/avalon/ent/battleanalyticsprojection"
	"github.com/lishangbu/avalon/ent/battleauthoritativesummary"
	"github.com/lishangbu/avalon/ent/battleoutbox"
	battle "github.com/lishangbu/avalon/internal/battle"
)

const (
	// BattleTerminalOutboxTopic 是正常完成或已中断 Battle 写入的唯一可靠异步事件类型。
	BattleTerminalOutboxTopic = "battle.terminal.v1"
	// BattleOverviewProjectionKey 是按全部权威终局事实重建的首版 Battle 总览投影稳定键。
	BattleOverviewProjectionKey = "battle.overview.v1"
	// battleOverviewProjectionSchemaVersion 是 Battle 总览 JSON 结构的当前不可变解释版本。
	battleOverviewProjectionSchemaVersion uint32 = 3
)

// AnalyticsProjection 是从权威 Battle Summary 按确定规则得出的只读分析总览。
//
// 它不承担任何核心业务决策；任意时刻都可以使用 RebuildAnalyticsProjection 从
// battle_authoritative_summary 完整覆盖重建。
type AnalyticsProjection struct {
	// SchemaVersion 是 Payload 的结构与统计口径版本。
	SchemaVersion uint32 `json:"schemaVersion"`
	// CompletedBattles 是已纳入投影的全部终局 Battle 数量，包含 completed、canceled 与 interrupted。
	CompletedBattles int64 `json:"completedBattles"`
	// PvPBattles 是 PvP Battle 的终局数量。
	PvPBattles int64 `json:"pvpBattles"`
	// PvEBattles 是 PvE Battle 的终局数量。
	PvEBattles int64 `json:"pveBattles"`
	// ChallengeBattles 是 Challenge 来源的终局数量。
	ChallengeBattles int64 `json:"challengeBattles"`
	// TrainingBattles 是 Training 来源的终局数量。
	TrainingBattles int64 `json:"trainingBattles"`
	// EncounterBattles 是 Encounter 来源的终局数量。
	EncounterBattles int64 `json:"encounterBattles"`
	// SideOneWins 是第一方获胜的正常完成 Battle 数量。
	SideOneWins int64 `json:"sideOneWins"`
	// SideTwoWins 是第二方获胜的正常完成 Battle 数量。
	SideTwoWins int64 `json:"sideTwoWins"`
	// Draws 是终局原因为 draw 的正常完成 Battle 数量。
	Draws int64 `json:"draws"`
	// NoContests 是终局原因为 no_contest 的正常完成 Battle 数量。
	NoContests int64 `json:"noContests"`
	// InterruptedBattles 是没有产生胜负、被运行时或启动故障中断的 Battle 数量。
	InterruptedBattles int64 `json:"interruptedBattles"`
	// CanceledBattles 是 Runtime 启动前由参与者明确取消的 Battle 数量。
	CanceledBattles int64 `json:"canceledBattles"`
	// LastCompletedAt 是纳入投影的最新一条权威终局 UTC 时间；没有历史时为零值。
	LastCompletedAt time.Time `json:"lastCompletedAt,omitempty"`
}

// AnalyticsDrainResult 是一次 Outbox 消费扫描实际发布的数量及最新投影快照。
type AnalyticsDrainResult struct {
	// Published 是本次成功应用并标记已发布的终局 Outbox 数量。
	Published int `json:"published"`
	// Projection 是最后一条 Outbox 提交后的总览；没有待处理记录时为零值。
	Projection AnalyticsProjection `json:"projection"`
}

// DrainTerminalOutbox 有界地消费待发布的 Battle 终局 Outbox，并把每条事实幂等写入总览投影。
//
// 每条记录在同一个 PostgreSQL 事务中完成行锁领取、权威 Summary 读取、投影更新和 published 标记。
// 事务回滚时这些写入会共同回滚，Asynq 的重复投递只能再次处理尚未发布的记录，不会重复累计统计。
func (adapter *Adapters) DrainTerminalOutbox(
	ctx context.Context,
	observedAt time.Time,
	maximum int,
) (AnalyticsDrainResult, error) {
	if adapter == nil || adapter.pool == nil || adapter.newID == nil || observedAt.IsZero() || maximum < 1 || maximum > 1_000 {
		return AnalyticsDrainResult{}, battle.ErrInvalidBattle
	}
	result := AnalyticsDrainResult{}
	for range maximum {
		processed, projection, outboxID, err := adapter.processNextTerminalOutbox(ctx, observedAt.UTC())
		if err != nil {
			if outboxID != snowflake.ID(0) {
				if recordErr := adapter.recordOutboxFailure(ctx, outboxID, err); recordErr != nil {
					return result, fmt.Errorf("处理 Battle Outbox: %w；记录失败: %v", err, recordErr)
				}
			}
			return result, fmt.Errorf("处理 Battle Outbox: %w", err)
		}
		if !processed {
			return result, nil
		}
		result.Published++
		result.Projection = projection
	}
	return result, nil
}

// RebuildAnalyticsProjection 使用全部权威 Summary 覆盖重建总览，并确认其中已有的终局 Outbox 已发布。
//
// 它是故障修复和验证入口，而非日常消费路径。重建和批量 published 标记在同一事务完成；重建期间刚提交
// 的新终局会在该事务之后保留待处理 Outbox，由下一轮 Asynq 任务增量应用。
func (adapter *Adapters) RebuildAnalyticsProjection(ctx context.Context, observedAt time.Time) (AnalyticsProjection, error) {
	if adapter == nil || adapter.pool == nil || adapter.newID == nil || observedAt.IsZero() {
		return AnalyticsProjection{}, battle.ErrInvalidBattle
	}
	projection := newAnalyticsProjection()
	err := adapter.pool.WithinTransaction(ctx, func(transactionCtx context.Context) error {
		client := adapter.pool.Client(transactionCtx)
		rows, err := client.BattleAuthoritativeSummary.Query().Order(battleauthoritativesummary.ByCompletedAt()).All(transactionCtx)
		if err != nil {
			return fmt.Errorf("读取 Battle 权威摘要: %w", err)
		}
		for _, summary := range rows {
			if err := projection.apply(authoritativeSummaryFromEnt(summary)); err != nil {
				return err
			}
		}
		_, currentVersion, err := loadAnalyticsProjection(transactionCtx, client, adapter.newID, observedAt.UTC())
		if err != nil {
			return err
		}
		version, err := incrementProjectionVersion(currentVersion)
		if err != nil {
			return err
		}
		if err := persistAnalyticsProjection(transactionCtx, client, projection, version, observedAt.UTC()); err != nil {
			return err
		}
		if _, err := client.BattleOutbox.Update().Where(battleoutbox.TopicEQ(BattleTerminalOutboxTopic), battleoutbox.PublishedAtIsNil()).SetPublishedAt(observedAt.UTC()).Save(transactionCtx); err != nil {
			return fmt.Errorf("确认重建前终局 Outbox 已发布: %w", err)
		}
		return nil
	})
	return projection, err
}

// processNextTerminalOutbox 领取并处理一条待发布 Outbox；没有可处理记录时返回 false。
func (adapter *Adapters) processNextTerminalOutbox(
	ctx context.Context,
	observedAt time.Time,
) (bool, AnalyticsProjection, snowflake.ID, error) {
	projection := AnalyticsProjection{}
	var processingID snowflake.ID
	err := adapter.pool.WithinTransaction(ctx, func(transactionCtx context.Context) error {
		client := adapter.pool.Client(transactionCtx)
		outbox, err := client.BattleOutbox.Query().Where(battleoutbox.PublishedAtIsNil()).Order(battleoutbox.ByCreatedAt()).First(transactionCtx)
		if avalonent.IsNotFound(err) {
			return nil
		}
		if err != nil {
			return fmt.Errorf("锁定待发布 Battle Outbox: %w", err)
		}
		processingID = snowflake.ID(outbox.ID)
		if outbox.Topic != BattleTerminalOutboxTopic {
			return fmt.Errorf("不支持的 Battle Outbox 主题 %q", outbox.Topic)
		}
		summary, err := client.BattleAuthoritativeSummary.Query().Where(battleauthoritativesummary.IDEQ(outbox.BattleID)).Only(transactionCtx)
		if avalonent.IsNotFound(err) {
			return errors.New("Battle Outbox 缺少权威终局摘要")
		}
		if err != nil {
			return fmt.Errorf("读取 Battle Outbox 权威摘要: %w", err)
		}
		if summary.ID != outbox.BattleID {
			return errors.New("Battle Outbox 与权威摘要身份不一致")
		}
		var currentVersion int64
		projection, currentVersion, err = loadAnalyticsProjection(transactionCtx, client, adapter.newID, observedAt)
		if err != nil {
			return err
		}
		if err := projection.apply(authoritativeSummaryFromEnt(summary)); err != nil {
			return err
		}
		version, err := incrementProjectionVersion(currentVersion)
		if err != nil {
			return err
		}
		if err := persistAnalyticsProjection(transactionCtx, client, projection, version, observedAt); err != nil {
			return err
		}
		rows, err := client.BattleOutbox.UpdateOne(outbox).Where(battleoutbox.PublishedAtIsNil()).SetPublishedAt(observedAt.UTC()).Save(transactionCtx)
		if err != nil {
			return fmt.Errorf("标记 Battle Outbox 已发布: %w", err)
		}
		if rows == nil {
			return errors.New("Battle Outbox 发布状态并发冲突")
		}
		return nil
	})
	if err != nil {
		return false, AnalyticsProjection{}, processingID, err
	}
	if processingID == snowflake.ID(0) {
		return false, AnalyticsProjection{}, snowflake.ID(0), nil
	}
	return true, projection, processingID, nil
}

// recordOutboxFailure 在前一处理事务已回滚后记录可观察的失败次数和脱敏错误摘要。
func (adapter *Adapters) recordOutboxFailure(ctx context.Context, outboxID snowflake.ID, cause error) error {
	if outboxID == snowflake.ID(0) || cause == nil {
		return battle.ErrInvalidBattle
	}
	message := truncateOutboxError(cause.Error())
	rows, err := adapter.pool.Client(ctx).BattleOutbox.Update().Where(battleoutbox.IDEQ(outboxID), battleoutbox.PublishedAtIsNil()).AddAttempts(1).SetLastError(message).Save(ctx)
	if err != nil {
		return fmt.Errorf("记录 Battle Outbox 失败: %w", err)
	}
	if rows != 1 {
		return errors.New("Battle Outbox 失败状态并发冲突")
	}
	return nil
}

// newAnalyticsProjection 返回没有任何已处理终局事实的当前版本总览。
func newAnalyticsProjection() AnalyticsProjection {
	return AnalyticsProjection{SchemaVersion: battleOverviewProjectionSchemaVersion}
}

// loadAnalyticsProjection 在当前事务中确保、取得并锁住唯一总览行，返回其已提交版本。
//
// 先 INSERT ... ON CONFLICT DO NOTHING 再 FOR UPDATE，使首条 Outbox 被并发 Worker 领取时同样
// 串行化，避免两条首发事件都基于空投影写入而丢失累计值。
func loadAnalyticsProjection(ctx context.Context, client *avalonent.Client, newID snowflake.Source, observedAt time.Time) (AnalyticsProjection, int64, error) {
	initial := newAnalyticsProjection()
	payload, err := json.Marshal(initial)
	if err != nil {
		return AnalyticsProjection{}, 0, fmt.Errorf("编码初始 Battle 分析投影: %w", err)
	}
	row, err := client.BattleAnalyticsProjection.Query().Where(battleanalyticsprojection.ProjectionKeyEQ(BattleOverviewProjectionKey)).Only(ctx)
	if avalonent.IsNotFound(err) {
		projectionID, idErr := newID.Next(ctx)
		if idErr != nil {
			return AnalyticsProjection{}, 0, fmt.Errorf("生成 Battle 分析投影标识: %w", idErr)
		}
		err = client.BattleAnalyticsProjection.Create().SetID(projectionID).SetProjectionKey(BattleOverviewProjectionKey).
			SetProjectionVersion(0).SetPayload(payload).SetRefreshedAt(observedAt.UTC()).
			OnConflictColumns(battleanalyticsprojection.FieldProjectionKey).DoNothing().Exec(ctx)
		if err == nil {
			row, err = client.BattleAnalyticsProjection.Query().Where(battleanalyticsprojection.ProjectionKeyEQ(BattleOverviewProjectionKey)).Only(ctx)
		}
	}
	if err != nil {
		return AnalyticsProjection{}, 0, fmt.Errorf("锁定 Battle 分析投影: %w", err)
	}
	projection := AnalyticsProjection{}
	if err := json.Unmarshal(row.Payload, &projection); err != nil {
		return AnalyticsProjection{}, 0, fmt.Errorf("解析 Battle 分析投影: %w", err)
	}
	if projection.SchemaVersion != battleOverviewProjectionSchemaVersion {
		return AnalyticsProjection{}, 0, errors.New("Battle 分析投影结构版本不受支持")
	}
	return projection, row.ProjectionVersion, nil
}

// incrementProjectionVersion 返回投影行锁保护的当前版本的下一个稳定版本。
func incrementProjectionVersion(current int64) (int64, error) {
	if current < 0 || current == math.MaxInt64 {
		return 0, errors.New("Battle 分析投影版本已耗尽")
	}
	return current + 1, nil
}

// persistAnalyticsProjection 编码并覆盖保存当前总览；调用方必须已经持有该投影行锁。
func persistAnalyticsProjection(ctx context.Context, client *avalonent.Client, projection AnalyticsProjection, version int64, observedAt time.Time) error {
	payload, err := json.Marshal(projection)
	if err != nil {
		return fmt.Errorf("编码 Battle 分析投影: %w", err)
	}
	if _, err := client.BattleAnalyticsProjection.Update().Where(battleanalyticsprojection.ProjectionKeyEQ(BattleOverviewProjectionKey)).SetProjectionVersion(version).SetPayload(payload).SetRefreshedAt(observedAt.UTC()).Save(ctx); err != nil {
		return fmt.Errorf("保存 Battle 分析投影: %w", err)
	}
	return nil
}

// apply 将一条权威终局摘要纳入总览，拒绝任何缺失数据库约束仍无法表达的领域矛盾。
type authoritativeSummary struct {
	ID             snowflake.ID
	Mode           string
	SourceType     string
	WinnerSide     *int16
	TerminalReason string
	CompletedAt    time.Time
}

// authoritativeSummaryFromEnt 将 Ent 权威摘要转换为投影计算所需的最小事实。
func authoritativeSummaryFromEnt(row *avalonent.BattleAuthoritativeSummary) authoritativeSummary {
	return authoritativeSummary{ID: snowflake.ID(row.ID), Mode: row.Mode, SourceType: row.SourceType, WinnerSide: row.WinnerSide, TerminalReason: row.TerminalReason, CompletedAt: row.CompletedAt}
}

func (projection *AnalyticsProjection) apply(summary authoritativeSummary) error {
	if projection == nil || summary.ID == snowflake.ID(0) || summary.CompletedAt.IsZero() {
		return errors.New("Battle 权威摘要无效")
	}
	if projection.SchemaVersion != battleOverviewProjectionSchemaVersion || projection.CompletedBattles == math.MaxInt64 {
		return errors.New("Battle 分析投影无效")
	}
	projection.CompletedBattles++
	switch battle.BattleMode(summary.Mode) {
	case battle.BattleModePvP:
		projection.PvPBattles++
	case battle.BattleModePvE:
		projection.PvEBattles++
	default:
		return fmt.Errorf("Battle 权威摘要模式无效: %q", summary.Mode)
	}
	switch battle.BattleSourceType(summary.SourceType) {
	case battle.BattleSourceChallenge:
		projection.ChallengeBattles++
	case battle.BattleSourceTraining:
		projection.TrainingBattles++
	case battle.BattleSourceEncounter:
		projection.EncounterBattles++
	default:
		return fmt.Errorf("Battle 权威摘要来源无效: %q", summary.SourceType)
	}
	if summary.WinnerSide != nil {
		switch battle.ParticipantSide(*summary.WinnerSide) {
		case battle.ParticipantSideOne:
			projection.SideOneWins++
		case battle.ParticipantSideTwo:
			projection.SideTwoWins++
		default:
			return errors.New("Battle 权威摘要胜方无效")
		}
	}
	switch battle.TerminalReason(summary.TerminalReason) {
	case battle.TerminalReasonDraw:
		projection.Draws++
	case battle.TerminalReasonNoContest:
		projection.NoContests++
	case battle.TerminalReasonCanceled:
		projection.CanceledBattles++
	case battle.TerminalReasonStartupFailed, battle.TerminalReasonRuntimePanic,
		battle.TerminalReasonLeaseLost, battle.TerminalReasonRuntimeFailed, battle.TerminalReasonRecoveryExhausted:
		projection.InterruptedBattles++
	}
	completedAt := summary.CompletedAt.UTC()
	if projection.LastCompletedAt.IsZero() || completedAt.After(projection.LastCompletedAt) {
		projection.LastCompletedAt = completedAt
	}
	return nil
}

// truncateOutboxError 将基础设施错误限制为数据库列允许的最大字符数，避免错误记录本身再次失败。
func truncateOutboxError(message string) string {
	const maximumRunes = 512
	message = strings.TrimSpace(message)
	runes := []rune(message)
	if len(runes) <= maximumRunes {
		return message
	}
	return string(runes[:maximumRunes])
}
