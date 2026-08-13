package api

import (
	"context"
	"errors"
	"strconv"

	kratoserrors "github.com/go-kratos/kratos/v3/errors"
	domainv1 "github.com/lishangbu/avalon/api/gen/go/avalon/domain/v1"
	"github.com/lishangbu/avalon/internal/gamedata/referencedictionary"
	"github.com/lishangbu/avalon/internal/platform/idempotency"
)

func (service *KratosService) listReferenceDictionary(ctx context.Context, kind referencedictionary.Kind) ([]referencedictionary.Entry, error) {
	if _, err := gameDataPrincipal(ctx); err != nil {
		return nil, err
	}
	values, err := service.services.ReferenceDictionaries.List(ctx, kind)
	if err != nil {
		return nil, service.referenceDictionaryError(ctx, err)
	}
	return values, nil
}

func (service *KratosService) createReferenceDictionary(ctx context.Context, key string, entry referencedictionary.Entry) (referencedictionary.Entry, error) {
	principal, err := gameDataPrincipal(ctx)
	if err != nil {
		return referencedictionary.Entry{}, err
	}
	write, err := gameDataWriteContext(ctx, principal.AccountID, key)
	if err != nil {
		return referencedictionary.Entry{}, err
	}
	value, err := service.services.ReferenceDictionaries.Create(ctx, referencedictionary.CreateCommand{GameDataWriteContext: write, Entry: entry})
	if err != nil {
		return referencedictionary.Entry{}, service.referenceDictionaryError(ctx, err)
	}
	return value, nil
}

func (service *KratosService) updateReferenceDictionary(ctx context.Context, key, rawID, rawVersion string, entry referencedictionary.Entry) (referencedictionary.Entry, error) {
	principal, err := gameDataPrincipal(ctx)
	if err != nil {
		return referencedictionary.Entry{}, err
	}
	entry.ID, err = gameDataIdentifier(rawID, "INVALID_REFERENCE_DICTIONARY_ID")
	if err != nil {
		return referencedictionary.Entry{}, err
	}
	version, err := gameDataVersion(rawVersion)
	if err != nil {
		return referencedictionary.Entry{}, err
	}
	write, err := gameDataWriteContext(ctx, principal.AccountID, key)
	if err != nil {
		return referencedictionary.Entry{}, err
	}
	value, err := service.services.ReferenceDictionaries.Update(ctx, referencedictionary.UpdateCommand{GameDataWriteContext: write, Entry: entry, ExpectedVersion: version})
	if err != nil {
		return referencedictionary.Entry{}, service.referenceDictionaryError(ctx, err)
	}
	return value, nil
}

func (service *KratosService) referenceDictionaryError(ctx context.Context, err error) error {
	switch {
	case errors.Is(err, referencedictionary.ErrInvalid):
		return kratoserrors.BadRequest("INVALID_REFERENCE_DICTIONARY", "引用资料字段无效")
	case errors.Is(err, referencedictionary.ErrNotFound):
		return kratoserrors.NotFound("REFERENCE_DICTIONARY_NOT_FOUND", "引用资料不存在")
	case errors.Is(err, referencedictionary.ErrConflict), errors.Is(err, idempotency.ErrConflict):
		return kratoserrors.Conflict("REFERENCE_DICTIONARY_CONFLICT", "引用资料编码、版本或幂等请求冲突")
	default:
		service.logger.ErrorContext(ctx, "引用资料服务调用失败", "error", err)
		return kratoserrors.InternalServer("REFERENCE_DICTIONARY_FAILED", "服务端无法完成请求")
	}
}

func referenceText(value *string) string {
	if value == nil {
		return ""
	}
	return *value
}
func growthRateMessage(v referencedictionary.Entry) *domainv1.GameGrowthRate {
	return &domainv1.GameGrowthRate{Id: v.ID.String(), Code: v.Code, Name: v.Name, Formula: referenceText(v.Formula), Description: referenceText(v.Description), Enabled: v.Enabled, Version: strconv.FormatInt(v.Version, 10)}
}
func referenceMessage(v referencedictionary.Entry) *domainv1.GameCreatureReferenceDictionary {
	return &domainv1.GameCreatureReferenceDictionary{Id: v.ID.String(), Code: v.Code, Name: v.Name, SortOrder: v.SortOrder, Enabled: v.Enabled, Version: strconv.FormatInt(v.Version, 10)}
}
func currencyMessage(v referencedictionary.Entry) *domainv1.GameCurrency {
	return &domainv1.GameCurrency{Id: v.ID.String(), Code: v.Code, Name: v.Name, Symbol: referenceText(v.Symbol), Enabled: v.Enabled, Version: strconv.FormatInt(v.Version, 10)}
}

