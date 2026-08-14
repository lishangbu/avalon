package api

import (
	"context"
	"errors"
	"strconv"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	kratoserrors "github.com/go-kratos/kratos/v3/errors"
	domainv1 "github.com/lishangbu/avalon/api/gen/go/avalon/domain/v1"
	"github.com/lishangbu/avalon/internal/gamedata/administration"
	"github.com/lishangbu/avalon/internal/gamedata/creaturemetadata"
	"github.com/lishangbu/avalon/internal/platform/idempotency"
	"github.com/lishangbu/avalon/internal/security/authentication"
)

// ListGameCreatureReferenceOptions 返回 Species 表单使用的小型独立引用资料。
func (service *KratosService) ListGameCreatureReferenceOptions(ctx context.Context, _ *domainv1.ListGameCreatureReferenceOptionsRequest) (*domainv1.ListGameCreatureReferenceOptionsResponse, error) {
	if _, err := gameDataPrincipal(ctx); err != nil {
		return nil, err
	}
	value, err := service.services.CreatureAdministration.GetReferenceOptions(ctx)
	if err != nil {
		return nil, service.creatureDataError(ctx, "GAME_CREATURE_REFERENCE_OPTIONS_FAILED", err)
	}
	return &domainv1.ListGameCreatureReferenceOptionsResponse{HttpStatusCode: 200, Body: creatureReferenceOptionsMessage(value)}, nil
}

// ListGameCreatureSpecies 分页查询 Species，不加载十万级技能学习关系。
func (service *KratosService) ListGameCreatureSpecies(ctx context.Context, request *domainv1.ListGameCreatureSpeciesRequest) (*domainv1.ListGameCreatureSpeciesResponse, error) {
	if _, err := gameDataPrincipal(ctx); err != nil {
		return nil, err
	}
	page, pageSize := gameDataPage(request.GetPage(), request.GetPageSize())
	result, err := service.services.CreatureAdministration.ListSpecies(ctx, creaturemetadata.SpeciesListQuery{Page: page, PageSize: pageSize, Q: request.GetQ(), Enabled: request.Enabled})
	if err != nil {
		return nil, service.creatureDataError(ctx, "GAME_CREATURE_SPECIES_LIST_FAILED", err)
	}
	items := make([]*domainv1.GameCreatureSpecies, len(result.Items))
	for index, value := range result.Items {
		items[index] = creatureSpeciesMessage(value)
	}
	return &domainv1.ListGameCreatureSpeciesResponse{HttpStatusCode: 200, Body: &domainv1.GameCreatureSpeciesPage{Items: items, Total: strconv.FormatInt(result.Total, 10), Page: result.Page, PageSize: result.PageSize}}, nil
}

// CreateGameCreatureSpecies 在维护窗口中创建单条 Species。
func (service *KratosService) CreateGameCreatureSpecies(ctx context.Context, request *domainv1.CreateGameCreatureSpeciesRequest) (*domainv1.CreateGameCreatureSpeciesResponse, error) {
	principal, body, writeContext, err := service.speciesCreateContext(ctx, request)
	if err != nil {
		return nil, err
	}
	species, err := speciesFromCreateBody(body)
	if err != nil {
		return nil, err
	}
	writeContext.ActorAccountID = principal.AccountID
	created, err := service.services.CreatureAdministration.CreateSpecies(ctx, creaturemetadata.CreateSpeciesCommand{GameDataWriteContext: writeContext, Species: species})
	if err != nil {
		return nil, service.creatureDataError(ctx, "GAME_CREATURE_SPECIES_CREATE_FAILED", err)
	}
	return &domainv1.CreateGameCreatureSpeciesResponse{HttpStatusCode: 201, Body: creatureSpeciesMessage(created)}, nil
}

// GetGameCreatureSpecies 查询单条 Species 及其蛋组关系。
func (service *KratosService) GetGameCreatureSpecies(ctx context.Context, request *domainv1.GetGameCreatureSpeciesRequest) (*domainv1.GetGameCreatureSpeciesResponse, error) {
	if _, err := gameDataPrincipal(ctx); err != nil {
		return nil, err
	}
	id, err := gameDataIdentifier(request.GetSpeciesId(), "INVALID_CREATURE_SPECIES_ID")
	if err != nil {
		return nil, err
	}
	value, err := service.services.CreatureAdministration.GetSpecies(ctx, id)
	if err != nil {
		return nil, service.creatureDataError(ctx, "GAME_CREATURE_SPECIES_QUERY_FAILED", err)
	}
	return &domainv1.GetGameCreatureSpeciesResponse{HttpStatusCode: 200, Body: creatureSpeciesMessage(value)}, nil
}

