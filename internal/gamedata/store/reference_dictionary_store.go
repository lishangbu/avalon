package store

import (
	"context"
	"errors"
	"fmt"
	"strings"
	"time"

	"github.com/jackc/pgx/v5/pgconn"
	avalonent "github.com/lishangbu/avalon/ent"
	"github.com/lishangbu/avalon/ent/gamecurrency"
	"github.com/lishangbu/avalon/ent/gameegggroup"
	"github.com/lishangbu/avalon/ent/gamegrowthrate"
	"github.com/lishangbu/avalon/ent/gamehabitat"
	"github.com/lishangbu/avalon/ent/gamespeciescolor"
	"github.com/lishangbu/avalon/ent/gamespeciesshape"
	"github.com/lishangbu/avalon/internal/gamedata/administration"
	"github.com/lishangbu/avalon/internal/gamedata/referencedictionary"
	"github.com/lishangbu/avalon/internal/platform/database"
	"github.com/lishangbu/avalon/internal/platform/idempotency"
)

// ListReferenceDictionary 从资源对应的规范化表读取全部记录。
func (s *Store) ListReferenceDictionary(ctx context.Context, kind referencedictionary.Kind) ([]referencedictionary.Entry, error) {
	client := s.pool.Client(ctx)
	switch kind {
	case referencedictionary.KindGrowthRate:
		rows, err := client.GameGrowthRate.Query().Order(gamegrowthrate.ByCode()).All(ctx)
		if err != nil {
			return nil, err
		}
		result := make([]referencedictionary.Entry, len(rows))
		for i, row := range rows {
			result[i] = referencedictionary.Entry{ID: row.ID, Kind: kind, Code: row.Code, Name: row.Name, Formula: row.Formula, Description: row.Description, Enabled: row.Enabled, Version: row.Version}
		}
		return result, nil
	case referencedictionary.KindHabitat:
		rows, err := client.GameHabitat.Query().Order(gamehabitat.BySortOrder(), gamehabitat.ByCode()).All(ctx)
		if err != nil {
			return nil, err
		}
		result := make([]referencedictionary.Entry, len(rows))
		for i, row := range rows {
			result[i] = referencedictionary.Entry{ID: row.ID, Kind: kind, Code: row.Code, Name: row.Name, SortOrder: row.SortOrder, Enabled: row.Enabled, Version: row.Version}
		}
		return result, nil
	case referencedictionary.KindSpeciesColor:
		rows, err := client.GameSpeciesColor.Query().Order(gamespeciescolor.BySortOrder(), gamespeciescolor.ByCode()).All(ctx)
		if err != nil {
			return nil, err
		}
		result := make([]referencedictionary.Entry, len(rows))
		for i, row := range rows {
			result[i] = referencedictionary.Entry{ID: row.ID, Kind: kind, Code: row.Code, Name: row.Name, SortOrder: row.SortOrder, Enabled: row.Enabled, Version: row.Version}
		}
		return result, nil
	case referencedictionary.KindSpeciesShape:
		rows, err := client.GameSpeciesShape.Query().Order(gamespeciesshape.BySortOrder(), gamespeciesshape.ByCode()).All(ctx)
		if err != nil {
			return nil, err
		}
		result := make([]referencedictionary.Entry, len(rows))
		for i, row := range rows {
			result[i] = referencedictionary.Entry{ID: row.ID, Kind: kind, Code: row.Code, Name: row.Name, SortOrder: row.SortOrder, Enabled: row.Enabled, Version: row.Version}
		}
		return result, nil
	case referencedictionary.KindEggGroup:
		rows, err := client.GameEggGroup.Query().Order(gameegggroup.BySortOrder(), gameegggroup.ByCode()).All(ctx)
		if err != nil {
			return nil, err
		}
		result := make([]referencedictionary.Entry, len(rows))
		for i, row := range rows {
			result[i] = referencedictionary.Entry{ID: row.ID, Kind: kind, Code: row.Code, Name: row.Name, SortOrder: row.SortOrder, Enabled: row.Enabled, Version: row.Version}
		}
		return result, nil
	case referencedictionary.KindCurrency:
		rows, err := client.GameCurrency.Query().Order(gamecurrency.ByCode()).All(ctx)
		if err != nil {
			return nil, err
		}
		result := make([]referencedictionary.Entry, len(rows))
		for i, row := range rows {
			result[i] = referencedictionary.Entry{ID: row.ID, Kind: kind, Code: row.Code, Name: row.Name, Symbol: row.Symbol, Enabled: row.Enabled, Version: row.Version}
		}
		return result, nil
	default:
		return nil, referencedictionary.ErrInvalid
	}
}

