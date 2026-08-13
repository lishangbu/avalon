package store

import (
	"context"
	"errors"
	"fmt"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
	"time"

	"github.com/jackc/pgx/v5/pgconn"
	"github.com/jackc/pgx/v5/pgtype"
	avalonent "github.com/lishangbu/avalon/ent"
	"github.com/lishangbu/avalon/ent/gamecreature"
	"github.com/lishangbu/avalon/ent/gameegggroup"
	"github.com/lishangbu/avalon/ent/gamegrowthrate"
	"github.com/lishangbu/avalon/ent/gamehabitat"
	"github.com/lishangbu/avalon/ent/gamespecies"
	"github.com/lishangbu/avalon/ent/gamespeciescolor"
	"github.com/lishangbu/avalon/ent/gamespeciesegggroup"
	"github.com/lishangbu/avalon/ent/gamespeciesshape"
	"github.com/lishangbu/avalon/ent/predicate"
	"github.com/lishangbu/avalon/internal/gamedata/administration"
	"github.com/lishangbu/avalon/internal/gamedata/creaturemetadata"
	"github.com/lishangbu/avalon/internal/platform/database"
	"github.com/lishangbu/avalon/internal/platform/idempotency"
)

const (
	createCreatureSpeciesOperationID = "game-data.creature-species.create"
	updateCreatureSpeciesOperationID = "game-data.creature-species.update"
	createCreatureOperationID        = "game-data.creature.create"
	updateCreatureOperationID        = "game-data.creature.update"
)

type creatureDataTransactionStore struct {
	parent   *Store
	client   *avalonent.Client
	executor database.Transaction
}

// GetReferenceOptions 读取 Species 管理表单使用的小型字典资料。
func (s *Store) GetReferenceOptions(ctx context.Context) (creaturemetadata.ReferenceOptions, error) {
	client := s.pool.Client(ctx)
	var result creaturemetadata.ReferenceOptions
	eggs, err := client.GameEggGroup.Query().Order(gameegggroup.BySortOrder(), gameegggroup.ByCode()).All(ctx)
	if err != nil {
		return result, fmt.Errorf("查询蛋组选项: %w", err)
	}
	result.EggGroups = make([]creaturemetadata.EggGroup, len(eggs))
	for index, row := range eggs {
		result.EggGroups[index] = creaturemetadata.EggGroup{ID: snowflake.ID(row.ID), Code: row.Code, Name: row.Name, SortOrder: row.SortOrder, Enabled: row.Enabled}
	}
	growthRates, err := client.GameGrowthRate.Query().Order(gamegrowthrate.ByCode()).All(ctx)
	if err != nil {
		return result, fmt.Errorf("查询成长速率选项: %w", err)
	}
	result.GrowthRates = make([]creaturemetadata.GrowthRate, len(growthRates))
	for index, row := range growthRates {
		result.GrowthRates[index] = creaturemetadata.GrowthRate{ID: snowflake.ID(row.ID), Code: row.Code, Name: row.Name, Formula: row.Formula, Description: row.Description, Enabled: row.Enabled}
	}
	habitats, err := client.GameHabitat.Query().Order(gamehabitat.BySortOrder(), gamehabitat.ByCode()).All(ctx)
	if err != nil {
		return result, fmt.Errorf("查询栖息地选项: %w", err)
	}
	result.Habitats = make([]creaturemetadata.Habitat, len(habitats))
	for index, row := range habitats {
		result.Habitats[index] = creaturemetadata.Habitat{ID: snowflake.ID(row.ID), Code: row.Code, Name: row.Name, SortOrder: row.SortOrder, Enabled: row.Enabled}
	}
	colors, err := client.GameSpeciesColor.Query().Order(gamespeciescolor.BySortOrder(), gamespeciescolor.ByCode()).All(ctx)
	if err != nil {
		return result, fmt.Errorf("查询 Species 颜色选项: %w", err)
	}
	result.Colors = make([]creaturemetadata.SpeciesColor, len(colors))
	for index, row := range colors {
		result.Colors[index] = creaturemetadata.SpeciesColor{ID: snowflake.ID(row.ID), Code: row.Code, Name: row.Name, SortOrder: row.SortOrder, Enabled: row.Enabled}
	}
	shapes, err := client.GameSpeciesShape.Query().Order(gamespeciesshape.BySortOrder(), gamespeciesshape.ByCode()).All(ctx)
	if err != nil {
		return result, fmt.Errorf("查询 Species 外形选项: %w", err)
	}
	result.Shapes = make([]creaturemetadata.SpeciesShape, len(shapes))
	for index, row := range shapes {
		result.Shapes[index] = creaturemetadata.SpeciesShape{ID: snowflake.ID(row.ID), Code: row.Code, Name: row.Name, SortOrder: row.SortOrder, Enabled: row.Enabled}
	}
	return result, nil
}

