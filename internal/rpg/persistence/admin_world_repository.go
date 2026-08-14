package persistence

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"strings"
	"time"

	"github.com/jackc/pgx/v5/pgconn"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
	rpg "github.com/lishangbu/avalon/internal/rpg"

	avalonent "github.com/lishangbu/avalon/ent"
	"github.com/lishangbu/avalon/ent/rpglocation"
	"github.com/lishangbu/avalon/ent/rpglocationexit"
	"github.com/lishangbu/avalon/ent/rpgregion"
	"github.com/lishangbu/avalon/ent/rpgtopologyintegrityreport"
	"github.com/lishangbu/avalon/internal/gamedata/stablecode"
	platformaudit "github.com/lishangbu/avalon/internal/platform/audit"
	"github.com/lishangbu/avalon/internal/platform/database"
	"github.com/lishangbu/avalon/internal/platform/idempotency"
)

func boundedPageSize(size, maximum int) int {
	if size < 1 {
		return 50
	}
	if size > maximum {
		return maximum
	}
	return size
}

// ListRegions 读取完整 Region 资料，管理员 RPC 不裁剪 Discovery。
func (adapter *Adapters) ListRegions(ctx context.Context, pageSize int) ([]rpg.AdminRegion, error) {
	rows, err := adapter.pool.Client(ctx).RpgRegion.Query().Order(rpgregion.ByCode(), rpgregion.ByID()).Limit(boundedPageSize(pageSize, 200)).All(ctx)
	if err != nil {
		return nil, fmt.Errorf("查询 Region: %w", err)
	}
	result := make([]rpg.AdminRegion, 0, len(rows))
	for _, row := range rows {
		description := ""
		if row.Description != nil {
			description = *row.Description
		}
		result = append(result, rpg.AdminRegion{ID: row.ID, Code: row.Code, Name: row.Name, Description: description, Enabled: row.Enabled, Version: row.Version})
	}
	return result, nil
}

// ListLocations 读取完整 Location 拓扑。
func (adapter *Adapters) ListLocations(ctx context.Context, pageSize int) ([]rpg.AdminLocation, error) {
	rows, err := adapter.pool.Client(ctx).RpgLocation.Query().Order(rpglocation.ByCode(), rpglocation.ByID()).Limit(boundedPageSize(pageSize, 200)).All(ctx)
	if err != nil {
		return nil, fmt.Errorf("查询 Location: %w", err)
	}
	result := make([]rpg.AdminLocation, 0, len(rows))
	for _, row := range rows {
		parentID := snowflake.ID(0)
		if row.ParentID != nil {
			parentID = *row.ParentID
		}
		description := ""
		if row.Description != nil {
			description = *row.Description
		}
		result = append(result, rpg.AdminLocation{ID: row.ID, RegionID: row.RegionID, ParentID: parentID, Code: row.Code, Name: row.Name, LocationType: row.LocationType, Description: description, Enabled: row.Enabled, DefaultSpawn: row.DefaultSpawn, Version: row.Version})
	}
	return result, nil
}

// CreateLocation 创建版本为一的 Location。
func (adapter *Adapters) CreateLocation(ctx context.Context, command rpg.SaveLocationCommand) (rpg.AdminLocation, error) {
	command.Location = normalizeAdminLocation(command.Location)
	if !validLocationWrite(command, false) {
		return rpg.AdminLocation{}, rpg.ErrInvalidAdminWorld
	}
	id, err := adapter.newID.Next(ctx)
	if err != nil {
		return rpg.AdminLocation{}, err
	}
	command.Location.ID, command.Location.Version = id, 1
	return adapter.saveLocation(ctx, command, true)
}

// UpdateLocation 使用预期版本完整更新 Location。
func (adapter *Adapters) UpdateLocation(ctx context.Context, command rpg.SaveLocationCommand) (rpg.AdminLocation, error) {
	command.Location = normalizeAdminLocation(command.Location)
	if !validLocationWrite(command, true) {
		return rpg.AdminLocation{}, rpg.ErrInvalidAdminWorld
	}
	command.Location.Version = command.ExpectedVersion + 1
	return adapter.saveLocation(ctx, command, false)
}

