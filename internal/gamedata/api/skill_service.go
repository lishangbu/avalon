package api

import (
	"context"
	"errors"
	"strconv"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	kratoserrors "github.com/go-kratos/kratos/v3/errors"
	domainv1 "github.com/lishangbu/avalon/api/gen/go/avalon/domain/v1"
	"github.com/lishangbu/avalon/internal/gamedata/skill"
	"github.com/lishangbu/avalon/internal/platform/idempotency"
)

// ListGameSkills 分页查询维护窗口中的技能主体资料。
func (service *KratosService) ListGameSkills(ctx context.Context, request *domainv1.ListGameSkillsRequest) (*domainv1.ListGameSkillsResponse, error) {
	if _, err := gameDataPrincipal(ctx); err != nil {
		return nil, err
	}
	elementID, err := optionalGameDataIdentifier(request.GetElementId(), "INVALID_ELEMENT_ID")
	if err != nil {
		return nil, err
	}
	damageClassID, err := optionalGameDataIdentifier(request.GetDamageClassId(), "INVALID_SKILL_DAMAGE_CLASS_ID")
	if err != nil {
		return nil, err
	}
	page, pageSize := gameDataPage(request.GetPage(), request.GetPageSize())
	sortValue := request.GetSort()
	if sortValue == "" {
		sortValue = string(skill.SortCodeAscending)
	}
	result, err := service.services.Skills.List(ctx, skill.ListQuery{
		Page: page, PageSize: pageSize, Q: request.GetQ(), Code: request.GetCode(), Name: request.GetName(),
		ElementID: elementID, DamageClassID: damageClassID, Accuracy: optionalInt32(request.GetAccuracy()), Power: optionalInt32(request.GetPower()),
		PP: optionalInt32(request.GetPp()), Priority: optionalInt32(request.GetPriority()), EffectChance: optionalInt32(request.GetEffectChance()),
		Enabled: request.Enabled, Sort: skill.Sort(sortValue),
	})
	if err != nil {
		return nil, service.skillError(ctx, "GAME_SKILL_LIST_FAILED", err)
	}
	items := make([]*domainv1.GameSkill, len(result.Items))
	for index := range result.Items {
		items[index] = gameSkillMessage(result.Items[index])
	}
	return &domainv1.ListGameSkillsResponse{HttpStatusCode: 200, Body: &domainv1.GameSkillPage{Items: items, Total: strconv.FormatInt(result.Total, 10), Page: result.Page, PageSize: result.PageSize}}, nil
}

// CreateGameSkill 在维护窗口中创建独立的技能主体资料。
func (service *KratosService) CreateGameSkill(ctx context.Context, request *domainv1.CreateGameSkillRequest) (*domainv1.CreateGameSkillResponse, error) {
	principal, err := gameDataPrincipal(ctx)
	if err != nil {
		return nil, err
	}
	if request.GetBody() == nil {
		return nil, kratoserrors.BadRequest("INVALID_REQUEST", "请求格式无效")
	}
	elementID, err := optionalGameDataIdentifier(request.GetBody().GetElementId(), "INVALID_ELEMENT_ID")
	if err != nil {
		return nil, err
	}
	damageClassID, err := optionalGameDataIdentifier(request.GetBody().GetDamageClassId(), "INVALID_SKILL_DAMAGE_CLASS_ID")
	if err != nil {
		return nil, err
	}
	rules, err := skillRulesFromMessage(request.GetBody().GetRules())
	if err != nil {
		return nil, err
	}
	writeContext, err := gameDataWriteContext(ctx, principal.AccountID, request.GetHeaderIdempotencyKey())
	if err != nil {
		return nil, err
	}
	created, err := service.services.Skills.Create(ctx, skill.CreateCommand{
		GameDataWriteContext: writeContext,
		OptionalValues:       skill.OptionalValues{ElementID: elementID, DamageClassID: damageClassID, Accuracy: optionalInt32(request.GetBody().GetAccuracy()), Power: optionalInt32(request.GetBody().GetPower()), PP: optionalInt32(request.GetBody().GetPp()), EffectChance: optionalInt32(request.GetBody().GetEffectChance()), Effect: nullableText(request.GetBody().GetEffect()), ShortEffect: nullableText(request.GetBody().GetShortEffect()), Description: nullableText(request.GetBody().GetDescription())},
		Code:                 request.GetBody().GetCode(), Name: request.GetBody().GetName(), Priority: request.GetBody().GetPriority(), Rules: rules, Enabled: request.GetBody().GetEnabled(),
	})
	if err != nil {
		return nil, service.skillError(ctx, "GAME_SKILL_CREATE_FAILED", err)
	}
	return &domainv1.CreateGameSkillResponse{HttpStatusCode: 201, Body: gameSkillMessage(created)}, nil
}

// GetGameSkill 查询维护窗口中指定稳定身份的技能主体资料。
func (service *KratosService) GetGameSkill(ctx context.Context, request *domainv1.GetGameSkillRequest) (*domainv1.GetGameSkillResponse, error) {
	if _, err := gameDataPrincipal(ctx); err != nil {
		return nil, err
	}
	skillID, err := gameDataIdentifier(request.GetSkillId(), "INVALID_SKILL_ID")
	if err != nil {
		return nil, err
	}
	value, err := service.services.Skills.Get(ctx, skillID)
	if err != nil {
		return nil, service.skillError(ctx, "GAME_SKILL_QUERY_FAILED", err)
	}
	return &domainv1.GetGameSkillResponse{HttpStatusCode: 200, Body: gameSkillMessage(value)}, nil
}