// WithinCreatureData 执行 Species 或 Creature 单记录写入所需的 PostgreSQL 事务。
func (s *Store) WithinCreatureData(ctx context.Context, work func(creaturemetadata.ManagementWriter) error) error {
	return s.pool.WithinTransaction(ctx, func(transactionCtx context.Context) error {
		executor := database.Executor(transactionCtx, s.pool)
		return work(&creatureDataTransactionStore{parent: s, client: s.pool.Client(transactionCtx), executor: executor})
	})
}

// ListSpecies 返回管理端所需的 Species 有界资料页。
func (s *Store) ListSpecies(ctx context.Context, query creaturemetadata.SpeciesListQuery) (creaturemetadata.SpeciesPage, error) {
	filters := make([]predicate.GameSpecies, 0, 2)
	if query.Q != "" {
		filters = append(filters, gamespecies.Or(gamespecies.CodeContainsFold(query.Q), gamespecies.NameContainsFold(query.Q)))
	}
	if query.Enabled != nil {
		filters = append(filters, gamespecies.EnabledEQ(*query.Enabled))
	}
	client := s.pool.Client(ctx)
	total, err := client.GameSpecies.Query().Where(filters...).Count(ctx)
	if err != nil {
		return creaturemetadata.SpeciesPage{}, fmt.Errorf("统计 Species: %w", err)
	}
	rows, err := client.GameSpecies.Query().Where(filters...).Order(gamespecies.ByNationalDexNumber(), gamespecies.ByID()).Limit(int(query.PageSize)).Offset(int(query.Page-1) * int(query.PageSize)).All(ctx)
	if err != nil {
		return creaturemetadata.SpeciesPage{}, fmt.Errorf("查询 Species 页: %w", err)
	}
	items := make([]creaturemetadata.ManagedSpecies, len(rows))
	ids := make([]snowflake.ID, len(rows))
	for i := range rows {
		ids[i] = rows[i].ID
	}
	eggRows, err := client.GameSpeciesEggGroup.Query().Where(gamespeciesegggroup.SpeciesIDIn(ids...)).All(ctx)
	if err != nil {
		return creaturemetadata.SpeciesPage{}, fmt.Errorf("查询 Species 蛋组: %w", err)
	}
	eggs := make(map[snowflake.ID][]snowflake.ID)
	for _, egg := range eggRows {
		sid := snowflake.ID(egg.SpeciesID)
		eggs[sid] = append(eggs[sid], snowflake.ID(egg.EggGroupID))
	}
	for index, row := range rows {
		items[index] = creaturemetadata.ManagedSpecies{Species: creaturemetadata.Species{ID: snowflake.ID(row.ID), NationalDexNumber: row.NationalDexNumber, Code: row.Code, Name: row.Name, GrowthRateID: optionalIdentifier(row.GrowthRateID), HabitatID: optionalIdentifier(row.HabitatID), ColorID: optionalIdentifier(row.ColorID), ShapeID: optionalIdentifier(row.ShapeID), Genus: row.Genus, PokedexEntry: row.PokedexEntry, Description: row.Description, Profile: row.Profile, DesignOrigin: row.DesignOrigin, Trivia: row.Trivia, GenderDifferences: row.GenderDifferences, FormsSwitchable: row.FormsSwitchable, Enabled: row.Enabled, EggGroupIDs: eggs[snowflake.ID(row.ID)]}, Version: row.Version}
	}
	return creaturemetadata.SpeciesPage{Items: items, Total: int64(total), Page: query.Page, PageSize: query.PageSize}, nil
}

