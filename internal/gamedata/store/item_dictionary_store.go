package store

import (
	"context"
	"errors"
	"fmt"
	"strings"
	"time"

	"github.com/jackc/pgx/v5/pgconn"
	avalonent "github.com/lishangbu/avalon/ent"
	"github.com/lishangbu/avalon/ent/gameitemattribute"
	"github.com/lishangbu/avalon/ent/gameitemflingeffect"
	"github.com/lishangbu/avalon/ent/gameitempocket"
	"github.com/lishangbu/avalon/internal/gamedata/administration"
	"github.com/lishangbu/avalon/internal/gamedata/itemdictionary"
	"github.com/lishangbu/avalon/internal/platform/database"
	"github.com/lishangbu/avalon/internal/platform/idempotency"
)

// ListItemDictionary 从对应规范化表按稳定编码读取字典记录。
func (s *Store) ListItemDictionary(ctx context.Context, kind itemdictionary.Kind) ([]itemdictionary.Entry, error) {
	client := s.pool.Client(ctx)
	switch kind {
	case itemdictionary.KindPocket:
		rows, err := client.GameItemPocket.Query().Order(gameitempocket.BySortOrder(), gameitempocket.ByCode()).All(ctx)
		if err != nil {
			return nil, err
		}
		result := make([]itemdictionary.Entry, len(rows))
		for i, row := range rows {
			result[i] = itemdictionary.Entry{ID: row.ID, Kind: kind, Code: row.Code, Name: row.Name, SortOrder: row.SortOrder, Enabled: row.Enabled, Version: row.Version}
		}
		return result, nil
	case itemdictionary.KindAttribute:
		rows, err := client.GameItemAttribute.Query().Order(gameitemattribute.ByCode()).All(ctx)
		if err != nil {
			return nil, err
		}
		result := make([]itemdictionary.Entry, len(rows))
		for i, row := range rows {
			result[i] = itemdictionary.Entry{ID: row.ID, Kind: kind, Code: row.Code, Name: row.Name, Description: row.Description, Enabled: row.Enabled, Version: row.Version}
		}
		return result, nil
	case itemdictionary.KindFlingEffect:
		rows, err := client.GameItemFlingEffect.Query().Order(gameitemflingeffect.ByCode()).All(ctx)
		if err != nil {
			return nil, err
		}
		result := make([]itemdictionary.Entry, len(rows))
		for i, row := range rows {
			result[i] = itemdictionary.Entry{ID: row.ID, Kind: kind, Code: row.Code, Name: row.Name, Description: row.Effect, Enabled: row.Enabled, Version: row.Version}
		}
		return result, nil
	default:
		return nil, itemdictionary.ErrInvalid
	}
}

// CreateItemDictionary 在单个事务中创建字典、审计与幂等响应。
func (s *Store) CreateItemDictionary(ctx context.Context, entry itemdictionary.Entry, write administration.GameDataWriteContext, createdAt time.Time) (itemdictionary.Entry, error) {
	return s.writeItemDictionary(ctx, entry, 0, write, createdAt, true)
}

// UpdateItemDictionary 在单个事务中使用乐观版本更新字典、审计与幂等响应。
func (s *Store) UpdateItemDictionary(ctx context.Context, entry itemdictionary.Entry, expectedVersion int64, write administration.GameDataWriteContext, updatedAt time.Time) (itemdictionary.Entry, error) {
	return s.writeItemDictionary(ctx, entry, expectedVersion, write, updatedAt, false)
}

func (s *Store) writeItemDictionary(ctx context.Context, entry itemdictionary.Entry, expectedVersion int64, write administration.GameDataWriteContext, at time.Time, create bool) (itemdictionary.Entry, error) {
	digest, err := idempotency.Digest(struct {
		Entry           itemdictionary.Entry
		ExpectedVersion int64
	}{entry, expectedVersion})
	if err != nil {
		return itemdictionary.Entry{}, err
	}
	request := idempotency.Request{ActorAccountID: write.ActorAccountID, OperationID: "game-data.item-dictionary." + string(entry.Kind), Key: write.IdempotencyKey, RequestDigest: digest, CreatedAt: at}
	result := entry
	err = s.pool.WithinTransaction(ctx, func(txCtx context.Context) error {
		client := s.pool.Client(txCtx)
		persistent := idempotency.NewPersistentWriter(newEntAdministrationRecords(client, s.newID))
		replay, claimErr := idempotency.ClaimResponse(txCtx, persistent, request, &result)
		if claimErr != nil || replay {
			return claimErr
		}
		previous, saveErr := saveItemDictionary(txCtx, client, entry, expectedVersion, at, create)
		if saveErr != nil {
			return saveErr
		}
		if auditErr := s.recordGameDataAudit(txCtx, database.Executor(txCtx, s.pool), write.ActorAccountID, "game-data.item-dictionary.saved", tableForItemDictionary(entry.Kind), entry.ID, write.RequestID, at, previous, &result); auditErr != nil {
			return auditErr
		}
		return idempotency.Complete(txCtx, persistent, request, result)
	})
	if err != nil {
		return itemdictionary.Entry{}, err
	}
	return result, nil
}