// ListGameGrowthRates 读取全部成长速率。
func (service *KratosService) ListGameGrowthRates(ctx context.Context, _ *domainv1.ListGameGrowthRatesRequest) (*domainv1.ListGameGrowthRatesResponse, error) {
	values, err := service.listReferenceDictionary(ctx, referencedictionary.KindGrowthRate)
	if err != nil {
		return nil, err
	}
	items := make([]*domainv1.GameGrowthRate, len(values))
	for i, v := range values {
		items[i] = growthRateMessage(v)
	}
	return &domainv1.ListGameGrowthRatesResponse{HttpStatusCode: 200, Body: &domainv1.GameGrowthRatePage{Items: items}}, nil
}

// CreateGameGrowthRate 创建成长速率。
func (service *KratosService) CreateGameGrowthRate(ctx context.Context, r *domainv1.CreateGameGrowthRateRequest) (*domainv1.CreateGameGrowthRateResponse, error) {
	if r.GetBody() == nil {
		return nil, kratoserrors.BadRequest("INVALID_REQUEST", "请求格式无效")
	}
	b := r.GetBody()
	v, e := service.createReferenceDictionary(ctx, r.GetHeaderIdempotencyKey(), referencedictionary.Entry{Kind: referencedictionary.KindGrowthRate, Code: b.GetCode(), Name: b.GetName(), Formula: nullableText(b.GetFormula()), Description: nullableText(b.GetDescription()), Enabled: b.GetEnabled()})
	if e != nil {
		return nil, e
	}
	return &domainv1.CreateGameGrowthRateResponse{HttpStatusCode: 201, Body: growthRateMessage(v)}, nil
}

// UpdateGameGrowthRate 更新成长速率。
func (service *KratosService) UpdateGameGrowthRate(ctx context.Context, r *domainv1.UpdateGameGrowthRateRequest) (*domainv1.UpdateGameGrowthRateResponse, error) {
	if r.GetBody() == nil {
		return nil, kratoserrors.BadRequest("INVALID_REQUEST", "请求格式无效")
	}
	b := r.GetBody()
	v, e := service.updateReferenceDictionary(ctx, r.GetHeaderIdempotencyKey(), r.GetGrowthRateId(), b.GetExpectedVersion(), referencedictionary.Entry{Kind: referencedictionary.KindGrowthRate, Code: b.GetCode(), Name: b.GetName(), Formula: nullableText(b.GetFormula()), Description: nullableText(b.GetDescription()), Enabled: b.GetEnabled()})
	if e != nil {
		return nil, e
	}
	return &domainv1.UpdateGameGrowthRateResponse{HttpStatusCode: 200, Body: growthRateMessage(v)}, nil
}

func (service *KratosService) listSimple(ctx context.Context, kind referencedictionary.Kind) (*domainv1.GameCreatureReferenceDictionaryPage, error) {
	values, err := service.listReferenceDictionary(ctx, kind)
	if err != nil {
		return nil, err
	}
	items := make([]*domainv1.GameCreatureReferenceDictionary, len(values))
	for i, v := range values {
		items[i] = referenceMessage(v)
	}
	return &domainv1.GameCreatureReferenceDictionaryPage{Items: items}, nil
}
func simpleEntry(kind referencedictionary.Kind, b interface {
	GetCode() string
	GetName() string
	GetSortOrder() int32
	GetEnabled() bool
}) referencedictionary.Entry {
	return referencedictionary.Entry{Kind: kind, Code: b.GetCode(), Name: b.GetName(), SortOrder: b.GetSortOrder(), Enabled: b.GetEnabled()}
}