func normalizeAdminLocation(value rpg.AdminLocation) rpg.AdminLocation {
	value.Code = strings.TrimSpace(value.Code)
	value.Name = strings.TrimSpace(value.Name)
	value.LocationType = strings.TrimSpace(value.LocationType)
	value.Description = strings.TrimSpace(value.Description)
	return value
}
func validLocationWrite(command rpg.SaveLocationCommand, requireID bool) bool {
	validType := map[string]bool{"world": true, "settlement": true, "route": true, "wild": true, "dungeon": true, "interior": true, "arena": true}[command.Location.LocationType]
	return (!requireID || command.Location.ID.IsValid()) && command.Location.RegionID.IsValid() && command.Location.ParentID != command.Location.ID && command.Write.ActorAccountID.IsValid() && idempotency.ValidKey(command.Write.IdempotencyKey) && strings.TrimSpace(command.Write.RequestID) != "" && stablecode.Valid(command.Location.Code) && command.Location.Name != "" && len([]rune(command.Location.Name)) <= 120 && len([]rune(command.Location.Description)) <= 4000 && validType && (!command.Location.DefaultSpawn || command.Location.Enabled) && (!requireID || command.ExpectedVersion > 0)
}

func (adapter *Adapters) saveLocation(ctx context.Context, command rpg.SaveLocationCommand, create bool) (rpg.AdminLocation, error) {
	digest, err := idempotency.Digest(struct {
		Location        rpg.AdminLocation
		ExpectedVersion int64
	}{command.Location, command.ExpectedVersion})
	if err != nil {
		return rpg.AdminLocation{}, err
	}
	now := time.Now().UTC()
	request := idempotency.Request{ActorAccountID: command.Write.ActorAccountID, OperationID: "rpg.location.save", Key: command.Write.IdempotencyKey, RequestDigest: digest, CreatedAt: now}
	result := command.Location
	err = adapter.pool.WithinTransaction(ctx, func(txCtx context.Context) error {
		client := adapter.pool.Client(txCtx)
		writer := idempotency.NewPersistentWriter(idempotency.NewAdminEntRecords(client, adapter.newID))
		replay, claimErr := idempotency.ClaimResponse(txCtx, writer, request, &result)
		if claimErr != nil || replay {
			return claimErr
		}
		if _, queryErr := client.RpgRegion.Query().Where(rpgregion.IDEQ(result.RegionID)).Only(txCtx); queryErr != nil {
			return adminWorldRepositoryError(queryErr)
		}
		if result.ParentID.IsValid() {
			parent, queryErr := client.RpgLocation.Query().Where(rpglocation.IDEQ(result.ParentID)).Only(txCtx)
			if queryErr != nil {
				return adminWorldRepositoryError(queryErr)
			}
			if parent.RegionID != result.RegionID {
				return rpg.ErrInvalidAdminWorld
			}
		}
		if result.DefaultSpawn {
			count, countErr := client.RpgLocation.Query().Where(rpglocation.DefaultSpawnEQ(true), rpglocation.IDNEQ(result.ID)).Count(txCtx)
			if countErr != nil {
				return countErr
			}
			if count > 0 {
				return rpg.ErrAdminWorldConflict
			}
		}
		var before *rpg.AdminLocation
		if create {
			builder := client.RpgLocation.Create().SetID(result.ID).SetRegionID(result.RegionID).SetCode(result.Code).SetName(result.Name).SetLocationType(result.LocationType).SetDefaultSpawn(result.DefaultSpawn).SetEnabled(result.Enabled).SetVersion(1).SetCreatedAt(now).SetUpdatedAt(now)
			if result.ParentID.IsValid() {
				builder.SetParentID(result.ParentID)
			}
			if result.Description != "" {
				builder.SetDescription(result.Description)
			}
			if _, saveErr := builder.Save(txCtx); saveErr != nil {
				return adminWorldRepositoryError(saveErr)
			}
		} else {
			row, queryErr := client.RpgLocation.Query().Where(rpglocation.IDEQ(result.ID)).Only(txCtx)
			if queryErr != nil {
				return adminWorldRepositoryError(queryErr)
			}
			parent := snowflake.ID(0)
			if row.ParentID != nil {
				parent = *row.ParentID
			}
			description := ""
			if row.Description != nil {
				description = *row.Description
			}
			old := rpg.AdminLocation{ID: row.ID, RegionID: row.RegionID, ParentID: parent, Code: row.Code, Name: row.Name, LocationType: row.LocationType, Description: description, Enabled: row.Enabled, DefaultSpawn: row.DefaultSpawn, Version: row.Version}
			before = &old
			builder := client.RpgLocation.UpdateOne(row).Where(rpglocation.VersionEQ(command.ExpectedVersion)).SetRegionID(result.RegionID).SetCode(result.Code).SetName(result.Name).SetLocationType(result.LocationType).SetDefaultSpawn(result.DefaultSpawn).SetEnabled(result.Enabled).SetVersion(result.Version).SetUpdatedAt(now)
			if result.ParentID.IsValid() {
				builder.SetParentID(result.ParentID)
			} else {
				builder.ClearParentID()
			}
			if result.Description != "" {
				builder.SetDescription(result.Description)
			} else {
				builder.ClearDescription()
			}
			if _, saveErr := builder.Save(txCtx); saveErr != nil {
				return adminWorldRepositoryError(saveErr)
			}
		}
		if validationErr := adapter.validateStoredTopology(txCtx); validationErr != nil {
			return validationErr
		}
		changes, marshalErr := json.Marshal(struct {
			Before *rpg.AdminLocation `json:"before,omitempty"`
			After  rpg.AdminLocation  `json:"after"`
		}{before, result})
		if marshalErr != nil {
			return marshalErr
		}
		auditID, idErr := adapter.newID.Next(txCtx)
		if idErr != nil {
			return idErr
		}
		objectID, reason := result.ID.String(), "administrative_change"
		if auditErr := platformaudit.Append(txCtx, database.Executor(txCtx, adapter.pool), platformaudit.AdminLedger, platformaudit.Entry{ID: auditID, ActorAccountID: &command.Write.ActorAccountID, ActorKind: "admin", ActionCode: "rpg.location.saved", ObjectType: "rpg_location", ObjectID: &objectID, RequestID: command.Write.RequestID, Reason: &reason, Changes: changes, CreatedAt: now}); auditErr != nil {
			return auditErr
		}
		return idempotency.Complete(txCtx, writer, request, result)
	})
	if err != nil {
		return rpg.AdminLocation{}, err
	}
	return result, nil
}