func saveItemDictionary(ctx context.Context, client *avalonent.Client, entry itemdictionary.Entry, expectedVersion int64, at time.Time, create bool) (*itemdictionary.Entry, error) {
	var previous *itemdictionary.Entry
	switch entry.Kind {
	case itemdictionary.KindPocket:
		if create {
			_, err := client.GameItemPocket.Create().SetID(entry.ID).SetCode(entry.Code).SetName(entry.Name).SetSortOrder(entry.SortOrder).SetEnabled(entry.Enabled).SetVersion(1).SetCreatedAt(at).SetUpdatedAt(at).Save(ctx)
			return nil, dictionaryError(err)
		}
		row, err := client.GameItemPocket.Query().Where(gameitempocket.IDEQ(entry.ID)).Only(ctx)
		if err != nil {
			return nil, dictionaryError(err)
		}
		value := itemdictionary.Entry{ID: row.ID, Kind: entry.Kind, Code: row.Code, Name: row.Name, SortOrder: row.SortOrder, Enabled: row.Enabled, Version: row.Version}
		previous = &value
		_, err = client.GameItemPocket.UpdateOne(row).Where(gameitempocket.VersionEQ(expectedVersion)).SetCode(entry.Code).SetName(entry.Name).SetSortOrder(entry.SortOrder).SetEnabled(entry.Enabled).SetVersion(entry.Version).SetUpdatedAt(at).Save(ctx)
		return previous, dictionaryError(err)
	case itemdictionary.KindAttribute:
		if create {
			_, err := client.GameItemAttribute.Create().SetID(entry.ID).SetCode(entry.Code).SetName(entry.Name).SetNillableDescription(entry.Description).SetEnabled(entry.Enabled).SetVersion(1).SetCreatedAt(at).SetUpdatedAt(at).Save(ctx)
			return nil, dictionaryError(err)
		}
		row, err := client.GameItemAttribute.Query().Where(gameitemattribute.IDEQ(entry.ID)).Only(ctx)
		if err != nil {
			return nil, dictionaryError(err)
		}
		value := itemdictionary.Entry{ID: row.ID, Kind: entry.Kind, Code: row.Code, Name: row.Name, Description: row.Description, Enabled: row.Enabled, Version: row.Version}
		previous = &value
		_, err = client.GameItemAttribute.UpdateOne(row).Where(gameitemattribute.VersionEQ(expectedVersion)).SetCode(entry.Code).SetName(entry.Name).SetNillableDescription(entry.Description).SetEnabled(entry.Enabled).SetVersion(entry.Version).SetUpdatedAt(at).Save(ctx)
		return previous, dictionaryError(err)
	case itemdictionary.KindFlingEffect:
		if create {
			_, err := client.GameItemFlingEffect.Create().SetID(entry.ID).SetCode(entry.Code).SetName(entry.Name).SetNillableEffect(entry.Description).SetEnabled(entry.Enabled).SetVersion(1).SetCreatedAt(at).SetUpdatedAt(at).Save(ctx)
			return nil, dictionaryError(err)
		}
		row, err := client.GameItemFlingEffect.Query().Where(gameitemflingeffect.IDEQ(entry.ID)).Only(ctx)
		if err != nil {
			return nil, dictionaryError(err)
		}
		value := itemdictionary.Entry{ID: row.ID, Kind: entry.Kind, Code: row.Code, Name: row.Name, Description: row.Effect, Enabled: row.Enabled, Version: row.Version}
		previous = &value
		_, err = client.GameItemFlingEffect.UpdateOne(row).Where(gameitemflingeffect.VersionEQ(expectedVersion)).SetCode(entry.Code).SetName(entry.Name).SetNillableEffect(entry.Description).SetEnabled(entry.Enabled).SetVersion(entry.Version).SetUpdatedAt(at).Save(ctx)
		return previous, dictionaryError(err)
	default:
		return nil, itemdictionary.ErrInvalid
	}
}

func dictionaryError(err error) error {
	if err == nil {
		return nil
	}
	if avalonent.IsNotFound(err) {
		return itemdictionary.ErrNotFound
	}
	var pg *pgconn.PgError
	if errors.As(err, &pg) && pg.Code == "23505" {
		return itemdictionary.ErrConflict
	}
	return fmt.Errorf("保存道具字典: %w", err)
}
func tableForItemDictionary(kind itemdictionary.Kind) string {
	return "game_item_" + strings.ReplaceAll(string(kind), "-", "_")
}