// GetSpecies 返回指定 Species 及其蛋组关系。
func (s *Store) GetSpecies(ctx context.Context, id snowflake.ID) (creaturemetadata.ManagedSpecies, error) {
	row, err := s.pool.Client(ctx).GameSpecies.Query().Where(gamespecies.IDEQ(id)).Only(ctx)
	if avalonent.IsNotFound(err) {
		return creaturemetadata.ManagedSpecies{}, creaturemetadata.ErrCreatureDataNotFound
	}
	if err != nil {
		return creaturemetadata.ManagedSpecies{}, fmt.Errorf("查询 Species: %w", err)
	}
	eggRows, err := s.pool.Client(ctx).GameSpeciesEggGroup.Query().Where(gamespeciesegggroup.SpeciesIDEQ(id)).All(ctx)
	if err != nil {
		return creaturemetadata.ManagedSpecies{}, fmt.Errorf("查询 Species 蛋组: %w", err)
	}
	eggGroupIDs := make([]snowflake.ID, len(eggRows))
	for index, eggGroup := range eggRows {
		eggGroupIDs[index] = snowflake.ID(eggGroup.EggGroupID)
	}
	return creaturemetadata.ManagedSpecies{Species: creaturemetadata.Species{ID: snowflake.ID(row.ID), NationalDexNumber: row.NationalDexNumber, Code: row.Code, Name: row.Name, GrowthRateID: optionalIdentifier(row.GrowthRateID), HabitatID: optionalIdentifier(row.HabitatID), ColorID: optionalIdentifier(row.ColorID), ShapeID: optionalIdentifier(row.ShapeID), Genus: row.Genus, PokedexEntry: row.PokedexEntry, Description: row.Description, Profile: row.Profile, DesignOrigin: row.DesignOrigin, Trivia: row.Trivia, GenderDifferences: row.GenderDifferences, FormsSwitchable: row.FormsSwitchable, Enabled: row.Enabled, EggGroupIDs: eggGroupIDs}, Version: row.Version}, nil
}

// ListCreatures 返回管理端所需的 Creature 有界资料页。
func (s *Store) ListCreatures(ctx context.Context, query creaturemetadata.CreatureListQuery) (creaturemetadata.CreaturePage, error) {
	filters := make([]predicate.GameCreature, 0, 3)
	if query.Q != "" {
		filters = append(filters, gamecreature.Or(gamecreature.CodeContainsFold(query.Q), gamecreature.NameContainsFold(query.Q)))
	}
	if query.SpeciesID != nil {
		filters = append(filters, gamecreature.SpeciesIDEQ(*query.SpeciesID))
	}
	if query.Enabled != nil {
		filters = append(filters, gamecreature.EnabledEQ(*query.Enabled))
	}
	client := s.pool.Client(ctx)
	total, err := client.GameCreature.Query().Where(filters...).Count(ctx)
	if err != nil {
		return creaturemetadata.CreaturePage{}, fmt.Errorf("统计 Creature: %w", err)
	}
	rows, err := client.GameCreature.Query().Where(filters...).Order(gamecreature.ByCode(), gamecreature.ByID()).Limit(int(query.PageSize)).Offset(int(query.Page-1) * int(query.PageSize)).All(ctx)
	if err != nil {
		return creaturemetadata.CreaturePage{}, fmt.Errorf("查询 Creature 页: %w", err)
	}
	items := make([]creaturemetadata.ManagedCreature, len(rows))
	for index, row := range rows {
		items[index] = creaturemetadata.ManagedCreature{Creature: creaturemetadata.Creature{ID: snowflake.ID(row.ID), Code: row.Code, Name: row.Name, SpeciesID: snowflake.ID(row.SpeciesID), InheritsFromCreatureID: optionalIdentifier(row.InheritsFromCreatureID), Height: row.Height, Weight: row.Weight, BaseExperience: row.BaseExperience, CaptureRate: row.CaptureRate, HatchCycles: row.HatchCycles, GenderRatio: &creaturemetadata.GenderRatio{MaleEighths: int32(row.MaleEighths), FemaleEighths: int32(row.FemaleEighths)}, DefaultForm: row.DefaultForm, Enabled: row.Enabled}, Version: row.Version}
	}
	return creaturemetadata.CreaturePage{Items: items, Total: int64(total), Page: query.Page, PageSize: query.PageSize}, nil
}

