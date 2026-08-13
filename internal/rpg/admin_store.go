package rpg

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"strings"
	"time"

	"github.com/jackc/pgx/v5/pgconn"
	"github.com/lishangbu/avalon/internal/platform/snowflake"

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

var (
	// ErrInvalidAdminWorld 表示 RPG 管理写入字段无效。
	ErrInvalidAdminWorld = errors.New("RPG 管理资料字段无效")
	// ErrAdminWorldNotFound 表示 RPG 管理资料不存在。
	ErrAdminWorldNotFound = errors.New("RPG 管理资料不存在")
	// ErrAdminWorldConflict 表示稳定编码或乐观版本冲突。
	ErrAdminWorldConflict = errors.New("RPG 管理资料冲突")
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
func (store *EntWorldStore) ListRegions(ctx context.Context, pageSize int) ([]AdminRegion, error) {
	rows, err := store.pool.Client(ctx).RpgRegion.Query().Order(rpgregion.ByCode(), rpgregion.ByID()).Limit(boundedPageSize(pageSize, 200)).All(ctx)
	if err != nil {
		return nil, fmt.Errorf("查询 Region: %w", err)
	}
	result := make([]AdminRegion, 0, len(rows))
	for _, row := range rows {
		description := ""
		if row.Description != nil {
			description = *row.Description
		}
		result = append(result, AdminRegion{ID: row.ID, Code: row.Code, Name: row.Name, Description: description, Enabled: row.Enabled, Version: row.Version})
	}
	return result, nil
}

// ListLocations 读取完整 Location 拓扑。
func (store *EntWorldStore) ListLocations(ctx context.Context, pageSize int) ([]AdminLocation, error) {
	rows, err := store.pool.Client(ctx).RpgLocation.Query().Order(rpglocation.ByCode(), rpglocation.ByID()).Limit(boundedPageSize(pageSize, 200)).All(ctx)
	if err != nil {
		return nil, fmt.Errorf("查询 Location: %w", err)
	}
	result := make([]AdminLocation, 0, len(rows))
	for _, row := range rows {
		parentID := snowflake.ID(0)
		if row.ParentID != nil {
			parentID = *row.ParentID
		}
		description := ""
		if row.Description != nil {
			description = *row.Description
		}
		result = append(result, AdminLocation{ID: row.ID, RegionID: row.RegionID, ParentID: parentID, Code: row.Code, Name: row.Name, LocationType: row.LocationType, Description: description, Enabled: row.Enabled, DefaultSpawn: row.DefaultSpawn, Version: row.Version})
	}
	return result, nil
}

// CreateLocation 创建版本为一的 Location。
func (store *EntWorldStore) CreateLocation(ctx context.Context, command SaveLocationCommand) (AdminLocation, error) {
	command.Location = normalizeAdminLocation(command.Location)
	if !validLocationWrite(command, false) {
		return AdminLocation{}, ErrInvalidAdminWorld
	}
	id, err := store.newID.Next(ctx)
	if err != nil {
		return AdminLocation{}, err
	}
	command.Location.ID, command.Location.Version = id, 1
	return store.saveLocation(ctx, command, true)
}

// UpdateLocation 使用预期版本完整更新 Location。
func (store *EntWorldStore) UpdateLocation(ctx context.Context, command SaveLocationCommand) (AdminLocation, error) {
	command.Location = normalizeAdminLocation(command.Location)
	if !validLocationWrite(command, true) {
		return AdminLocation{}, ErrInvalidAdminWorld
	}
	command.Location.Version = command.ExpectedVersion + 1
	return store.saveLocation(ctx, command, false)
}

func normalizeAdminLocation(value AdminLocation) AdminLocation {
	value.Code = strings.TrimSpace(value.Code)
	value.Name = strings.TrimSpace(value.Name)
	value.LocationType = strings.TrimSpace(value.LocationType)
	value.Description = strings.TrimSpace(value.Description)
	return value
}
func validLocationWrite(command SaveLocationCommand, requireID bool) bool {
	validType := map[string]bool{"world": true, "settlement": true, "route": true, "wild": true, "dungeon": true, "interior": true, "arena": true}[command.Location.LocationType]
	return (!requireID || command.Location.ID.IsValid()) && command.Location.RegionID.IsValid() && command.Location.ParentID != command.Location.ID && command.Write.ActorAccountID.IsValid() && idempotency.ValidKey(command.Write.IdempotencyKey) && strings.TrimSpace(command.Write.RequestID) != "" && stablecode.Valid(command.Location.Code) && command.Location.Name != "" && len([]rune(command.Location.Name)) <= 120 && len([]rune(command.Location.Description)) <= 4000 && validType && (!command.Location.DefaultSpawn || command.Location.Enabled) && (!requireID || command.ExpectedVersion > 0)
}

func (store *EntWorldStore) saveLocation(ctx context.Context, command SaveLocationCommand, create bool) (AdminLocation, error) {
	digest, err := idempotency.Digest(struct {
		Location        AdminLocation
		ExpectedVersion int64
	}{command.Location, command.ExpectedVersion})
	if err != nil {
		return AdminLocation{}, err
	}
	now := time.Now().UTC()
	request := idempotency.Request{ActorAccountID: command.Write.ActorAccountID, OperationID: "rpg.location.save", Key: command.Write.IdempotencyKey, RequestDigest: digest, CreatedAt: now}
	result := command.Location
	err = store.pool.WithinTransaction(ctx, func(txCtx context.Context) error {
		client := store.pool.Client(txCtx)
		writer := idempotency.NewPersistentWriter(idempotency.NewAdminEntRecords(client, store.newID))
		replay, claimErr := idempotency.ClaimResponse(txCtx, writer, request, &result)
		if claimErr != nil || replay {
			return claimErr
		}
		if _, queryErr := client.RpgRegion.Query().Where(rpgregion.IDEQ(result.RegionID)).Only(txCtx); queryErr != nil {
			return adminWorldStoreError(queryErr)
		}
		if result.ParentID.IsValid() {
			parent, queryErr := client.RpgLocation.Query().Where(rpglocation.IDEQ(result.ParentID)).Only(txCtx)
			if queryErr != nil {
				return adminWorldStoreError(queryErr)
			}
			if parent.RegionID != result.RegionID {
				return ErrInvalidAdminWorld
			}
		}
		if result.DefaultSpawn {
			count, countErr := client.RpgLocation.Query().Where(rpglocation.DefaultSpawnEQ(true), rpglocation.IDNEQ(result.ID)).Count(txCtx)
			if countErr != nil {
				return countErr
			}
			if count > 0 {
				return ErrAdminWorldConflict
			}
		}
		var before *AdminLocation
		if create {
			builder := client.RpgLocation.Create().SetID(result.ID).SetRegionID(result.RegionID).SetCode(result.Code).SetName(result.Name).SetLocationType(result.LocationType).SetDefaultSpawn(result.DefaultSpawn).SetEnabled(result.Enabled).SetVersion(1).SetCreatedAt(now).SetUpdatedAt(now)
			if result.ParentID.IsValid() {
				builder.SetParentID(result.ParentID)
			}
			if result.Description != "" {
				builder.SetDescription(result.Description)
			}
			if _, saveErr := builder.Save(txCtx); saveErr != nil {
				return adminWorldStoreError(saveErr)
			}
		} else {
			row, queryErr := client.RpgLocation.Query().Where(rpglocation.IDEQ(result.ID)).Only(txCtx)
			if queryErr != nil {
				return adminWorldStoreError(queryErr)
			}
			parent := snowflake.ID(0)
			if row.ParentID != nil {
				parent = *row.ParentID
			}
			description := ""
			if row.Description != nil {
				description = *row.Description
			}
			old := AdminLocation{ID: row.ID, RegionID: row.RegionID, ParentID: parent, Code: row.Code, Name: row.Name, LocationType: row.LocationType, Description: description, Enabled: row.Enabled, DefaultSpawn: row.DefaultSpawn, Version: row.Version}
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
				return adminWorldStoreError(saveErr)
			}
		}
		if validationErr := store.validateStoredTopology(txCtx); validationErr != nil {
			return validationErr
		}
		changes, marshalErr := json.Marshal(struct {
			Before *AdminLocation `json:"before,omitempty"`
			After  AdminLocation  `json:"after"`
		}{before, result})
		if marshalErr != nil {
			return marshalErr
		}
		auditID, idErr := store.newID.Next(txCtx)
		if idErr != nil {
			return idErr
		}
		objectID, reason := result.ID.String(), "administrative_change"
		if auditErr := platformaudit.Append(txCtx, database.Executor(txCtx, store.pool), platformaudit.AdminLedger, platformaudit.Entry{ID: auditID, ActorAccountID: &command.Write.ActorAccountID, ActorKind: "admin", ActionCode: "rpg.location.saved", ObjectType: "rpg_location", ObjectID: &objectID, RequestID: command.Write.RequestID, Reason: &reason, Changes: changes, CreatedAt: now}); auditErr != nil {
			return auditErr
		}
		return idempotency.Complete(txCtx, writer, request, result)
	})
	if err != nil {
		return AdminLocation{}, err
	}
	return result, nil
}

