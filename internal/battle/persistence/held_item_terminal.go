package persistence

import (
	"context"
	"encoding/json"
	"fmt"

	avalonent "github.com/lishangbu/avalon/ent"
	"github.com/lishangbu/avalon/ent/battleturnrecord"
	"github.com/lishangbu/avalon/ent/playercharactercreature"
	battle "github.com/lishangbu/avalon/internal/battle"
	"github.com/lishangbu/avalon/internal/battleengine"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// heldItemConsumption 是从版本化 Battle Event 归一出的持有道具消费权威事实。
//
// Side 与 Position 指向 Battle 冻结成员；ItemID 保存消费前的资料身份。该类型刻意不接收成员终态，
// 因为道具转移同样会令原持有者终态为空，不能据此推断一次真实消费。
type heldItemConsumption struct {
	Side     battleengine.Side
	Position battleengine.MemberPosition
	ItemID   snowflake.ID
}

// heldItemConsumptionEnvelope 只解码所有消费事件共享的稳定定位字段。
// 具体采用哪个成员字段由 kind 白名单决定，未知事件不会参与持久化写回。
type heldItemConsumptionEnvelope struct {
	Kind          battleengine.EventKind `json:"kind"`
	SchemaVersion uint32                 `json:"schemaVersion"`
	Actor         battleengine.MemberRef `json:"actor"`
	Target        battleengine.MemberRef `json:"target"`
	Holder        battleengine.MemberRef `json:"holder"`
	Member        battleengine.MemberRef `json:"member"`
	ItemID        snowflake.ID           `json:"itemId"`
	SourceItemID  snowflake.ID           `json:"sourceItemId"`
}

// heldItemConsumptionsFromEvents 从一个回合的权威事件中提取显式消费事实。
// heldItemTransferred 不属于消费白名单，因此道具交接不会被终局逻辑误判为销毁。
func heldItemConsumptionsFromEvents(events []json.RawMessage) ([]heldItemConsumption, error) {
	consumptions := make([]heldItemConsumption, 0)
	for _, raw := range events {
		var envelope heldItemConsumptionEnvelope
		if err := json.Unmarshal(raw, &envelope); err != nil {
			return nil, fmt.Errorf("解析 Battle 持有道具事件: %w", err)
		}
		var ref battleengine.MemberRef
		itemID := envelope.ItemID
		switch envelope.Kind {
		case battleengine.EventKindHeldItemElementDamageBoostConsumed:
			ref = envelope.Actor
		case battleengine.EventKindHeldItemElementDamageReductionConsumed,
			battleengine.EventKindHeldItemStatReactionConsumed:
			ref = envelope.Target
		case battleengine.EventKindHeldItemTriggeredConsumed:
			ref = envelope.Holder
		case battleengine.EventKindSkillChargeSkippedByItem:
			ref = envelope.Actor
		case battleengine.EventKindHeldItemParalysisCured,
			battleengine.EventKindHeldItemSleepCured,
			battleengine.EventKindHeldItemPoisonCured,
			battleengine.EventKindHeldItemBurnCured,
			battleengine.EventKindHeldItemFreezeCured,
			battleengine.EventKindHeldItemAllMajorStatusCured,
			battleengine.EventKindHeldItemConfusionCured:
			ref = envelope.Member
		case battleengine.EventKindFatalDamageSurvived:
			// FatalDamageSurvived 同时承载特性与道具来源；只有显式 SourceItemID 才是消费。
			if !envelope.SourceItemID.IsValid() {
				continue
			}
			ref, itemID = envelope.Target, envelope.SourceItemID
		default:
			continue
		}
		if envelope.SchemaVersion != 1 || !ref.Side.Valid() || !ref.Position.Valid() || !itemID.IsValid() {
			return nil, fmt.Errorf("解析 Battle 持有道具消费: 事件 %q 字段无效", envelope.Kind)
		}
		consumptions = append(consumptions, heldItemConsumption{Side: ref.Side, Position: ref.Position, ItemID: itemID})
	}
	return consumptions, nil
}

// applyHeldItemConsumptionsEnt 在正常 completed Battle 的终局事务中消费可映射的 Owned Creature 道具。
//
// Encounter Party Snapshot 是当前唯一冻结 PlayerCharacterCreatureID 的参赛快照。Training 与 PvP 的
// Team Snapshot 尚无实例身份，函数会明确跳过这些无法权威映射的成员，绝不以 Creature 资料 ID 代替实例 ID。
func (store *Adapters) applyHeldItemConsumptionsEnt(ctx context.Context, client *avalonent.Client, session battle.Battle) error {
	if session.Status != battle.StatusCompleted {
		return nil
	}
	rows, err := client.BattleTurnRecord.Query().Where(battleturnrecord.BattleIDEQ(session.ID)).Order(battleturnrecord.ByStateVersion()).All(ctx)
	if err != nil {
		return fmt.Errorf("读取 Battle 持有道具消费事件: %w", err)
	}
	consumptions := make([]heldItemConsumption, 0)
	for _, row := range rows {
		var events []json.RawMessage
		if err := json.Unmarshal(row.Events, &events); err != nil {
			return fmt.Errorf("解析 Battle 持有道具消费事件: %w", err)
		}
		values, err := heldItemConsumptionsFromEvents(events)
		if err != nil {
			return err
		}
		consumptions = append(consumptions, values...)
	}

	for _, consumption := range consumptions {
		ownedID, playerID, ok := ownedCreatureForBattleMember(session, consumption.Side, consumption.Position)
		if !ok {
			continue
		}
		updated, err := client.PlayerCharacterCreature.Update().Where(
			playercharactercreature.IDEQ(ownedID),
			playercharactercreature.PlayerCharacterIDEQ(playerID),
			playercharactercreature.HeldItemIDEQ(consumption.ItemID),
		).ClearHeldItemID().AddVersion(1).SetUpdatedAt(session.CompletedAt).Save(ctx)
		if err != nil {
			return fmt.Errorf("写回 Battle 持有道具消费: %w", err)
		}
		// 零行表示道具已由同一终局重试消费，或 Battle 后持久状态已不再匹配冻结事实；两者都不能归还库存。
		_ = updated
	}
	return nil
}

// ownedCreatureForBattleMember 把引擎成员引用映射到 Encounter 创建时冻结的 Owned Creature 身份。
func ownedCreatureForBattleMember(session battle.Battle, side battleengine.Side, position battleengine.MemberPosition) (snowflake.ID, snowflake.ID, bool) {
	for _, participant := range session.Participants {
		if participant.IsBot || participant.Party == nil || battleengine.Side(participant.Side) != side {
			continue
		}
		for _, member := range participant.Party.Members {
			if member.Position == int16(position) && member.PlayerCharacterCreatureID.IsValid() && participant.PlayerCharacterID.IsValid() {
				return member.PlayerCharacterCreatureID, participant.PlayerCharacterID, true
			}
		}
	}
	return 0, 0, false
}