// ListExits 读取完整有向出口及规范化规则 JSON。
func (adapter *Adapters) ListExits(ctx context.Context, pageSize int) ([]rpg.AdminExit, error) {
	rows, err := adapter.pool.Client(ctx).RpgLocationExit.Query().Order(rpglocationexit.BySourceLocationID(), rpglocationexit.BySortOrder(), rpglocationexit.ByID()).Limit(boundedPageSize(pageSize, 200)).All(ctx)
	if err != nil {
		return nil, fmt.Errorf("查询 Location Exit: %w", err)
	}
	result := make([]rpg.AdminExit, 0, len(rows))
	for _, row := range rows {
		condition, _ := json.Marshal(row.Condition)
		effect, _ := json.Marshal(row.Effect)
		description := ""
		if row.Description != nil {
			description = *row.Description
		}
		result = append(result, rpg.AdminExit{ID: row.ID, SourceLocationID: row.SourceLocationID, TargetLocationID: row.TargetLocationID, Code: row.Code, Name: row.Name, Description: description, SortOrder: row.SortOrder, ConditionJSON: string(condition), EffectJSON: string(effect), Enabled: row.Enabled, Version: row.Version})
	}
	return result, nil
}

// CreateExit 创建版本为一的 Location Exit。
func (adapter *Adapters) CreateExit(ctx context.Context, command rpg.SaveExitCommand) (rpg.AdminExit, error) {
	command.Exit = normalizeAdminExit(command.Exit)
	if !validExitWrite(command, false) {
		return rpg.AdminExit{}, rpg.ErrInvalidAdminWorld
	}
	id, err := adapter.newID.Next(ctx)
	if err != nil {
		return rpg.AdminExit{}, err
	}
	command.Exit.ID, command.Exit.Version = id, 1
	return adapter.saveExit(ctx, command, true)
}