// UpdateGameCreatureSpecies 使用记录级乐观锁更新 Species。
func (service *KratosService) UpdateGameCreatureSpecies(ctx context.Context, request *domainv1.UpdateGameCreatureSpeciesRequest) (*domainv1.UpdateGameCreatureSpeciesResponse, error) {
	principal, err := gameDataPrincipal(ctx)
	if err != nil {
		return nil, err
	}
	body := request.GetBody()
	if body == nil {
		return nil, kratoserrors.BadRequest("INVALID_REQUEST", "请求格式无效")
	}
	id, err := gameDataIdentifier(request.GetSpeciesId(), "INVALID_CREATURE_SPECIES_ID")
	if err != nil {
		return nil, err
	}
	version, err := gameDataVersion(body.GetExpectedVersion())
	if err != nil {
		return nil, err
	}
	writeContext, err := gameDataWriteContext(ctx, principal.AccountID, request.GetHeaderIdempotencyKey())
	if err != nil {
		return nil, err
	}
	species, err := speciesFromUpdateBody(id, body)
	if err != nil {
		return nil, err
	}
	updated, err := service.services.CreatureAdministration.UpdateSpecies(ctx, creaturemetadata.UpdateSpeciesCommand{GameDataWriteContext: writeContext, Species: species, ExpectedVersion: version})
	if err != nil {
		return nil, service.creatureDataError(ctx, "GAME_CREATURE_SPECIES_UPDATE_FAILED", err)
	}
	return &domainv1.UpdateGameCreatureSpeciesResponse{HttpStatusCode: 200, Body: creatureSpeciesMessage(updated)}, nil
}

// ListGameCreatures 分页查询可参战 Creature，并支持按 Species 限定。
func (service *KratosService) ListGameCreatures(ctx context.Context, request *domainv1.ListGameCreaturesRequest) (*domainv1.ListGameCreaturesResponse, error) {
	if _, err := gameDataPrincipal(ctx); err != nil {
		return nil, err
	}
	speciesID, err := optionalGameDataIdentifier(request.GetSpeciesId(), "INVALID_CREATURE_SPECIES_ID")
	if err != nil {
		return nil, err
	}
	page, pageSize := gameDataPage(request.GetPage(), request.GetPageSize())
	result, err := service.services.CreatureAdministration.ListCreatures(ctx, creaturemetadata.CreatureListQuery{Page: page, PageSize: pageSize, Q: request.GetQ(), SpeciesID: speciesID, Enabled: request.Enabled})
	if err != nil {
		return nil, service.creatureDataError(ctx, "GAME_CREATURE_LIST_FAILED", err)
	}
	items := make([]*domainv1.GameCreature, len(result.Items))
	for index, value := range result.Items {
		items[index] = creatureMessage(value)
	}
	return &domainv1.ListGameCreaturesResponse{HttpStatusCode: 200, Body: &domainv1.GameCreaturePage{Items: items, Total: strconv.FormatInt(result.Total, 10), Page: result.Page, PageSize: result.PageSize}}, nil
}

// CreateGameCreature 在维护窗口中创建单条可参战 Creature。
func (service *KratosService) CreateGameCreature(ctx context.Context, request *domainv1.CreateGameCreatureRequest) (*domainv1.CreateGameCreatureResponse, error) {
	principal, err := gameDataPrincipal(ctx)
	if err != nil {
		return nil, err
	}
	body := request.GetBody()
	if body == nil || body.GetGenderRatio() == nil {
		return nil, kratoserrors.BadRequest("INVALID_REQUEST", "请求格式无效")
	}
	writeContext, err := gameDataWriteContext(ctx, principal.AccountID, request.GetHeaderIdempotencyKey())
	if err != nil {
		return nil, err
	}
	command, err := creatureCreateCommand(body)
	if err != nil {
		return nil, err
	}
	command.GameDataWriteContext = writeContext
	created, err := service.services.CreatureAdministration.CreateCreature(ctx, command)
	if err != nil {
		return nil, service.creatureDataError(ctx, "GAME_CREATURE_CREATE_FAILED", err)
	}
	return &domainv1.CreateGameCreatureResponse{HttpStatusCode: 201, Body: creatureMessage(created)}, nil
}

// GetGameCreature 查询单条可参战 Creature。
func (service *KratosService) GetGameCreature(ctx context.Context, request *domainv1.GetGameCreatureRequest) (*domainv1.GetGameCreatureResponse, error) {
	if _, err := gameDataPrincipal(ctx); err != nil {
		return nil, err
	}
	id, err := gameDataIdentifier(request.GetCreatureId(), "INVALID_CREATURE_ID")
	if err != nil {
		return nil, err
	}
	value, err := service.services.CreatureAdministration.GetCreature(ctx, id)
	if err != nil {
		return nil, service.creatureDataError(ctx, "GAME_CREATURE_QUERY_FAILED", err)
	}
	return &domainv1.GetGameCreatureResponse{HttpStatusCode: 200, Body: creatureMessage(value)}, nil
}

