package persistence

import (
	"context"
	"crypto/sha256"
	"fmt"
	"time"

	avalonent "github.com/lishangbu/avalon/ent"
	"github.com/lishangbu/avalon/ent/playercharacterposition"
	"github.com/lishangbu/avalon/ent/playercharacterquest"
	"github.com/lishangbu/avalon/ent/playercharacterquestobjective"
	"github.com/lishangbu/avalon/ent/playercharacterquestrewardclaim"
	"github.com/lishangbu/avalon/ent/rpgquest"
	"github.com/lishangbu/avalon/ent/rpgquestobjective"
	"github.com/lishangbu/avalon/internal/platform/idempotency"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
	rpg "github.com/lishangbu/avalon/internal/rpg"
)

// ListAvailableQuests 返回当前地点、前置完成事实和重复领取状态共同允许开始的任务。
func (adapter *Adapters) ListAvailableQuests(ctx context.Context, accountID snowflake.ID) ([]rpg.AvailableQuest, error) {
	client := adapter.pool.Client(ctx)
	playerID, err := activePlayerCharacterID(ctx, client, accountID)
	if err != nil {
		return nil, err
	}
	position, err := client.PlayerCharacterPosition.Query().Where(playercharacterposition.PlayerCharacterIDEQ(playerID)).Only(ctx)
	if err != nil {
		return nil, fmt.Errorf("读取 Quest 所在位置: %w", err)
	}
	quests, err := client.RpgQuest.Query().Where(rpgquest.EnabledEQ(true)).WithStartNpc().Order(rpgquest.ByCode(), rpgquest.ByID()).All(ctx)
	if err != nil {
		return nil, fmt.Errorf("读取可开始 Quest: %w", err)
	}
	progresses, err := client.PlayerCharacterQuest.Query().Where(playercharacterquest.PlayerCharacterIDEQ(playerID)).All(ctx)
	if err != nil {
		return nil, fmt.Errorf("读取 Quest Progress: %w", err)
	}
	byQuest := make(map[snowflake.ID]*avalonent.PlayerCharacterQuest, len(progresses))
	for _, progress := range progresses {
		byQuest[progress.QuestID] = progress
	}
	result := make([]rpg.AvailableQuest, 0, len(quests))
	for _, quest := range quests {
		if quest.StartNpcID != nil && (quest.Edges.StartNpc == nil || quest.Edges.StartNpc.LocationID != position.LocationID) {
			continue
		}
		if quest.PrerequisiteQuestID != nil {
			prerequisite := byQuest[*quest.PrerequisiteQuestID]
			if prerequisite == nil || prerequisite.CompletionCount < 1 {
				continue
			}
		}
		progress := byQuest[quest.ID]
		if progress != nil {
			if !quest.Repeatable || progress.Status != "completed" {
				continue
			}
			claimed, claimErr := client.PlayerCharacterQuestRewardClaim.Query().Where(playercharacterquestrewardclaim.PlayerCharacterQuestIDEQ(progress.ID), playercharacterquestrewardclaim.CompletionCountEQ(progress.CompletionCount)).Exist(ctx)
			if claimErr != nil {
				return nil, fmt.Errorf("读取 Quest Reward Claim: %w", claimErr)
			}
			if !claimed {
				continue
			}
		}
		result = append(result, rpg.AvailableQuest{QuestID: quest.ID, Code: quest.Code, Name: quest.Name, Description: quest.Description, QuestType: quest.QuestType, Repeatable: quest.Repeatable})
	}
	return result, nil
}

// ListQuestProgress 返回当前活动角色全部已开始任务及其定义目标进度。
func (adapter *Adapters) ListQuestProgress(ctx context.Context, accountID snowflake.ID) ([]rpg.QuestProgress, error) {
	client := adapter.pool.Client(ctx)
	playerID, err := activePlayerCharacterID(ctx, client, accountID)
	if err != nil {
		return nil, err
	}
	rows, err := client.PlayerCharacterQuest.Query().Where(playercharacterquest.PlayerCharacterIDEQ(playerID)).WithQuest(func(query *avalonent.RpgQuestQuery) {
		query.WithObjectives(func(objectives *avalonent.RpgQuestObjectiveQuery) {
			objectives.Where(rpgquestobjective.EnabledEQ(true)).Order(rpgquestobjective.ByPosition(), rpgquestobjective.ByID())
		})
	}).Order(playercharacterquest.ByStartedAt(), playercharacterquest.ByID()).All(ctx)
	if err != nil {
		return nil, fmt.Errorf("读取 Quest Progress: %w", err)
	}
	return adapter.questProgressValues(ctx, client, playerID, rows)
}

