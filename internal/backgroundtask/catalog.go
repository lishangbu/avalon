// Package backgroundtask 定义管理端、PostgreSQL 调度器与 Worker 共同使用的任务类型目录。
package backgroundtask

const (
	// BattleLifecycle 扫描并结算到期 Challenge、Preview 与 Active Battle。
	BattleLifecycle = "battle.expire-lifecycle.v1"
	// BattleAnalytics 消费 Battle Outbox 并更新可重建分析投影。
	BattleAnalytics = "battle.drain-analytics.v1"
	// BattleReplayVerification 校验持久 Battle 回放与当前 Battle Engine 输出。
	BattleReplayVerification = "verification.battle-replay.v1"
	// AuditHashVerification 校验管理员与玩家管理审计哈希链。
	AuditHashVerification = "verification.audit-hash-chain.v1"
)

// Definition 是不依赖执行器实现的稳定任务元数据。
type Definition struct {
	// Kind 是 PostgreSQL、Outbox 与 Asynq 共用的任务类型。
	Kind string
	// Singleton 表示同一时刻最多允许一个该类型的非终态任务。
	Singleton bool
}

// Definitions 返回当前程序支持的全部任务类型；调用方不得修改返回切片中的定义。
func Definitions() []Definition {
	return []Definition{
		{Kind: BattleLifecycle, Singleton: true},
		{Kind: BattleAnalytics, Singleton: true},
		{Kind: BattleReplayVerification},
		{Kind: AuditHashVerification, Singleton: true},
	}
}

// Lookup 返回任务类型的唯一代码定义。
func Lookup(kind string) (Definition, bool) {
	for _, definition := range Definitions() {
		if definition.Kind == kind {
			return definition, true
		}
	}
	return Definition{}, false
}

// IsKnown 判断管理端输入是否属于当前代码任务目录。
func IsKnown(kind string) bool {
	_, exists := Lookup(kind)
	return exists
}