// UpdateExit 使用预期版本完整更新 Location Exit。
func (adapter *Adapters) UpdateExit(ctx context.Context, command rpg.SaveExitCommand) (rpg.AdminExit, error) {
	command.Exit = normalizeAdminExit(command.Exit)
	if !validExitWrite(command, true) {
		return rpg.AdminExit{}, rpg.ErrInvalidAdminWorld
	}
	command.Exit.Version = command.ExpectedVersion + 1
	return adapter.saveExit(ctx, command, false)
}

func normalizeAdminExit(value rpg.AdminExit) rpg.AdminExit {
	value.Code = strings.TrimSpace(value.Code)
	value.Name = strings.TrimSpace(value.Name)
	value.Description = strings.TrimSpace(value.Description)
	value.ConditionJSON = strings.TrimSpace(value.ConditionJSON)
	value.EffectJSON = strings.TrimSpace(value.EffectJSON)
	return value
}

func validExitWrite(command rpg.SaveExitCommand, requireID bool) bool {
	if (!requireID || command.Exit.ID.IsValid()) && command.Exit.SourceLocationID.IsValid() && command.Exit.TargetLocationID.IsValid() && command.Exit.SourceLocationID != command.Exit.TargetLocationID && command.Write.ActorAccountID.IsValid() && idempotency.ValidKey(command.Write.IdempotencyKey) && strings.TrimSpace(command.Write.RequestID) != "" && stablecode.Valid(command.Exit.Code) && command.Exit.Name != "" && len([]rune(command.Exit.Name)) <= 120 && len([]rune(command.Exit.Description)) <= 4000 && command.Exit.SortOrder >= 0 && (!requireID || command.ExpectedVersion > 0) {
		if _, err := rpg.CompileCondition(json.RawMessage(command.Exit.ConditionJSON)); err != nil {
			return false
		}
		if _, err := rpg.CompileEffect(json.RawMessage(command.Exit.EffectJSON)); err != nil {
			return false
		}
		return true
	}
	return false
}

