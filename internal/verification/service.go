// Package verification 编排持久 Battle 回放与审计哈希链的受控后台校验。
package verification

import (
	"context"
	"errors"
	"fmt"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/lishangbu/avalon/internal/battleengine"
	"github.com/lishangbu/avalon/internal/platform/audit"
)

// Result 是允许写入 Asynq 输出并由管理端读取的安全校验摘要。
//
// 该结构不包含回放命令、事件、随机轨迹、成员状态、审计业务 changes 或链尾哈希。
type Result struct {
	// Passed 表示校验对象与当前权威规则或链式摘要完全一致。
	Passed bool `json:"passed"`
	// Summary 是不含敏感载荷、适合管理员直接查看的简体中文结论。
	Summary string `json:"summary"`
}

// ReplayArchiveLoader 是持久 Battle 回放校验所需的最小只读存储边界。
type ReplayArchiveLoader interface {
	// LoadReplayArchive 读取冻结初始状态和全部权威 Turn Record，不向 HTTP 层暴露档案。
	LoadReplayArchive(context.Context, snowflake.ID) (battleengine.GoldenReplay, error)
}

// BattleReplayService 使用当前 Battle Engine 严格重放指定持久 Battle。
type BattleReplayService struct {
	// loader 只负责读取被冻结的完整回放输入。
	loader ReplayArchiveLoader
}

// NewBattleReplayService 使用显式回放存储边界创建校验服务。
func NewBattleReplayService(loader ReplayArchiveLoader) *BattleReplayService {
	return &BattleReplayService{loader: loader}
}

// Verify 严格比较每回合随机轨迹、事件顺序和状态摘要，并仅返回安全结论。
func (service *BattleReplayService) Verify(ctx context.Context, battleID snowflake.ID) (Result, error) {
	if service == nil || service.loader == nil || battleID == snowflake.ID(0) {
		return Result{}, errors.New("持久 Battle 回放校验服务未配置")
	}
	replay, err := service.loader.LoadReplayArchive(ctx, battleID)
	if err != nil {
		return Result{}, err
	}
	result, err := battleengine.ReplayGolden(replay)
	if err != nil {
		return Result{Passed: false, Summary: "持久 Battle 回放与当前 Battle Engine 输出不一致"}, nil
	}
	return Result{
		Passed:  true,
		Summary: fmt.Sprintf("%d 个回合的持久 Battle 回放校验通过", result.ReplayedTurns),
	}, nil
}

// AuditHashVerifier 是审计校验服务依赖的最小只读基础设施边界。
type AuditHashVerifier interface {
	// Verify 独立重算全部审计记录摘要并与每个账本的持久链尾比较。
	Verify(context.Context) (audit.VerificationReport, error)
}

// AuditHashService 把审计完整性校验结果转换成可安全展示的后台任务结论。
type AuditHashService struct {
	// verifier 只读取数据库审计账本，不修改任何业务记录。
	verifier AuditHashVerifier
}

// NewAuditHashService 使用显式审计验证器创建校验服务。
func NewAuditHashService(verifier AuditHashVerifier) *AuditHashService {
	return &AuditHashService{verifier: verifier}
}

// Verify 区分确定的链完整性失败与应交给 Asynq 重试的基础设施故障。
func (service *AuditHashService) Verify(ctx context.Context) (Result, error) {
	if service == nil || service.verifier == nil {
		return Result{}, errors.New("审计哈希链校验服务未配置")
	}
	report, err := service.verifier.Verify(ctx)
	if errors.Is(err, audit.ErrHashChainInvalid) {
		return Result{Passed: false, Summary: "审计哈希链校验未通过"}, nil
	}
	if err != nil {
		return Result{}, err
	}
	var entries int64
	for _, ledger := range report.Ledgers {
		entries += ledger.Entries
	}
	return Result{
		Passed:  true,
		Summary: fmt.Sprintf("%d 个审计账本、%d 条记录的哈希链校验通过", len(report.Ledgers), entries),
	}, nil
}
