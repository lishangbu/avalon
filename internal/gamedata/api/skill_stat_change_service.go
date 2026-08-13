package api

import (
	"context"
	"errors"
	"strconv"

	kratoserrors "github.com/go-kratos/kratos/v3/errors"
	domainv1 "github.com/lishangbu/avalon/api/gen/go/avalon/domain/v1"
	"github.com/lishangbu/avalon/internal/gamedata/skillstatchange"
	"github.com/lishangbu/avalon/internal/platform/idempotency"
)

// ListGameSkillStatChanges 分页查询维护窗口中的技能数值变化。
func (service *KratosService) ListGameSkillStatChanges(ctx context.Context, request *domainv1.ListGameSkillStatChangesRequest) (*domainv1.ListGameSkillStatChangesResponse, error) {
	if _, err := gameDataPrincipal(ctx); err != nil {
		return nil, err
	}
	skillID, err := optionalGameDataIdentifier(request.GetSkillId(), "INVALID_SKILL_ID")
	if err != nil {
		return nil, err
	}
	statID, err := optionalGameDataIdentifier(request.GetStatId(), "INVALID_STAT_ID")
	if err != nil {
		return nil, err
	}
	page, pageSize := gameDataPage(request.GetPage(), request.GetPageSize())
	sortValue := request.GetSort()
	if sortValue == "" {
		sortValue = string(skillstatchange.SortSkillAscending)
	}
	result, err := service.services.SkillStatChanges.List(ctx, skillstatchange.ListQuery{
		Page: page, PageSize: pageSize, Q: request.GetQ(), SkillID: skillID, StatID: statID,
		ChangeValue: optionalInt32(request.GetChangeValue()), Sort: skillstatchange.Sort(sortValue),
	})
	if err != nil {
		return nil, service.skillStatChangeError(ctx, "GAME_SKILL_STAT_CHANGE_LIST_FAILED", err)
	}
	items := make([]*domainv1.GameSkillStatChange, len(result.Items))
	for index := range result.Items {
		items[index] = gameSkillStatChangeMessage(result.Items[index])
	}
	return &domainv1.ListGameSkillStatChangesResponse{HttpStatusCode: 200, Body: &domainv1.GameSkillStatChangePage{Items: items, Total: strconv.FormatInt(result.Total, 10), Page: result.Page, PageSize: result.PageSize}}, nil
}

// CreateGameSkillStatChange 在维护窗口中创建独立的技能数值变化。
func (service *KratosService) CreateGameSkillStatChange(ctx context.Context, request *domainv1.CreateGameSkillStatChangeRequest) (*domainv1.CreateGameSkillStatChangeResponse, error) {
	principal, err := gameDataPrincipal(ctx)
	if err != nil {
		return nil, err
	}
	if request.GetBody() == nil || request.GetBody().GetChangeValue() == nil {
		return nil, kratoserrors.BadRequest("INVALID_REQUEST", "请求格式无效")
	}
	skillID, err := gameDataIdentifier(request.GetBody().GetSkillId(), "INVALID_SKILL_ID")
	if err != nil {
		return nil, err
	}
	statID, err := gameDataIdentifier(request.GetBody().GetStatId(), "INVALID_STAT_ID")
	if err != nil {
		return nil, err
	}
	writeContext, err := gameDataWriteContext(ctx, principal.AccountID, request.GetHeaderIdempotencyKey())
	if err != nil {
		return nil, err
	}
	created, err := service.services.SkillStatChanges.Create(ctx, skillstatchange.CreateCommand{GameDataWriteContext: writeContext, SkillID: skillID, StatID: statID, ChangeValue: request.GetBody().GetChangeValue().GetValue()})
	if err != nil {
		return nil, service.skillStatChangeError(ctx, "GAME_SKILL_STAT_CHANGE_CREATE_FAILED", err)
	}
	return &domainv1.CreateGameSkillStatChangeResponse{HttpStatusCode: 201, Body: gameSkillStatChangeMessage(created)}, nil
}

// GetGameSkillStatChange 查询维护窗口中指定稳定身份的技能数值变化。
func (service *KratosService) GetGameSkillStatChange(ctx context.Context, request *domainv1.GetGameSkillStatChangeRequest) (*domainv1.GetGameSkillStatChangeResponse, error) {
	if _, err := gameDataPrincipal(ctx); err != nil {
		return nil, err
	}
	changeID, err := gameDataIdentifier(request.GetChangeId(), "INVALID_SKILL_STAT_CHANGE_ID")
	if err != nil {
		return nil, err
	}
	value, err := service.services.SkillStatChanges.Get(ctx, changeID)
	if err != nil {
		return nil, service.skillStatChangeError(ctx, "GAME_SKILL_STAT_CHANGE_QUERY_FAILED", err)
	}
	return &domainv1.GetGameSkillStatChangeResponse{HttpStatusCode: 200, Body: gameSkillStatChangeMessage(value)}, nil
}