func (adapter *Adapters) saveExit(ctx context.Context, command rpg.SaveExitCommand, create bool) (rpg.AdminExit, error) {
	digest, err := idempotency.Digest(struct {
		Exit            rpg.AdminExit
		ExpectedVersion int64
	}{command.Exit, command.ExpectedVersion})
	if err != nil {
		return rpg.AdminExit{}, err
	}
	now := time.Now().UTC()
	request := idempotency.Request{ActorAccountID: command.Write.ActorAccountID, OperationID: "rpg.location_exit.save", Key: command.Write.IdempotencyKey, RequestDigest: digest, CreatedAt: now}
	result := command.Exit
	err = adapter.pool.WithinTransaction(ctx, func(txCtx context.Context) error {
		client := adapter.pool.Client(txCtx)
		writer := idempotency.NewPersistentWriter(idempotency.NewAdminEntRecords(client, adapter.newID))
		replay, claimErr := idempotency.ClaimResponse(txCtx, writer, request, &result)
		if claimErr != nil || replay {
			return claimErr
		}
		if _, queryErr := client.RpgLocation.Query().Where(rpglocation.IDEQ(result.SourceLocationID)).Only(txCtx); queryErr != nil {
			return adminWorldRepositoryError(queryErr)
		}
		if _, queryErr := client.RpgLocation.Query().Where(rpglocation.IDEQ(result.TargetLocationID)).Only(txCtx); queryErr != nil {
			return adminWorldRepositoryError(queryErr)
		}
		condition := json.RawMessage(result.ConditionJSON)
		var effect json.RawMessage
		if result.EffectJSON != "" && result.EffectJSON != "null" {
			effect = json.RawMessage(result.EffectJSON)
		}
		var before *rpg.AdminExit
		if create {
			builder := client.RpgLocationExit.Create().SetID(result.ID).SetSourceLocationID(result.SourceLocationID).SetTargetLocationID(result.TargetLocationID).SetCode(result.Code).SetName(result.Name).SetSortOrder(result.SortOrder).SetCondition(condition).SetEnabled(result.Enabled).SetVersion(1).SetCreatedAt(now).SetUpdatedAt(now)
			if result.Description != "" {
				builder.SetDescription(result.Description)
			}
			if len(effect) > 0 {
				builder.SetEffect(effect)
			}
			if _, saveErr := builder.Save(txCtx); saveErr != nil {
				return adminWorldRepositoryError(saveErr)
			}
		} else {
			row, queryErr := client.RpgLocationExit.Query().Where(rpglocationexit.IDEQ(result.ID)).Only(txCtx)
			if queryErr != nil {
				return adminWorldRepositoryError(queryErr)
			}
			description := ""
			if row.Description != nil {
				description = *row.Description
			}
			oldCondition, _ := json.Marshal(row.Condition)
			oldEffect, _ := json.Marshal(row.Effect)
			old := rpg.AdminExit{ID: row.ID, SourceLocationID: row.SourceLocationID, TargetLocationID: row.TargetLocationID, Code: row.Code, Name: row.Name, Description: description, SortOrder: row.SortOrder, ConditionJSON: string(oldCondition), EffectJSON: string(oldEffect), Enabled: row.Enabled, Version: row.Version}
			before = &old
			builder := client.RpgLocationExit.UpdateOne(row).Where(rpglocationexit.VersionEQ(command.ExpectedVersion)).SetSourceLocationID(result.SourceLocationID).SetTargetLocationID(result.TargetLocationID).SetCode(result.Code).SetName(result.Name).SetSortOrder(result.SortOrder).SetCondition(condition).SetEnabled(result.Enabled).SetVersion(result.Version).SetUpdatedAt(now)
			if result.Description == "" {
				builder.ClearDescription()
			} else {
				builder.SetDescription(result.Description)
			}
			if len(effect) == 0 {
				builder.ClearEffect()
			} else {
				builder.SetEffect(effect)
			}
			if _, saveErr := builder.Save(txCtx); saveErr != nil {
				return adminWorldRepositoryError(saveErr)
			}
		}
		if validationErr := adapter.validateStoredTopology(txCtx); validationErr != nil {
			return validationErr
		}
		changes, marshalErr := json.Marshal(struct {
			Before *rpg.AdminExit `json:"before,omitempty"`
			After  rpg.AdminExit  `json:"after"`
		}{before, result})
		if marshalErr != nil {
			return marshalErr
		}
		auditID, idErr := adapter.newID.Next(txCtx)
		if idErr != nil {
			return idErr
		}
		objectID, reason := result.ID.String(), "administrative_change"
		if auditErr := platformaudit.Append(txCtx, database.Executor(txCtx, adapter.pool), platformaudit.AdminLedger, platformaudit.Entry{ID: auditID, ActorAccountID: &command.Write.ActorAccountID, ActorKind: "admin", ActionCode: "rpg.location_exit.saved", ObjectType: "rpg_location_exit", ObjectID: &objectID, RequestID: command.Write.RequestID, Reason: &reason, Changes: changes, CreatedAt: now}); auditErr != nil {
			return auditErr
		}
		return idempotency.Complete(txCtx, writer, request, result)
	})
	if err != nil {
		return rpg.AdminExit{}, err
	}
	return result, nil
}

// ListIntegrityReports 读取最近的不可变拓扑报告及问题明细。
func (adapter *Adapters) ListIntegrityReports(ctx context.Context, pageSize int) ([]rpg.AdminIntegrityReport, error) {
	rows, err := adapter.pool.Client(ctx).RpgTopologyIntegrityReport.Query().WithIssues().Order(rpgtopologyintegrityreport.ByCheckedAt(), rpgtopologyintegrityreport.ByID()).Limit(boundedPageSize(pageSize, 100)).All(ctx)
	if err != nil {
		return nil, fmt.Errorf("查询拓扑完整性报告: %w", err)
	}
	result := make([]rpg.AdminIntegrityReport, 0, len(rows))
	for _, row := range rows {
		report := rpg.AdminIntegrityReport{ID: row.ID, CheckedAt: row.CheckedAt.UTC(), Passed: row.State == "passed", Issues: make([]rpg.AdminIntegrityIssue, 0, len(row.Edges.Issues))}
		for _, issue := range row.Edges.Issues {
			resourceID := ""
			if issue.ResourceID != nil {
				resourceID = issue.ResourceID.String()
			}
			report.Issues = append(report.Issues, rpg.AdminIntegrityIssue{ReasonCode: issue.ReasonCode, ResourceID: resourceID, Message: issue.Message})
		}
		result = append(result, report)
	}
	return result, nil
}

