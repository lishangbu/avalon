package worker

import (
	"context"
	"errors"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/lishangbu/avalon/internal/backgroundtask"
	"github.com/lishangbu/avalon/internal/verification"
)

const (
	// BattleReplayVerificationTaskKind 是持久 Battle 回放校验的稳定任务类型。
	BattleReplayVerificationTaskKind = backgroundtask.BattleReplayVerification
	// AuditHashVerificationTaskKind 是管理员审计哈希链校验的稳定任务类型。
	AuditHashVerificationTaskKind = backgroundtask.AuditHashVerification
)

// BattleReplayVerificationArgs 保存人工发起持久 Battle 回放校验所需的最小请求事实。
type BattleReplayVerificationArgs struct {
	// BattleID 是要读取冻结回放档案的稳定 Battle Identifier。
	BattleID snowflake.ID `json:"matchId"`
	// ActorAccountID 是发起命令的已认证管理员 Identifier，仅用于任务事实追踪。
	ActorAccountID snowflake.ID `json:"actorAccountId"`
	// RequestID 关联管理 RPC 请求、任务参数和管理员审计记录。
	RequestID string `json:"requestId"`
}

// AuditHashVerificationArgs 保存人工或周期审计哈希链校验的触发事实。
type AuditHashVerificationArgs struct {
	// Trigger 是 manual 或 periodic，用于区分管理员命令与动态调度。
	Trigger string `json:"trigger"`
	// ActorAccountID 是人工触发时的管理员 Identifier；周期任务为空。
	ActorAccountID *snowflake.ID `json:"actorAccountId,omitempty"`
	// RequestID 是人工触发时的管理 HTTP 请求标识；周期任务为空。
	RequestID string `json:"requestId,omitempty"`
}

// BattleReplayVerificationRunner 是回放 Worker 调用的最小应用服务边界。
type BattleReplayVerificationRunner interface {
	// Verify 读取指定 Battle 的冻结档案并返回不含原始回放内容的校验摘要。
	Verify(context.Context, snowflake.ID) (verification.Result, error)
}

// AuditHashVerificationRunner 是审计 Worker 调用的最小应用服务边界。
type AuditHashVerificationRunner interface {
	// Verify 独立重算审计哈希链并返回不含业务 changes 的校验摘要。
	Verify(context.Context) (verification.Result, error)
}

// BattleReplayVerificationWorker 执行单个持久 Battle 的严格离线回放校验。
type BattleReplayVerificationWorker struct {
	// runner 编排回放档案读取与纯引擎严格重放。
	runner BattleReplayVerificationRunner
}

// NewBattleReplayVerificationWorker 创建回放校验 Worker。
func NewBattleReplayVerificationWorker(runner BattleReplayVerificationRunner) *BattleReplayVerificationWorker {
	return &BattleReplayVerificationWorker{runner: runner}
}

// Run 校验参数、执行严格重放并返回可持久化的安全摘要。
func (worker *BattleReplayVerificationWorker) Run(ctx context.Context, args BattleReplayVerificationArgs) (verification.Result, error) {
	if worker == nil || worker.runner == nil || !args.BattleID.IsValid() || !args.ActorAccountID.IsValid() || args.RequestID == "" {
		return verification.Result{}, errors.New("持久 Battle 回放校验 worker 未配置或参数无效")
	}
	return worker.runner.Verify(ctx, args.BattleID)
}

// AuditHashVerificationWorker 执行全部管理员与玩家管理审计账本的独立哈希校验。
type AuditHashVerificationWorker struct {
	// runner 编排只读审计哈希链重算。
	runner AuditHashVerificationRunner
}

// NewAuditHashVerificationWorker 创建人工和动态调度共用的审计校验 Worker。
func NewAuditHashVerificationWorker(runner AuditHashVerificationRunner) *AuditHashVerificationWorker {
	return &AuditHashVerificationWorker{runner: runner}
}

// Run 校验触发事实、重算哈希链并返回安全摘要。
func (worker *AuditHashVerificationWorker) Run(ctx context.Context, args AuditHashVerificationArgs) (verification.Result, error) {
	if worker == nil || worker.runner == nil || !validAuditHashVerificationArgs(args) {
		return verification.Result{}, errors.New("审计哈希链校验 worker 未配置或参数无效")
	}
	return worker.runner.Verify(ctx)
}

func validAuditHashVerificationArgs(args AuditHashVerificationArgs) bool {
	if args.Trigger == "periodic" {
		return args.ActorAccountID == nil && args.RequestID == ""
	}
	if args.Trigger != "manual" || args.ActorAccountID == nil || !args.ActorAccountID.IsValid() || args.RequestID == "" {
		return false
	}
	return true
}