// GetCreature 返回指定的单条 Creature 资料。
func (s *Store) GetCreature(ctx context.Context, id snowflake.ID) (creaturemetadata.ManagedCreature, error) {
	row, err := s.pool.Client(ctx).GameCreature.Query().Where(gamecreature.IDEQ(id)).Only(ctx)
	if avalonent.IsNotFound(err) {
		return creaturemetadata.ManagedCreature{}, creaturemetadata.ErrCreatureDataNotFound
	}
	if err != nil {
		return creaturemetadata.ManagedCreature{}, fmt.Errorf("查询 Creature: %w", err)
	}
	return creaturemetadata.ManagedCreature{Creature: creaturemetadata.Creature{ID: snowflake.ID(row.ID), Code: row.Code, Name: row.Name, SpeciesID: snowflake.ID(row.SpeciesID), InheritsFromCreatureID: optionalIdentifier(row.InheritsFromCreatureID), Height: row.Height, Weight: row.Weight, BaseExperience: row.BaseExperience, CaptureRate: row.CaptureRate, HatchCycles: row.HatchCycles, GenderRatio: &creaturemetadata.GenderRatio{MaleEighths: int32(row.MaleEighths), FemaleEighths: int32(row.FemaleEighths)}, DefaultForm: row.DefaultForm, Enabled: row.Enabled}, Version: row.Version}, nil
}

// CreateSpecies 在维护事务中创建 Species、蛋组关系、审计和新资料修订。
func (w *creatureDataTransactionStore) CreateSpecies(ctx context.Context, record creaturemetadata.CreateSpeciesRecord) (creaturemetadata.ManagedSpecies, error) {
	value := record.Species
	replay, request, writer, err := claimCreatureResponse(ctx, w.client, w.parent.newID, record.ActorAccountID, createCreatureSpeciesOperationID, record.IdempotencyKey, record.At, &value)
	if err != nil || replay {
		return value, err
	}
	row, err := w.client.GameSpecies.Create().SetID(value.ID).SetNationalDexNumber(value.NationalDexNumber).SetCode(value.Code).SetName(value.Name).SetNillableGrowthRateID(optionalEntIdentifier(value.GrowthRateID)).SetNillableHabitatID(optionalEntIdentifier(value.HabitatID)).SetNillableColorID(optionalEntIdentifier(value.ColorID)).SetNillableShapeID(optionalEntIdentifier(value.ShapeID)).SetNillableGenus(value.Genus).SetNillablePokedexEntry(value.PokedexEntry).SetNillableDescription(value.Description).SetNillableProfile(value.Profile).SetNillableDesignOrigin(value.DesignOrigin).SetNillableTrivia(value.Trivia).SetGenderDifferences(value.GenderDifferences).SetFormsSwitchable(value.FormsSwitchable).SetEnabled(value.Enabled).SetVersion(value.Version).SetCreatedAt(record.At.UTC()).SetUpdatedAt(record.At.UTC()).Save(ctx)
	if err != nil {
		return creaturemetadata.ManagedSpecies{}, creatureDataDatabaseError("创建 Species", err)
	}
	value = creaturemetadata.ManagedSpecies{Species: creaturemetadata.Species{ID: snowflake.ID(row.ID), NationalDexNumber: row.NationalDexNumber, Code: row.Code, Name: row.Name, GrowthRateID: optionalIdentifier(row.GrowthRateID), HabitatID: optionalIdentifier(row.HabitatID), ColorID: optionalIdentifier(row.ColorID), ShapeID: optionalIdentifier(row.ShapeID), Genus: row.Genus, PokedexEntry: row.PokedexEntry, Description: row.Description, Profile: row.Profile, DesignOrigin: row.DesignOrigin, Trivia: row.Trivia, GenderDifferences: row.GenderDifferences, FormsSwitchable: row.FormsSwitchable, Enabled: row.Enabled, EggGroupIDs: value.EggGroupIDs}, Version: row.Version}
	if err := w.replaceSpeciesEggGroups(ctx, value.ID, value.EggGroupIDs); err != nil {
		return creaturemetadata.ManagedSpecies{}, err
	}
	if err := w.completeCreatureWrite(ctx, record.GameDataWriteContext, record.At, "game-data.creature-species.created", "game_species", value.ID, nil, &value, request, writer, value); err != nil {
		return creaturemetadata.ManagedSpecies{}, err
	}
	return value, nil
}

