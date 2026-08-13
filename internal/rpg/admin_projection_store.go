package rpg

import (
	"context"
	"strings"
	"time"

	"github.com/lishangbu/avalon/ent/asset"
	"github.com/lishangbu/avalon/ent/rpglocation"
	"github.com/lishangbu/avalon/ent/rpgmapprojection"
	"github.com/lishangbu/avalon/ent/rpgmapprojectionlocation"
	"github.com/lishangbu/avalon/internal/gamedata/stablecode"
	"github.com/lishangbu/avalon/internal/platform/idempotency"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// ListMapProjections 返回地图投影及其地点展示关系。
func (store *EntWorldStore) ListMapProjections(ctx context.Context, pageSize int) ([]AdminMapProjection, error) {
	client := store.pool.Client(ctx)
	rows, err := client.RpgMapProjection.Query().Order(rpgmapprojection.ByCode(), rpgmapprojection.ByID()).Limit(boundedPageSize(pageSize, 200)).All(ctx)
	if err != nil {
		return nil, err
	}
	result := make([]AdminMapProjection, 0, len(rows))
	indexes := map[snowflake.ID]int{}
	for _, row := range rows {
		indexes[row.ID] = len(result)
		result = append(result, AdminMapProjection{ID: row.ID, Code: row.Code, Name: row.Name, LayoutVersion: row.LayoutVersion, Enabled: row.Enabled, Locations: []AdminMapProjectionLocation{}})
	}
	locations, err := client.RpgMapProjectionLocation.Query().Order(rpgmapprojectionlocation.ByProjectionID(), rpgmapprojectionlocation.ByID()).All(ctx)
	if err != nil {
		return nil, err
	}
	for _, row := range locations {
		index, ok := indexes[row.ProjectionID]
		if !ok {
			continue
		}
		icon, background := snowflake.ID(0), snowflake.ID(0)
		if row.IconAssetID != nil {
			icon = *row.IconAssetID
		}
		if row.BackgroundAssetID != nil {
			background = *row.BackgroundAssetID
		}
		result[index].Locations = append(result[index].Locations, AdminMapProjectionLocation{ID: row.ID, LocationID: row.LocationID, IconAssetID: icon, BackgroundAssetID: background, X: row.X, Y: row.Y, Z: row.Z})
	}
	return result, nil
}

// CreateMapProjection 创建布局版本为一的地图投影。
func (store *EntWorldStore) CreateMapProjection(ctx context.Context, command SaveMapProjectionCommand) (AdminMapProjection, error) {
	command.Projection = normalizeProjection(command.Projection)
	if !validProjection(command, false) {
		return AdminMapProjection{}, ErrInvalidAdminWorld
	}
	id, err := store.newID.Next(ctx)
	if err != nil {
		return AdminMapProjection{}, err
	}
	command.Projection.ID, command.Projection.LayoutVersion = id, 1
	return store.saveMapProjection(ctx, command, true)
}

// UpdateMapProjection 使用布局版本完整替换地点展示关系。
func (store *EntWorldStore) UpdateMapProjection(ctx context.Context, command SaveMapProjectionCommand) (AdminMapProjection, error) {
	command.Projection = normalizeProjection(command.Projection)
	if !validProjection(command, true) {
		return AdminMapProjection{}, ErrInvalidAdminWorld
	}
	command.Projection.LayoutVersion = command.ExpectedLayoutVersion + 1
	return store.saveMapProjection(ctx, command, false)
}
func normalizeProjection(value AdminMapProjection) AdminMapProjection {
	value.Code = strings.TrimSpace(value.Code)
	value.Name = strings.TrimSpace(value.Name)
	return value
}
func validProjection(command SaveMapProjectionCommand, update bool) bool {
	value := command.Projection
	if (update && (!value.ID.IsValid() || command.ExpectedLayoutVersion <= 0)) || !command.Write.ActorAccountID.IsValid() || !idempotency.ValidKey(command.Write.IdempotencyKey) || command.Write.RequestID == "" || !stablecode.Valid(value.Code) || value.Name == "" || len([]rune(value.Name)) > 120 {
		return false
	}
	seen := map[snowflake.ID]bool{}
	for _, location := range value.Locations {
		if !location.LocationID.IsValid() || seen[location.LocationID] {
			return false
		}
		seen[location.LocationID] = true
	}
	return true
}
func (store *EntWorldStore) saveMapProjection(ctx context.Context, command SaveMapProjectionCommand, create bool) (AdminMapProjection, error) {
	result := command.Projection
	digest, err := idempotency.Digest(struct {
		Value    AdminMapProjection
		Expected int64
	}{result, command.ExpectedLayoutVersion})
	if err != nil {
		return result, err
	}
	now := time.Now().UTC()
	request := idempotency.Request{ActorAccountID: command.Write.ActorAccountID, OperationID: "rpg.map_projection.save", Key: command.Write.IdempotencyKey, RequestDigest: digest, CreatedAt: now}
	err = store.pool.WithinTransaction(ctx, func(txctx context.Context) error {
		client := store.pool.Client(txctx)
		writer := idempotency.NewPersistentWriter(idempotency.NewAdminEntRecords(client, store.newID))
		replay, claimErr := idempotency.ClaimResponse(txctx, writer, request, &result)
		if claimErr != nil || replay {
			return claimErr
		}
		for _, item := range result.Locations {
			if _, findErr := client.RpgLocation.Query().Where(rpglocation.IDEQ(item.LocationID)).Only(txctx); findErr != nil {
				return adminWorldStoreError(findErr)
			}
			for _, assetID := range []snowflake.ID{item.IconAssetID, item.BackgroundAssetID} {
				if assetID.IsValid() {
					if _, findErr := client.Asset.Query().Where(asset.IDEQ(assetID)).Only(txctx); findErr != nil {
						return adminWorldStoreError(findErr)
					}
				}
			}
		}
		var before *AdminMapProjection
		if create {
			if _, saveErr := client.RpgMapProjection.Create().SetID(result.ID).SetCode(result.Code).SetName(result.Name).SetLayoutVersion(1).SetEnabled(result.Enabled).SetCreatedAt(now).SetUpdatedAt(now).Save(txctx); saveErr != nil {
				return adminWorldStoreError(saveErr)
			}
		} else {
			row, findErr := client.RpgMapProjection.Query().Where(rpgmapprojection.IDEQ(result.ID)).Only(txctx)
			if findErr != nil {
				return adminWorldStoreError(findErr)
			}
			old := AdminMapProjection{ID: row.ID, Code: row.Code, Name: row.Name, LayoutVersion: row.LayoutVersion, Enabled: row.Enabled}
			before = &old
			if _, saveErr := client.RpgMapProjection.UpdateOne(row).Where(rpgmapprojection.LayoutVersionEQ(command.ExpectedLayoutVersion)).SetCode(result.Code).SetName(result.Name).SetLayoutVersion(result.LayoutVersion).SetEnabled(result.Enabled).SetUpdatedAt(now).Save(txctx); saveErr != nil {
				return adminWorldStoreError(saveErr)
			}
			if _, deleteErr := client.RpgMapProjectionLocation.Delete().Where(rpgmapprojectionlocation.ProjectionIDEQ(result.ID)).Exec(txctx); deleteErr != nil {
				return deleteErr
			}
		}
		for index := range result.Locations {
			id, idErr := store.newID.Next(txctx)
			if idErr != nil {
				return idErr
			}
			result.Locations[index].ID = id
			item := result.Locations[index]
			builder := client.RpgMapProjectionLocation.Create().SetID(id).SetProjectionID(result.ID).SetLocationID(item.LocationID).SetX(item.X).SetY(item.Y).SetZ(item.Z)
			if item.IconAssetID.IsValid() {
				builder.SetIconAssetID(item.IconAssetID)
			}
			if item.BackgroundAssetID.IsValid() {
				builder.SetBackgroundAssetID(item.BackgroundAssetID)
			}
			if _, saveErr := builder.Save(txctx); saveErr != nil {
				return adminWorldStoreError(saveErr)
			}
		}
		return store.auditAndComplete(txctx, writer, request, command.Write, "rpg.map_projection.saved", "rpg_map_projection", result.ID, before, result, now)
	})
	if err != nil {
		return AdminMapProjection{}, err
	}
	return result, nil
}
