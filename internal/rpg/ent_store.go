package rpg

import (
	"bytes"
	"context"
	"crypto/sha256"
	"encoding/json"
	"fmt"
	"time"

	"entgo.io/ent/dialect/sql"
	"github.com/lishangbu/avalon/internal/platform/snowflake"

	avalonent "github.com/lishangbu/avalon/ent"
	"github.com/lishangbu/avalon/ent/activeplayercharacter"
	"github.com/lishangbu/avalon/ent/gamebattleformat"
	entplayercharacter "github.com/lishangbu/avalon/ent/playercharacter"
	"github.com/lishangbu/avalon/ent/playercharactercheckpoint"
	"github.com/lishangbu/avalon/ent/playercharactercreature"
	"github.com/lishangbu/avalon/ent/playercharacterdiscoveredexit"
	"github.com/lishangbu/avalon/ent/playercharacterdiscoveredlocation"
	"github.com/lishangbu/avalon/ent/playercharacterencounterusage"
	"github.com/lishangbu/avalon/ent/playercharacteridempotencyrecord"
	"github.com/lishangbu/avalon/ent/playercharacterinventoryitem"
	"github.com/lishangbu/avalon/ent/playercharacterparty"
	"github.com/lishangbu/avalon/ent/playercharacterpartymember"
	"github.com/lishangbu/avalon/ent/playercharacterpendingencounter"
	"github.com/lishangbu/avalon/ent/playercharacterposition"
	"github.com/lishangbu/avalon/ent/playercharacterprofession"
	"github.com/lishangbu/avalon/ent/playercharacterquestobjective"
	"github.com/lishangbu/avalon/ent/playercharactertraversal"
	"github.com/lishangbu/avalon/ent/playercharacterworldstate"
	"github.com/lishangbu/avalon/ent/rpgcheckpoint"
	"github.com/lishangbu/avalon/ent/rpgencounterentry"
	"github.com/lishangbu/avalon/ent/rpgencountertable"
	"github.com/lishangbu/avalon/ent/rpglocation"
	"github.com/lishangbu/avalon/ent/rpglocationexit"
	"github.com/lishangbu/avalon/ent/rpgmapprojection"
	"github.com/lishangbu/avalon/ent/rpgmapprojectionlocation"
	"github.com/lishangbu/avalon/ent/rpgquestobjective"
	"github.com/lishangbu/avalon/internal/platform/database"
)

const pendingEncounterLifetime = 10 * time.Minute

// EntWorldStore 使用一个 PostgreSQL 事务提交位置、发现、遭遇、幂等结果和 Outbox。
type EntWorldStore struct {
	pool  *database.Pool
	newID snowflake.Source
}

// GetPendingEncounter 返回角色最近一条仍未过期的待处理遭遇。
func (store *EntWorldStore) GetPendingEncounter(ctx context.Context, accountID snowflake.ID, now time.Time) (*PendingEncounter, error) {
	client := store.pool.Client(ctx)
	playerID, err := activePlayerCharacterID(ctx, client, accountID)
	if err != nil {
		return nil, err
	}
	row, err := client.PlayerCharacterPendingEncounter.Query().Where(playercharacterpendingencounter.PlayerCharacterIDEQ(playerID), playercharacterpendingencounter.StateEQ("pending"), playercharacterpendingencounter.ExpiresAtGT(now.UTC())).Order(avalonent.Asc(playercharacterpendingencounter.FieldCreatedAt)).First(ctx)
	if avalonent.IsNotFound(err) {
		return nil, nil
	}
	if err != nil {
		return nil, fmt.Errorf("查询 Pending Encounter: %w", err)
	}
	return &PendingEncounter{ID: snowflake.ID(row.ID), EncounterEntryID: snowflake.ID(row.EncounterEntryID), BattleID: optionalIdentifier(row.BattleID), State: row.State, ExpiresAt: row.ExpiresAt.UTC()}, nil
}