// UpdateGameCreature 使用记录级乐观锁更新单条可参战 Creature。
func (service *KratosService) UpdateGameCreature(ctx context.Context, request *domainv1.UpdateGameCreatureRequest) (*domainv1.UpdateGameCreatureResponse, error) {
	principal, err := gameDataPrincipal(ctx)
	if err != nil {
		return nil, err
	}
	body := request.GetBody()
	if body == nil || body.GetGenderRatio() == nil {
		return nil, kratoserrors.BadRequest("INVALID_REQUEST", "请求格式无效")
	}
	id, err := gameDataIdentifier(request.GetCreatureId(), "INVALID_CREATURE_ID")
	if err != nil {
		return nil, err
	}
	version, err := gameDataVersion(body.GetExpectedVersion())
	if err != nil {
		return nil, err
	}
	writeContext, err := gameDataWriteContext(ctx, principal.AccountID, request.GetHeaderIdempotencyKey())
	if err != nil {
		return nil, err
	}
	command, err := creatureUpdateCommand(id, version, body)
	if err != nil {
		return nil, err
	}
	command.GameDataWriteContext = writeContext
	updated, err := service.services.CreatureAdministration.UpdateCreature(ctx, command)
	if err != nil {
		return nil, service.creatureDataError(ctx, "GAME_CREATURE_UPDATE_FAILED", err)
	}
	return &domainv1.UpdateGameCreatureResponse{HttpStatusCode: 200, Body: creatureMessage(updated)}, nil
}

// GetGameCreatureRelations 查询一个 Creature 的形态、能力、学习、特性、携带物、皮肤与进化关系。
func (service *KratosService) GetGameCreatureRelations(ctx context.Context, request *domainv1.GetGameCreatureRelationsRequest) (*domainv1.GetGameCreatureRelationsResponse, error) {
	if _, err := gameDataPrincipal(ctx); err != nil {
		return nil, err
	}
	creatureID, err := gameDataIdentifier(request.GetCreatureId(), "INVALID_CREATURE_ID")
	if err != nil {
		return nil, err
	}
	value, err := service.services.CreatureAdministration.GetCreatureRelations(ctx, creatureID)
	if err != nil {
		return nil, service.creatureDataError(ctx, "GAME_CREATURE_RELATIONS_QUERY_FAILED", err)
	}
	return &domainv1.GetGameCreatureRelationsResponse{HttpStatusCode: 200, Body: creatureRelationsMessage(value)}, nil
}

// ReplaceGameCreatureRelations 原子保存一个 Creature 的完整关系集合。
func (service *KratosService) ReplaceGameCreatureRelations(ctx context.Context, request *domainv1.ReplaceGameCreatureRelationsRequest) (*domainv1.ReplaceGameCreatureRelationsResponse, error) {
	principal, err := gameDataPrincipal(ctx)
	if err != nil {
		return nil, err
	}
	body := request.GetBody()
	if body == nil || body.GetRelations() == nil {
		return nil, kratoserrors.BadRequest("INVALID_REQUEST", "请求格式无效")
	}
	creatureID, err := gameDataIdentifier(request.GetCreatureId(), "INVALID_CREATURE_ID")
	if err != nil {
		return nil, err
	}
	writeContext, err := gameDataWriteContext(ctx, principal.AccountID, request.GetHeaderIdempotencyKey())
	if err != nil {
		return nil, err
	}
	relations, err := creatureRelationsFromMessage(creatureID, body.GetRelations())
	if err != nil {
		return nil, err
	}
	value, err := service.services.CreatureAdministration.ReplaceRelations(ctx, creaturemetadata.ReplaceRelationsCommand{GameDataWriteContext: writeContext, CreatureID: creatureID, Relations: relations})
	if err != nil {
		return nil, service.creatureDataError(ctx, "GAME_CREATURE_RELATIONS_REPLACE_FAILED", err)
	}
	return &domainv1.ReplaceGameCreatureRelationsResponse{HttpStatusCode: 200, Body: creatureRelationsMessage(value)}, nil
}

