package api

import (
	"context"
	"errors"
	"log/slog"
	"strconv"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	kratoserrors "github.com/go-kratos/kratos/v3/errors"
	domainv1 "github.com/lishangbu/avalon/api/gen/go/avalon/domain/v1"
	"github.com/lishangbu/avalon/internal/gamedata/administration"
	"github.com/lishangbu/avalon/internal/gamedata/element"
	"github.com/lishangbu/avalon/internal/platform/httpapi"
	"github.com/lishangbu/avalon/internal/platform/idempotency"
	"github.com/lishangbu/avalon/internal/security/authentication"
)

// NativeServices 汇集游戏资料 Kratos 传输层所需的显式应用用例。
//
// 字段按独立资料类型保留，不使用通用 CRUD 接口合并不同领域语义；后续资料服务迁移会逐项加入，
// 使入口代码能在编译期检查每项依赖是否已经完整装配。
type NativeServices struct {
	// Assets 管理对象上传确认与 Ready 对象稳定公开读取位置。
	Assets AssetService
	// BattleRules 管理 BattleFormat 及其 Clause、Restriction、Mechanic 组成项。
	BattleRules BattleRuleService
	// BotStrategies 管理不可变版本的 Training Bot 策略资料。
	BotStrategies BotStrategyService
	// Elements 管理实时属性资料。
	Elements ElementService
	// ElementEffectiveness 管理实时属性克制倍率。
	ElementEffectiveness ElementEffectivenessService
	// Natures 管理 Team 成员可选择的 Nature 资料。
	Natures NatureService
	// Abilities 管理实时特性资料。
	Abilities AbilityService
	// CreatureMetadata 读取 Team、维护校验和 Battle 所需的完整关系投影。
	CreatureMetadata CreatureMetadataService
	// CreatureAdministration 管理 Species 与 Creature 的记录级资料。
	CreatureAdministration CreatureAdministrationService
	// ItemCategories 管理实时道具分类。
	ItemCategories ItemCategoryService
	// ItemDictionaries 管理道具 Pocket、Attribute 与 Fling Effect。
	ItemDictionaries ItemDictionaryService
	// Items 管理实时道具资料。
	Items ItemService
	// Stats 管理实时数值项资料。
	Stats StatService
	// DamageClasses 管理实时技能伤害分类。
	DamageClasses SkillDamageClassService
	// Skills 管理实时技能主体资料。
	Skills SkillService
	// SkillAilments 管理实时技能异常资料。
	SkillAilments SkillAilmentService
	// SkillCategories 管理实时技能分类。
	SkillCategories SkillCategoryService
	// SkillTargets 管理实时技能目标资料。
	SkillTargets SkillTargetService
	// SkillLearnMethods 管理实时技能学习方式。
	SkillLearnMethods SkillLearnMethodService
	// SkillStatChanges 管理实时技能数值变化。
	SkillStatChanges SkillStatChangeService
}

// AssetService 是管理资料服务组合对象存储 RPC 所需的最小原生接口。
//
// 资产领域保持独立实现；此接口只负责把同一份 GameDataService Proto 契约组合为单个注册点，
// 不会把资产生命周期并入游戏资料 CRUD 模型。
type AssetService interface {
	ListAssets(context.Context, *domainv1.ListAssetsRequest) (*domainv1.ListAssetsResponse, error)
	CreateAssetUpload(context.Context, *domainv1.CreateAssetUploadRequest) (*domainv1.CreateAssetUploadResponse, error)
	ConfirmAssetUpload(context.Context, *domainv1.ConfirmAssetUploadRequest) (*domainv1.ConfirmAssetUploadResponse, error)
	CreateAssetDownload(context.Context, *domainv1.CreateAssetDownloadRequest) (*domainv1.CreateAssetDownloadResponse, error)
}

// KratosService 直接实现生成的 GameDataService 中除 Asset 外的游戏资料 RPC。
type KratosService struct {
	// services 按资料领域保存全部显式应用用例，不提供通用 CRUD 分派。
	services NativeServices
	// logger 记录传输边界无法映射的内部错误。
	logger *slog.Logger
}

// NewKratosService 使用显式游戏资料应用用例创建原生 Kratos 服务。
func NewKratosService(services NativeServices, logger *slog.Logger) *KratosService {
	if logger == nil {
		logger = slog.Default()
	}
	return &KratosService{services: services, logger: logger}
}

// ListAssets 委派独立资产领域分页查询当前管理员账号拥有的对象。
func (service *KratosService) ListAssets(ctx context.Context, request *domainv1.ListAssetsRequest) (*domainv1.ListAssetsResponse, error) {
	return service.services.Assets.ListAssets(ctx, request)
}

// CreateAssetUpload 创建对象直传授权，并由独立资产领域校验媒体类型、摘要与大小。
func (service *KratosService) CreateAssetUpload(ctx context.Context, request *domainv1.CreateAssetUploadRequest) (*domainv1.CreateAssetUploadResponse, error) {
	return service.services.Assets.CreateAssetUpload(ctx, request)
}