// StartQuest 原子建立目标进度，或在上一轮奖励已领取后重置可重复任务。
func (adapter *Adapters) StartQuest(ctx context.Context, command rpg.StartQuestCommand) (rpg.QuestProgress, error) {
	var result rpg.QuestProgress
	if !command.AccountID.IsValid() || !command.QuestID.IsValid() || !idempotency.ValidKey(command.IdempotencyKey) {
		return result, rpg.ErrQuestUnavailable
	}
	now := command.Now.UTC()
	if command.Now.IsZero() {
		now = time.Now().UTC()
	}
	digest := sha256.Sum256([]byte(command.QuestID.String()))
	err := adapter.pool.WithinTransaction(ctx, func(txctx context.Context) error {
		client := adapter.pool.Client(txctx)
		playerID, err := activePlayerCharacterID(txctx, client, command.AccountID)
		if err != nil {
			return err
		}
		replayed, err := adapter.claimPlayerResponse(txctx, client, playerID, "rpg.quest.start", command.IdempotencyKey, digest[:], &result, now)
		if err != nil || replayed {
			return err
		}
		quest, err := client.RpgQuest.Query().Where(rpgquest.IDEQ(command.QuestID), rpgquest.EnabledEQ(true)).WithStartNpc().WithObjectives(func(query *avalonent.RpgQuestObjectiveQuery) {
			query.Where(rpgquestobjective.EnabledEQ(true)).Order(rpgquestobjective.ByPosition(), rpgquestobjective.ByID())
		}).Only(txctx)
		if avalonent.IsNotFound(err) {
			return rpg.ErrQuestUnavailable
		}
		if err != nil {
			return fmt.Errorf("读取待开始 Quest: %w", err)
		}
		if err = validateQuestLocation(txctx, client, playerID, quest.StartNpcID, quest.Edges.StartNpc); err != nil {
			return err
		}
		if quest.PrerequisiteQuestID != nil {
			ok, queryErr := client.PlayerCharacterQuest.Query().Where(playercharacterquest.PlayerCharacterIDEQ(playerID), playercharacterquest.QuestIDEQ(*quest.PrerequisiteQuestID), playercharacterquest.CompletionCountGTE(1)).Exist(txctx)
			if queryErr != nil {
				return queryErr
			}
			if !ok {
				return rpg.ErrQuestUnavailable
			}
		}
		progress, err := client.PlayerCharacterQuest.Query().Where(playercharacterquest.PlayerCharacterIDEQ(playerID), playercharacterquest.QuestIDEQ(quest.ID)).ForUpdate().Only(txctx)
		if avalonent.IsNotFound(err) {
			progressID, nextErr := adapter.newID.Next(txctx)
			if nextErr != nil {
				return nextErr
			}
			progress, err = client.PlayerCharacterQuest.Create().SetID(progressID).SetPlayerCharacterID(playerID).SetQuestID(quest.ID).SetStatus("active").SetStartedAt(now).SetCompletionCount(0).SetVersion(1).Save(txctx)
			if err != nil {
				return fmt.Errorf("创建 Quest Progress: %w", err)
			}
			for _, objective := range quest.Edges.Objectives {
				id, nextErr := adapter.newID.Next(txctx)
				if nextErr != nil {
					return nextErr
				}
				if _, err = client.PlayerCharacterQuestObjective.Create().SetID(id).SetPlayerCharacterID(playerID).SetQuestID(quest.ID).SetObjectiveID(objective.ID).SetCurrentCount(0).Save(txctx); err != nil {
					return fmt.Errorf("创建 Quest Objective Progress: %w", err)
				}
			}
		} else {
			if err != nil {
				return fmt.Errorf("读取 Quest Progress: %w", err)
			}
			if !quest.Repeatable || progress.Status != "completed" {
				return rpg.ErrQuestUnavailable
			}
			claimed, queryErr := client.PlayerCharacterQuestRewardClaim.Query().Where(playercharacterquestrewardclaim.PlayerCharacterQuestIDEQ(progress.ID), playercharacterquestrewardclaim.CompletionCountEQ(progress.CompletionCount)).Exist(txctx)
			if queryErr != nil {
				return queryErr
			}
			if !claimed {
				return rpg.ErrQuestUnavailable
			}
			progress, err = client.PlayerCharacterQuest.UpdateOne(progress).SetStatus("active").SetStartedAt(now).ClearCompletedAt().SetVersion(progress.Version + 1).Save(txctx)
			if err != nil {
				return err
			}
			if _, err = client.PlayerCharacterQuestObjective.Update().Where(playercharacterquestobjective.PlayerCharacterIDEQ(playerID), playercharacterquestobjective.QuestIDEQ(quest.ID)).SetCurrentCount(0).ClearCompletedAt().Save(txctx); err != nil {
				return err
			}
		}
		values, err := adapter.questProgressValues(txctx, client, playerID, []*avalonent.PlayerCharacterQuest{progress})
		if err != nil {
			return err
		}
		result = values[0]
		return adapter.completePlayerResponse(txctx, client, playerID, "rpg.quest.start", command.IdempotencyKey, result)
	})
	return result, err
}