// CreateReferenceDictionary 在单一事务中创建资料、管理审计与幂等响应。
func (s *Store) CreateReferenceDictionary(ctx context.Context, entry referencedictionary.Entry, write administration.GameDataWriteContext, at time.Time) (referencedictionary.Entry, error) {
	return s.writeReferenceDictionary(ctx, entry, 0, write, at, true)
}

// UpdateReferenceDictionary 在单一事务中按乐观版本更新资料。
func (s *Store) UpdateReferenceDictionary(ctx context.Context, entry referencedictionary.Entry, expectedVersion int64, write administration.GameDataWriteContext, at time.Time) (referencedictionary.Entry, error) {
	return s.writeReferenceDictionary(ctx, entry, expectedVersion, write, at, false)
}

func (s *Store) writeReferenceDictionary(ctx context.Context, entry referencedictionary.Entry, expectedVersion int64, write administration.GameDataWriteContext, at time.Time, create bool) (referencedictionary.Entry, error) {
	digest, err := idempotency.Digest(struct {
		Entry           referencedictionary.Entry
		ExpectedVersion int64
	}{entry, expectedVersion})
	if err != nil {
		return referencedictionary.Entry{}, err
	}
	request := idempotency.Request{ActorAccountID: write.ActorAccountID, OperationID: "game-data.reference-dictionary." + string(entry.Kind), Key: write.IdempotencyKey, RequestDigest: digest, CreatedAt: at}
	result := entry
	err = s.pool.WithinTransaction(ctx, func(txCtx context.Context) error {
		client := s.pool.Client(txCtx)
		persistent := idempotency.NewPersistentWriter(newEntAdministrationRecords(client, s.newID))
		replay, claimErr := idempotency.ClaimResponse(txCtx, persistent, request, &result)
		if claimErr != nil || replay {
			return claimErr
		}
		previous, saveErr := saveReferenceDictionary(txCtx, client, entry, expectedVersion, at, create)
		if saveErr != nil {
			return saveErr
		}
		if auditErr := s.recordGameDataAudit(txCtx, database.Executor(txCtx, s.pool), write.ActorAccountID, "game-data.reference-dictionary.saved", tableForReferenceDictionary(entry.Kind), entry.ID, write.RequestID, at, previous, &result); auditErr != nil {
			return auditErr
		}
		return idempotency.Complete(txCtx, persistent, request, result)
	})
	if err != nil {
		return referencedictionary.Entry{}, err
	}
	return result, nil
}