// ListExits 读取完整有向出口及规范化规则 JSON。
func (store *EntWorldStore) ListExits(ctx context.Context, pageSize int) ([]AdminExit, error) {
	rows, err := store.pool.Client(ctx).RpgLocationExit.Query().Order(rpglocationexit.BySourceLocationID(), rpglocationexit.BySortOrder(), rpglocationexit.ByID()).Limit(boundedPageSize(pageSize, 200)).All(ctx)
	if err != nil {
		return nil, fmt.Errorf("查询 Location Exit: %w", err)
	}
	result := make([]AdminExit, 0, len(rows))
	for _, row := range rows {
		condition, _ := json.Marshal(row.Condition)
		effect, _ := json.Marshal(row.Effect)
		description := ""
		if row.Description != nil {
			description = *row.Description
		}
		result = append(result, AdminExit{ID: row.ID, SourceLocationID: row.SourceLocationID, TargetLocationID: row.TargetLocationID, Code: row.Code, Name: row.Name, Description: description, SortOrder: row.SortOrder, ConditionJSON: string(condition), EffectJSON: string(effect), Enabled: row.Enabled, Version: row.Version})
	}
	return result, nil
}

// CreateExit 创建版本为一的 Location Exit。
func (store *EntWorldStore) CreateExit(ctx context.Context, command SaveExitCommand) (AdminExit, error) {
	command.Exit = normalizeAdminExit(command.Exit)
	if !validExitWrite(command, false) {
		return AdminExit{}, ErrInvalidAdminWorld
	}
	id, err := store.newID.Next(ctx)
	if err != nil {
		return AdminExit{}, err
	}
	command.Exit.ID, command.Exit.Version = id, 1
	return store.saveExit(ctx, command, true)
}