// ResolvePendingEncounter 只在事务中接受或取消待处理遭遇；接受后的 Battle 创建由同一终态钩子接管。
func (store *EntWorldStore) ResolvePendingEncounter(ctx context.Context, command ResolveEncounterCommand) (PendingEncounter, error) {
	var result PendingEncounter
	if command.Now.IsZero() {
		command.Now = time.Now().UTC()
	}
	err := store.pool.WithinTransaction(ctx, func(transactionCtx context.Context) error {
		client := store.pool.Client(transactionCtx)
		playerID, err := activePlayerCharacterID(transactionCtx, client, command.AccountID)
		if err != nil {
			return err
		}
		digest := sha256.Sum256([]byte(command.PendingEncounterID.String() + ":" + string(command.Resolution)))
		replayed, err := store.claimPlayerResponse(transactionCtx, client, playerID, "rpg.pending-encounter.resolve", command.IdempotencyKey, digest[:], &result, command.Now.UTC())
		if err != nil || replayed {
			return err
		}
		row, err := client.PlayerCharacterPendingEncounter.Query().Where(playercharacterpendingencounter.IDEQ(command.PendingEncounterID), playercharacterpendingencounter.PlayerCharacterIDEQ(playerID)).ForUpdate().Only(transactionCtx)
		if avalonent.IsNotFound(err) {
			return ErrExitUnavailable
		}
		if err != nil {
			return fmt.Errorf("锁定 Pending Encounter: %w", err)
		}
		if row.State == "pending" && !row.ExpiresAt.After(command.Now.UTC()) {
			updated, err := client.PlayerCharacterPendingEncounter.UpdateOne(row).SetState("expired").SetResolvedAt(command.Now.UTC()).Save(transactionCtx)
			if err != nil {
				return err
			}
			result = PendingEncounter{ID: snowflake.ID(updated.ID), EncounterEntryID: snowflake.ID(updated.EncounterEntryID), BattleID: optionalIdentifier(updated.BattleID), State: updated.State, ExpiresAt: updated.ExpiresAt.UTC()}
			return store.completePlayerResponse(transactionCtx, client, playerID, "rpg.pending-encounter.resolve", command.IdempotencyKey, result)
		}
		if row.State != "pending" {
			result = PendingEncounter{ID: snowflake.ID(row.ID), EncounterEntryID: snowflake.ID(row.EncounterEntryID), BattleID: optionalIdentifier(row.BattleID), State: row.State, ExpiresAt: row.ExpiresAt.UTC()}
			return store.completePlayerResponse(transactionCtx, client, playerID, "rpg.pending-encounter.resolve", command.IdempotencyKey, result)
		}
		state := ""
		switch command.Resolution {
		case EncounterResolutionAccept:
			state = "accepted"
		case EncounterResolutionCancel:
			state = "cancelled"
		default:
			return ErrExitUnavailable
		}
		var battleID snowflake.ID
		if command.Resolution == EncounterResolutionAccept {
			character, characterErr := client.PlayerCharacter.Query().Where(entplayercharacter.IDEQ(playerID)).Only(transactionCtx)
			if characterErr != nil {
				return fmt.Errorf("读取 Encounter PlayerCharacter: %w", characterErr)
			}
			party, partyErr := client.PlayerCharacterParty.Query().Where(playercharacterparty.PlayerCharacterIDEQ(playerID)).WithMembers(func(query *avalonent.PlayerCharacterPartyMemberQuery) {
				query.Order(playercharacterpartymember.ByPosition())
			}).Only(transactionCtx)
			if partyErr != nil || len(party.Edges.Members) == 0 {
				return ErrExitUnavailable
			}
			members := make([]string, 0, len(party.Edges.Members))
			for _, member := range party.Edges.Members {
				members = append(members, snowflake.ID(member.PlayerCharacterCreatureID).String())
			}
			partySnapshot, _ := json.Marshal(map[string]any{"party_id": snowflake.ID(party.ID).String(), "version": party.Version, "members": members})
			format, formatErr := client.GameBattleFormat.Query().Where(gamebattleformat.EnabledEQ(true), gamebattleformat.EncounterAvailableEQ(true)).Order(gamebattleformat.ByIsDefault(sql.OrderDesc()), gamebattleformat.ByCode()).First(transactionCtx)
			if formatErr != nil {
				return fmt.Errorf("读取 Encounter BattleFormat: %w", formatErr)
			}
			formatSnapshot, marshalErr := json.Marshal(format)
			if marshalErr != nil {
				return fmt.Errorf("冻结 Encounter BattleFormat: %w", marshalErr)
			}
			sessionFormat, marshalErr := json.Marshal(map[string]any{
				"rosterCount": format.RosterCount, "selectCount": format.SelectCount,
				"activeParticipantsPerSide": format.ActiveParticipantsPerSide,
				"previewDuration":           time.Duration(format.PreviewSeconds) * time.Second,
				"turnDuration":              time.Duration(format.TurnSeconds) * time.Second,
				"battleDuration":            time.Duration(format.BattleSeconds) * time.Second,
			})
			if marshalErr != nil {
				return fmt.Errorf("冻结 Encounter 执行赛制: %w", marshalErr)
			}
			battleID, err = store.newID.Next(transactionCtx)
			if err != nil {
				return fmt.Errorf("生成 PvE Battle 标识: %w", err)
			}
			if _, err := client.Battle.Create().SetID(battleID).SetMode("pve").SetSourceType("encounter").SetStatus("created").SetPendingEncounterID(row.ID).SetBattleFormatID(format.ID).SetBattleFormatSnapshot(formatSnapshot).SetFormat(sessionFormat).SetPreviewDeadlineAt(command.Now.UTC().Add(time.Duration(format.PreviewSeconds) * time.Second)).SetBattleDeadlineAt(command.Now.UTC().Add(time.Duration(format.BattleSeconds) * time.Second)).SetStateVersion(0).SetVersion(1).SetCreatedAt(command.Now.UTC()).SetUpdatedAt(command.Now.UTC()).Save(transactionCtx); err != nil {
				return fmt.Errorf("创建 PvE Battle: %w", err)
			}
			playerParticipantID, idErr := store.newID.Next(transactionCtx)
			if idErr != nil {
				return fmt.Errorf("生成 PvE Battle 玩家参赛方标识: %w", idErr)
			}
			if _, err := client.BattleParticipant.Create().SetID(playerParticipantID).SetBattleID(battleID).SetSide(1).SetParticipantType("player_character").SetInputType("party").SetAccountID(character.AccountID).SetPlayerCharacterID(playerID).SetDisplayName(character.DisplayName).SetSourcePartyID(party.ID).SetSourcePartyVersion(party.Version).SetInputSnapshot(partySnapshot).Save(transactionCtx); err != nil {
				return fmt.Errorf("创建 PvE Battle 玩家参赛方: %w", err)
			}
			botParticipantID, idErr := store.newID.Next(transactionCtx)
			if idErr != nil {
				return fmt.Errorf("生成 PvE Battle 野生参赛方标识: %w", idErr)
			}
			if _, err := client.BattleParticipant.Create().SetID(botParticipantID).SetBattleID(battleID).SetSide(2).SetParticipantType("bot").SetInputType("generated").SetDisplayName("野生对手").SetInputSnapshot(row.RandomResult).SetBotCode("wild-encounter").SetBotStrategyVersion(1).Save(transactionCtx); err != nil {
				return fmt.Errorf("创建 PvE Battle 野生参赛方: %w", err)
			}
		}
		update := client.PlayerCharacterPendingEncounter.UpdateOne(row).SetState(state).SetResolvedAt(command.Now.UTC())
		if battleID != snowflake.ID(0) {
			update.SetBattleID(battleID)
		}
		updated, err := update.Save(transactionCtx)
		if err != nil {
			return fmt.Errorf("更新 Pending Encounter 状态: %w", err)
		}
		result = PendingEncounter{ID: snowflake.ID(updated.ID), EncounterEntryID: snowflake.ID(updated.EncounterEntryID), BattleID: optionalIdentifier(updated.BattleID), State: updated.State, ExpiresAt: updated.ExpiresAt.UTC()}
		return store.completePlayerResponse(transactionCtx, client, playerID, "rpg.pending-encounter.resolve", command.IdempotencyKey, result)
	})
	return result, err
}