// UpdateGameSkill 按完整替换契约更新技能主体的全部可空字段。
func (service *KratosService) UpdateGameSkill(ctx context.Context, request *domainv1.UpdateGameSkillRequest) (*domainv1.UpdateGameSkillResponse, error) {
	principal, err := gameDataPrincipal(ctx)
	if err != nil {
		return nil, err
	}
	if request.GetBody() == nil {
		return nil, kratoserrors.BadRequest("INVALID_REQUEST", "请求格式无效")
	}
	skillID, err := gameDataIdentifier(request.GetSkillId(), "INVALID_SKILL_ID")
	if err != nil {
		return nil, err
	}
	elementID, err := optionalGameDataIdentifier(request.GetBody().GetElementId(), "INVALID_ELEMENT_ID")
	if err != nil {
		return nil, err
	}
	damageClassID, err := optionalGameDataIdentifier(request.GetBody().GetDamageClassId(), "INVALID_SKILL_DAMAGE_CLASS_ID")
	if err != nil {
		return nil, err
	}
	rules, err := skillRulesFromMessage(request.GetBody().GetRules())
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
	updated, err := service.services.Skills.Update(ctx, skill.UpdateCommand{
		GameDataWriteContext: writeContext, SkillID: skillID, ExpectedVersion: version, Code: request.GetBody().GetCode(), Name: request.GetBody().GetName(), Priority: request.GetBody().GetPriority(), Rules: rules, Enabled: request.GetBody().GetEnabled(),
		Changes: skill.OptionalChanges{
			ElementID: skill.Change[snowflake.ID]{Specified: true, Value: elementID}, DamageClassID: skill.Change[snowflake.ID]{Specified: true, Value: damageClassID},
			Accuracy: skill.Change[int32]{Specified: true, Value: optionalInt32(request.GetBody().GetAccuracy())}, Power: skill.Change[int32]{Specified: true, Value: optionalInt32(request.GetBody().GetPower())},
			PP: skill.Change[int32]{Specified: true, Value: optionalInt32(request.GetBody().GetPp())}, EffectChance: skill.Change[int32]{Specified: true, Value: optionalInt32(request.GetBody().GetEffectChance())},
			Effect: skill.Change[string]{Specified: true, Value: nullableText(request.GetBody().GetEffect())}, ShortEffect: skill.Change[string]{Specified: true, Value: nullableText(request.GetBody().GetShortEffect())}, Description: skill.Change[string]{Specified: true, Value: nullableText(request.GetBody().GetDescription())},
		},
	})
	if err != nil {
		return nil, service.skillError(ctx, "GAME_SKILL_UPDATE_FAILED", err)
	}
	return &domainv1.UpdateGameSkillResponse{HttpStatusCode: 200, Body: gameSkillMessage(updated)}, nil
}

// DeleteGameSkill 使用独立乐观版本禁用未被引用的技能主体资料。
func gameSkillMessage(value skill.Skill) *domainv1.GameSkill {
	message := &domainv1.GameSkill{Id: value.ID.String(), Code: value.Code, Name: value.Name, Priority: value.Priority, Enabled: value.Enabled, Version: strconv.FormatInt(value.Version, 10), Rules: skillRulesMessage(value.Rules)}
	if value.ElementID != nil {
		message.ElementId = value.ElementID.String()
	}
	if value.DamageClassID != nil {
		message.DamageClassId = value.DamageClassID.String()
	}
	if value.Accuracy != nil {
		message.Accuracy = *value.Accuracy
	}
	if value.Power != nil {
		message.Power = *value.Power
	}
	if value.PP != nil {
		message.Pp = *value.PP
	}
	if value.EffectChance != nil {
		message.EffectChance = *value.EffectChance
	}
	if value.Effect != nil {
		message.Effect = *value.Effect
	}
	if value.ShortEffect != nil {
		message.ShortEffect = *value.ShortEffect
	}
	if value.Description != nil {
		message.Description = *value.Description
	}
	return message
}

func (service *KratosService) skillError(ctx context.Context, reason string, err error) error {
	switch {
	case errors.Is(err, skill.ErrInvalidSkill):
		return kratoserrors.BadRequest("INVALID_GAME_SKILL", "技能主体字段无效")
	case errors.Is(err, skill.ErrSkillNotFound), errors.Is(err, skill.ErrSkillDependencyNotFound), errors.Is(err, skill.ErrSkillNotFound):
		return kratoserrors.NotFound("GAME_SKILL_NOT_FOUND", "技能主体、依赖或维护窗口不存在")
	case errors.Is(err, skill.ErrSkillVersionConflict), errors.Is(err, skill.ErrSkillCodeConflict), errors.Is(err, skill.ErrSkillReferenced), errors.Is(err, skill.ErrSkillVersionConflict), errors.Is(err, idempotency.ErrConflict):
		return kratoserrors.Conflict("GAME_SKILL_CONFLICT", "技能主体状态、版本或幂等请求冲突")
	default:
		service.logger.ErrorContext(ctx, "技能主体 Kratos 服务调用失败", "reason", reason, "error", err)
		return kratoserrors.InternalServer(reason, "服务端无法完成请求")
	}
}