// UpdateExit 使用预期版本完整更新 Location Exit。
func (store *EntWorldStore) UpdateExit(ctx context.Context, command SaveExitCommand) (AdminExit, error) {
	command.Exit = normalizeAdminExit(command.Exit)
	if !validExitWrite(command, true) {
		return AdminExit{}, ErrInvalidAdminWorld
	}
	command.Exit.Version = command.ExpectedVersion + 1
	return store.saveExit(ctx, command, false)
}

func normalizeAdminExit(value AdminExit) AdminExit {
	value.Code = strings.TrimSpace(value.Code)
	value.Name = strings.TrimSpace(value.Name)
	value.Description = strings.TrimSpace(value.Description)
	value.ConditionJSON = strings.TrimSpace(value.ConditionJSON)
	value.EffectJSON = strings.TrimSpace(value.EffectJSON)
	return value
}

func validExitWrite(command SaveExitCommand, requireID bool) bool {
	if (!requireID || command.Exit.ID.IsValid()) && command.Exit.SourceLocationID.IsValid() && command.Exit.TargetLocationID.IsValid() && command.Exit.SourceLocationID != command.Exit.TargetLocationID && command.Write.ActorAccountID.IsValid() && idempotency.ValidKey(command.Write.IdempotencyKey) && strings.TrimSpace(command.Write.RequestID) != "" && stablecode.Valid(command.Exit.Code) && command.Exit.Name != "" && len([]rune(command.Exit.Name)) <= 120 && len([]rune(command.Exit.Description)) <= 4000 && command.Exit.SortOrder >= 0 && (!requireID || command.ExpectedVersion > 0) {
		if _, err := CompileCondition(json.RawMessage(command.Exit.ConditionJSON)); err != nil {
			return false
		}
		if _, err := CompileEffect(json.RawMessage(command.Exit.EffectJSON)); err != nil {
			return false
		}
		return true
	}
	return false
}