// ListGameHabitats 读取全部栖息地。
func (s *KratosService) ListGameHabitats(c context.Context, _ *domainv1.ListGameHabitatsRequest) (*domainv1.ListGameHabitatsResponse, error) {
	p, e := s.listSimple(c, referencedictionary.KindHabitat)
	return &domainv1.ListGameHabitatsResponse{HttpStatusCode: 200, Body: p}, e
}
func (s *KratosService) CreateGameHabitat(c context.Context, r *domainv1.CreateGameHabitatRequest) (*domainv1.CreateGameHabitatResponse, error) {
	if r.GetBody() == nil {
		return nil, kratoserrors.BadRequest("INVALID_REQUEST", "请求格式无效")
	}
	v, e := s.createReferenceDictionary(c, r.GetHeaderIdempotencyKey(), simpleEntry(referencedictionary.KindHabitat, r.GetBody()))
	return &domainv1.CreateGameHabitatResponse{HttpStatusCode: 201, Body: referenceMessage(v)}, e
}
func (s *KratosService) UpdateGameHabitat(c context.Context, r *domainv1.UpdateGameHabitatRequest) (*domainv1.UpdateGameHabitatResponse, error) {
	if r.GetBody() == nil {
		return nil, kratoserrors.BadRequest("INVALID_REQUEST", "请求格式无效")
	}
	v, e := s.updateReferenceDictionary(c, r.GetHeaderIdempotencyKey(), r.GetHabitatId(), r.GetBody().GetExpectedVersion(), simpleEntry(referencedictionary.KindHabitat, r.GetBody()))
	return &domainv1.UpdateGameHabitatResponse{HttpStatusCode: 200, Body: referenceMessage(v)}, e
}
func (s *KratosService) ListGameSpeciesColors(c context.Context, _ *domainv1.ListGameSpeciesColorsRequest) (*domainv1.ListGameSpeciesColorsResponse, error) {
	p, e := s.listSimple(c, referencedictionary.KindSpeciesColor)
	return &domainv1.ListGameSpeciesColorsResponse{HttpStatusCode: 200, Body: p}, e
}
func (s *KratosService) CreateGameSpeciesColor(c context.Context, r *domainv1.CreateGameSpeciesColorRequest) (*domainv1.CreateGameSpeciesColorResponse, error) {
	if r.GetBody() == nil {
		return nil, kratoserrors.BadRequest("INVALID_REQUEST", "请求格式无效")
	}
	v, e := s.createReferenceDictionary(c, r.GetHeaderIdempotencyKey(), simpleEntry(referencedictionary.KindSpeciesColor, r.GetBody()))
	return &domainv1.CreateGameSpeciesColorResponse{HttpStatusCode: 201, Body: referenceMessage(v)}, e
}
func (s *KratosService) UpdateGameSpeciesColor(c context.Context, r *domainv1.UpdateGameSpeciesColorRequest) (*domainv1.UpdateGameSpeciesColorResponse, error) {
	if r.GetBody() == nil {
		return nil, kratoserrors.BadRequest("INVALID_REQUEST", "请求格式无效")
	}
	v, e := s.updateReferenceDictionary(c, r.GetHeaderIdempotencyKey(), r.GetColorId(), r.GetBody().GetExpectedVersion(), simpleEntry(referencedictionary.KindSpeciesColor, r.GetBody()))
	return &domainv1.UpdateGameSpeciesColorResponse{HttpStatusCode: 200, Body: referenceMessage(v)}, e
}
func (s *KratosService) ListGameSpeciesShapes(c context.Context, _ *domainv1.ListGameSpeciesShapesRequest) (*domainv1.ListGameSpeciesShapesResponse, error) {
	p, e := s.listSimple(c, referencedictionary.KindSpeciesShape)
	return &domainv1.ListGameSpeciesShapesResponse{HttpStatusCode: 200, Body: p}, e
}
func (s *KratosService) CreateGameSpeciesShape(c context.Context, r *domainv1.CreateGameSpeciesShapeRequest) (*domainv1.CreateGameSpeciesShapeResponse, error) {
	if r.GetBody() == nil {
		return nil, kratoserrors.BadRequest("INVALID_REQUEST", "请求格式无效")
	}
	v, e := s.createReferenceDictionary(c, r.GetHeaderIdempotencyKey(), simpleEntry(referencedictionary.KindSpeciesShape, r.GetBody()))
	return &domainv1.CreateGameSpeciesShapeResponse{HttpStatusCode: 201, Body: referenceMessage(v)}, e
}
func (s *KratosService) UpdateGameSpeciesShape(c context.Context, r *domainv1.UpdateGameSpeciesShapeRequest) (*domainv1.UpdateGameSpeciesShapeResponse, error) {
	if r.GetBody() == nil {
		return nil, kratoserrors.BadRequest("INVALID_REQUEST", "请求格式无效")
	}
	v, e := s.updateReferenceDictionary(c, r.GetHeaderIdempotencyKey(), r.GetShapeId(), r.GetBody().GetExpectedVersion(), simpleEntry(referencedictionary.KindSpeciesShape, r.GetBody()))
	return &domainv1.UpdateGameSpeciesShapeResponse{HttpStatusCode: 200, Body: referenceMessage(v)}, e
}
func (s *KratosService) ListGameEggGroups(c context.Context, _ *domainv1.ListGameEggGroupsRequest) (*domainv1.ListGameEggGroupsResponse, error) {
	p, e := s.listSimple(c, referencedictionary.KindEggGroup)
	return &domainv1.ListGameEggGroupsResponse{HttpStatusCode: 200, Body: p}, e
}
func (s *KratosService) CreateGameEggGroup(c context.Context, r *domainv1.CreateGameEggGroupRequest) (*domainv1.CreateGameEggGroupResponse, error) {
	if r.GetBody() == nil {
		return nil, kratoserrors.BadRequest("INVALID_REQUEST", "请求格式无效")
	}
	v, e := s.createReferenceDictionary(c, r.GetHeaderIdempotencyKey(), simpleEntry(referencedictionary.KindEggGroup, r.GetBody()))
	return &domainv1.CreateGameEggGroupResponse{HttpStatusCode: 201, Body: referenceMessage(v)}, e
}
func (s *KratosService) UpdateGameEggGroup(c context.Context, r *domainv1.UpdateGameEggGroupRequest) (*domainv1.UpdateGameEggGroupResponse, error) {
	if r.GetBody() == nil {
		return nil, kratoserrors.BadRequest("INVALID_REQUEST", "请求格式无效")
	}
	v, e := s.updateReferenceDictionary(c, r.GetHeaderIdempotencyKey(), r.GetEggGroupId(), r.GetBody().GetExpectedVersion(), simpleEntry(referencedictionary.KindEggGroup, r.GetBody()))
	return &domainv1.UpdateGameEggGroupResponse{HttpStatusCode: 200, Body: referenceMessage(v)}, e
}