// GetCheckpoint 返回角色当前选择的 Checkpoint 资料。
func (store *EntWorldStore) GetCheckpoint(ctx context.Context, accountID snowflake.ID) (*Checkpoint, error) {
	client := store.pool.Client(ctx)
	playerID, err := activePlayerCharacterID(ctx, client, accountID)
	if err != nil {
		return nil, err
	}
	row, err := client.PlayerCharacterCheckpoint.Query().Where(playercharactercheckpoint.PlayerCharacterIDEQ(playerID)).WithCheckpoint().Only(ctx)
	if avalonent.IsNotFound(err) {
		return nil, nil
	}
	if err != nil {
		return nil, fmt.Errorf("查询 PlayerCharacter Checkpoint: %w", err)
	}
	return &Checkpoint{ID: snowflake.ID(row.Edges.Checkpoint.ID), LocationID: snowflake.ID(row.Edges.Checkpoint.LocationID), Code: row.Edges.Checkpoint.Code, Name: row.Edges.Checkpoint.Name, Version: row.Version}, nil
}

// SetCheckpoint 在当前地点允许且版本匹配时更新恢复点。
func (store *EntWorldStore) SetCheckpoint(ctx context.Context, command SetCheckpointCommand) (Checkpoint, error) {
	var result Checkpoint
	if command.Now.IsZero() {
		command.Now = time.Now().UTC()
	}
	err := store.pool.WithinTransaction(ctx, func(transactionCtx context.Context) error {
		client := store.pool.Client(transactionCtx)
		playerID, err := activePlayerCharacterID(transactionCtx, client, command.AccountID)
		if err != nil {
			return err
		}
		digest := sha256.Sum256([]byte(fmt.Sprintf("%s:%d", command.CheckpointID, command.ExpectedVersion)))
		replayed, err := store.claimPlayerResponse(transactionCtx, client, playerID, "rpg.checkpoint.set", command.IdempotencyKey, digest[:], &result, command.Now.UTC())
		if err != nil || replayed {
			return err
		}
		position, err := client.PlayerCharacterPosition.Query().Where(playercharacterposition.PlayerCharacterIDEQ(playerID)).ForUpdate().Only(transactionCtx)
		if err != nil {
			return err
		}
		checkpoint, err := client.RpgCheckpoint.Query().Where(rpgcheckpoint.IDEQ(command.CheckpointID), rpgcheckpoint.EnabledEQ(true)).Only(transactionCtx)
		if avalonent.IsNotFound(err) {
			return ErrExitUnavailable
		}
		if err != nil {
			return err
		}
		if checkpoint.LocationID != position.LocationID {
			return ErrExitUnavailable
		}
		if checkpoint.SetCondition != nil {
			compiled, compileErr := CompileCondition(checkpoint.SetCondition)
			if compileErr != nil {
				return ErrExitConditionNotMet
			}
			ctxValue, contextErr := loadConditionContext(transactionCtx, client, playerID)
			if contextErr != nil {
				return contextErr
			}
			if !compiled.Evaluate(ctxValue) {
				return ErrExitConditionNotMet
			}
		}
		current, err := client.PlayerCharacterCheckpoint.Query().Where(playercharactercheckpoint.PlayerCharacterIDEQ(playerID)).ForUpdate().Only(transactionCtx)
		if avalonent.IsNotFound(err) {
			if command.ExpectedVersion != 0 {
				return ErrPositionConflict
			}
			checkpointBindingID, idErr := store.newID.Next(transactionCtx)
			if idErr != nil {
				return idErr
			}
			current, err = client.PlayerCharacterCheckpoint.Create().SetID(checkpointBindingID).SetPlayerCharacterID(playerID).SetCheckpointID(checkpoint.ID).SetVersion(1).SetUpdatedAt(command.Now.UTC()).Save(transactionCtx)
		} else if err == nil {
			if current.Version != command.ExpectedVersion {
				return ErrPositionConflict
			}
			current, err = client.PlayerCharacterCheckpoint.UpdateOne(current).SetCheckpointID(checkpoint.ID).SetVersion(current.Version + 1).SetUpdatedAt(command.Now.UTC()).Save(transactionCtx)
		}
		if err != nil {
			return fmt.Errorf("保存 PlayerCharacter Checkpoint: %w", err)
		}
		result = Checkpoint{ID: snowflake.ID(checkpoint.ID), LocationID: snowflake.ID(checkpoint.LocationID), Code: checkpoint.Code, Name: checkpoint.Name, Version: current.Version}
		return store.completePlayerResponse(transactionCtx, client, playerID, "rpg.checkpoint.set", command.IdempotencyKey, result)
	})
	return result, err
}

// GetParty 返回当前角色的有序 Party。
func (store *EntWorldStore) GetParty(ctx context.Context, accountID snowflake.ID) (Party, error) {
	client := store.pool.Client(ctx)
	playerID, err := activePlayerCharacterID(ctx, client, accountID)
	if err != nil {
		return Party{}, err
	}
	party, err := client.PlayerCharacterParty.Query().Where(playercharacterparty.PlayerCharacterIDEQ(playerID)).WithMembers(func(q *avalonent.PlayerCharacterPartyMemberQuery) { q.Order(playercharacterpartymember.ByPosition()) }).Only(ctx)
	if err != nil {
		return Party{}, fmt.Errorf("查询 RPG Party: %w", err)
	}
	result := Party{ID: snowflake.ID(party.ID), Version: party.Version, Members: make([]PartyMember, 0, len(party.Edges.Members))}
	for _, member := range party.Edges.Members {
		result.Members = append(result.Members, PartyMember{Position: member.Position, PlayerCharacterCreatureID: snowflake.ID(member.PlayerCharacterCreatureID)})
	}
	return result, nil
}