func (store *EntWorldStore) saveExit(ctx context.Context, command SaveExitCommand, create bool) (AdminExit, error) {
	digest, err := idempotency.Digest(struct {
		Exit            AdminExit
		ExpectedVersion int64
	}{command.Exit, command.ExpectedVersion})
	if err != nil {
		return AdminExit{}, err
	}
	now := time.Now().UTC()
	request := idempotency.Request{ActorAccountID: command.Write.ActorAccountID, OperationID: "rpg.location_exit.save", Key: command.Write.IdempotencyKey, RequestDigest: digest, CreatedAt: now}
	result := command.Exit
	err = store.pool.WithinTransaction(ctx, func(txCtx context.Context) error {
		client := store.pool.Client(txCtx)
		writer := idempotency.NewPersistentWriter(idempotency.NewAdminEntRecords(client, store.newID))
		replay, claimErr := idempotency.ClaimResponse(txCtx, writer, request, &result)
		if claimErr != nil || replay {
			return claimErr
		}
		if _, queryErr := client.RpgLocation.Query().Where(rpglocation.IDEQ(result.SourceLocationID)).Only(txCtx); queryErr != nil {
			return adminWorldStoreError(queryErr)
		}
		if _, queryErr := client.RpgLocation.Query().Where(rpglocation.IDEQ(result.TargetLocationID)).Only(txCtx); queryErr != nil {
			return adminWorldStoreError(queryErr)
		}
		condition := json.RawMessage(result.ConditionJSON)
		var effect json.RawMessage
		if result.EffectJSON != "" && result.EffectJSON != "null" {
			effect = json.RawMessage(result.EffectJSON)
		}
		var before *AdminExit
		if create {
			builder := client.RpgLocationExit.Create().SetID(result.ID).SetSourceLocationID(result.SourceLocationID).SetTargetLocationID(result.TargetLocationID).SetCode(result.Code).SetName(result.Name).SetSortOrder(result.SortOrder).SetCondition(condition).SetEnabled(result.Enabled).SetVersion(1).SetCreatedAt(now).SetUpdatedAt(now)
			if result.Description != "" {
				builder.SetDescription(result.Description)
			}
			if len(effect) > 0 {
				builder.SetEffect(effect)
			}
			if _, saveErr := builder.Save(txCtx); saveErr != nil {
				return adminWorldStoreError(saveErr)
			}
		} else {
			row, queryErr := client.RpgLocationExit.Query().Where(rpglocationexit.IDEQ(result.ID)).Only(txCtx)
			if queryErr != nil {
				return adminWorldStoreError(queryErr)
			}
			description := ""
			if row.Description != nil {
				description = *row.Description
			}
			oldCondition, _ := json.Marshal(row.Condition)
			oldEffect, _ := json.Marshal(row.Effect)
			old := AdminExit{ID: row.ID, SourceLocationID: row.SourceLocationID, TargetLocationID: row.TargetLocationID, Code: row.Code, Name: row.Name, Description: description, SortOrder: row.SortOrder, ConditionJSON: string(oldCondition), EffectJSON: string(oldEffect), Enabled: row.Enabled, Version: row.Version}
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
				return adminWorldStoreError(saveErr)
			}
		}
		if validationErr := store.validateStoredTopology(txCtx); validationErr != nil {
			return validationErr
		}
		changes, marshalErr := json.Marshal(struct {
			Before *AdminExit `json:"before,omitempty"`
			After  AdminExit  `json:"after"`
		}{before, result})
		if marshalErr != nil {
			return marshalErr
		}
		auditID, idErr := store.newID.Next(txCtx)
		if idErr != nil {
			return idErr
		}
		objectID, reason := result.ID.String(), "administrative_change"
		if auditErr := platformaudit.Append(txCtx, database.Executor(txCtx, store.pool), platformaudit.AdminLedger, platformaudit.Entry{ID: auditID, ActorAccountID: &command.Write.ActorAccountID, ActorKind: "admin", ActionCode: "rpg.location_exit.saved", ObjectType: "rpg_location_exit", ObjectID: &objectID, RequestID: command.Write.RequestID, Reason: &reason, Changes: changes, CreatedAt: now}); auditErr != nil {
			return auditErr
		}
		return idempotency.Complete(txCtx, writer, request, result)
	})
	if err != nil {
		return AdminExit{}, err
	}
	return result, nil
}