// ListGameCurrencies 读取全部货币。
func (s *KratosService) ListGameCurrencies(c context.Context, _ *domainv1.ListGameCurrenciesRequest) (*domainv1.ListGameCurrenciesResponse, error) {
	values, e := s.listReferenceDictionary(c, referencedictionary.KindCurrency)
	if e != nil {
		return nil, e
	}
	items := make([]*domainv1.GameCurrency, len(values))
	for i, v := range values {
		items[i] = currencyMessage(v)
	}
	return &domainv1.ListGameCurrenciesResponse{HttpStatusCode: 200, Body: &domainv1.GameCurrencyPage{Items: items}}, nil
}
func (s *KratosService) CreateGameCurrency(c context.Context, r *domainv1.CreateGameCurrencyRequest) (*domainv1.CreateGameCurrencyResponse, error) {
	if r.GetBody() == nil {
		return nil, kratoserrors.BadRequest("INVALID_REQUEST", "请求格式无效")
	}
	b := r.GetBody()
	v, e := s.createReferenceDictionary(c, r.GetHeaderIdempotencyKey(), referencedictionary.Entry{Kind: referencedictionary.KindCurrency, Code: b.GetCode(), Name: b.GetName(), Symbol: nullableText(b.GetSymbol()), Enabled: b.GetEnabled()})
	return &domainv1.CreateGameCurrencyResponse{HttpStatusCode: 201, Body: currencyMessage(v)}, e
}
func (s *KratosService) UpdateGameCurrency(c context.Context, r *domainv1.UpdateGameCurrencyRequest) (*domainv1.UpdateGameCurrencyResponse, error) {
	if r.GetBody() == nil {
		return nil, kratoserrors.BadRequest("INVALID_REQUEST", "请求格式无效")
	}
	b := r.GetBody()
	v, e := s.updateReferenceDictionary(c, r.GetHeaderIdempotencyKey(), r.GetCurrencyId(), b.GetExpectedVersion(), referencedictionary.Entry{Kind: referencedictionary.KindCurrency, Code: b.GetCode(), Name: b.GetName(), Symbol: nullableText(b.GetSymbol()), Enabled: b.GetEnabled()})
	return &domainv1.UpdateGameCurrencyResponse{HttpStatusCode: 200, Body: currencyMessage(v)}, e
}