// ReplaceParty 在角色锁和版本校验内替换全部 Party 成员。
func (store *EntWorldStore) ReplaceParty(ctx context.Context, command ReplacePartyCommand) (Party, error) {
	var result Party
	if command.Now.IsZero() {
		command.Now = time.Now().UTC()
	}
	if len(command.Members) == 0 || len(command.Members) > 6 {
		return Party{}, ErrExitUnavailable
	}
	seen := map[int16]bool{}
	for _, member := range command.Members {
		if member.Position < 1 || member.Position > 6 || seen[member.Position] {
			return Party{}, ErrExitUnavailable
		}
		seen[member.Position] = true
	}
	err := store.pool.WithinTransaction(ctx, func(transactionCtx context.Context) error {
		client := store.pool.Client(transactionCtx)
		playerID, err := activePlayerCharacterID(transactionCtx, client, command.AccountID)
		if err != nil {
			return err
		}
		requestBytes, digestErr := json.Marshal(struct {
			Version int64
			Members []PartyMember
		}{command.ExpectedVersion, command.Members})
		if digestErr != nil {
			return digestErr
		}
		digest := sha256.Sum256(requestBytes)
		replayed, err := store.claimPlayerResponse(transactionCtx, client, playerID, "rpg.party.replace", command.IdempotencyKey, digest[:], &result, command.Now.UTC())
		if err != nil || replayed {
			return err
		}
		party, err := client.PlayerCharacterParty.Query().Where(playercharacterparty.PlayerCharacterIDEQ(playerID)).ForUpdate().Only(transactionCtx)
		if avalonent.IsNotFound(err) {
			partyID, idErr := store.newID.Next(transactionCtx)
			if idErr != nil {
				return idErr
			}
			party, err = client.PlayerCharacterParty.Create().SetID(partyID).SetPlayerCharacterID(playerID).SetName("探索 Party").SetVersion(1).SetCreatedAt(command.Now.UTC()).SetUpdatedAt(command.Now.UTC()).Save(transactionCtx)
			command.ExpectedVersion = 0
		} else if err != nil {
			return err
		} else if party.Version != command.ExpectedVersion {
			return ErrPositionConflict
		}
		ids := make([]snowflake.ID, 0, len(command.Members))
		for _, member := range command.Members {
			ids = append(ids, member.PlayerCharacterCreatureID)
		}
		owned, err := client.PlayerCharacterCreature.Query().Where(playercharactercreature.IDIn(ids...), playercharactercreature.PlayerCharacterIDEQ(playerID)).Count(transactionCtx)
		if err != nil || owned != len(ids) {
			return ErrExitUnavailable
		}
		if _, err := client.PlayerCharacterPartyMember.Delete().Where(playercharacterpartymember.PartyID(internalPartyID(party))).Exec(transactionCtx); err != nil {
			return err
		}
		for _, member := range command.Members {
			memberID, idErr := store.newID.Next(transactionCtx)
			if idErr != nil {
				return idErr
			}
			if _, err := client.PlayerCharacterPartyMember.Create().SetID(memberID).SetPartyID(party.ID).SetPosition(member.Position).SetPlayerCharacterCreatureID(member.PlayerCharacterCreatureID).Save(transactionCtx); err != nil {
				return err
			}
		}
		party, err = client.PlayerCharacterParty.UpdateOne(party).SetVersion(party.Version + 1).SetUpdatedAt(command.Now.UTC()).Save(transactionCtx)
		if err != nil {
			return err
		}
		result = Party{ID: snowflake.ID(party.ID), Version: party.Version, Members: append([]PartyMember(nil), command.Members...)}
		return store.completePlayerResponse(transactionCtx, client, playerID, "rpg.party.replace", command.IdempotencyKey, result)
	})
	return result, err
}

func internalPartyID(party *avalonent.PlayerCharacterParty) snowflake.ID { return party.ID }

// NewEntWorldStore 创建 RPG 世界 PostgreSQL 适配器。
func NewEntWorldStore(pool *database.Pool, newID snowflake.Source) *EntWorldStore {
	return &EntWorldStore{pool: pool, newID: newID}
}