// CompleteQuest 校验全部目标和交付地点后，原子增加完成轮次与进度版本。
func (adapter *Adapters) CompleteQuest(ctx context.Context, command rpg.CompleteQuestCommand) (rpg.QuestProgress, error) {
	var result rpg.QuestProgress
	if !command.AccountID.IsValid() || !command.QuestID.IsValid() || command.ExpectedVersion <= 0 || !idempotency.ValidKey(command.IdempotencyKey) {
		return result, rpg.ErrQuestUnavailable
	}
	now := command.Now.UTC()
	if command.Now.IsZero() {
		now = time.Now().UTC()
	}
	digest := sha256.Sum256([]byte(fmt.Sprintf("%s:%d", command.QuestID, command.ExpectedVersion)))
	err := adapter.pool.WithinTransaction(ctx, func(txctx context.Context) error {
		client := adapter.pool.Client(txctx)
		playerID, err := activePlayerCharacterID(txctx, client, command.AccountID)
		if err != nil {
			return err
		}
		replayed, err := adapter.claimPlayerResponse(txctx, client, playerID, "rpg.quest.complete", command.IdempotencyKey, digest[:], &result, now)
		if err != nil || replayed {
			return err
		}
		progress, err := client.PlayerCharacterQuest.Query().Where(playercharacterquest.PlayerCharacterIDEQ(playerID), playercharacterquest.QuestIDEQ(command.QuestID)).WithQuest(func(query *avalonent.RpgQuestQuery) {
			query.WithTurnInNpc().WithObjectives(func(objectives *avalonent.RpgQuestObjectiveQuery) {
				objectives.Where(rpgquestobjective.EnabledEQ(true))
			})
		}).ForUpdate().Only(txctx)
		if avalonent.IsNotFound(err) {
			return rpg.ErrQuestUnavailable
		}
		if err != nil {
			return err
		}
		if progress.Version != command.ExpectedVersion {
			return rpg.ErrQuestProgressConflict
		}
		if progress.Status != "active" || progress.Edges.Quest == nil || !progress.Edges.Quest.Enabled {
			return rpg.ErrQuestUnavailable
		}
		quest := progress.Edges.Quest
		if err = validateQuestLocation(txctx, client, playerID, quest.TurnInNpcID, quest.Edges.TurnInNpc); err != nil {
			return err
		}
		counts, err := client.PlayerCharacterQuestObjective.Query().Where(playercharacterquestobjective.PlayerCharacterIDEQ(playerID), playercharacterquestobjective.QuestIDEQ(quest.ID)).All(txctx)
		if err != nil {
			return err
		}
		byObjective := make(map[snowflake.ID]int32, len(counts))
		for _, value := range counts {
			byObjective[value.ObjectiveID] = value.CurrentCount
		}
		for _, objective := range quest.Edges.Objectives {
			if byObjective[objective.ID] < objective.RequiredCount {
				return rpg.ErrQuestObjectivesIncomplete
			}
		}
		progress, err = client.PlayerCharacterQuest.UpdateOne(progress).Where(playercharacterquest.VersionEQ(command.ExpectedVersion)).SetStatus("completed").SetCompletedAt(now).SetCompletionCount(progress.CompletionCount + 1).SetVersion(progress.Version + 1).Save(txctx)
		if avalonent.IsNotFound(err) {
			return rpg.ErrQuestProgressConflict
		}
		if err != nil {
			return err
		}
		values, err := adapter.questProgressValues(txctx, client, playerID, []*avalonent.PlayerCharacterQuest{progress})
		if err != nil {
			return err
		}
		result = values[0]
		return adapter.completePlayerResponse(txctx, client, playerID, "rpg.quest.complete", command.IdempotencyKey, result)
	})
	return result, err
}