// ConfirmAssetUpload 确认对象已经按预期写入私有桶并推进资产版本。
func (service *KratosService) ConfirmAssetUpload(ctx context.Context, request *domainv1.ConfirmAssetUploadRequest) (*domainv1.ConfirmAssetUploadResponse, error) {
	return service.services.Assets.ConfirmAssetUpload(ctx, request)
}

// CreateAssetDownload 为已就绪资产返回稳定公开读取位置。
func (service *KratosService) CreateAssetDownload(ctx context.Context, request *domainv1.CreateAssetDownloadRequest) (*domainv1.CreateAssetDownloadResponse, error) {
	return service.services.Assets.CreateAssetDownload(ctx, request)
}

// ListGameElements 分页查询维护窗口中的属性资料。
func (service *KratosService) ListGameElements(
	ctx context.Context,
	request *domainv1.ListGameElementsRequest,
) (*domainv1.ListGameElementsResponse, error) {
	if _, err := gameDataPrincipal(ctx); err != nil {
		return nil, err
	}
	page := request.GetPage()
	if page == 0 {
		page = 1
	}
	pageSize := request.GetPageSize()
	if pageSize == 0 {
		pageSize = 20
	}
	sortValue := request.GetSort()
	if sortValue == "" {
		sortValue = string(element.SortCodeAscending)
	}
	result, err := service.services.Elements.List(ctx, element.ListQuery{
		Page: page, PageSize: pageSize, Q: request.GetQ(), Code: request.GetCode(), Name: request.GetName(),
		SortOrder: optionalInt32(request.GetSortOrder()), Enabled: request.Enabled, Sort: element.Sort(sortValue),
	})
	if err != nil {
		return nil, service.elementError(ctx, "GAME_ELEMENT_LIST_FAILED", err)
	}
	items := make([]*domainv1.GameElement, len(result.Items))
	for index := range result.Items {
		items[index] = gameElementMessage(result.Items[index])
	}
	return &domainv1.ListGameElementsResponse{HttpStatusCode: 200, Body: &domainv1.GameElementPage{
		Items: items, Total: strconv.FormatInt(result.Total, 10), Page: result.Page, PageSize: result.PageSize,
	}}, nil
}

// CreateGameElement 在维护窗口中创建属性资料。
func (service *KratosService) CreateGameElement(
	ctx context.Context,
	request *domainv1.CreateGameElementRequest,
) (*domainv1.CreateGameElementResponse, error) {
	principal, err := gameDataPrincipal(ctx)
	if err != nil {
		return nil, err
	}
	if request.GetBody() == nil {
		return nil, kratoserrors.BadRequest("INVALID_REQUEST", "请求格式无效")
	}
	writeContext, err := gameDataWriteContext(ctx, principal.AccountID, request.GetHeaderIdempotencyKey())
	if err != nil {
		return nil, err
	}
	created, err := service.services.Elements.Create(ctx, element.CreateCommand{
		GameDataWriteContext: writeContext, Code: request.GetBody().GetCode(), Name: request.GetBody().GetName(),
		SortOrder: request.GetBody().GetSortOrder(), Enabled: request.GetBody().GetEnabled(),
	})
	if err != nil {
		return nil, service.elementError(ctx, "GAME_ELEMENT_CREATE_FAILED", err)
	}
	return &domainv1.CreateGameElementResponse{HttpStatusCode: 201, Body: gameElementMessage(created)}, nil
}

// GetGameElement 查询维护窗口中指定稳定身份的属性资料。
func (service *KratosService) GetGameElement(
	ctx context.Context,
	request *domainv1.GetGameElementRequest,
) (*domainv1.GetGameElementResponse, error) {
	if _, err := gameDataPrincipal(ctx); err != nil {
		return nil, err
	}
	elementID, err := gameDataIdentifier(request.GetElementId(), "INVALID_ELEMENT_ID")
	if err != nil {
		return nil, err
	}
	value, err := service.services.Elements.Get(ctx, elementID)
	if err != nil {
		return nil, service.elementError(ctx, "GAME_ELEMENT_QUERY_FAILED", err)
	}
	return &domainv1.GetGameElementResponse{HttpStatusCode: 200, Body: gameElementMessage(value)}, nil
}