// UpdateSpecies 使用记录级乐观锁更新 Species 及其蛋组关系。
func (w *creatureDataTransactionStore) UpdateSpecies(ctx context.Context, record creaturemetadata.UpdateSpeciesRecord) (creaturemetadata.ManagedSpecies, error) {
	value := record.Species
	replay, request, writer, err := claimCreatureResponse(ctx, w.client, w.parent.newID, record.ActorAccountID, updateCreatureSpeciesOperationID, record.IdempotencyKey, record.At, &value)
	if err != nil || replay {
		return value, err
	}
	currentRow, err := w.client.GameSpecies.Query().Where(gamespecies.IDEQ(value.ID)).Only(ctx)
	if avalonent.IsNotFound(err) {
		return creaturemetadata.ManagedSpecies{}, creaturemetadata.ErrCreatureDataNotFound
	}
	if err != nil {
		return creaturemetadata.ManagedSpecies{}, fmt.Errorf("锁定 Species: %w", err)
	}
	current := creaturemetadata.ManagedSpecies{Species: creaturemetadata.Species{ID: snowflake.ID(currentRow.ID), NationalDexNumber: currentRow.NationalDexNumber, Code: currentRow.Code, Name: currentRow.Name, GrowthRateID: optionalIdentifier(currentRow.GrowthRateID), HabitatID: optionalIdentifier(currentRow.HabitatID), ColorID: optionalIdentifier(currentRow.ColorID), ShapeID: optionalIdentifier(currentRow.ShapeID), Genus: currentRow.Genus, PokedexEntry: currentRow.PokedexEntry, Description: currentRow.Description, Profile: currentRow.Profile, DesignOrigin: currentRow.DesignOrigin, Trivia: currentRow.Trivia, GenderDifferences: currentRow.GenderDifferences, FormsSwitchable: currentRow.FormsSwitchable, Enabled: currentRow.Enabled}, Version: currentRow.Version}
	if current.Version != record.ExpectedVersion {
		return creaturemetadata.ManagedSpecies{}, creaturemetadata.ErrCreatureDataConflict
	}
	row, err := w.client.GameSpecies.UpdateOne(currentRow).Where(gamespecies.VersionEQ(record.ExpectedVersion)).SetNationalDexNumber(value.NationalDexNumber).SetCode(value.Code).SetName(value.Name).SetNillableGrowthRateID(optionalEntIdentifier(value.GrowthRateID)).SetNillableHabitatID(optionalEntIdentifier(value.HabitatID)).SetNillableColorID(optionalEntIdentifier(value.ColorID)).SetNillableShapeID(optionalEntIdentifier(value.ShapeID)).SetNillableGenus(value.Genus).SetNillablePokedexEntry(value.PokedexEntry).SetNillableDescription(value.Description).SetNillableProfile(value.Profile).SetNillableDesignOrigin(value.DesignOrigin).SetNillableTrivia(value.Trivia).SetGenderDifferences(value.GenderDifferences).SetFormsSwitchable(value.FormsSwitchable).SetEnabled(value.Enabled).SetVersion(value.Version).SetUpdatedAt(record.At.UTC()).Save(ctx)
	if avalonent.IsNotFound(err) {
		return creaturemetadata.ManagedSpecies{}, creaturemetadata.ErrCreatureDataConflict
	}
	if err != nil {
		return creaturemetadata.ManagedSpecies{}, creatureDataDatabaseError("更新 Species", err)
	}
	value = creaturemetadata.ManagedSpecies{Species: creaturemetadata.Species{ID: snowflake.ID(row.ID), NationalDexNumber: row.NationalDexNumber, Code: row.Code, Name: row.Name, GrowthRateID: optionalIdentifier(row.GrowthRateID), HabitatID: optionalIdentifier(row.HabitatID), ColorID: optionalIdentifier(row.ColorID), ShapeID: optionalIdentifier(row.ShapeID), Genus: row.Genus, PokedexEntry: row.PokedexEntry, Description: row.Description, Profile: row.Profile, DesignOrigin: row.DesignOrigin, Trivia: row.Trivia, GenderDifferences: row.GenderDifferences, FormsSwitchable: row.FormsSwitchable, Enabled: row.Enabled, EggGroupIDs: value.EggGroupIDs}, Version: row.Version}
	if err := w.replaceSpeciesEggGroups(ctx, value.ID, value.EggGroupIDs); err != nil {
		return creaturemetadata.ManagedSpecies{}, err
	}
	if err := w.completeCreatureWrite(ctx, record.GameDataWriteContext, record.At, "game-data.creature-species.updated", "game_species", value.ID, nil, &value, request, writer, value); err != nil {
		return creaturemetadata.ManagedSpecies{}, err
	}
	return value, nil
}