// GetMap 只读取活动角色已经发现的地点和出口。
func (store *EntWorldStore) GetMap(ctx context.Context, accountID snowflake.ID) (WorldMap, error) {
	client := store.pool.Client(ctx)
	playerID, err := activePlayerCharacterID(ctx, client, accountID)
	if err != nil {
		return WorldMap{}, err
	}
	discoveredLocations, err := client.PlayerCharacterDiscoveredLocation.Query().Where(playercharacterdiscoveredlocation.PlayerCharacterIDEQ(playerID)).All(ctx)
	if err != nil {
		return WorldMap{}, fmt.Errorf("查询已发现 Location: %w", err)
	}
	locationIDs := make([]snowflake.ID, 0, len(discoveredLocations))
	for _, row := range discoveredLocations {
		locationIDs = append(locationIDs, row.LocationID)
	}
	locations := make([]*avalonent.RpgLocation, 0)
	if len(locationIDs) > 0 {
		locations, err = client.RpgLocation.Query().Where(rpglocation.IDIn(locationIDs...)).Order(rpglocation.ByCode()).All(ctx)
		if err != nil {
			return WorldMap{}, fmt.Errorf("查询已发现 Location 资料: %w", err)
		}
	}
	coordinates := map[snowflake.ID][3]int32{}
	projection, projectionErr := client.RpgMapProjection.Query().Where(rpgmapprojection.EnabledEQ(true)).Order(avalonent.Asc(rpgmapprojection.FieldCode), avalonent.Desc(rpgmapprojection.FieldLayoutVersion)).First(ctx)
	if projectionErr == nil && len(locationIDs) > 0 {
		rows, queryErr := client.RpgMapProjectionLocation.Query().Where(rpgmapprojectionlocation.ProjectionIDEQ(projection.ID), rpgmapprojectionlocation.LocationIDIn(locationIDs...)).All(ctx)
		if queryErr != nil {
			return WorldMap{}, fmt.Errorf("查询地图展示投影: %w", queryErr)
		}
		for _, row := range rows {
			coordinates[row.LocationID] = [3]int32{row.X, row.Y, row.Z}
		}
	} else if projectionErr != nil && !avalonent.IsNotFound(projectionErr) {
		return WorldMap{}, fmt.Errorf("查询启用地图投影: %w", projectionErr)
	}
	positionRow, err := client.PlayerCharacterPosition.Query().Where(playercharacterposition.PlayerCharacterIDEQ(playerID)).Only(ctx)
	if err != nil {
		return WorldMap{}, fmt.Errorf("查询当前 RPG Position: %w", err)
	}
	result := WorldMap{Locations: make([]WorldLocation, 0, len(locations)), Position: Position{LocationID: snowflake.ID(positionRow.LocationID), MoveSequence: positionRow.MoveSequence, Version: positionRow.Version, UpdatedAt: positionRow.UpdatedAt.UTC()}}
	for _, row := range locations {
		var parentID snowflake.ID
		if row.ParentID != nil {
			parentID = snowflake.ID(*row.ParentID)
		}
		point := coordinates[row.ID]
		result.Locations = append(result.Locations, WorldLocation{ID: snowflake.ID(row.ID), RegionID: snowflake.ID(row.RegionID), ParentID: parentID, Code: row.Code, Name: row.Name, LocationType: row.LocationType, X: point[0], Y: point[1], Z: point[2]})
	}
	discoveredExits, err := client.PlayerCharacterDiscoveredExit.Query().Where(playercharacterdiscoveredexit.PlayerCharacterIDEQ(playerID)).All(ctx)
	if err != nil {
		return WorldMap{}, fmt.Errorf("查询已发现 Location Exit: %w", err)
	}
	exitIDs := make([]snowflake.ID, 0, len(discoveredExits))
	for _, row := range discoveredExits {
		exitIDs = append(exitIDs, row.LocationExitID)
	}
	if len(exitIDs) > 0 {
		exits, queryErr := client.RpgLocationExit.Query().Where(rpglocationexit.IDIn(exitIDs...)).Order(rpglocationexit.BySourceLocationID(), rpglocationexit.BySortOrder(), rpglocationexit.ByID()).All(ctx)
		if queryErr != nil {
			return WorldMap{}, fmt.Errorf("查询已发现 Location Exit 资料: %w", queryErr)
		}
		for _, row := range exits {
			result.Exits = append(result.Exits, WorldExit{ID: snowflake.ID(row.ID), SourceLocationID: snowflake.ID(row.SourceLocationID), TargetLocationID: snowflake.ID(row.TargetLocationID), Code: row.Code, Name: row.Name, SortOrder: row.SortOrder})
		}
	}
	return result, nil
}