func validateQuestLocation(ctx context.Context, client *avalonent.Client, playerID snowflake.ID, npcID *snowflake.ID, npc *avalonent.RpgNpc) error {
	if npcID == nil {
		return nil
	}
	if npc == nil {
		return rpg.ErrQuestUnavailable
	}
	position, err := client.PlayerCharacterPosition.Query().Where(playercharacterposition.PlayerCharacterIDEQ(playerID)).Only(ctx)
	if err != nil {
		return fmt.Errorf("读取 Quest 所在位置: %w", err)
	}
	if position.LocationID != npc.LocationID {
		return rpg.ErrQuestUnavailable
	}
	return nil
}

func (adapter *Adapters) questProgressValues(ctx context.Context, client *avalonent.Client, playerID snowflake.ID, rows []*avalonent.PlayerCharacterQuest) ([]rpg.QuestProgress, error) {
	result := make([]rpg.QuestProgress, 0, len(rows))
	for _, row := range rows {
		quest := row.Edges.Quest
		if quest == nil {
			var err error
			quest, err = client.RpgQuest.Query().Where(rpgquest.IDEQ(row.QuestID)).WithObjectives(func(query *avalonent.RpgQuestObjectiveQuery) {
				query.Where(rpgquestobjective.EnabledEQ(true)).Order(rpgquestobjective.ByPosition(), rpgquestobjective.ByID())
			}).Only(ctx)
			if err != nil {
				return nil, err
			}
		}
		progressRows, err := client.PlayerCharacterQuestObjective.Query().Where(playercharacterquestobjective.PlayerCharacterIDEQ(playerID), playercharacterquestobjective.QuestIDEQ(row.QuestID)).All(ctx)
		if err != nil {
			return nil, err
		}
		byID := make(map[snowflake.ID]*avalonent.PlayerCharacterQuestObjective, len(progressRows))
		for _, progress := range progressRows {
			byID[progress.ObjectiveID] = progress
		}
		value := rpg.QuestProgress{QuestID: quest.ID, Code: quest.Code, Name: quest.Name, Description: quest.Description, Status: row.Status, CompletionCount: row.CompletionCount, Version: row.Version, StartedAt: row.StartedAt, CompletedAt: row.CompletedAt, Objectives: []rpg.QuestObjectiveProgress{}}
		for _, objective := range quest.Edges.Objectives {
			current := int32(0)
			var completed *time.Time
			if progress := byID[objective.ID]; progress != nil {
				current, completed = progress.CurrentCount, progress.CompletedAt
			}
			value.Objectives = append(value.Objectives, rpg.QuestObjectiveProgress{ObjectiveID: objective.ID, Code: objective.Code, ObjectiveType: objective.ObjectiveType, CurrentCount: current, RequiredCount: objective.RequiredCount, Description: objective.Description, CompletedAt: completed})
		}
		result = append(result, value)
	}
	return result, nil
}