// CreateCreature 在维护事务中创建单条 Creature、审计和新资料修订。
func (w *creatureDataTransactionStore) CreateCreature(ctx context.Context, record creaturemetadata.CreateCreatureRecord) (creaturemetadata.ManagedCreature, error) {
	value := record.Creature
	replay, request, writer, err := claimCreatureResponse(ctx, w.client, w.parent.newID, record.ActorAccountID, createCreatureOperationID, record.IdempotencyKey, record.At, &value)
	if err != nil || replay {
		return value, err
	}
	row, err := w.client.GameCreature.Create().SetID(value.ID).SetCode(value.Code).SetName(value.Name).SetSpeciesID(value.SpeciesID).SetNillableInheritsFromCreatureID(optionalEntIdentifier(value.InheritsFromCreatureID)).SetNillableHeight(value.Height).SetNillableWeight(value.Weight).SetNillableBaseExperience(value.BaseExperience).SetNillableCaptureRate(value.CaptureRate).SetNillableHatchCycles(value.HatchCycles).SetMaleEighths(int16(value.GenderRatio.MaleEighths)).SetFemaleEighths(int16(value.GenderRatio.FemaleEighths)).SetDefaultForm(value.DefaultForm).SetEnabled(value.Enabled).SetVersion(value.Version).SetCreatedAt(record.At.UTC()).SetUpdatedAt(record.At.UTC()).Save(ctx)
	if err != nil {
		return creaturemetadata.ManagedCreature{}, creatureDataDatabaseError("创建 Creature", err)
	}
	value = creaturemetadata.ManagedCreature{Creature: creaturemetadata.Creature{ID: snowflake.ID(row.ID), Code: row.Code, Name: row.Name, SpeciesID: snowflake.ID(row.SpeciesID), InheritsFromCreatureID: optionalIdentifier(row.InheritsFromCreatureID), Height: row.Height, Weight: row.Weight, BaseExperience: row.BaseExperience, CaptureRate: row.CaptureRate, HatchCycles: row.HatchCycles, GenderRatio: &creaturemetadata.GenderRatio{MaleEighths: int32(row.MaleEighths), FemaleEighths: int32(row.FemaleEighths)}, DefaultForm: row.DefaultForm, Enabled: row.Enabled}, Version: row.Version}
	if err := w.completeCreatureWrite(ctx, record.GameDataWriteContext, record.At, "game-data.creature.created", "game_creature", value.ID, nil, &value, request, writer, value); err != nil {
		return creaturemetadata.ManagedCreature{}, err
	}
	return value, nil
}