func (service *KratosService) speciesCreateContext(ctx context.Context, request *domainv1.CreateGameCreatureSpeciesRequest) (authentication.Principal, *domainv1.CreateGameCreatureSpeciesBody, administration.GameDataWriteContext, error) {
	principal, err := gameDataPrincipal(ctx)
	if err != nil {
		return authentication.Principal{}, nil, administration.GameDataWriteContext{}, err
	}
	body := request.GetBody()
	if body == nil {
		return authentication.Principal{}, nil, administration.GameDataWriteContext{}, kratoserrors.BadRequest("INVALID_REQUEST", "请求格式无效")
	}
	writeContext, err := gameDataWriteContext(ctx, principal.AccountID, request.GetHeaderIdempotencyKey())
	return principal, body, writeContext, err
}

func speciesFromCreateBody(body *domainv1.CreateGameCreatureSpeciesBody) (creaturemetadata.Species, error) {
	return parseSpecies(snowflake.ID(0), body.GetNationalDexNumber(), body.GetCode(), body.GetName(), body.GetGrowthRateId(), body.GetHabitatId(), body.GetColorId(), body.GetShapeId(), body.GetEggGroupIds(), body.GetGenus(), body.GetPokedexEntry(), body.GetDescription(), body.GetProfile(), body.GetDesignOrigin(), body.GetTrivia(), body.GetGenderDifferences(), body.GetFormsSwitchable(), body.GetEnabled())
}

func speciesFromUpdateBody(id snowflake.ID, body *domainv1.UpdateGameCreatureSpeciesBody) (creaturemetadata.Species, error) {
	return parseSpecies(id, body.GetNationalDexNumber(), body.GetCode(), body.GetName(), body.GetGrowthRateId(), body.GetHabitatId(), body.GetColorId(), body.GetShapeId(), body.GetEggGroupIds(), body.GetGenus(), body.GetPokedexEntry(), body.GetDescription(), body.GetProfile(), body.GetDesignOrigin(), body.GetTrivia(), body.GetGenderDifferences(), body.GetFormsSwitchable(), body.GetEnabled())
}

func parseSpecies(id snowflake.ID, nationalDexNumber int32, code, name, growthRaw, habitatRaw, colorRaw, shapeRaw string, eggRaw []string, genus, entry, description, profile, designOrigin, trivia string, differences, switchable, enabled bool) (creaturemetadata.Species, error) {
	growth, err := optionalGameDataIdentifier(growthRaw, "INVALID_GROWTH_RATE_ID")
	if err != nil {
		return creaturemetadata.Species{}, err
	}
	habitat, err := optionalGameDataIdentifier(habitatRaw, "INVALID_HABITAT_ID")
	if err != nil {
		return creaturemetadata.Species{}, err
	}
	color, err := optionalGameDataIdentifier(colorRaw, "INVALID_SPECIES_COLOR_ID")
	if err != nil {
		return creaturemetadata.Species{}, err
	}
	shape, err := optionalGameDataIdentifier(shapeRaw, "INVALID_SPECIES_SHAPE_ID")
	if err != nil {
		return creaturemetadata.Species{}, err
	}
	eggs := make([]snowflake.ID, len(eggRaw))
	for index, raw := range eggRaw {
		eggs[index], err = gameDataIdentifier(raw, "INVALID_EGG_GROUP_ID")
		if err != nil {
			return creaturemetadata.Species{}, err
		}
	}
	return creaturemetadata.Species{ID: id, NationalDexNumber: nationalDexNumber, Code: code, Name: name, GrowthRateID: growth, HabitatID: habitat, ColorID: color, ShapeID: shape, EggGroupIDs: eggs, Genus: nullableText(genus), PokedexEntry: nullableText(entry), Description: nullableText(description), Profile: nullableText(profile), DesignOrigin: nullableText(designOrigin), Trivia: nullableText(trivia), GenderDifferences: differences, FormsSwitchable: switchable, Enabled: enabled}, nil
}

func creatureCreateCommand(body *domainv1.CreateGameCreatureBody) (creaturemetadata.CreateCreatureCommand, error) {
	speciesID, parentID, err := parseCreatureReferences(body.GetSpeciesId(), body.GetInheritsFromCreatureId())
	if err != nil {
		return creaturemetadata.CreateCreatureCommand{}, err
	}
	return creaturemetadata.CreateCreatureCommand{Code: body.GetCode(), Name: body.GetName(), SpeciesID: speciesID, InheritsFromCreatureID: parentID, Height: body.Height, Weight: body.Weight, BaseExperience: body.BaseExperience, CaptureRate: body.CaptureRate, HatchCycles: body.HatchCycles, GenderRatio: genderRatio(body.GetGenderRatio()), DefaultForm: body.GetDefaultForm(), Enabled: body.GetEnabled()}, nil
}