// CreateRegion 创建版本为一的 Region，并在同一事务中写入幂等响应和管理审计。
func (adapter *Adapters) CreateRegion(ctx context.Context, command rpg.SaveRegionCommand) (rpg.AdminRegion, error) {
	command.Region.Code = strings.TrimSpace(command.Region.Code)
	command.Region.Name = strings.TrimSpace(command.Region.Name)
	command.Region.Description = strings.TrimSpace(command.Region.Description)
	if !validRegionWrite(command, false) {
		return rpg.AdminRegion{}, rpg.ErrInvalidAdminWorld
	}
	id, err := adapter.newID.Next(ctx)
	if err != nil {
		return rpg.AdminRegion{}, err
	}
	command.Region.ID, command.Region.Version = id, 1
	return adapter.saveRegion(ctx, command, true)
}

// UpdateRegion 使用预期版本完整更新 Region。
func (adapter *Adapters) UpdateRegion(ctx context.Context, command rpg.SaveRegionCommand) (rpg.AdminRegion, error) {
	command.Region.Code = strings.TrimSpace(command.Region.Code)
	command.Region.Name = strings.TrimSpace(command.Region.Name)
	command.Region.Description = strings.TrimSpace(command.Region.Description)
	if !validRegionWrite(command, true) {
		return rpg.AdminRegion{}, rpg.ErrInvalidAdminWorld
	}
	command.Region.Version = command.ExpectedVersion + 1
	return adapter.saveRegion(ctx, command, false)
}

func validRegionWrite(command rpg.SaveRegionCommand, requireID bool) bool {
	return (!requireID || command.Region.ID.IsValid()) && command.Write.ActorAccountID.IsValid() &&
		idempotency.ValidKey(command.Write.IdempotencyKey) && strings.TrimSpace(command.Write.RequestID) != "" &&
		stablecode.Valid(command.Region.Code) && command.Region.Name != "" && len([]rune(command.Region.Name)) <= 120 &&
		len([]rune(command.Region.Description)) <= 4000 && (!requireID || command.ExpectedVersion > 0)
}