// UpdateCreature 使用记录级乐观锁更新单条 Creature。
func (w *creatureDataTransactionStore) UpdateCreature(ctx context.Context, record creaturemetadata.UpdateCreatureRecord) (creaturemetadata.ManagedCreature, error) {
	value := record.Creature
	replay, request, writer, err := claimCreatureResponse(ctx, w.client, w.parent.newID, record.ActorAccountID, updateCreatureOperationID, record.IdempotencyKey, record.At, &value)
	if err != nil || replay {
		return value, err
	}
	currentRow, err := w.client.GameCreature.Query().Where(gamecreature.IDEQ(value.ID)).Only(ctx)
	if avalonent.IsNotFound(err) {
		return creaturemetadata.ManagedCreature{}, creaturemetadata.ErrCreatureDataNotFound
	}
	if err != nil {
		return creaturemetadata.ManagedCreature{}, fmt.Errorf("锁定 Creature: %w", err)
	}
	current := creaturemetadata.ManagedCreature{Creature: creaturemetadata.Creature{ID: snowflake.ID(currentRow.ID), Code: currentRow.Code, Name: currentRow.Name, SpeciesID: snowflake.ID(currentRow.SpeciesID), InheritsFromCreatureID: optionalIdentifier(currentRow.InheritsFromCreatureID), Height: currentRow.Height, Weight: currentRow.Weight, BaseExperience: currentRow.BaseExperience, CaptureRate: currentRow.CaptureRate, HatchCycles: currentRow.HatchCycles, GenderRatio: &creaturemetadata.GenderRatio{MaleEighths: int32(currentRow.MaleEighths), FemaleEighths: int32(currentRow.FemaleEighths)}, DefaultForm: currentRow.DefaultForm, Enabled: currentRow.Enabled}, Version: currentRow.Version}
	if current.Version != record.ExpectedVersion {
		return creaturemetadata.ManagedCreature{}, creaturemetadata.ErrCreatureDataConflict
	}
	row, err := w.client.GameCreature.UpdateOne(currentRow).Where(gamecreature.VersionEQ(record.ExpectedVersion)).SetCode(value.Code).SetName(value.Name).SetSpeciesID(value.SpeciesID).SetNillableInheritsFromCreatureID(optionalEntIdentifier(value.InheritsFromCreatureID)).SetNillableHeight(value.Height).SetNillableWeight(value.Weight).SetNillableBaseExperience(value.BaseExperience).SetNillableCaptureRate(value.CaptureRate).SetNillableHatchCycles(value.HatchCycles).SetMaleEighths(int16(value.GenderRatio.MaleEighths)).SetFemaleEighths(int16(value.GenderRatio.FemaleEighths)).SetDefaultForm(value.DefaultForm).SetEnabled(value.Enabled).SetVersion(value.Version).SetUpdatedAt(record.At.UTC()).Save(ctx)
	if avalonent.IsNotFound(err) {
		return creaturemetadata.ManagedCreature{}, creaturemetadata.ErrCreatureDataConflict
	}
	if err != nil {
		return creaturemetadata.ManagedCreature{}, creatureDataDatabaseError("更新 Creature", err)
	}
	value = creaturemetadata.ManagedCreature{Creature: creaturemetadata.Creature{ID: snowflake.ID(row.ID), Code: row.Code, Name: row.Name, SpeciesID: snowflake.ID(row.SpeciesID), InheritsFromCreatureID: optionalIdentifier(row.InheritsFromCreatureID), Height: row.Height, Weight: row.Weight, BaseExperience: row.BaseExperience, CaptureRate: row.CaptureRate, HatchCycles: row.HatchCycles, GenderRatio: &creaturemetadata.GenderRatio{MaleEighths: int32(row.MaleEighths), FemaleEighths: int32(row.FemaleEighths)}, DefaultForm: row.DefaultForm, Enabled: row.Enabled}, Version: row.Version}
	if err := w.completeCreatureWrite(ctx, record.GameDataWriteContext, record.At, "game-data.creature.updated", "game_creature", value.ID, nil, &value, request, writer, value); err != nil {
		return creaturemetadata.ManagedCreature{}, err
	}
	return value, nil
}

func (w *creatureDataTransactionStore) replaceSpeciesEggGroups(ctx context.Context, speciesID snowflake.ID, eggGroupIDs []snowflake.ID) error {
	if _, err := w.client.GameSpeciesEggGroup.Delete().Where(gamespeciesegggroup.SpeciesIDEQ(speciesID)).Exec(ctx); err != nil {
		return fmt.Errorf("替换 Species 蛋组: %w", err)
	}
	for _, eggGroupID := range eggGroupIDs {
		relationID, err := w.parent.newID.Next(ctx)
		if err != nil {
			return fmt.Errorf("生成 Species 蛋组关系 Identifier: %w", err)
		}
		if _, err := w.client.GameSpeciesEggGroup.Create().SetID(relationID).SetSpeciesID(speciesID).SetEggGroupID(eggGroupID).Save(ctx); err != nil {
			return creatureDataDatabaseError("写入 Species 蛋组", err)
		}
	}
	return nil
}