// Traverse 以行锁和唯一幂等键保证并发移动只有一次成功。
func (store *EntWorldStore) Traverse(ctx context.Context, command TraversalCommand) (TraversalResult, error) {
	var result TraversalResult
	err := store.pool.WithinTransaction(ctx, func(transactionCtx context.Context) error {
		client := store.pool.Client(transactionCtx)
		playerID, err := activePlayerCharacterID(transactionCtx, client, command.AccountID)
		if err != nil {
			return err
		}
		digest := sha256.Sum256([]byte(fmt.Sprintf("%s:%d", command.ExitID, command.ExpectedPositionVersion)))
		existing, err := client.PlayerCharacterTraversal.Query().Where(playercharactertraversal.PlayerCharacterIDEQ(playerID), playercharactertraversal.IdempotencyKeyEQ(command.IdempotencyKey)).Only(transactionCtx)
		if err == nil {
			if !bytes.Equal(existing.RequestDigest, digest[:]) {
				return ErrIdempotencyConflict
			}
			if decodeErr := json.Unmarshal(existing.Response, &result); decodeErr != nil {
				return fmt.Errorf("解码 Traversal 幂等响应: %w", decodeErr)
			}
			result.Replayed = true
			return nil
		}
		if !avalonent.IsNotFound(err) {
			return fmt.Errorf("查询 Traversal 幂等记录: %w", err)
		}
		position, err := client.PlayerCharacterPosition.Query().Where(playercharacterposition.PlayerCharacterIDEQ(playerID)).ForUpdate().Only(transactionCtx)
		if avalonent.IsNotFound(err) {
			return ErrPositionConflict
		}
		if err != nil {
			return fmt.Errorf("锁定 PlayerCharacter Position: %w", err)
		}
		if position.Version != command.ExpectedPositionVersion {
			return ErrPositionConflict
		}
		blocking, err := client.PlayerCharacterPendingEncounter.Query().Where(playercharacterpendingencounter.PlayerCharacterIDEQ(playerID), playercharacterpendingencounter.StateEQ("pending"), playercharacterpendingencounter.ExpiresAtGT(command.Now.UTC())).Exist(transactionCtx)
		if err != nil {
			return fmt.Errorf("检查待处理 Encounter: %w", err)
		}
		if blocking {
			return ErrPendingEncounterBlocksMovement
		}
		exit, err := client.RpgLocationExit.Query().Where(rpglocationexit.IDEQ(command.ExitID), rpglocationexit.EnabledEQ(true)).Only(transactionCtx)
		if avalonent.IsNotFound(err) {
			return ErrExitUnavailable
		}
		if err != nil {
			return fmt.Errorf("查询 Location Exit: %w", err)
		}
		if exit.SourceLocationID != position.LocationID {
			return ErrExitUnavailable
		}
		target, err := client.RpgLocation.Query().Where(rpglocation.IDEQ(exit.TargetLocationID), rpglocation.EnabledEQ(true)).Only(transactionCtx)
		if avalonent.IsNotFound(err) {
			return ErrExitUnavailable
		}
		if err != nil {
			return fmt.Errorf("查询 Location Exit 目标: %w", err)
		}
		condition, err := CompileCondition(exit.Condition)
		if err != nil {
			return ErrExitConditionNotMet
		}
		conditionContext, err := loadConditionContext(transactionCtx, client, playerID)
		if err != nil {
			return fmt.Errorf("读取出口条件上下文: %w", err)
		}
		if !condition.Evaluate(conditionContext) {
			return ErrExitConditionNotMet
		}
		effect, err := CompileEffect(exit.Effect)
		if err != nil {
			return ErrExitConditionNotMet
		}
		nextVersion, nextSequence := position.Version+1, position.MoveSequence+1
		updated, err := client.PlayerCharacterPosition.UpdateOne(position).Where(playercharacterposition.VersionEQ(position.Version)).SetLocationID(target.ID).SetMoveSequence(nextSequence).SetVersion(nextVersion).SetUpdatedAt(command.Now.UTC()).Save(transactionCtx)
		if avalonent.IsNotFound(err) {
			return ErrPositionConflict
		}
		if err != nil {
			return fmt.Errorf("更新 PlayerCharacter Position: %w", err)
		}
		discoveredLocationID, idErr := store.newID.Next(transactionCtx)
		if idErr != nil {
			return fmt.Errorf("生成 Location Discovery Identifier: %w", idErr)
		}
		if err := client.PlayerCharacterDiscoveredLocation.Create().SetID(discoveredLocationID).SetPlayerCharacterID(playerID).SetLocationID(target.ID).SetSource("traversal").SetDiscoveredAt(command.Now.UTC()).OnConflictColumns(playercharacterdiscoveredlocation.FieldPlayerCharacterID, playercharacterdiscoveredlocation.FieldLocationID).Ignore().Exec(transactionCtx); err != nil {
			return fmt.Errorf("记录 Location Discovery: %w", err)
		}
		discoveredExitID, idErr := store.newID.Next(transactionCtx)
		if idErr != nil {
			return fmt.Errorf("生成 Exit Discovery Identifier: %w", idErr)
		}
		if err := client.PlayerCharacterDiscoveredExit.Create().SetID(discoveredExitID).SetPlayerCharacterID(playerID).SetLocationExitID(exit.ID).SetDiscoveredAt(command.Now.UTC()).OnConflictColumns(playercharacterdiscoveredexit.FieldPlayerCharacterID, playercharacterdiscoveredexit.FieldLocationExitID).Ignore().Exec(transactionCtx); err != nil {
			return fmt.Errorf("记录 Exit Discovery: %w", err)
		}
		if effect != nil {
			before := cloneWorldSwitches(conditionContext.WorldStateSwitch)
			beforeObjectives := cloneObjectiveProgress(conditionContext.QuestObjectives)
			effect.Apply(&conditionContext)
			for key, value := range conditionContext.WorldStateSwitch {
				if before[key] == value {
					continue
				}
				state, stateErr := client.PlayerCharacterWorldState.Query().Where(playercharacterworldstate.PlayerCharacterIDEQ(playerID), playercharacterworldstate.StateKeyEQ(key)).Only(transactionCtx)
				if avalonent.IsNotFound(stateErr) {
					worldStateID, idErr := store.newID.Next(transactionCtx)
					if idErr != nil {
						return idErr
					}
					_, stateErr = client.PlayerCharacterWorldState.Create().SetID(worldStateID).SetPlayerCharacterID(playerID).SetStateKey(key).SetBooleanValue(value).SetVersion(1).SetUpdatedAt(command.Now.UTC()).Save(transactionCtx)
				} else if stateErr == nil {
					_, stateErr = client.PlayerCharacterWorldState.UpdateOne(state).SetBooleanValue(value).ClearIntegerValue().ClearTextValue().SetVersion(state.Version + 1).SetUpdatedAt(command.Now.UTC()).Save(transactionCtx)
				}
				if stateErr != nil {
					return fmt.Errorf("应用 Traversal Effect: %w", stateErr)
				}
			}
			for key, value := range conditionContext.QuestObjectives {
				if beforeObjectives[key] == value {
					continue
				}
				progress, progressErr := client.PlayerCharacterQuestObjective.Query().Where(playercharacterquestobjective.PlayerCharacterIDEQ(playerID), playercharacterquestobjective.HasObjectiveWith(rpgquestobjective.CodeEQ(key))).Only(transactionCtx)
				if progressErr != nil {
					return fmt.Errorf("读取 Quest Objective Traversal Effect: %w", progressErr)
				}
				if _, progressErr = client.PlayerCharacterQuestObjective.UpdateOne(progress).SetCurrentCount(value).Save(transactionCtx); progressErr != nil {
					return fmt.Errorf("更新 Quest Objective Traversal Effect: %w", progressErr)
				}
			}
		}
		outgoing, err := client.RpgLocationExit.Query().Where(rpglocationexit.SourceLocationIDEQ(target.ID), rpglocationexit.EnabledEQ(true)).All(transactionCtx)
		if err != nil {
			return fmt.Errorf("查询目标 Location 出口: %w", err)
		}
		for _, discovered := range outgoing {
			discoveredID, idErr := store.newID.Next(transactionCtx)
			if idErr != nil {
				return fmt.Errorf("生成目标 Location Exit Discovery Identifier: %w", idErr)
			}
			if err := client.PlayerCharacterDiscoveredExit.Create().SetID(discoveredID).SetPlayerCharacterID(playerID).SetLocationExitID(discovered.ID).SetDiscoveredAt(command.Now.UTC()).OnConflictColumns(playercharacterdiscoveredexit.FieldPlayerCharacterID, playercharacterdiscoveredexit.FieldLocationExitID).Ignore().Exec(transactionCtx); err != nil {
				return fmt.Errorf("记录目标 Location Exit Discovery: %w", err)
			}
		}
		traversalID, idErr := store.newID.Next(transactionCtx)
		if idErr != nil {
			return idErr
		}
		traversal, err := client.PlayerCharacterTraversal.Create().SetID(traversalID).SetPlayerCharacterID(playerID).SetLocationExitID(exit.ID).SetSourceLocationID(position.LocationID).SetTargetLocationID(target.ID).SetPositionVersionBefore(position.Version).SetPositionVersionAfter(nextVersion).SetIdempotencyKey(command.IdempotencyKey).SetRequestDigest(digest[:]).SetResponse(json.RawMessage(`{}`)).SetCreatedAt(command.Now.UTC()).Save(transactionCtx)
		if err != nil {
			return fmt.Errorf("创建 Traversal 事实: %w", err)
		}
		pending, err := store.sampleEncounter(transactionCtx, client, playerID, traversal.ID, target.ID, nextSequence, command.Now.UTC())
		if err != nil {
			return err
		}
		result = TraversalResult{Position: Position{LocationID: snowflake.ID(updated.LocationID), MoveSequence: updated.MoveSequence, Version: updated.Version, UpdatedAt: updated.UpdatedAt.UTC()}, PendingEncounter: pending}
		response, err := json.Marshal(result)
		if err != nil {
			return fmt.Errorf("编码 Traversal 响应: %w", err)
		}
		if err := client.PlayerCharacterTraversal.UpdateOne(traversal).SetResponse(response).Exec(transactionCtx); err != nil {
			return fmt.Errorf("保存 Traversal 幂等响应: %w", err)
		}
		payload, _ := json.Marshal(map[string]string{"traversal_id": traversalID.String(), "player_character_id": playerID.String()})
		outboxID, idErr := store.newID.Next(transactionCtx)
		if idErr != nil {
			return idErr
		}
		if _, err := client.OutboxMessage.Create().SetID(outboxID).SetTopic("rpg.traversal.committed").SetAggregateID(traversalID).SetPayload(payload).SetState("pending").SetAttemptCount(0).SetAvailableAt(command.Now.UTC()).SetCreatedAt(command.Now.UTC()).SetUpdatedAt(command.Now.UTC()).Save(transactionCtx); err != nil {
			return fmt.Errorf("创建 Traversal Outbox: %w", err)
		}
		return nil
	})
	return result, err
}

