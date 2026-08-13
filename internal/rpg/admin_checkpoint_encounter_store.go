package rpg

import (
	"context"
	"encoding/json"
	"strings"
	"time"

	avalonent "github.com/lishangbu/avalon/ent"
	"github.com/lishangbu/avalon/ent/gamecreature"
	"github.com/lishangbu/avalon/ent/gamecreatureform"
	"github.com/lishangbu/avalon/ent/rpgcheckpoint"
	"github.com/lishangbu/avalon/ent/rpgencounterentry"
	"github.com/lishangbu/avalon/ent/rpgencountertable"
	"github.com/lishangbu/avalon/ent/rpglocation"
	"github.com/lishangbu/avalon/internal/gamedata/stablecode"
	platformaudit "github.com/lishangbu/avalon/internal/platform/audit"
	"github.com/lishangbu/avalon/internal/platform/database"
	"github.com/lishangbu/avalon/internal/platform/idempotency"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// ListCheckpoints 返回完整恢复点规则资料。
func (store *EntWorldStore) ListCheckpoints(ctx context.Context, pageSize int) ([]AdminCheckpoint, error) {
	rows, err := store.pool.Client(ctx).RpgCheckpoint.Query().Order(rpgcheckpoint.ByCode(), rpgcheckpoint.ByID()).Limit(boundedPageSize(pageSize, 200)).All(ctx)
	if err != nil {
		return nil, err
	}
	result := make([]AdminCheckpoint, 0, len(rows))
	for _, row := range rows {
		description := ""
		if row.Description != nil {
			description = *row.Description
		}
		setRaw, _ := json.Marshal(row.SetCondition)
		recoveryRaw, _ := json.Marshal(row.RecoveryCondition)
		result = append(result, AdminCheckpoint{ID: row.ID, LocationID: row.LocationID, Code: row.Code, Name: row.Name, Description: description, SetConditionJSON: optionalRuleJSON(setRaw), RecoveryConditionJSON: optionalRuleJSON(recoveryRaw), Enabled: row.Enabled, Version: row.Version})
	}
	return result, nil
}

// CreateCheckpoint 创建版本为一的恢复点。
func (store *EntWorldStore) CreateCheckpoint(ctx context.Context, command SaveCheckpointCommand) (AdminCheckpoint, error) {
	command.Checkpoint = normalizeCheckpoint(command.Checkpoint)
	if !validCheckpoint(command, false) {
		return AdminCheckpoint{}, ErrInvalidAdminWorld
	}
	id, err := store.newID.Next(ctx)
	if err != nil {
		return AdminCheckpoint{}, err
	}
	command.Checkpoint.ID, command.Checkpoint.Version = id, 1
	return store.saveCheckpoint(ctx, command, true)
}

// UpdateCheckpoint 使用预期版本完整更新恢复点。
func (store *EntWorldStore) UpdateCheckpoint(ctx context.Context, command SaveCheckpointCommand) (AdminCheckpoint, error) {
	command.Checkpoint = normalizeCheckpoint(command.Checkpoint)
	if !validCheckpoint(command, true) {
		return AdminCheckpoint{}, ErrInvalidAdminWorld
	}
	command.Checkpoint.Version = command.ExpectedVersion + 1
	return store.saveCheckpoint(ctx, command, false)
}
func normalizeCheckpoint(value AdminCheckpoint) AdminCheckpoint {
	value.Code = strings.TrimSpace(value.Code)
	value.Name = strings.TrimSpace(value.Name)
	value.Description = strings.TrimSpace(value.Description)
	value.SetConditionJSON = strings.TrimSpace(value.SetConditionJSON)
	value.RecoveryConditionJSON = strings.TrimSpace(value.RecoveryConditionJSON)
	return value
}
func validCheckpoint(command SaveCheckpointCommand, update bool) bool {
	value := command.Checkpoint
	if (update && (!value.ID.IsValid() || command.ExpectedVersion <= 0)) || !value.LocationID.IsValid() || !command.Write.ActorAccountID.IsValid() || !idempotency.ValidKey(command.Write.IdempotencyKey) || command.Write.RequestID == "" || !stablecode.Valid(value.Code) || value.Name == "" || len([]rune(value.Name)) > 120 || len([]rune(value.Description)) > 4000 {
		return false
	}
	for _, raw := range []string{value.SetConditionJSON, value.RecoveryConditionJSON} {
		if raw != "" {
			if _, err := CompileCondition(json.RawMessage(raw)); err != nil {
				return false
			}
		}
	}
	return true
}
func (store *EntWorldStore) saveCheckpoint(ctx context.Context, command SaveCheckpointCommand, create bool) (AdminCheckpoint, error) {
	result := command.Checkpoint
	digest, err := idempotency.Digest(struct {
		Value    AdminCheckpoint
		Expected int64
	}{result, command.ExpectedVersion})
	if err != nil {
		return result, err
	}
	now := time.Now().UTC()
	request := idempotency.Request{ActorAccountID: command.Write.ActorAccountID, OperationID: "rpg.checkpoint.save", Key: command.Write.IdempotencyKey, RequestDigest: digest, CreatedAt: now}
	err = store.pool.WithinTransaction(ctx, func(txctx context.Context) error {
		client := store.pool.Client(txctx)
		writer := idempotency.NewPersistentWriter(idempotency.NewAdminEntRecords(client, store.newID))
		replay, claimErr := idempotency.ClaimResponse(txctx, writer, request, &result)
		if claimErr != nil || replay {
			return claimErr
		}
		if _, findErr := client.RpgLocation.Query().Where(rpglocation.IDEQ(result.LocationID)).Only(txctx); findErr != nil {
			return adminWorldStoreError(findErr)
		}
		var before *AdminCheckpoint
		if create {
			builder := client.RpgCheckpoint.Create().SetID(result.ID).SetLocationID(result.LocationID).SetCode(result.Code).SetName(result.Name).SetEnabled(result.Enabled).SetVersion(1).SetCreatedAt(now).SetUpdatedAt(now)
			applyCheckpointCreate(builder, result)
			if _, saveErr := builder.Save(txctx); saveErr != nil {
				return adminWorldStoreError(saveErr)
			}
		} else {
			row, findErr := client.RpgCheckpoint.Query().Where(rpgcheckpoint.IDEQ(result.ID)).Only(txctx)
			if findErr != nil {
				return adminWorldStoreError(findErr)
			}
			old := checkpointFromRow(row)
			before = &old
			builder := client.RpgCheckpoint.UpdateOne(row).Where(rpgcheckpoint.VersionEQ(command.ExpectedVersion)).SetLocationID(result.LocationID).SetCode(result.Code).SetName(result.Name).SetEnabled(result.Enabled).SetVersion(result.Version).SetUpdatedAt(now)
			applyCheckpointUpdate(builder, result)
			if _, saveErr := builder.Save(txctx); saveErr != nil {
				return adminWorldStoreError(saveErr)
			}
		}
		return store.auditAndComplete(txctx, writer, request, command.Write, "rpg.checkpoint.saved", "rpg_checkpoint", result.ID, before, result, now)
	})
	if err != nil {
		return AdminCheckpoint{}, err
	}
	return result, nil
}

func applyCheckpointCreate(builder *avalonent.RpgCheckpointCreate, value AdminCheckpoint) {
	if value.Description != "" {
		builder.SetDescription(value.Description)
	}
	if value.SetConditionJSON != "" {
		builder.SetSetCondition(json.RawMessage(value.SetConditionJSON))
	}
	if value.RecoveryConditionJSON != "" {
		builder.SetRecoveryCondition(json.RawMessage(value.RecoveryConditionJSON))
	}
}
func applyCheckpointUpdate(builder *avalonent.RpgCheckpointUpdateOne, value AdminCheckpoint) {
	if value.Description == "" {
		builder.ClearDescription()
	} else {
		builder.SetDescription(value.Description)
	}
	if value.SetConditionJSON == "" {
		builder.ClearSetCondition()
	} else {
		builder.SetSetCondition(json.RawMessage(value.SetConditionJSON))
	}
	if value.RecoveryConditionJSON == "" {
		builder.ClearRecoveryCondition()
	} else {
		builder.SetRecoveryCondition(json.RawMessage(value.RecoveryConditionJSON))
	}
}
func checkpointFromRow(row *avalonent.RpgCheckpoint) AdminCheckpoint {
	description := ""
	if row.Description != nil {
		description = *row.Description
	}
	setRaw, _ := json.Marshal(row.SetCondition)
	recoveryRaw, _ := json.Marshal(row.RecoveryCondition)
	return AdminCheckpoint{ID: row.ID, LocationID: row.LocationID, Code: row.Code, Name: row.Name, Description: description, SetConditionJSON: optionalRuleJSON(setRaw), RecoveryConditionJSON: optionalRuleJSON(recoveryRaw), Enabled: row.Enabled, Version: row.Version}
}
func optionalRuleJSON(raw []byte) string {
	value := strings.TrimSpace(string(raw))
	if value == "" || value == "null" {
		return ""
	}
	return value
}

// ListEncounterTables 返回遭遇表及其全部候选关系。
func (store *EntWorldStore) ListEncounterTables(ctx context.Context, pageSize int) ([]AdminEncounterTable, error) {
	client := store.pool.Client(ctx)
	rows, err := client.RpgEncounterTable.Query().Order(rpgencountertable.ByCode(), rpgencountertable.ByID()).Limit(boundedPageSize(pageSize, 200)).All(ctx)
	if err != nil {
		return nil, err
	}
	result := make([]AdminEncounterTable, 0, len(rows))
	indexes := map[snowflake.ID]int{}
	for _, row := range rows {
		indexes[row.ID] = len(result)
		result = append(result, AdminEncounterTable{ID: row.ID, LocationID: row.LocationID, Code: row.Code, Name: row.Name, TriggerProbabilityBPS: row.TriggerProbabilityBps, CooldownMoves: row.CooldownMoves, MaximumUses: row.MaximumUses, Enabled: row.Enabled, Version: row.Version, Entries: []AdminEncounterEntry{}})
	}
	entries, err := client.RpgEncounterEntry.Query().Order(rpgencounterentry.ByID()).All(ctx)
	if err != nil {
		return nil, err
	}
	for _, row := range entries {
		index, ok := indexes[row.EncounterTableID]
		if !ok {
			continue
		}
		form := snowflake.ID(0)
		if row.FormID != nil {
			form = *row.FormID
		}
		result[index].Entries = append(result[index].Entries, AdminEncounterEntry{ID: row.ID, CreatureID: row.CreatureID, FormID: form, MinimumLevel: row.MinimumLevel, MaximumLevel: row.MaximumLevel, Weight: row.Weight, Enabled: row.Enabled})
	}
	return result, nil
}

// CreateEncounterTable 创建包含候选关系的遭遇聚合。
func (store *EntWorldStore) CreateEncounterTable(ctx context.Context, command SaveEncounterTableCommand) (AdminEncounterTable, error) {
	command.Table = normalizeEncounter(command.Table)
	if !validEncounter(command, false) {
		return AdminEncounterTable{}, ErrInvalidAdminWorld
	}
	id, err := store.newID.Next(ctx)
	if err != nil {
		return AdminEncounterTable{}, err
	}
	command.Table.ID, command.Table.Version = id, 1
	return store.saveEncounterTable(ctx, command, true)
}

// UpdateEncounterTable 使用父表预期版本完整替换候选关系。
func (store *EntWorldStore) UpdateEncounterTable(ctx context.Context, command SaveEncounterTableCommand) (AdminEncounterTable, error) {
	command.Table = normalizeEncounter(command.Table)
	if !validEncounter(command, true) {
		return AdminEncounterTable{}, ErrInvalidAdminWorld
	}
	command.Table.Version = command.ExpectedVersion + 1
	return store.saveEncounterTable(ctx, command, false)
}
func normalizeEncounter(value AdminEncounterTable) AdminEncounterTable {
	value.Code = strings.TrimSpace(value.Code)
	value.Name = strings.TrimSpace(value.Name)
	return value
}
func validEncounter(command SaveEncounterTableCommand, update bool) bool {
	value := command.Table
	if (update && (!value.ID.IsValid() || command.ExpectedVersion <= 0)) || !value.LocationID.IsValid() || !command.Write.ActorAccountID.IsValid() || !idempotency.ValidKey(command.Write.IdempotencyKey) || command.Write.RequestID == "" || !stablecode.Valid(value.Code) || value.Name == "" || len([]rune(value.Name)) > 120 || value.TriggerProbabilityBPS < 0 || value.TriggerProbabilityBPS > 10000 || value.CooldownMoves < 0 || value.MaximumUses != nil && *value.MaximumUses <= 0 {
		return false
	}
	for _, entry := range value.Entries {
		if !entry.CreatureID.IsValid() || entry.MinimumLevel < 1 || entry.MaximumLevel < entry.MinimumLevel || entry.MaximumLevel > 100 || entry.Weight <= 0 {
			return false
		}
	}
	return true
}
func (store *EntWorldStore) saveEncounterTable(ctx context.Context, command SaveEncounterTableCommand, create bool) (AdminEncounterTable, error) {
	result := command.Table
	digest, err := idempotency.Digest(struct {
		Value    AdminEncounterTable
		Expected int64
	}{result, command.ExpectedVersion})
	if err != nil {
		return result, err
	}
	now := time.Now().UTC()
	request := idempotency.Request{ActorAccountID: command.Write.ActorAccountID, OperationID: "rpg.encounter_table.save", Key: command.Write.IdempotencyKey, RequestDigest: digest, CreatedAt: now}
	err = store.pool.WithinTransaction(ctx, func(txctx context.Context) error {
		client := store.pool.Client(txctx)
		writer := idempotency.NewPersistentWriter(idempotency.NewAdminEntRecords(client, store.newID))
		replay, claimErr := idempotency.ClaimResponse(txctx, writer, request, &result)
		if claimErr != nil || replay {
			return claimErr
		}
		if _, findErr := client.RpgLocation.Query().Where(rpglocation.IDEQ(result.LocationID)).Only(txctx); findErr != nil {
			return adminWorldStoreError(findErr)
		}
		if referenceErr := validateEncounterReferences(txctx, client, result.Entries); referenceErr != nil {
			return referenceErr
		}
		var before *AdminEncounterTable
		if create {
			if _, saveErr := client.RpgEncounterTable.Create().SetID(result.ID).SetLocationID(result.LocationID).SetCode(result.Code).SetName(result.Name).SetEncounterMethod("walk").SetTriggerProbabilityBps(result.TriggerProbabilityBPS).SetCooldownMoves(result.CooldownMoves).SetNillableMaximumUses(result.MaximumUses).SetEnabled(result.Enabled).SetVersion(1).SetCreatedAt(now).SetUpdatedAt(now).Save(txctx); saveErr != nil {
				return adminWorldStoreError(saveErr)
			}
		} else {
			row, findErr := client.RpgEncounterTable.Query().Where(rpgencountertable.IDEQ(result.ID)).Only(txctx)
			if findErr != nil {
				return adminWorldStoreError(findErr)
			}
			old := AdminEncounterTable{ID: row.ID, LocationID: row.LocationID, Code: row.Code, Name: row.Name, TriggerProbabilityBPS: row.TriggerProbabilityBps, CooldownMoves: row.CooldownMoves, MaximumUses: row.MaximumUses, Enabled: row.Enabled, Version: row.Version}
			before = &old
			if _, saveErr := client.RpgEncounterTable.UpdateOne(row).Where(rpgencountertable.VersionEQ(command.ExpectedVersion)).SetLocationID(result.LocationID).SetCode(result.Code).SetName(result.Name).SetEncounterMethod("walk").SetTriggerProbabilityBps(result.TriggerProbabilityBPS).SetCooldownMoves(result.CooldownMoves).SetNillableMaximumUses(result.MaximumUses).SetEnabled(result.Enabled).SetVersion(result.Version).SetUpdatedAt(now).Save(txctx); saveErr != nil {
				return adminWorldStoreError(saveErr)
			}
			if _, deleteErr := client.RpgEncounterEntry.Delete().Where(rpgencounterentry.EncounterTableIDEQ(result.ID)).Exec(txctx); deleteErr != nil {
				return deleteErr
			}
		}
		for index := range result.Entries {
			id, idErr := store.newID.Next(txctx)
			if idErr != nil {
				return idErr
			}
			result.Entries[index].ID = id
			entry := result.Entries[index]
			builder := client.RpgEncounterEntry.Create().SetID(id).SetEncounterTableID(result.ID).SetCreatureID(entry.CreatureID).SetMinimumLevel(entry.MinimumLevel).SetMaximumLevel(entry.MaximumLevel).SetWeight(entry.Weight).SetEnabled(entry.Enabled)
			if entry.FormID.IsValid() {
				builder.SetFormID(entry.FormID)
			}
			if _, saveErr := builder.Save(txctx); saveErr != nil {
				return adminWorldStoreError(saveErr)
			}
		}
		return store.auditAndComplete(txctx, writer, request, command.Write, "rpg.encounter_table.saved", "rpg_encounter_table", result.ID, before, result, now)
	})
	if err != nil {
		return AdminEncounterTable{}, err
	}
	return result, nil
}
func validateEncounterReferences(ctx context.Context, client *avalonent.Client, entries []AdminEncounterEntry) error {
	for _, entry := range entries {
		if _, err := client.GameCreature.Query().Where(gamecreature.IDEQ(entry.CreatureID)).Only(ctx); err != nil {
			return adminWorldStoreError(err)
		}
		if entry.FormID.IsValid() {
			if _, err := client.GameCreatureForm.Query().Where(gamecreatureform.IDEQ(entry.FormID), gamecreatureform.CreatureIDEQ(entry.CreatureID)).Only(ctx); err != nil {
				return adminWorldStoreError(err)
			}
		}
	}
	return nil
}

func (store *EntWorldStore) auditAndComplete(ctx context.Context, writer idempotency.Writer, request idempotency.Request, write AdminWriteContext, action, objectType string, id snowflake.ID, before, after any, now time.Time) error {
	changes, err := json.Marshal(struct {
		Before any `json:"before,omitempty"`
		After  any `json:"after"`
	}{before, after})
	if err != nil {
		return err
	}
	auditID, err := store.newID.Next(ctx)
	if err != nil {
		return err
	}
	objectID, reason := id.String(), "administrative_change"
	if err := platformaudit.Append(ctx, database.Executor(ctx, store.pool), platformaudit.AdminLedger, platformaudit.Entry{ID: auditID, ActorAccountID: &write.ActorAccountID, ActorKind: "admin", ActionCode: action, ObjectType: objectType, ObjectID: &objectID, RequestID: write.RequestID, Reason: &reason, Changes: changes, CreatedAt: now}); err != nil {
		return err
	}
	return idempotency.Complete(ctx, writer, request, after)
}