func saveReferenceDictionary(ctx context.Context, client *avalonent.Client, entry referencedictionary.Entry, expectedVersion int64, at time.Time, create bool) (*referencedictionary.Entry, error) {
	if create {
		switch entry.Kind {
		case referencedictionary.KindGrowthRate:
			_, err := client.GameGrowthRate.Create().SetID(entry.ID).SetCode(entry.Code).SetName(entry.Name).SetNillableFormula(entry.Formula).SetNillableDescription(entry.Description).SetEnabled(entry.Enabled).SetVersion(1).SetCreatedAt(at).SetUpdatedAt(at).Save(ctx)
			return nil, referenceDictionaryError(err)
		case referencedictionary.KindHabitat:
			_, err := client.GameHabitat.Create().SetID(entry.ID).SetCode(entry.Code).SetName(entry.Name).SetSortOrder(entry.SortOrder).SetEnabled(entry.Enabled).SetVersion(1).SetCreatedAt(at).SetUpdatedAt(at).Save(ctx)
			return nil, referenceDictionaryError(err)
		case referencedictionary.KindSpeciesColor:
			_, err := client.GameSpeciesColor.Create().SetID(entry.ID).SetCode(entry.Code).SetName(entry.Name).SetSortOrder(entry.SortOrder).SetEnabled(entry.Enabled).SetVersion(1).SetCreatedAt(at).SetUpdatedAt(at).Save(ctx)
			return nil, referenceDictionaryError(err)
		case referencedictionary.KindSpeciesShape:
			_, err := client.GameSpeciesShape.Create().SetID(entry.ID).SetCode(entry.Code).SetName(entry.Name).SetSortOrder(entry.SortOrder).SetEnabled(entry.Enabled).SetVersion(1).SetCreatedAt(at).SetUpdatedAt(at).Save(ctx)
			return nil, referenceDictionaryError(err)
		case referencedictionary.KindEggGroup:
			_, err := client.GameEggGroup.Create().SetID(entry.ID).SetCode(entry.Code).SetName(entry.Name).SetSortOrder(entry.SortOrder).SetEnabled(entry.Enabled).SetVersion(1).SetCreatedAt(at).SetUpdatedAt(at).Save(ctx)
			return nil, referenceDictionaryError(err)
		case referencedictionary.KindCurrency:
			_, err := client.GameCurrency.Create().SetID(entry.ID).SetCode(entry.Code).SetName(entry.Name).SetNillableSymbol(entry.Symbol).SetEnabled(entry.Enabled).SetVersion(1).SetCreatedAt(at).SetUpdatedAt(at).Save(ctx)
			return nil, referenceDictionaryError(err)
		default:
			return nil, referencedictionary.ErrInvalid
		}
	}
	switch entry.Kind {
	case referencedictionary.KindGrowthRate:
		row, err := client.GameGrowthRate.Query().Where(gamegrowthrate.IDEQ(entry.ID)).Only(ctx)
		if err != nil {
			return nil, referenceDictionaryError(err)
		}
		old := referencedictionary.Entry{ID: row.ID, Kind: entry.Kind, Code: row.Code, Name: row.Name, Formula: row.Formula, Description: row.Description, Enabled: row.Enabled, Version: row.Version}
		_, err = client.GameGrowthRate.UpdateOne(row).Where(gamegrowthrate.VersionEQ(expectedVersion)).SetCode(entry.Code).SetName(entry.Name).SetNillableFormula(entry.Formula).SetNillableDescription(entry.Description).SetEnabled(entry.Enabled).SetVersion(entry.Version).SetUpdatedAt(at).Save(ctx)
		return &old, referenceDictionaryError(err)
	case referencedictionary.KindHabitat:
		row, err := client.GameHabitat.Query().Where(gamehabitat.IDEQ(entry.ID)).Only(ctx)
		if err != nil {
			return nil, referenceDictionaryError(err)
		}
		old := referencedictionary.Entry{ID: row.ID, Kind: entry.Kind, Code: row.Code, Name: row.Name, SortOrder: row.SortOrder, Enabled: row.Enabled, Version: row.Version}
		_, err = client.GameHabitat.UpdateOne(row).Where(gamehabitat.VersionEQ(expectedVersion)).SetCode(entry.Code).SetName(entry.Name).SetSortOrder(entry.SortOrder).SetEnabled(entry.Enabled).SetVersion(entry.Version).SetUpdatedAt(at).Save(ctx)
		return &old, referenceDictionaryError(err)
	case referencedictionary.KindSpeciesColor:
		row, err := client.GameSpeciesColor.Query().Where(gamespeciescolor.IDEQ(entry.ID)).Only(ctx)
		if err != nil {
			return nil, referenceDictionaryError(err)
		}
		old := referencedictionary.Entry{ID: row.ID, Kind: entry.Kind, Code: row.Code, Name: row.Name, SortOrder: row.SortOrder, Enabled: row.Enabled, Version: row.Version}
		_, err = client.GameSpeciesColor.UpdateOne(row).Where(gamespeciescolor.VersionEQ(expectedVersion)).SetCode(entry.Code).SetName(entry.Name).SetSortOrder(entry.SortOrder).SetEnabled(entry.Enabled).SetVersion(entry.Version).SetUpdatedAt(at).Save(ctx)
		return &old, referenceDictionaryError(err)
	case referencedictionary.KindSpeciesShape:
		row, err := client.GameSpeciesShape.Query().Where(gamespeciesshape.IDEQ(entry.ID)).Only(ctx)
		if err != nil {
			return nil, referenceDictionaryError(err)
		}
		old := referencedictionary.Entry{ID: row.ID, Kind: entry.Kind, Code: row.Code, Name: row.Name, SortOrder: row.SortOrder, Enabled: row.Enabled, Version: row.Version}
		_, err = client.GameSpeciesShape.UpdateOne(row).Where(gamespeciesshape.VersionEQ(expectedVersion)).SetCode(entry.Code).SetName(entry.Name).SetSortOrder(entry.SortOrder).SetEnabled(entry.Enabled).SetVersion(entry.Version).SetUpdatedAt(at).Save(ctx)
		return &old, referenceDictionaryError(err)
	case referencedictionary.KindEggGroup:
		row, err := client.GameEggGroup.Query().Where(gameegggroup.IDEQ(entry.ID)).Only(ctx)
		if err != nil {
			return nil, referenceDictionaryError(err)
		}
		old := referencedictionary.Entry{ID: row.ID, Kind: entry.Kind, Code: row.Code, Name: row.Name, SortOrder: row.SortOrder, Enabled: row.Enabled, Version: row.Version}
		_, err = client.GameEggGroup.UpdateOne(row).Where(gameegggroup.VersionEQ(expectedVersion)).SetCode(entry.Code).SetName(entry.Name).SetSortOrder(entry.SortOrder).SetEnabled(entry.Enabled).SetVersion(entry.Version).SetUpdatedAt(at).Save(ctx)
		return &old, referenceDictionaryError(err)
	case referencedictionary.KindCurrency:
		row, err := client.GameCurrency.Query().Where(gamecurrency.IDEQ(entry.ID)).Only(ctx)
		if err != nil {
			return nil, referenceDictionaryError(err)
		}
		old := referencedictionary.Entry{ID: row.ID, Kind: entry.Kind, Code: row.Code, Name: row.Name, Symbol: row.Symbol, Enabled: row.Enabled, Version: row.Version}
		_, err = client.GameCurrency.UpdateOne(row).Where(gamecurrency.VersionEQ(expectedVersion)).SetCode(entry.Code).SetName(entry.Name).SetNillableSymbol(entry.Symbol).SetEnabled(entry.Enabled).SetVersion(entry.Version).SetUpdatedAt(at).Save(ctx)
		return &old, referenceDictionaryError(err)
	default:
		return nil, referencedictionary.ErrInvalid
	}
}

func referenceDictionaryError(err error) error {
	if err == nil {
		return nil
	}
	if avalonent.IsNotFound(err) {
		return referencedictionary.ErrNotFound
	}
	var pg *pgconn.PgError
	if errors.As(err, &pg) && pg.Code == "23505" {
		return referencedictionary.ErrConflict
	}
	return fmt.Errorf("保存引用字典: %w", err)
}

func tableForReferenceDictionary(kind referencedictionary.Kind) string {
	switch kind {
	case referencedictionary.KindGrowthRate:
		return "game_growth_rate"
	case referencedictionary.KindHabitat:
		return "game_habitat"
	case referencedictionary.KindSpeciesColor:
		return "game_species_color"
	case referencedictionary.KindSpeciesShape:
		return "game_species_shape"
	case referencedictionary.KindEggGroup:
		return "game_egg_group"
	case referencedictionary.KindCurrency:
		return "game_currency"
	default:
		return "game_" + strings.ReplaceAll(string(kind), "-", "_")
	}
}