func (adapter *Adapters) saveRegion(ctx context.Context, command rpg.SaveRegionCommand, create bool) (rpg.AdminRegion, error) {
	digest, err := idempotency.Digest(struct {
		Region          rpg.AdminRegion
		ExpectedVersion int64
	}{command.Region, command.ExpectedVersion})
	if err != nil {
		return rpg.AdminRegion{}, err
	}
	now := time.Now().UTC()
	request := idempotency.Request{ActorAccountID: command.Write.ActorAccountID, OperationID: "rpg.region.save", Key: command.Write.IdempotencyKey, RequestDigest: digest, CreatedAt: now}
	result := command.Region
	err = adapter.pool.WithinTransaction(ctx, func(txCtx context.Context) error {
		client := adapter.pool.Client(txCtx)
		writer := idempotency.NewPersistentWriter(idempotency.NewAdminEntRecords(client, adapter.newID))
		replay, claimErr := idempotency.ClaimResponse(txCtx, writer, request, &result)
		if claimErr != nil || replay {
			return claimErr
		}
		var before *rpg.AdminRegion
		if create {
			builder := client.RpgRegion.Create().SetID(result.ID).SetCode(result.Code).SetName(result.Name).SetEnabled(result.Enabled).SetVersion(1).SetCreatedAt(now).SetUpdatedAt(now)
			if result.Description != "" {
				builder.SetDescription(result.Description)
			}
			if _, saveErr := builder.Save(txCtx); saveErr != nil {
				return adminWorldRepositoryError(saveErr)
			}
		} else {
			row, queryErr := client.RpgRegion.Query().Where(rpgregion.IDEQ(result.ID)).Only(txCtx)
			if queryErr != nil {
				return adminWorldRepositoryError(queryErr)
			}
			description := ""
			if row.Description != nil {
				description = *row.Description
			}
			old := rpg.AdminRegion{ID: row.ID, Code: row.Code, Name: row.Name, Description: description, Enabled: row.Enabled, Version: row.Version}
			before = &old
			builder := client.RpgRegion.UpdateOne(row).Where(rpgregion.VersionEQ(command.ExpectedVersion)).SetCode(result.Code).SetName(result.Name).SetEnabled(result.Enabled).SetVersion(result.Version).SetUpdatedAt(now)
			if result.Description == "" {
				builder.ClearDescription()
			} else {
				builder.SetDescription(result.Description)
			}
			if _, saveErr := builder.Save(txCtx); saveErr != nil {
				return adminWorldRepositoryError(saveErr)
			}
		}
		if validationErr := adapter.validateStoredTopology(txCtx); validationErr != nil {
			return validationErr
		}
		changes, marshalErr := json.Marshal(struct {
			Before *rpg.AdminRegion `json:"before,omitempty"`
			After  rpg.AdminRegion  `json:"after"`
		}{before, result})
		if marshalErr != nil {
			return marshalErr
		}
		auditID, idErr := adapter.newID.Next(txCtx)
		if idErr != nil {
			return idErr
		}
		objectID, reason := result.ID.String(), "administrative_change"
		if auditErr := platformaudit.Append(txCtx, database.Executor(txCtx, adapter.pool), platformaudit.AdminLedger, platformaudit.Entry{ID: auditID, ActorAccountID: &command.Write.ActorAccountID, ActorKind: "admin", ActionCode: "rpg.region.saved", ObjectType: "rpg_region", ObjectID: &objectID, RequestID: command.Write.RequestID, Reason: &reason, Changes: changes, CreatedAt: now}); auditErr != nil {
			return auditErr
		}
		return idempotency.Complete(txCtx, writer, request, result)
	})
	if err != nil {
		return rpg.AdminRegion{}, err
	}
	return result, nil
}

func adminWorldRepositoryError(err error) error {
	if err == nil {
		return nil
	}
	if avalonent.IsNotFound(err) {
		return rpg.ErrAdminWorldNotFound
	}
	var pg *pgconn.PgError
	if errors.As(err, &pg) && pg.Code == "23505" {
		return rpg.ErrAdminWorldConflict
	}
	if avalonent.IsConstraintError(err) {
		return rpg.ErrAdminWorldConflict
	}
	return fmt.Errorf("保存 RPG 管理资料: %w", err)
}

func (adapter *Adapters) validateStoredTopology(ctx context.Context) error {
	client := adapter.pool.Client(ctx)
	regionRows, err := client.RpgRegion.Query().All(ctx)
	if err != nil {
		return err
	}
	locationRows, err := client.RpgLocation.Query().All(ctx)
	if err != nil {
		return err
	}
	exitRows, err := client.RpgLocationExit.Query().All(ctx)
	if err != nil {
		return err
	}
	regions := make([]rpg.RegionNode, len(regionRows))
	for i, row := range regionRows {
		regions[i] = rpg.RegionNode{ID: row.ID, Code: row.Code, Enabled: row.Enabled}
	}
	locations := make([]rpg.LocationNode, len(locationRows))
	for i, row := range locationRows {
		parent := snowflake.ID(0)
		if row.ParentID != nil {
			parent = *row.ParentID
		}
		locations[i] = rpg.LocationNode{ID: row.ID, RegionID: row.RegionID, ParentID: parent, Code: row.Code, Enabled: row.Enabled, DefaultSpawn: row.DefaultSpawn}
	}
	exits := make([]rpg.ExitNode, len(exitRows))
	for i, row := range exitRows {
		exits[i] = rpg.ExitNode{ID: row.ID, SourceID: row.SourceLocationID, TargetID: row.TargetLocationID, Code: row.Code, Enabled: row.Enabled}
	}
	if report := rpg.ValidateTopology(regions, locations, exits); !report.Passed {
		return rpg.ErrInvalidAdminWorld
	}
	return nil
}