func (store *EntWorldStore) sampleEncounter(ctx context.Context, client *avalonent.Client, playerID snowflake.ID, traversalID, locationID snowflake.ID, moveSequence int64, now time.Time) (*PendingEncounter, error) {
	table, err := client.RpgEncounterTable.Query().Where(rpgencountertable.LocationIDEQ(locationID), rpgencountertable.EncounterMethodEQ("walk"), rpgencountertable.EnabledEQ(true)).First(ctx)
	if avalonent.IsNotFound(err) {
		return nil, nil
	}
	if err != nil {
		return nil, fmt.Errorf("查询 walk Encounter Table: %w", err)
	}
	usage, err := client.PlayerCharacterEncounterUsage.Query().Where(playercharacterencounterusage.PlayerCharacterIDEQ(playerID), playercharacterencounterusage.EncounterTableIDEQ(table.ID)).ForUpdate().Only(ctx)
	if err != nil && !avalonent.IsNotFound(err) {
		return nil, fmt.Errorf("锁定 Encounter Usage: %w", err)
	}
	if usage != nil {
		if table.MaximumUses != nil && usage.UseCount >= *table.MaximumUses {
			return nil, nil
		}
		if usage.LastTriggerMoveSequence != nil && moveSequence-*usage.LastTriggerMoveSequence <= table.CooldownMoves {
			return nil, nil
		}
	}
	source, err := NewRandomSource()
	if err != nil {
		return nil, err
	}
	trigger, err := source.DrawUint32("walk-trigger", 0, 10000)
	if err != nil {
		return nil, err
	}
	if int32(trigger) >= table.TriggerProbabilityBps {
		return nil, nil
	}
	entries, err := client.RpgEncounterEntry.Query().Where(rpgencounterentry.EncounterTableIDEQ(table.ID), rpgencounterentry.EnabledEQ(true)).Order(rpgencounterentry.ByID()).All(ctx)
	if err != nil {
		return nil, fmt.Errorf("查询 Encounter Entries: %w", err)
	}
	if len(entries) == 0 {
		return nil, nil
	}
	var total uint32
	for _, entry := range entries {
		total += uint32(entry.Weight)
	}
	weighted, err := source.DrawUint32("walk-entry", 1, total)
	if err != nil {
		return nil, err
	}
	selected := entries[0]
	cursor := uint32(0)
	for _, entry := range entries {
		cursor += uint32(entry.Weight)
		if weighted < cursor {
			selected = entry
			break
		}
	}
	span := uint32(selected.MaximumLevel-selected.MinimumLevel) + 1
	levelOffset, err := source.DrawUint32("walk-level", 2, span)
	if err != nil {
		return nil, err
	}
	level := selected.MinimumLevel + int16(levelOffset)
	resultJSON, _ := json.Marshal(map[string]any{"entry_id": snowflake.ID(selected.ID).String(), "level": level, "trigger_roll": trigger, "entry_roll": weighted})
	pendingID, idErr := store.newID.Next(ctx)
	if idErr != nil {
		return nil, idErr
	}
	seed := append([]byte(nil), source.seed[:]...)
	row, err := client.PlayerCharacterPendingEncounter.Create().SetID(pendingID).SetPlayerCharacterID(playerID).SetTraversalID(traversalID).SetEncounterTableID(table.ID).SetEncounterEntryID(selected.ID).SetEncounterTableVersion(table.Version).SetEncounterLevel(level).SetRandomAlgorithm(source.Algorithm()).SetRandomSeed(seed).SetRandomDrawNumber(2).SetRandomResult(resultJSON).SetState("pending").SetExpiresAt(now.Add(pendingEncounterLifetime)).SetCreatedAt(now).Save(ctx)
	if err != nil {
		return nil, fmt.Errorf("创建 Pending Encounter: %w", err)
	}
	if usage == nil {
		usageID, idErr := store.newID.Next(ctx)
		if idErr != nil {
			return nil, idErr
		}
		_, err = client.PlayerCharacterEncounterUsage.Create().SetID(usageID).SetPlayerCharacterID(playerID).SetEncounterTableID(table.ID).SetUseCount(1).SetLastTriggerMoveSequence(moveSequence).SetLastUsedAt(now).SetVersion(1).Save(ctx)
	} else {
		_, err = client.PlayerCharacterEncounterUsage.UpdateOne(usage).SetUseCount(usage.UseCount + 1).SetLastTriggerMoveSequence(moveSequence).SetLastUsedAt(now).SetVersion(usage.Version + 1).Save(ctx)
	}
	if err != nil {
		return nil, fmt.Errorf("更新 Encounter Usage: %w", err)
	}
	return &PendingEncounter{ID: snowflake.ID(row.ID), EncounterEntryID: snowflake.ID(row.EncounterEntryID), BattleID: optionalIdentifier(row.BattleID), State: row.State, ExpiresAt: row.ExpiresAt.UTC()}, nil
}