func creatureUpdateCommand(id snowflake.ID, version int64, body *domainv1.UpdateGameCreatureBody) (creaturemetadata.UpdateCreatureCommand, error) {
	speciesID, parentID, err := parseCreatureReferences(body.GetSpeciesId(), body.GetInheritsFromCreatureId())
	if err != nil {
		return creaturemetadata.UpdateCreatureCommand{}, err
	}
	return creaturemetadata.UpdateCreatureCommand{ID: id, ExpectedVersion: version, Code: body.GetCode(), Name: body.GetName(), SpeciesID: speciesID, InheritsFromCreatureID: parentID, Height: body.Height, Weight: body.Weight, BaseExperience: body.BaseExperience, CaptureRate: body.CaptureRate, HatchCycles: body.HatchCycles, GenderRatio: genderRatio(body.GetGenderRatio()), DefaultForm: body.GetDefaultForm(), Enabled: body.GetEnabled()}, nil
}

func parseCreatureReferences(speciesRaw, parentRaw string) (snowflake.ID, *snowflake.ID, error) {
	speciesID, err := gameDataIdentifier(speciesRaw, "INVALID_CREATURE_SPECIES_ID")
	if err != nil {
		return snowflake.ID(0), nil, err
	}
	parentID, err := optionalGameDataIdentifier(parentRaw, "INVALID_INHERITED_CREATURE_ID")
	return speciesID, parentID, err
}

func genderRatio(value *domainv1.GameCreatureGenderRatio) creaturemetadata.GenderRatio {
	return creaturemetadata.GenderRatio{MaleEighths: value.GetMaleEighths(), FemaleEighths: value.GetFemaleEighths()}
}

func creatureSpeciesMessage(value creaturemetadata.ManagedSpecies) *domainv1.GameCreatureSpecies {
	eggs := make([]string, len(value.EggGroupIDs))
	for index, id := range value.EggGroupIDs {
		eggs[index] = id.String()
	}
	return &domainv1.GameCreatureSpecies{Id: value.ID.String(), NationalDexNumber: value.NationalDexNumber, Code: value.Code, Name: value.Name, GrowthRateId: optionalIdentifierString(value.GrowthRateID), HabitatId: optionalIdentifierString(value.HabitatID), ColorId: optionalIdentifierString(value.ColorID), ShapeId: optionalIdentifierString(value.ShapeID), EggGroupIds: eggs, Genus: optionalString(value.Genus), PokedexEntry: optionalString(value.PokedexEntry), Description: optionalString(value.Description), Profile: optionalString(value.Profile), DesignOrigin: optionalString(value.DesignOrigin), Trivia: optionalString(value.Trivia), GenderDifferences: value.GenderDifferences, FormsSwitchable: value.FormsSwitchable, Enabled: value.Enabled, Version: strconv.FormatInt(value.Version, 10)}
}

func creatureReferenceOptionsMessage(value creaturemetadata.ReferenceOptions) *domainv1.GameCreatureReferenceOptions {
	result := &domainv1.GameCreatureReferenceOptions{
		EggGroups:   make([]*domainv1.GameCreatureDictionaryEntry, len(value.EggGroups)),
		GrowthRates: make([]*domainv1.GameCreatureGrowthRate, len(value.GrowthRates)),
		Habitats:    make([]*domainv1.GameCreatureDictionaryEntry, len(value.Habitats)),
		Colors:      make([]*domainv1.GameCreatureDictionaryEntry, len(value.Colors)),
		Shapes:      make([]*domainv1.GameCreatureDictionaryEntry, len(value.Shapes)),
	}
	for index, item := range value.EggGroups {
		result.EggGroups[index] = creatureDictionaryMessage(item.ID, item.Code, item.Name, item.SortOrder, item.Enabled)
	}
	for index, item := range value.GrowthRates {
		result.GrowthRates[index] = &domainv1.GameCreatureGrowthRate{Id: item.ID.String(), Code: item.Code, Name: item.Name, Formula: optionalString(item.Formula), Description: optionalString(item.Description), Enabled: item.Enabled}
	}
	for index, item := range value.Habitats {
		result.Habitats[index] = creatureDictionaryMessage(item.ID, item.Code, item.Name, item.SortOrder, item.Enabled)
	}
	for index, item := range value.Colors {
		result.Colors[index] = creatureDictionaryMessage(item.ID, item.Code, item.Name, item.SortOrder, item.Enabled)
	}
	for index, item := range value.Shapes {
		result.Shapes[index] = creatureDictionaryMessage(item.ID, item.Code, item.Name, item.SortOrder, item.Enabled)
	}
	return result
}