// UpdateGameElement 使用乐观版本更新维护窗口中的属性资料。
func (service *KratosService) UpdateGameElement(
	ctx context.Context,
	request *domainv1.UpdateGameElementRequest,
) (*domainv1.UpdateGameElementResponse, error) {
	principal, err := gameDataPrincipal(ctx)
	if err != nil {
		return nil, err
	}
	if request.GetBody() == nil {
		return nil, kratoserrors.BadRequest("INVALID_REQUEST", "请求格式无效")
	}
	elementID, err := gameDataIdentifier(request.GetElementId(), "INVALID_ELEMENT_ID")
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
	updated, err := service.services.Elements.Update(ctx, element.UpdateCommand{
		GameDataWriteContext: writeContext, ElementID: elementID, ExpectedVersion: version,
		Code: request.GetBody().GetCode(), Name: request.GetBody().GetName(),
		SortOrder: request.GetBody().GetSortOrder(), Enabled: request.GetBody().GetEnabled(),
	})
	if err != nil {
		return nil, service.elementError(ctx, "GAME_ELEMENT_UPDATE_FAILED", err)
	}
	return &domainv1.UpdateGameElementResponse{HttpStatusCode: 200, Body: gameElementMessage(updated)}, nil
}

// DeleteGameElement 使用乐观版本禁用未被引用的属性资料。
func gameDataPrincipal(ctx context.Context) (authentication.Principal, error) {
	principal, ok := authentication.PrincipalFromContext(ctx)
	if !ok || principal.AccountID == snowflake.ID(0) {
		return authentication.Principal{}, kratoserrors.Unauthorized("SESSION_INVALID", "登录会话无效")
	}
	return principal, nil
}

func gameDataIdentifier(raw, reason string) (snowflake.ID, error) {
	value, err := snowflake.Parse(raw)
	if err != nil || value == snowflake.ID(0) {
		return snowflake.ID(0), kratoserrors.BadRequest(reason, "标识格式无效")
	}
	return value, nil
}

// optionalGameDataIdentifier 把空查询参数解释为未设置，并严格校验非空 Identifier。
func optionalGameDataIdentifier(raw, reason string) (*snowflake.ID, error) {
	if raw == "" {
		return nil, nil
	}
	value, err := gameDataIdentifier(raw, reason)
	if err != nil {
		return nil, err
	}
	return &value, nil
}

// gameDataIdentifiers 严格解析一组必填 Identifier，并保留输入顺序。
func gameDataIdentifiers(values []string, reason string) ([]snowflake.ID, error) {
	result := make([]snowflake.ID, len(values))
	for index, raw := range values {
		value, err := gameDataIdentifier(raw, reason)
		if err != nil {
			return nil, err
		}
		result[index] = value
	}
	return result, nil
}

// optionalIdentifierString 将可空 Identifier 编码为空字符串或标准文本。
func optionalIdentifierString(value *snowflake.ID) string {
	if value == nil {
		return ""
	}
	return value.String()
}

func optionalIdentifierPtr(raw string) *snowflake.ID {
	if raw == "" {
		return nil
	}
	value, err := snowflake.Parse(raw)
	if err != nil || value == snowflake.ID(0) {
		return nil
	}
	return &value
}

func optionalText(raw string) *string {
	if raw == "" {
		return nil
	}
	return &raw
}

func gameDataVersion(raw string) (int64, error) {
	value, err := strconv.ParseInt(raw, 10, 64)
	if err != nil || value < 1 {
		return 0, kratoserrors.BadRequest("INVALID_VERSION", "版本格式无效")
	}
	return value, nil
}

func gameDataWriteContext(
	ctx context.Context,
	actorAccountID snowflake.ID,
	idempotencyKey string,
) (administration.GameDataWriteContext, error) {
	return administration.NewGameDataWriteContext(actorAccountID, idempotencyKey, httpapi.RequestIDFromContext(ctx)), nil
}

func optionalInt32(value int32) *int32 {
	if value == 0 {
		return nil
	}
	return &value
}

func gameElementMessage(value element.Element) *domainv1.GameElement {
	return &domainv1.GameElement{
		Id: value.ID.String(), Code: value.Code, Name: value.Name, SortOrder: value.SortOrder,
		Enabled: value.Enabled, Version: strconv.FormatInt(value.Version, 10),
	}
}

func (service *KratosService) elementError(ctx context.Context, reason string, err error) error {
	switch {
	case errors.Is(err, element.ErrInvalidElement):
		return kratoserrors.BadRequest("INVALID_GAME_ELEMENT", "属性资料字段无效")
	case errors.Is(err, element.ErrElementNotFound), errors.Is(err, element.ErrElementNotFound):
		return kratoserrors.NotFound("GAME_ELEMENT_NOT_FOUND", "属性资料或维护窗口不存在")
	case errors.Is(err, element.ErrElementVersionConflict), errors.Is(err, element.ErrElementCodeConflict),
		errors.Is(err, element.ErrElementReferenced),
		errors.Is(err, element.ErrElementVersionConflict), errors.Is(err, idempotency.ErrConflict):
		return kratoserrors.Conflict("GAME_ELEMENT_CONFLICT", "属性资料状态、版本或幂等请求冲突")
	default:
		service.logger.ErrorContext(ctx, "游戏资料 Kratos 服务调用失败", "reason", reason, "error", err)
		return kratoserrors.InternalServer(reason, "服务端无法完成请求")
	}
}