// ListIntegrityReports 读取最近的不可变拓扑报告及问题明细。
func (store *EntWorldStore) ListIntegrityReports(ctx context.Context, pageSize int) ([]AdminIntegrityReport, error) {
	rows, err := store.pool.Client(ctx).RpgTopologyIntegrityReport.Query().WithIssues().Order(rpgtopologyintegrityreport.ByCheckedAt(), rpgtopologyintegrityreport.ByID()).Limit(boundedPageSize(pageSize, 100)).All(ctx)
	if err != nil {
		return nil, fmt.Errorf("查询拓扑完整性报告: %w", err)
	}
	result := make([]AdminIntegrityReport, 0, len(rows))
	for _, row := range rows {
		report := AdminIntegrityReport{ID: row.ID, CheckedAt: row.CheckedAt.UTC(), Passed: row.State == "passed", Issues: make([]AdminIntegrityIssue, 0, len(row.Edges.Issues))}
		for _, issue := range row.Edges.Issues {
			resourceID := ""
			if issue.ResourceID != nil {
				resourceID = issue.ResourceID.String()
			}
			report.Issues = append(report.Issues, AdminIntegrityIssue{ReasonCode: issue.ReasonCode, ResourceID: resourceID, Message: issue.Message})
		}
		result = append(result, report)
	}
	return result, nil
}

// CreateRegion 创建版本为一的 Region，并在同一事务中写入幂等响应和管理审计。
func (store *EntWorldStore) CreateRegion(ctx context.Context, command SaveRegionCommand) (AdminRegion, error) {
	command.Region.Code = strings.TrimSpace(command.Region.Code)
	command.Region.Name = strings.TrimSpace(command.Region.Name)
	command.Region.Description = strings.TrimSpace(command.Region.Description)
	if !validRegionWrite(command, false) {
		return AdminRegion{}, ErrInvalidAdminWorld
	}
	id, err := store.newID.Next(ctx)
	if err != nil {
		return AdminRegion{}, err
	}
	command.Region.ID, command.Region.Version = id, 1
	return store.saveRegion(ctx, command, true)
}

// UpdateRegion 使用预期版本完整更新 Region。
func (store *EntWorldStore) UpdateRegion(ctx context.Context, command SaveRegionCommand) (AdminRegion, error) {
	command.Region.Code = strings.TrimSpace(command.Region.Code)
	command.Region.Name = strings.TrimSpace(command.Region.Name)
	command.Region.Description = strings.TrimSpace(command.Region.Description)
	if !validRegionWrite(command, true) {
		return AdminRegion{}, ErrInvalidAdminWorld
	}
	command.Region.Version = command.ExpectedVersion + 1
	return store.saveRegion(ctx, command, false)
}

func validRegionWrite(command SaveRegionCommand, requireID bool) bool {
	return (!requireID || command.Region.ID.IsValid()) && command.Write.ActorAccountID.IsValid() &&
		idempotency.ValidKey(command.Write.IdempotencyKey) && strings.TrimSpace(command.Write.RequestID) != "" &&
		stablecode.Valid(command.Region.Code) && command.Region.Name != "" && len([]rune(command.Region.Name)) <= 120 &&
		len([]rune(command.Region.Description)) <= 4000 && (!requireID || command.ExpectedVersion > 0)
}

