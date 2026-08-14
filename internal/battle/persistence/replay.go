package persistence

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	avalonent "github.com/lishangbu/avalon/ent"
	entbattle "github.com/lishangbu/avalon/ent/battle"
	"github.com/lishangbu/avalon/ent/battleturnrecord"
	"github.com/lishangbu/avalon/internal/battle"
	"github.com/lishangbu/avalon/internal/battleengine"
)

var (
	// ErrReplayUnavailable 表示 Battle 尚未成功启动，或持久化记录不足以构成可验证的离线回放。
	ErrReplayUnavailable = errors.New("对战回放不可用")
)

// LoadReplayArchive 读取指定 Battle 的冻结初始状态和全部权威 Turn Record，并转换为纯引擎回放输入。
//
// 本方法只读取数据库，不依赖当前实时资料、进程随机源或内存 Runtime。调用方必须位于受控运维或管理边界：
// 回放档案包含双方完整命令和事件，只能交给后台校验任务，不能直接作为玩家或管理员 RPC 响应暴露。
func (store *Adapters) LoadReplayArchive(ctx context.Context, battleID snowflake.ID) (battleengine.GoldenReplay, error) {
	if store == nil || store.pool == nil || battleID == snowflake.ID(0) {
		return battleengine.GoldenReplay{}, ErrReplayUnavailable
	}
	session, err := store.pool.Client(ctx).Battle.Query().Where(entbattle.IDEQ(battleID)).Only(ctx)
	if avalonent.IsNotFound(err) {
		return battleengine.GoldenReplay{}, ErrBattleNotFound
	}
	if err != nil {
		return battleengine.GoldenReplay{}, fmt.Errorf("读取 Battle 回放初始状态: %w", err)
	}
	initialPayload := []byte(session.InitialState)
	if len(initialPayload) == 0 {
		return battleengine.GoldenReplay{}, ErrReplayUnavailable
	}
	archive := battleengine.GoldenReplay{SchemaVersion: battleengine.GoldenReplaySchemaVersion}
	if err := json.Unmarshal(initialPayload, &archive.InitialState); err != nil {
		return battleengine.GoldenReplay{}, fmt.Errorf("解析 Battle 回放初始状态: %w", err)
	}
	records, err := store.pool.Client(ctx).BattleTurnRecord.Query().Where(battleturnrecord.BattleIDEQ(battleID)).Order(battleturnrecord.ByStateVersion()).All(ctx)
	if err != nil {
		return battleengine.GoldenReplay{}, fmt.Errorf("读取 Battle 回放记录: %w", err)
	}
	archive.Turns = make([]battleengine.GoldenReplayTurn, 0, len(records))
	for index, record := range records {
		if record.BattleID != battleID || record.StateVersion != int64(index+1) ||
			record.TurnNumber < 1 || record.SchemaVersion != int32(battleengine.GoldenReplaySchemaVersion) {
			return battleengine.GoldenReplay{}, fmt.Errorf("%w: 回合记录顺序或版本无效", ErrReplayUnavailable)
		}
		turn, decodeErr := replayTurnFromRecord(record)
		if decodeErr != nil {
			return battleengine.GoldenReplay{}, decodeErr
		}
		archive.Turns = append(archive.Turns, turn)
	}
	if len(archive.Turns) == 0 {
		return battleengine.GoldenReplay{}, ErrReplayUnavailable
	}
	return archive, nil
}

// LoadRuntimeSnapshot 从同一 Battle state_version 的持久事实重建可继续执行的 Runtime 快照。
func (store *Adapters) LoadRuntimeSnapshot(ctx context.Context, battleID snowflake.ID) (battle.RuntimeSnapshot, error) {
	battleValue, err := store.Get(ctx, battleID)
	if err != nil {
		return battle.RuntimeSnapshot{}, err
	}
	if battleValue.Status != battle.StatusRunning || battleValue.StartedAt.IsZero() {
		return battle.RuntimeSnapshot{}, ErrReplayUnavailable
	}
	row, err := store.pool.Client(ctx).Battle.Query().Where(entbattle.IDEQ(battleID)).Only(ctx)
	if err != nil {
		return battle.RuntimeSnapshot{}, fmt.Errorf("读取 Battle Runtime 快照: %w", err)
	}
	var initial battleengine.InitialState
	if err := json.Unmarshal(row.InitialState, &initial); err != nil {
		return battle.RuntimeSnapshot{}, fmt.Errorf("解析 Battle Runtime 初始状态: %w", err)
	}
	var randomSnapshot battleengine.RandomSourceSnapshot
	if err := json.Unmarshal(row.RandomSource, &randomSnapshot); err != nil {
		return battle.RuntimeSnapshot{}, fmt.Errorf("解析 Battle Runtime 随机源: %w", err)
	}
	randomSource, err := battleengine.RestoreRandomSource(randomSnapshot)
	if err != nil {
		return battle.RuntimeSnapshot{}, fmt.Errorf("恢复 Battle Runtime 随机源: %w", err)
	}
	records, err := store.pool.Client(ctx).BattleTurnRecord.Query().Where(battleturnrecord.BattleIDEQ(battleID)).Order(battleturnrecord.ByStateVersion()).All(ctx)
	if err != nil {
		return battle.RuntimeSnapshot{}, fmt.Errorf("读取 Battle Runtime 回合: %w", err)
	}
	if int64(len(records)) != battleValue.StateVersion {
		return battle.RuntimeSnapshot{}, ErrReplayUnavailable
	}
	turns := make([]battleengine.GoldenReplayTurn, 0, len(records))
	lastCommittedAt := battleValue.StartedAt
	for index, record := range records {
		if record.StateVersion != int64(index+1) {
			return battle.RuntimeSnapshot{}, ErrReplayUnavailable
		}
		turn, decodeErr := replayTurnFromRecord(record)
		if decodeErr != nil {
			return battle.RuntimeSnapshot{}, decodeErr
		}
		turns = append(turns, turn)
		lastCommittedAt = record.CreatedAt.UTC()
	}
	state, err := battleengine.RestoreState(initial, turns)
	if err != nil {
		return battle.RuntimeSnapshot{}, err
	}
	return battle.RuntimeSnapshot{Battle: battleValue, State: state, Random: randomSource, LastCommittedAt: lastCommittedAt}, nil
}

// replayTurnFromRecord 严格解码单条持久化 Turn Record，避免后台校验任务把损坏 JSON 当作空回合继续执行。
func replayTurnFromRecord(record *avalonent.BattleTurnRecord) (battleengine.GoldenReplayTurn, error) {
	turn := battleengine.GoldenReplayTurn{}
	if err := json.Unmarshal([]byte(record.Command), &turn.Command); err != nil {
		return battleengine.GoldenReplayTurn{}, fmt.Errorf("%w: 解析回放命令: %v", ErrReplayUnavailable, err)
	}
	if err := json.Unmarshal([]byte(record.RandomTrace), &turn.RandomTrace); err != nil {
		return battleengine.GoldenReplayTurn{}, fmt.Errorf("%w: 解析回放随机轨迹: %v", ErrReplayUnavailable, err)
	}
	if err := json.Unmarshal([]byte(record.Events), &turn.ExpectedEvents); err != nil {
		return battleengine.GoldenReplayTurn{}, fmt.Errorf("%w: 解析回放事件: %v", ErrReplayUnavailable, err)
	}
	if err := json.Unmarshal([]byte(record.StateSummary), &turn.ExpectedState); err != nil {
		return battleengine.GoldenReplayTurn{}, fmt.Errorf("%w: 解析回放状态摘要: %v", ErrReplayUnavailable, err)
	}
	return turn, nil
}