func creatureDictionaryMessage(id snowflake.ID, code, name string, sortOrder int32, enabled bool) *domainv1.GameCreatureDictionaryEntry {
	return &domainv1.GameCreatureDictionaryEntry{Id: id.String(), Code: code, Name: name, SortOrder: sortOrder, Enabled: enabled}
}

func creatureMessage(value creaturemetadata.ManagedCreature) *domainv1.GameCreature {
	ratio := value.GenderRatio
	if ratio == nil {
		ratio = &creaturemetadata.GenderRatio{}
	}
	return &domainv1.GameCreature{Id: value.ID.String(), Code: value.Code, Name: value.Name, SpeciesId: value.SpeciesID.String(), InheritsFromCreatureId: optionalIdentifierString(value.InheritsFromCreatureID), Height: optionalInt32Value(value.Height), Weight: optionalInt32Value(value.Weight), BaseExperience: optionalInt32Value(value.BaseExperience), CaptureRate: value.CaptureRate, HatchCycles: value.HatchCycles, DefaultForm: value.DefaultForm, Enabled: value.Enabled, GenderRatio: &domainv1.GameCreatureGenderRatio{MaleEighths: ratio.MaleEighths, FemaleEighths: ratio.FemaleEighths}, Version: strconv.FormatInt(value.Version, 10)}
}

func creatureRelationsFromMessage(creatureID snowflake.ID, message *domainv1.GameCreatureRelations) (creaturemetadata.CreatureRelations, error) {
	result := creaturemetadata.CreatureRelations{
		Forms: make([]creaturemetadata.Form, len(message.GetForms())), Stats: make([]creaturemetadata.StatBinding, len(message.GetStats())),
		SkillLearns: make([]creaturemetadata.SkillLearn, len(message.GetSkillLearns())), Abilities: make([]creaturemetadata.AbilityBinding, len(message.GetAbilities())),
		HeldItems: make([]creaturemetadata.HeldItem, len(message.GetHeldItems())), Skins: make([]creaturemetadata.Skin, len(message.GetSkins())), Evolutions: make([]creaturemetadata.Evolution, len(message.GetEvolutions())),
	}
	for index, value := range message.GetForms() {
		id, version, err := relationIdentity(value.GetId(), value.GetVersion())
		if err != nil {
			return creaturemetadata.CreatureRelations{}, err
		}
		elements, err := gameDataIdentifiers(value.GetElementIds(), "INVALID_ELEMENT_ID")
		if err != nil {
			return creaturemetadata.CreatureRelations{}, err
		}
		result.Forms[index] = creaturemetadata.Form{ID: id, Code: value.GetCode(), Name: value.GetName(), CreatureID: creatureID, FormName: nullableText(value.GetFormName()), SortOrder: optionalInt32(value.GetSortOrder()), FormOrder: optionalInt32(value.GetFormOrder()), BattleOnly: value.GetBattleOnly(), DefaultForm: value.GetDefaultForm(), EnhancedForm: value.GetEnhancedForm(), Enabled: value.GetEnabled(), Version: version, ElementIDs: elements}
	}
	for index, value := range message.GetStats() {
		id, version, err := relationIdentity(value.GetId(), value.GetVersion())
		if err != nil {
			return creaturemetadata.CreatureRelations{}, err
		}
		statID, err := gameDataIdentifier(value.GetStatId(), "INVALID_STAT_ID")
		if err != nil {
			return creaturemetadata.CreatureRelations{}, err
		}
		effort := value.GetEffort()
		result.Stats[index] = creaturemetadata.StatBinding{ID: id, CreatureID: creatureID, StatID: statID, BaseValue: value.GetBaseValue(), Effort: &effort, Enabled: value.GetEnabled(), Version: version}
	}
	for index, value := range message.GetSkillLearns() {
		id, version, err := relationIdentity(value.GetId(), value.GetVersion())
		if err != nil {
			return creaturemetadata.CreatureRelations{}, err
		}
		skillID, err := gameDataIdentifier(value.GetSkillId(), "INVALID_SKILL_ID")
		if err != nil {
			return creaturemetadata.CreatureRelations{}, err
		}
		methodID, err := gameDataIdentifier(value.GetLearnMethodId(), "INVALID_SKILL_LEARN_METHOD_ID")
		if err != nil {
			return creaturemetadata.CreatureRelations{}, err
		}
		result.SkillLearns[index] = creaturemetadata.SkillLearn{ID: id, CreatureID: creatureID, SkillID: skillID, LearnMethodID: methodID, LevelLearnedAt: value.GetLevelLearnedAt(), Enabled: value.GetEnabled(), Version: version}
	}
	for index, value := range message.GetAbilities() {
		id, version, err := relationIdentity(value.GetId(), value.GetVersion())
		if err != nil {
			return creaturemetadata.CreatureRelations{}, err
		}
		abilityID, err := gameDataIdentifier(value.GetAbilityId(), "INVALID_ABILITY_ID")
		if err != nil {
			return creaturemetadata.CreatureRelations{}, err
		}
		result.Abilities[index] = creaturemetadata.AbilityBinding{ID: id, CreatureID: creatureID, AbilityID: abilityID, Hidden: value.GetHidden(), Slot: value.GetSlot(), Enabled: value.GetEnabled(), Version: version}
	}
	for index, value := range message.GetHeldItems() {
		id, version, err := relationIdentity(value.GetId(), value.GetVersion())
		if err != nil {
			return creaturemetadata.CreatureRelations{}, err
		}
		itemID, err := gameDataIdentifier(value.GetItemId(), "INVALID_ITEM_ID")
		if err != nil {
			return creaturemetadata.CreatureRelations{}, err
		}
		result.HeldItems[index] = creaturemetadata.HeldItem{ID: id, CreatureID: creatureID, ItemID: itemID, Rarity: value.GetRarity(), Enabled: value.GetEnabled(), Version: version}
	}
	for index, value := range message.GetSkins() {
		id, version, err := relationIdentity(value.GetId(), value.GetVersion())
		if err != nil {
			return creaturemetadata.CreatureRelations{}, err
		}
		assetID, err := optionalGameDataIdentifier(value.GetAssetId(), "INVALID_ASSET_ID")
		if err != nil {
			return creaturemetadata.CreatureRelations{}, err
		}
		result.Skins[index] = creaturemetadata.Skin{ID: id, CreatureID: creatureID, Code: value.GetCode(), Name: value.GetName(), AssetID: assetID, Enabled: value.GetEnabled(), Version: version}
	}
	for index, value := range message.GetEvolutions() {
		id, version, err := relationIdentity(value.GetId(), value.GetVersion())
		if err != nil {
			return creaturemetadata.CreatureRelations{}, err
		}
		toCreatureID, err := gameDataIdentifier(value.GetToCreatureId(), "INVALID_EVOLUTION_TARGET_CREATURE_ID")
		if err != nil {
			return creaturemetadata.CreatureRelations{}, err
		}
		triggerItemID, err := optionalGameDataIdentifier(value.GetTriggerItemId(), "INVALID_EVOLUTION_TRIGGER_ITEM_ID")
		if err != nil {
			return creaturemetadata.CreatureRelations{}, err
		}
		requiredSkillID, err := optionalGameDataIdentifier(value.GetRequiredSkillId(), "INVALID_EVOLUTION_REQUIRED_SKILL_ID")
		if err != nil {
			return creaturemetadata.CreatureRelations{}, err
		}
		result.Evolutions[index] = creaturemetadata.Evolution{ID: id, FromCreatureID: creatureID, ToCreatureID: toCreatureID, TriggerType: creaturemetadata.EvolutionTriggerType(value.GetTriggerType()), MinimumLevel: value.MinimumLevel, TriggerItemID: triggerItemID, MinimumFriendship: value.MinimumFriendship, TimeOfDay: nullableText(value.GetTimeOfDay()), Gender: nullableText(value.GetGender()), RequiredSkillID: requiredSkillID, ConditionText: value.GetConditionText(), Enabled: value.GetEnabled(), Version: version}
	}
	return result, nil
}