func (store *EntWorldStore) saveRegion(ctx context.Context, command SaveRegionCommand, create bool) (AdminRegion, error) {
	digest, err := idempotency.Digest(struct {
		Region          AdminRegion
		ExpectedVersion int64
	}{command.Region, command.ExpectedVersion})
	if err != nil {
		return AdminRegion{}, err
	}
	now := time.Now().UTC()
	request := idempotency.Request{ActorAccountID: command.Write.ActorAccountID, OperationID: "rpg.region.save", Key: command.Write.IdempotencyKey, RequestDigest: digest, CreatedAt: now}
	result := command.Region
	err = store.pool.WithinTransaction(ctx, func(txCtx context.Context) error {
		client := store.pool.Client(txCtx)
		writer := idempotency.NewPersistentWriter(idempotency.NewAdminEntRecords(client, store.newID))
		replay, claimErr := idempotency.ClaimResponse(txCtx, writer, request, &result)
		if claimErr != nil || replay {
			return claimErr
		}
		var before *AdminRegion
		if create {
			builder := client.RpgRegion.Create().SetID(result.ID).SetCode(result.Code).SetName(result.Name).SetEnabled(result.Enabled).SetVersion(1).SetCreatedAt(now).SetUpdatedAt(now)
			if result.Description != "" {
				builder.SetDescription(result.Description)
			}
			if _, saveErr := builder.Save(txCtx); saveErr != nil {
				return adminWorldStoreError(saveErr)
			}
		} else {
			row, queryErr := client.RpgRegion.Query().Where(rpgregion.IDEQ(result.ID)).Only(txCtx)
			if queryErr != nil {
				return adminWorldStoreError(queryErr)
			}
			description := ""
			if row.Description != nil {
				description = *row.Description
			}
			old := AdminRegion{ID: row.ID, Code: row.Code, Name: row.Name, Description: description, Enabled: row.Enabled, Version: row.Version}
			before = &old
			builder := client.RpgRegion.UpdateOne(row).Where(rpgregion.VersionEQ(command.ExpectedVersion)).SetCode(result.Code).SetName(result.Name).SetEnabled(result.Enabled).SetVersion(result.Version).SetUpdatedAt(now)
			if result.Description == "" {
				builder.ClearDescription()
			} else {
				builder.SetDescription(result.Description)
			}
			if _, saveErr := builder.Save(txCtx); saveErr != nil {
				return adminWorldStoreError(saveErr)
			}
		}
		if validationErr := store.validateStoredTopology(txCtx); validationErr != nil {
			return validationErr
		}
		changes, marshalErr := json.Marshal(struct {
			Before *AdminRegion `json:"before,omitempty"`
			After  AdminRegion  `json:"after"`
		}{before, result})
		if marshalErr != nil {
			return marshalErr
		}
		auditID, idErr := store.newID.Next(txCtx)
		if idErr != nil {
			return idErr
		}
		objectID, reason := result.ID.String(), "administrative_change"
		if auditErr := platformaudit.Append(txCtx, database.Executor(txCtx, store.pool), platformaudit.AdminLedger, platformaudit.Entry{ID: auditID, ActorAccountID: &command.Write.ActorAccountID, ActorKind: "admin", ActionCode: "rpg.region.saved", ObjectType: "rpg_region", ObjectID: &objectID, RequestID: command.Write.RequestID, Reason: &reason, Changes: changes, CreatedAt: now}); auditErr != nil {
			return auditErr
		}
		return idempotency.Complete(txCtx, writer, request, result)
	})
	if err != nil {
		return AdminRegion{}, err
	}
	return result, nil
}

func adminWorldStoreError(err error) error {
	if err == nil {
		return nil
	}
	if avalonent.IsNotFound(err) {
		return ErrAdminWorldNotFound
	}
	var pg *pgconn.PgError
	if errors.As(err, &pg) && pg.Code == "23505" {
		return ErrAdminWorldConflict
	}
	if avalonent.IsConstraintError(err) {
		return ErrAdminWorldConflict
	}
	return fmt.Errorf("保存 RPG 管理资料: %w", err)
}

func (store *EntWorldStore) validateStoredTopology(ctx context.Context) error {
	client := store.pool.Client(ctx)
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
	regions := make([]RegionNode, len(regionRows))
	for i, row := range regionRows {
		regions[i] = RegionNode{ID: row.ID, Code: row.Code, Enabled: row.Enabled}
	}
	locations := make([]LocationNode, len(locationRows))
	for i, row := range locationRows {
		parent := snowflake.ID(0)
		if row.ParentID != nil {
			parent = *row.ParentID
		}
		locations[i] = LocationNode{ID: row.ID, RegionID: row.RegionID, ParentID: parent, Code: row.Code, Enabled: row.Enabled, DefaultSpawn: row.DefaultSpawn}
	}
	exits := make([]ExitNode, len(exitRows))
	for i, row := range exitRows {
		exits[i] = ExitNode{ID: row.ID, SourceID: row.SourceLocationID, TargetID: row.TargetLocationID, Code: row.Code, Enabled: row.Enabled}
	}
	if report := ValidateTopology(regions, locations, exits); !report.Passed {
		return ErrInvalidAdminWorld
	}
	return nil
}
