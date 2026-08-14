// Package api 实现 Battle 玩家 RPC 契约到领域用例的显式适配。
package api

import (
	"context"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	battle "github.com/lishangbu/avalon/internal/battle"
	"github.com/lishangbu/avalon/internal/playercharacter"
)

// BattleReader 返回 Battle RPC 所需的权威 Battle 对象与单个参与者披露事实。
type BattleReader interface {
	Get(context.Context, snowflake.ID) (battle.Battle, error)
	GetParticipantDisclosure(context.Context, snowflake.ID, snowflake.ID) (battle.DisclosureView, error)
}

// BattleQuery 返回 Battle 历史列表投影。
type BattleQuery interface {
	ListHistory(context.Context, snowflake.ID, int32, int32) (battle.HistoryPage, error)
}

// BattleRepository 提供 Battle Preview 和取消命令的聚合写入端口。
type BattleRepository interface {
	SubmitPreview(context.Context, snowflake.ID, battle.PreviewSubmissionCommand, time.Time) (battle.Battle, error)
	Cancel(context.Context, snowflake.ID, time.Time) (battle.Battle, error)
}

// TurnSubmitter 将真人 Participant 的完整回合提交发送给单实例 Battle Runtime Registry。
type TurnSubmitter interface {
	Submit(context.Context, snowflake.ID, battle.TurnSubmission) (battle.TurnSubmissionResult, error)
}

// BattleStarter 将双方 Preview 已齐备的 Battle 编译、持久化并激活为可接受回合的 Runtime。
type BattleStarter interface {
	Start(context.Context, battle.Battle) (battle.Battle, error)
}

// PlayerCharacterReader 通过账户所有权读取 PlayerCharacter，防止历史查询越权。
type PlayerCharacterReader interface {
	GetOwned(context.Context, snowflake.ID, snowflake.ID) (playercharacter.PlayerCharacter, error)
}