func relationIdentity(rawID, rawVersion string) (snowflake.ID, int64, error) {
	if rawID == "" && rawVersion == "" {
		return snowflake.ID(0), 0, nil
	}
	id, err := gameDataIdentifier(rawID, "INVALID_CREATURE_RELATION_ID")
	if err != nil {
		return snowflake.ID(0), 0, err
	}
	version, err := gameDataVersion(rawVersion)
	return id, version, err
}

func creatureRelationsMessage(value creaturemetadata.CreatureRelations) *domainv1.GameCreatureRelations {
	result := &domainv1.GameCreatureRelations{Forms: make([]*domainv1.GameCreatureForm, len(value.Forms)), Stats: make([]*domainv1.GameCreatureStatBinding, len(value.Stats)), SkillLearns: make([]*domainv1.GameCreatureSkillLearn, len(value.SkillLearns)), Abilities: make([]*domainv1.GameCreatureAbilityBinding, len(value.Abilities)), HeldItems: make([]*domainv1.GameCreatureHeldItem, len(value.HeldItems)), Skins: make([]*domainv1.GameCreatureSkin, len(value.Skins)), Evolutions: make([]*domainv1.GameCreatureEvolution, len(value.Evolutions))}
	for index, item := range value.Forms {
		elements := make([]string, len(item.ElementIDs))
		for elementIndex, id := range item.ElementIDs {
			elements[elementIndex] = id.String()
		}
		result.Forms[index] = &domainv1.GameCreatureForm{Id: item.ID.String(), Code: item.Code, Name: item.Name, CreatureId: item.CreatureID.String(), FormName: optionalString(item.FormName), SortOrder: optionalInt32Value(item.SortOrder), FormOrder: optionalInt32Value(item.FormOrder), BattleOnly: item.BattleOnly, DefaultForm: item.DefaultForm, EnhancedForm: item.EnhancedForm, Enabled: item.Enabled, ElementIds: elements, Version: strconv.FormatInt(item.Version, 10)}
	}
	for index, item := range value.Stats {
		result.Stats[index] = &domainv1.GameCreatureStatBinding{Id: item.ID.String(), CreatureId: item.CreatureID.String(), StatId: item.StatID.String(), BaseValue: item.BaseValue, Effort: optionalInt32Value(item.Effort), Enabled: item.Enabled, Version: strconv.FormatInt(item.Version, 10)}
	}
	for index, item := range value.SkillLearns {
		result.SkillLearns[index] = &domainv1.GameCreatureSkillLearn{Id: item.ID.String(), CreatureId: item.CreatureID.String(), SkillId: item.SkillID.String(), LearnMethodId: item.LearnMethodID.String(), LevelLearnedAt: item.LevelLearnedAt, Enabled: item.Enabled, Version: strconv.FormatInt(item.Version, 10)}
	}
	for index, item := range value.Abilities {
		result.Abilities[index] = &domainv1.GameCreatureAbilityBinding{Id: item.ID.String(), CreatureId: item.CreatureID.String(), AbilityId: item.AbilityID.String(), Hidden: item.Hidden, Slot: item.Slot, Enabled: item.Enabled, Version: strconv.FormatInt(item.Version, 10)}
	}
	for index, item := range value.HeldItems {
		result.HeldItems[index] = &domainv1.GameCreatureHeldItem{Id: item.ID.String(), CreatureId: item.CreatureID.String(), ItemId: item.ItemID.String(), Rarity: item.Rarity, Enabled: item.Enabled, Version: strconv.FormatInt(item.Version, 10)}
	}
	for index, item := range value.Skins {
		result.Skins[index] = &domainv1.GameCreatureSkin{Id: item.ID.String(), CreatureId: item.CreatureID.String(), Code: item.Code, Name: item.Name, AssetId: optionalIdentifierString(item.AssetID), Enabled: item.Enabled, Version: strconv.FormatInt(item.Version, 10)}
	}
	for index, item := range value.Evolutions {
		result.Evolutions[index] = &domainv1.GameCreatureEvolution{Id: item.ID.String(), FromCreatureId: item.FromCreatureID.String(), ToCreatureId: item.ToCreatureID.String(), TriggerType: string(item.TriggerType), MinimumLevel: item.MinimumLevel, TriggerItemId: optionalIdentifierString(item.TriggerItemID), MinimumFriendship: item.MinimumFriendship, TimeOfDay: optionalString(item.TimeOfDay), Gender: optionalString(item.Gender), RequiredSkillId: optionalIdentifierString(item.RequiredSkillID), ConditionText: item.ConditionText, Enabled: item.Enabled, Version: strconv.FormatInt(item.Version, 10)}
	}
	return result
}

