package rpg

import (
	"context"
	"fmt"

	avalonent "github.com/lishangbu/avalon/ent"
	"github.com/lishangbu/avalon/ent/playercharactercheckpoint"
	"github.com/lishangbu/avalon/ent/playercharactercreature"
	"github.com/lishangbu/avalon/ent/playercharacterposition"
	"github.com/lishangbu/avalon/internal/battle"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// HandleEncounterTerminal 在 Encounter 终局事务内写回 Party 生命，并在明确落败时按 Checkpoint 恢复。
//
// 返回值精确记录本事务写入的最终生命和实际使用的恢复点，随后由 Battle Store 冻结进权威摘要；
// 管理端因此不需要从已经继续变化的 Player Position 或 Owned Creature 当前值猜测历史结果。
func (store *EntWorldStore) HandleEncounterTerminal(ctx context.Context, command battle.EncounterTerminalCommand) (battle.EncounterTerminalResult, error) {
	result := battle.EncounterTerminalResult{Defeated: command.Defeated, Members: make([]battle.EncounterTerminalMember, 0, len(command.Members))}
	if store == nil || store.pool == nil || command.BattleID == 0 || command.PlayerCharacterID == 0 || command.CompletedAt.IsZero() || len(command.Members) == 0 {
		return battle.EncounterTerminalResult{}, fmt.Errorf("PvE Checkpoint 恢复命令无效")
	}
	client := store.pool.Client(ctx)
	checkpointEnabled := false
	var checkpointID, checkpointLocationID snowflake.ID
	if command.Defeated {
		binding, err := client.PlayerCharacterCheckpoint.Query().Where(playercharactercheckpoint.PlayerCharacterIDEQ(command.PlayerCharacterID)).WithCheckpoint().Only(ctx)
		if err != nil && !avalonent.IsNotFound(err) {
			return battle.EncounterTerminalResult{}, fmt.Errorf("读取 PvE 恢复 Checkpoint: %w", err)
		}
		if err == nil && binding.Edges.Checkpoint != nil && binding.Edges.Checkpoint.Enabled {
			checkpoint := binding.Edges.Checkpoint
			checkpointEnabled = true
			if checkpoint.RecoveryCondition != nil {
				condition, compileErr := CompileCondition(checkpoint.RecoveryCondition)
				if compileErr != nil {
					checkpointEnabled = false
				} else {
					conditionContext, contextErr := loadConditionContext(ctx, client, command.PlayerCharacterID)
					if contextErr != nil {
						return battle.EncounterTerminalResult{}, fmt.Errorf("读取 PvE 恢复条件上下文: %w", contextErr)
					}
					checkpointEnabled = condition.Evaluate(conditionContext)
				}
			}
			checkpointID = checkpoint.ID
			checkpointLocationID = checkpoint.LocationID
		}
	}
	for _, member := range command.Members {
		if member.PlayerCharacterCreatureID == 0 || member.CurrentHP < 0 || member.MaximumHP <= 0 || member.CurrentHP > member.MaximumHP {
			return battle.EncounterTerminalResult{}, fmt.Errorf("PvE Encounter 终局成员无效")
		}
		owned, ownedErr := client.PlayerCharacterCreature.Query().Where(playercharactercreature.IDEQ(member.PlayerCharacterCreatureID), playercharactercreature.PlayerCharacterIDEQ(command.PlayerCharacterID)).ForUpdate().Only(ctx)
		if ownedErr != nil {
			return battle.EncounterTerminalResult{}, fmt.Errorf("锁定 PvE Encounter Owned Creature: %w", ownedErr)
		}
		currentHP := member.CurrentHP
		if checkpointEnabled {
			currentHP = member.MaximumHP
		}
		if _, ownedErr = client.PlayerCharacterCreature.UpdateOne(owned).SetCurrentHp(currentHP).SetVersion(owned.Version + 1).SetUpdatedAt(command.CompletedAt.UTC()).Save(ctx); ownedErr != nil {
			return battle.EncounterTerminalResult{}, fmt.Errorf("写回 PvE Encounter Party 生命: %w", ownedErr)
		}
		result.Members = append(result.Members, battle.EncounterTerminalMember{PlayerCharacterCreatureID: member.PlayerCharacterCreatureID, CurrentHP: currentHP, MaximumHP: member.MaximumHP})
	}
	if !checkpointEnabled {
		return result, nil
	}
	position, err := client.PlayerCharacterPosition.Query().Where(playercharacterposition.PlayerCharacterIDEQ(command.PlayerCharacterID)).ForUpdate().Only(ctx)
	if err != nil {
		return battle.EncounterTerminalResult{}, fmt.Errorf("锁定 PvE 恢复位置: %w", err)
	}
	if _, err = client.PlayerCharacterPosition.UpdateOne(position).SetLocationID(checkpointLocationID).SetMoveSequence(position.MoveSequence + 1).SetVersion(position.Version + 1).SetUpdatedAt(command.CompletedAt.UTC()).Save(ctx); err != nil {
		return battle.EncounterTerminalResult{}, fmt.Errorf("恢复 PvE Player Position: %w", err)
	}
	result.CheckpointRecovered = true
	result.CheckpointID = checkpointID
	result.RecoveryLocationID = checkpointLocationID
	return result, nil
}
