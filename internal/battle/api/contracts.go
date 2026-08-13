// Package api 实现 Battle 玩家 RPC 契约到领域用例的显式适配。
package api

import (
	"context"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	battle "github.com/lishangbu/avalon/internal/battle"
	battlestore "github.com/lishangbu/avalon/internal/battle/store"
	"github.com/lishangbu/avalon/internal/playercharacter"
)

// BattleStore 提供 Battle RPC 服务需要的权威 Battle、Preview 和历史存储用例。
type BattleStore interface {
	Get(context.Context, snowflake.ID) (battle.Battle, error)
	SubmitPreview(context.Context, snowflake.ID, battle.PreviewSubmissionCommand, time.Time) (battle.Battle, error)
	Cancel(context.Context, snowflake.ID, time.Time) (battle.Battle, error)
	ListHistory(context.Context, snowflake.ID, int32, int32) (battlestore.HistoryPage, error)
	GetParticipantDisclosure(context.Context, snowflake.ID, snowflake.ID) (battle.DisclosureView, error)
}

// TurnSubmitter 将真人 Participant 的完整回合提交发送给单实例 Battle Runtime Registry。
type TurnSubmitter interface {
	Submit(context.Context, snowflake.ID, battle.TurnSubmission) (battle.TurnSubmissionResult, error)
}

// BattleStarter 将双方 Preview 已齐备的 Battle 编译、持久化并激活为可接受回合的 Runtime。
type BattleStarter interface {
	Start(context.Context, battle.Battle) (battle.Battle, error)
}

// PlayerCharacterQuery 通过账户所有权读取 PlayerCharacter，防止历史查询越权。
type PlayerCharacterQuery interface {
	GetOwned(context.Context, snowflake.ID, snowflake.ID) (playercharacter.PlayerCharacter, error)
}