func optionalString(value *string) string {
	if value == nil {
		return ""
	}
	return *value
}
func optionalInt32Value(value *int32) int32 {
	if value == nil {
		return 0
	}
	return *value
}

func (service *KratosService) creatureDataError(ctx context.Context, reason string, err error) error {
	switch {
	case errors.Is(err, creaturemetadata.ErrInvalidCreatureMetadata):
		return kratoserrors.BadRequest("INVALID_GAME_CREATURE_DATA", "Creature 资料字段无效")
	case errors.Is(err, creaturemetadata.ErrCreatureDataNotFound), errors.Is(err, creaturemetadata.ErrCreatureDataNotFound):
		return kratoserrors.NotFound("GAME_CREATURE_DATA_NOT_FOUND", "Creature 资料或维护窗口不存在")
	case errors.Is(err, creaturemetadata.ErrCreatureDataConflict), errors.Is(err, idempotency.ErrConflict):
		return kratoserrors.Conflict("GAME_CREATURE_DATA_CONFLICT", "Creature 资料状态、版本或幂等请求冲突")
	default:
		service.logger.ErrorContext(ctx, "Creature 资料 Kratos 服务调用失败", "reason", reason, "error", err)
		return kratoserrors.InternalServer(reason, "服务端无法完成请求")
	}
}
