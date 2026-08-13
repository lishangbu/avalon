package api

import (
	"context"
	"fmt"
	"reflect"
	"strconv"
	"strings"

	kratoserrors "github.com/go-kratos/kratos/v3/errors"
	domainv1 "github.com/lishangbu/avalon/api/gen/go/avalon/domain/v1"
	"github.com/lishangbu/avalon/internal/battleengine"
	"github.com/lishangbu/avalon/internal/gamedata/item"
	"github.com/lishangbu/avalon/internal/gamedata/itemrules"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// GetGameItemRules 读取一个道具的完整规范化规则聚合。
func (service *KratosService) GetGameItemRules(ctx context.Context, request *domainv1.GetGameItemRulesRequest) (*domainv1.GetGameItemRulesResponse, error) {
	if _, err := gameDataPrincipal(ctx); err != nil {
		return nil, err
	}
	itemID, err := gameDataIdentifier(request.GetItemId(), "INVALID_ITEM_ID")
	if err != nil {
		return nil, err
	}
	value, err := service.services.Items.GetRules(ctx, itemID)
	if err != nil {
		return nil, service.itemError(ctx, "GAME_ITEM_RULES_QUERY_FAILED", err)
	}
	body, err := gameItemRulesMessage(value)
	if err != nil {
		return nil, service.itemError(ctx, "GAME_ITEM_RULES_QUERY_FAILED", err)
	}
	return &domainv1.GetGameItemRulesResponse{HttpStatusCode: 200, Body: body}, nil
}

// ReplaceGameItemRules 使用道具主体版本整体替换全部规范化规则关系。
func (service *KratosService) ReplaceGameItemRules(ctx context.Context, request *domainv1.ReplaceGameItemRulesRequest) (*domainv1.ReplaceGameItemRulesResponse, error) {
	principal, err := gameDataPrincipal(ctx)
	if err != nil {
		return nil, err
	}
	if request.GetBody() == nil || request.GetBody().GetRules() == nil {
		return nil, kratoserrors.BadRequest("INVALID_REQUEST", "请求格式无效")
	}
	itemID, err := gameDataIdentifier(request.GetItemId(), "INVALID_ITEM_ID")
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
	rules, err := gameItemRulesValue(request.GetBody().GetRules(), itemID)
	if err != nil {
		return nil, kratoserrors.BadRequest("INVALID_GAME_ITEM_RULES", "道具规则字段无效")
	}
	value, err := service.services.Items.ReplaceRules(ctx, item.ReplaceRulesCommand{GameDataWriteContext: writeContext, ItemID: itemID, ExpectedVersion: version, Rules: rules})
	if err != nil {
		return nil, service.itemError(ctx, "GAME_ITEM_RULES_REPLACE_FAILED", err)
	}
	body, err := gameItemRulesMessage(value)
	if err != nil {
		return nil, service.itemError(ctx, "GAME_ITEM_RULES_REPLACE_FAILED", err)
	}
	return &domainv1.ReplaceGameItemRulesResponse{HttpStatusCode: 200, Body: body}, nil
}

func gameItemRulesMessage(value item.Rules) (*domainv1.GameItemRules, error) {
	message := &domainv1.GameItemRules{ItemId: value.ItemID.String(), Version: strconv.FormatInt(value.Version, 10)}
	if err := copyRuleScalars(reflect.ValueOf(value.Rules), reflect.ValueOf(message).Elem()); err != nil {
		return nil, err
	}
	setRuleMessageIdentifiers(message, value.Rules)
	return message, nil
}

func gameItemRulesValue(message *domainv1.GameItemRules, itemID snowflake.ID) (itemrules.Detail, error) {
	value := itemrules.Detail{ItemID: itemID}
	if err := copyRuleScalars(reflect.ValueOf(message).Elem(), reflect.ValueOf(&value).Elem()); err != nil {
		return itemrules.Detail{}, err
	}
	var err error
	value.HighestStatBoosterAbilityIDs, err = identifierList(message.GetHighestStatBoosterAbilityIds())
	if err != nil {
		return itemrules.Detail{}, err
	}
	ids := []struct {
		text   string
		target **snowflake.ID
	}{
		{message.GetElementDamageBoostElementId(), &value.ElementDamageBoostElementID}, {message.GetConsumableElementDamageBoostElementId(), &value.ConsumableElementDamageBoostElementID}, {message.GetElementDamageReductionElementId(), &value.ElementDamageReductionElementID}, {message.GetEndTurnHealForElementId(), &value.EndTurnHealForElementID}, {message.GetEndTurnDamageWithoutElementId(), &value.EndTurnDamageWithoutElementID}, {message.GetWaterDamageSpecialAttackBoostElementId(), &value.WaterDamageSpecialAttackBoostElementID}, {message.GetElectricDamageAttackBoostElementId(), &value.ElectricDamageAttackBoostElementID}, {message.GetWaterDamageSpecialDefenseBoostElementId(), &value.WaterDamageSpecialDefenseBoostElementID}, {message.GetIceDamageAttackBoostElementId(), &value.IceDamageAttackBoostElementID},
	}
	for _, entry := range ids {
		parsed, parseErr := optionalGameDataIdentifier(entry.text, "INVALID_RULE_IDENTIFIER")
		if parseErr != nil {
			return itemrules.Detail{}, parseErr
		}
		*entry.target = parsed
	}
	value.AccuracyMissStatStageBoostStat = battleengine.Stat(message.GetAccuracyMissStatStageBoostStat())
	return value, nil
}

// copyRuleScalars 严格复制契约与领域中同名的 bool/int32 字段；任一类型漂移都会终止请求。
func copyRuleScalars(source, target reflect.Value) error {
	for i := 0; i < target.NumField(); i++ {
		field := target.Type().Field(i)
		if field.Name == "state" || field.Name == "unknownFields" || field.Name == "sizeCache" {
			continue
		}
		if field.Type.Kind() != reflect.Bool && field.Type.Kind() != reflect.Int32 {
			continue
		}
		from := ruleFieldByCanonicalName(source, field.Name)
		if !from.IsValid() || from.Type() != field.Type {
			return fmt.Errorf("道具规则字段映射缺失或类型不一致: %s", field.Name)
		}
		target.Field(i).Set(from)
	}
	return nil
}

func ruleFieldByCanonicalName(value reflect.Value, name string) reflect.Value {
	want := strings.ToLower(strings.ReplaceAll(name, "_", ""))
	for index := 0; index < value.NumField(); index++ {
		candidate := value.Type().Field(index).Name
		if strings.ToLower(strings.ReplaceAll(candidate, "_", "")) == want {
			return value.Field(index)
		}
	}
	return reflect.Value{}
}

func setRuleMessageIdentifiers(message *domainv1.GameItemRules, value itemrules.Detail) {
	for _, id := range value.HighestStatBoosterAbilityIDs {
		message.HighestStatBoosterAbilityIds = append(message.HighestStatBoosterAbilityIds, id.String())
	}
	message.ElementDamageBoostElementId = idText(value.ElementDamageBoostElementID)
	message.ConsumableElementDamageBoostElementId = idText(value.ConsumableElementDamageBoostElementID)
	message.ElementDamageReductionElementId = idText(value.ElementDamageReductionElementID)
	message.EndTurnHealForElementId = idText(value.EndTurnHealForElementID)
	message.EndTurnDamageWithoutElementId = idText(value.EndTurnDamageWithoutElementID)
	message.WaterDamageSpecialAttackBoostElementId = idText(value.WaterDamageSpecialAttackBoostElementID)
	message.ElectricDamageAttackBoostElementId = idText(value.ElectricDamageAttackBoostElementID)
	message.WaterDamageSpecialDefenseBoostElementId = idText(value.WaterDamageSpecialDefenseBoostElementID)
	message.IceDamageAttackBoostElementId = idText(value.IceDamageAttackBoostElementID)
	message.AccuracyMissStatStageBoostStat = string(value.AccuracyMissStatStageBoostStat)
}

func idText(value *snowflake.ID) string {
	if value == nil {
		return ""
	}
	return value.String()
}
func identifierList(values []string) ([]snowflake.ID, error) {
	result := make([]snowflake.ID, 0, len(values))
	for _, text := range values {
		value, err := snowflake.Parse(text)
		if err != nil {
			return nil, err
		}
		result = append(result, value)
	}
	return result, nil
}