func claimCreatureResponse[T any](ctx context.Context, client *avalonent.Client, newID snowflake.Source, actorID snowflake.ID, operationID, key string, at time.Time, value *T) (bool, idempotency.Request, *idempotency.PersistentWriter, error) {
	return claimCreatureResponseForPayload(ctx, client, newID, actorID, operationID, key, at, *value, value)
}

func claimCreatureResponseForPayload[T any](ctx context.Context, client *avalonent.Client, newID snowflake.Source, actorID snowflake.ID, operationID, key string, at time.Time, payload any, value *T) (bool, idempotency.Request, *idempotency.PersistentWriter, error) {
	digest, err := idempotency.Digest(payload)
	if err != nil {
		return false, idempotency.Request{}, nil, fmt.Errorf("计算 Creature 资料幂��摘要: %w", err)
	}
	request := idempotency.Request{ActorAccountID: actorID, OperationID: operationID, Key: key, RequestDigest: digest, CreatedAt: at}
	writer := idempotency.NewPersistentWriter(newEntAdministrationRecords(client, newID))
	replay, err := idempotency.ClaimResponse(ctx, writer, request, value)
	return replay, request, writer, err
}

func (w *creatureDataTransactionStore) completeCreatureWrite[T any](ctx context.Context, writeContext administration.GameDataWriteContext, at time.Time, action, resource string, id snowflake.ID, before, after any, request idempotency.Request, writer *idempotency.PersistentWriter, response T) error {
	if err := w.parent.recordGameDataAudit(ctx, w.executor, writeContext.ActorAccountID, action, resource, id, writeContext.RequestID, at, before, after); err != nil {
		return err
	}
	return idempotency.Complete(ctx, writer, request, response)
}

func managedSpeciesFromValues(id pgtype.Int8, nationalDexNumber int32, code, name string, growthRateID, habitatID, colorID, shapeID pgtype.Int8, genus, pokedexEntry, description, profile, designOrigin, trivia pgtype.Text, genderDifferences, formsSwitchable, enabled bool, version int64, eggGroupIDs []snowflake.ID) creaturemetadata.ManagedSpecies {
	return creaturemetadata.ManagedSpecies{Species: creaturemetadata.Species{ID: domainIdentifier(id), NationalDexNumber: nationalDexNumber, Code: code, Name: name, GrowthRateID: nullableDomainIdentifier(growthRateID), HabitatID: nullableDomainIdentifier(habitatID), ColorID: nullableDomainIdentifier(colorID), ShapeID: nullableDomainIdentifier(shapeID), EggGroupIDs: eggGroupIDs, Genus: nullableDomainText(genus), PokedexEntry: nullableDomainText(pokedexEntry), Description: nullableDomainText(description), Profile: nullableDomainText(profile), DesignOrigin: nullableDomainText(designOrigin), Trivia: nullableDomainText(trivia), GenderDifferences: genderDifferences, FormsSwitchable: formsSwitchable, Enabled: enabled}, Version: version}
}

func managedCreatureFromValues(id pgtype.Int8, code, name string, speciesID, inheritedID pgtype.Int8, height, weight, baseExperience, captureRate, hatchCycles pgtype.Int4, male, female int16, defaultForm, enabled bool, version int64) creaturemetadata.ManagedCreature {
	return creaturemetadata.ManagedCreature{Creature: creaturemetadata.Creature{ID: domainIdentifier(id), Code: code, Name: name, SpeciesID: domainIdentifier(speciesID), InheritsFromCreatureID: nullableDomainIdentifier(inheritedID), Height: nullableDomainInt32(height), Weight: nullableDomainInt32(weight), BaseExperience: nullableDomainInt32(baseExperience), CaptureRate: nullableDomainInt32(captureRate), HatchCycles: nullableDomainInt32(hatchCycles), GenderRatio: &creaturemetadata.GenderRatio{MaleEighths: int32(male), FemaleEighths: int32(female)}, DefaultForm: defaultForm, Enabled: enabled}, Version: version}
}

func creatureDataDatabaseError(action string, err error) error {
	var databaseError *pgconn.PgError
	if errors.As(err, &databaseError) && (databaseError.Code == "23505" || databaseError.Code == "23503" || databaseError.Code == "23514") {
		return creaturemetadata.ErrCreatureDataConflict
	}
	return fmt.Errorf("%s: %w", action, err)
}