func activePlayerCharacterID(ctx context.Context, client *avalonent.Client, accountID snowflake.ID) (snowflake.ID, error) {
	row, err := client.ActivePlayerCharacter.Query().Where(activeplayercharacter.IDEQ(accountID)).Only(ctx)
	if avalonent.IsNotFound(err) {
		return snowflake.ID(0), ErrActivePlayerCharacterMissing
	}
	if err != nil {
		return snowflake.ID(0), fmt.Errorf("查询活动 PlayerCharacter: %w", err)
	}
	return snowflake.ID(row.PlayerCharacterID), nil
}

// optionalIdentifier 将 Ent 可空 Identifier 安全转换为领域层 Identifier；空值使用 snowflake.Nil 表示。
func optionalIdentifier(value *snowflake.ID) snowflake.ID {
	if value == nil {
		return snowflake.ID(0)
	}
	return *value
}

func loadConditionContext(ctx context.Context, client *avalonent.Client, playerID snowflake.ID) (ConditionContext, error) {
	character, err := client.PlayerCharacter.Query().Where(entplayercharacter.IDEQ(playerID)).Only(ctx)
	if err != nil {
		return ConditionContext{}, err
	}
	rows, err := client.PlayerCharacterWorldState.Query().Where(playercharacterworldstate.PlayerCharacterIDEQ(playerID)).All(ctx)
	if err != nil {
		return ConditionContext{}, err
	}
	result := ConditionContext{Level: character.Level, Items: map[string]int32{}, QuestObjectives: map[string]int32{}, Professions: map[string]bool{}, WorldStateSwitch: map[string]bool{}}
	for _, row := range rows {
		if row.BooleanValue != nil {
			result.WorldStateSwitch[row.StateKey] = *row.BooleanValue
		}
	}
	items, err := client.PlayerCharacterInventoryItem.Query().Where(playercharacterinventoryitem.PlayerCharacterIDEQ(playerID)).WithItem().All(ctx)
	if err != nil {
		return ConditionContext{}, err
	}
	for _, item := range items {
		if item.Edges.Item != nil && item.Quantity > 0 {
			result.Items[item.Edges.Item.Code] = int32(item.Quantity)
		}
	}
	professions, err := client.PlayerCharacterProfession.Query().Where(playercharacterprofession.PlayerCharacterIDEQ(playerID)).WithProfession().All(ctx)
	if err != nil {
		return ConditionContext{}, err
	}
	for _, profession := range professions {
		if profession.Edges.Profession != nil {
			result.Professions[profession.Edges.Profession.Code] = true
		}
	}
	objectives, err := client.PlayerCharacterQuestObjective.Query().Where(playercharacterquestobjective.PlayerCharacterIDEQ(playerID)).WithObjective().All(ctx)
	if err != nil {
		return ConditionContext{}, err
	}
	for _, objective := range objectives {
		if objective.Edges.Objective != nil {
			result.QuestObjectives[objective.Edges.Objective.Code] = objective.CurrentCount
		}
	}
	return result, nil
}

func cloneWorldSwitches(source map[string]bool) map[string]bool {
	result := make(map[string]bool, len(source))
	for key, value := range source {
		result[key] = value
	}
	return result
}

func cloneObjectiveProgress(source map[string]int32) map[string]int32 {
	result := make(map[string]int32, len(source))
	for key, value := range source {
		result[key] = value
	}
	return result
}

func (store *EntWorldStore) claimPlayerResponse(ctx context.Context, client *avalonent.Client, playerID snowflake.ID, operationID, key string, digest []byte, target any, now time.Time) (bool, error) {
	if key == "" {
		return false, ErrIdempotencyConflict
	}
	record, err := client.PlayerCharacterIdempotencyRecord.Query().Where(playercharacteridempotencyrecord.PlayerCharacterIDEQ(playerID), playercharacteridempotencyrecord.OperationIDEQ(operationID), playercharacteridempotencyrecord.IdempotencyKeyEQ(key)).Only(ctx)
	if err == nil {
		if !bytes.Equal(record.RequestDigest, digest) {
			return false, ErrIdempotencyConflict
		}
		if len(record.Response) == 0 || bytes.Equal(record.Response, []byte(`{}`)) {
			return false, ErrIdempotencyConflict
		}
		if err := json.Unmarshal(record.Response, target); err != nil {
			return false, fmt.Errorf("解码玩家幂等响应: %w", err)
		}
		return true, nil
	}
	if !avalonent.IsNotFound(err) {
		return false, fmt.Errorf("查询玩家幂等记录: %w", err)
	}
	recordID, idErr := store.newID.Next(ctx)
	if idErr != nil {
		return false, idErr
	}
	_, err = client.PlayerCharacterIdempotencyRecord.Create().SetID(recordID).SetPlayerCharacterID(playerID).SetOperationID(operationID).SetIdempotencyKey(key).SetRequestDigest(digest).SetResponse(json.RawMessage(`{}`)).SetCreatedAt(now).Save(ctx)
	if err != nil {
		return false, fmt.Errorf("认领玩家幂等键: %w", err)
	}
	return false, nil
}

func (store *EntWorldStore) completePlayerResponse(ctx context.Context, client *avalonent.Client, playerID snowflake.ID, operationID, key string, response any) error {
	payload, err := json.Marshal(response)
	if err != nil {
		return err
	}
	updated, err := client.PlayerCharacterIdempotencyRecord.Update().Where(playercharacteridempotencyrecord.PlayerCharacterIDEQ(playerID), playercharacteridempotencyrecord.OperationIDEQ(operationID), playercharacteridempotencyrecord.IdempotencyKeyEQ(key)).SetResponse(payload).Save(ctx)
	if err != nil {
		return fmt.Errorf("保存玩家幂等响应: %w", err)
	}
	if updated != 1 {
		return ErrIdempotencyConflict
	}
	return nil
}