// UpdateGameSkillStatChange 使用独立乐观版本更新技能数值变化。
func (service *KratosService) UpdateGameSkillStatChange(ctx context.Context, request *domainv1.UpdateGameSkillStatChangeRequest) (*domainv1.UpdateGameSkillStatChangeResponse, error) {
	principal, err := gameDataPrincipal(ctx)
	if err != nil {
		return nil, err
	}
	if request.GetBody() == nil || request.GetBody().GetChangeValue() == nil {
		return nil, kratoserrors.BadRequest("INVALID_REQUEST", "请求格式无效")
	}
	changeID, err := gameDataIdentifier(request.GetChangeId(), "INVALID_SKILL_STAT_CHANGE_ID")
	if err != nil {
		return nil, err
	}
	skillID, err := gameDataIdentifier(request.GetBody().GetSkillId(), "INVALID_SKILL_ID")
	if err != nil {
		return nil, err
	}
	statID, err := gameDataIdentifier(request.GetBody().GetStatId(), "INVALID_STAT_ID")
	if err != nil {
		return nil, err
	}
	version, err := gameDataVersion(request.GetBody().GetExpectedVersion())
	if err != nil {
		return nil, err
	}
	writeContext, err := gameDataWriteContext(ctx, principal.AccountID, request.GetHeaderIdempotencyKey())
	if err != nil {
		return nil, err
	}
	updated, err := service.services.SkillStatChanges.Update(ctx, skillstatchange.UpdateCommand{GameDataWriteContext: writeContext, ChangeID: changeID, SkillID: skillID, StatID: statID, ChangeValue: request.GetBody().GetChangeValue().GetValue(), ExpectedVersion: version})
	if err != nil {
		return nil, service.skillStatChangeError(ctx, "GAME_SKILL_STAT_CHANGE_UPDATE_FAILED", err)
	}
	return &domainv1.UpdateGameSkillStatChangeResponse{HttpStatusCode: 200, Body: gameSkillStatChangeMessage(updated)}, nil
}

// DisableGameSkillStatChange 使用独立乐观版本禁用技能数值变化。
func (service *KratosService) DisableGameSkillStatChange(ctx context.Context, request *domainv1.DisableGameSkillStatChangeRequest) (*domainv1.DisableGameSkillStatChangeResponse, error) {
	principal, err := gameDataPrincipal(ctx)
	if err != nil {
		return nil, err
	}
	changeID, err := gameDataIdentifier(request.GetChangeId(), "INVALID_SKILL_STAT_CHANGE_ID")
	if err != nil {
		return nil, err
	}
	version, err := gameDataVersion(request.GetExpectedVersion())
	if err != nil {
		return nil, err
	}
	writeContext, err := gameDataWriteContext(ctx, principal.AccountID, request.GetHeaderIdempotencyKey())
	if err != nil {
		return nil, err
	}
	if err := service.services.SkillStatChanges.Disable(ctx, skillstatchange.DisableCommand{GameDataWriteContext: writeContext, ChangeID: changeID, ExpectedVersion: version}); err != nil {
		return nil, service.skillStatChangeError(ctx, "GAME_SKILL_STAT_CHANGE_DISABLE_FAILED", err)
	}
	return &domainv1.DisableGameSkillStatChangeResponse{Body: &domainv1.GameSkillStatChangeDisabled{Disabled: true}}, nil
}

func gameSkillStatChangeMessage(value skillstatchange.Change) *domainv1.GameSkillStatChange {
	return &domainv1.GameSkillStatChange{Id: value.ID.String(), SkillId: value.SkillID.String(), StatId: value.StatID.String(), ChangeValue: value.ChangeValue, Version: strconv.FormatInt(value.Version, 10)}
}

func (service *KratosService) skillStatChangeError(ctx context.Context, reason string, err error) error {
	switch {
	case errors.Is(err, skillstatchange.ErrInvalidSkillStatChange):
		return kratoserrors.BadRequest("INVALID_GAME_SKILL_STAT_CHANGE", "技能数值变化字段无效")
	case errors.Is(err, skillstatchange.ErrSkillStatChangeNotFound), errors.Is(err, skillstatchange.ErrSkillStatChangeDependencyNotFound), errors.Is(err, skillstatchange.ErrSkillStatChangeNotFound):
		return kratoserrors.NotFound("GAME_SKILL_STAT_CHANGE_NOT_FOUND", "技能数值变化、依赖或维护窗口不存在")
	case errors.Is(err, skillstatchange.ErrSkillStatChangeVersionConflict), errors.Is(err, skillstatchange.ErrSkillStatChangeConflict), errors.Is(err, skillstatchange.ErrSkillStatChangeVersionConflict), errors.Is(err, idempotency.ErrConflict):
		return kratoserrors.Conflict("GAME_SKILL_STAT_CHANGE_CONFLICT", "技能数值变化状态、版本或幂等请求冲突")
	default:
		service.logger.ErrorContext(ctx, "技能数值变化 Kratos 服务调用失败", "reason", reason, "error", err)
		return kratoserrors.InternalServer(reason, "服务端无法完成请求")
	}
}
