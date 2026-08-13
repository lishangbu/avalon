package api_test

import (
	"context"
	"io"
	"log/slog"
	"testing"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	domainv1 "github.com/lishangbu/avalon/api/gen/go/avalon/domain/v1"
	"github.com/lishangbu/avalon/internal/gamedata/ability"
	"github.com/lishangbu/avalon/internal/gamedata/abilitydetail"
	gameapi "github.com/lishangbu/avalon/internal/gamedata/api"
	"github.com/lishangbu/avalon/internal/gamedata/battlerules"
	"github.com/lishangbu/avalon/internal/gamedata/element"
	"github.com/lishangbu/avalon/internal/gamedata/itemcategory"
	"github.com/lishangbu/avalon/internal/gamedata/skill"
	"github.com/lishangbu/avalon/internal/gamedata/skilldetail"
	"github.com/lishangbu/avalon/internal/gamedata/stat"
	"github.com/lishangbu/avalon/internal/security/authentication"
)

// TestKratosServiceDelegatesAssetList 验证组合服务只委派 Asset RPC，
// 不把对象生命周期并入通用游戏资料模型。
func TestKratosServiceDelegatesAssetList(t *testing.T) {
	t.Parallel()

	assets := &assetServiceStub{}
	service := gameapi.NewKratosService(gameapi.NativeServices{Assets: assets},
		slog.New(slog.NewTextHandler(io.Discard, nil)))
	request := &domainv1.ListAssetsRequest{Page: 2, PageSize: 10, Status: "ready"}

	response, err := service.ListAssets(context.Background(), request)
	if err != nil || response.GetBody().GetPage() != 2 || assets.listRequest != request {
		t.Fatalf("ListAssets() = %#v, error = %v", response, err)
	}
}

type assetServiceStub struct {
	listRequest *domainv1.ListAssetsRequest
}

func (stub *assetServiceStub) ListAssets(_ context.Context, request *domainv1.ListAssetsRequest) (*domainv1.ListAssetsResponse, error) {
	stub.listRequest = request
	return &domainv1.ListAssetsResponse{Body: &domainv1.AssetPage{Page: request.GetPage(), PageSize: request.GetPageSize()}}, nil
}
func (*assetServiceStub) CreateAssetUpload(context.Context, *domainv1.CreateAssetUploadRequest) (*domainv1.CreateAssetUploadResponse, error) {
	return nil, nil
}
func (*assetServiceStub) ConfirmAssetUpload(context.Context, *domainv1.ConfirmAssetUploadRequest) (*domainv1.ConfirmAssetUploadResponse, error) {
	return nil, nil
}
func (*assetServiceStub) CreateAssetDownload(context.Context, *domainv1.CreateAssetDownloadRequest) (*domainv1.CreateAssetDownloadResponse, error) {
	return nil, nil
}

// TestKratosServiceCreatesElement 验证空库修订零可以创建首条资料，并直接从 Proto 请求进入应用服务。
func TestKratosServiceCreatesElement(t *testing.T) {
	t.Parallel()

	accountID := snowflake.MustParse("1048576116")
	elementID := snowflake.MustParse("1048576117")
	elements := &nativeElementStub{created: element.Element{
		ID: elementID, Code: "fire", Name: "火", SortOrder: 10, Enabled: true, Version: 1,
	}}
	service := gameapi.NewKratosService(gameapi.NativeServices{Elements: elements},
		slog.New(slog.NewTextHandler(io.Discard, nil)))
	ctx := authentication.WithPrincipal(context.Background(), authentication.Principal{AccountID: accountID})

	response, err := service.CreateGameElement(ctx, &domainv1.CreateGameElementRequest{
		HeaderIdempotencyKey: "create-element",
		Body: &domainv1.CreateGameElementBody{
			Code: "fire", Name: "火", SortOrder: 10, Enabled: true},
	})
	if err != nil {
		t.Fatalf("CreateGameElement() error = %v", err)
	}
	if response.GetHttpStatusCode() != 201 || response.GetBody().GetId() != elementID.String() ||
		response.GetBody().GetCode() != "fire" {
		t.Fatalf("CreateGameElement() = %#v", response)
	}
	if elements.create.ActorAccountID != accountID || elements.create.IdempotencyKey != "create-element" {
		t.Fatalf("Create command = %+v", elements.create)
	}
}

// TestKratosServiceCreatesAbility 验证特性基础资料、文案和事件规则通过同一个主资源契约往返。
func TestKratosServiceCreatesAbility(t *testing.T) {
	t.Parallel()

	accountID := snowflake.MustParse("1048576119")
	abilityID := snowflake.MustParse("1048576120")
	effect, shortEffect, introduction := "生命较低时强化草属性技能。", "低生命强化。", "代表茂盛生命力的特性。"
	rules, valid := battlerules.NewAbility(abilitydetail.OptionalValues{SwitchInRevealOpponentHeldItems: true})
	if !valid {
		t.Fatal("测试特性规则无效")
	}
	abilities := &abilityServiceStub{created: ability.Ability{
		ID: abilityID, Code: "overgrow", Name: "茂盛", MainSeries: true,
		Effect: &effect, ShortEffect: &shortEffect, Introduction: &introduction,
		Rules: rules, Enabled: true, Version: 1,
	}}
	service := gameapi.NewKratosService(gameapi.NativeServices{Abilities: abilities},
		slog.New(slog.NewTextHandler(io.Discard, nil)))
	ctx := authentication.WithPrincipal(context.Background(), authentication.Principal{AccountID: accountID})

	response, err := service.CreateGameAbility(ctx, &domainv1.CreateGameAbilityRequest{
		HeaderIdempotencyKey: "create-ability",
		Body: &domainv1.CreateGameAbilityBody{
			Code: "overgrow", Name: "茂盛", MainSeries: true, Enabled: true,
			Effect: effect, ShortEffect: shortEffect, Introduction: introduction,
			Rules: &domainv1.GameAbilityRules{OnSwitchIn: &domainv1.GameAbilityRuleGroup{
				SwitchInRevealOpponentHeldItems: true,
			}},
		},
	})
	if err != nil {
		t.Fatalf("CreateGameAbility() error = %v", err)
	}
	if response.GetHttpStatusCode() != 201 || response.GetBody().GetId() != abilityID.String() ||
		response.GetBody().GetEffect() != effect || response.GetBody().GetShortEffect() != shortEffect ||
		response.GetBody().GetIntroduction() != introduction ||
		!response.GetBody().GetRules().GetOnSwitchIn().GetSwitchInRevealOpponentHeldItems() {
		t.Fatalf("CreateGameAbility() = %#v", response)
	}
	createdRules, rulesValid := abilities.createCommand.Rules.Values()
	if abilities.createCommand.ActorAccountID != accountID || !rulesValid ||
		!createdRules.SwitchInRevealOpponentHeldItems || abilities.createCommand.Effect == nil ||
		*abilities.createCommand.Effect != effect {
		t.Fatalf("Create command = %+v", abilities.createCommand)
	}
}

// TestKratosServiceCreatesSkill 验证技能基础数值、文案和 onUse 规则通过同一个主资源契约往返。
func TestKratosServiceCreatesSkill(t *testing.T) {
	t.Parallel()

	accountID := snowflake.MustParse("1048576129")
	skillID := snowflake.MustParse("1048576130")
	effect, shortEffect, description := "命中时与目标发生接触。", "接触攻击。", "用身体撞击目标。"
	rules, valid := battlerules.NewSkill(skilldetail.OptionalValues{MakesContact: true})
	if !valid {
		t.Fatal("测试技能规则无效")
	}
	skills := &skillServiceStub{created: skill.Skill{
		ID: skillID, OptionalValues: skill.OptionalValues{Effect: &effect, ShortEffect: &shortEffect, Description: &description},
		Code: "tackle", Name: "撞击", Priority: 0, Rules: rules, Enabled: true, Version: 1,
	}}
	service := gameapi.NewKratosService(gameapi.NativeServices{Skills: skills},
		slog.New(slog.NewTextHandler(io.Discard, nil)))
	ctx := authentication.WithPrincipal(context.Background(), authentication.Principal{AccountID: accountID})

	response, err := service.CreateGameSkill(ctx, &domainv1.CreateGameSkillRequest{
		HeaderIdempotencyKey: "create-skill",
		Body: &domainv1.CreateGameSkillBody{
			Code: "tackle", Name: "撞击", Priority: 0, Enabled: true,
			Effect: effect, ShortEffect: shortEffect, Description: description,
			Rules: &domainv1.GameSkillRules{OnUse: &domainv1.GameSkillOnUseRules{MakesContact: true}},
		},
	})
	if err != nil {
		t.Fatalf("CreateGameSkill() error = %v", err)
	}
	if response.GetHttpStatusCode() != 201 || response.GetBody().GetId() != skillID.String() ||
		response.GetBody().GetEffect() != effect || response.GetBody().GetShortEffect() != shortEffect ||
		response.GetBody().GetDescription() != description || !response.GetBody().GetRules().GetOnUse().GetMakesContact() {
		t.Fatalf("CreateGameSkill() = %#v", response)
	}
	createdRules, rulesValid := skills.createCommand.Rules.Values()
	if skills.createCommand.ActorAccountID != accountID || !rulesValid || !createdRules.MakesContact ||
		skills.createCommand.Effect == nil || *skills.createCommand.Effect != effect {
		t.Fatalf("Create command = %+v", skills.createCommand)
	}
}

// TestKratosServiceCreatesItemCategory 验证道具分类使用自己的强类型命令和响应。
func TestKratosServiceCreatesItemCategory(t *testing.T) {
	t.Parallel()

	accountID := snowflake.MustParse("1048576121")
	categoryID := snowflake.MustParse("1048576122")
	pocketID := snowflake.MustParse("1048576123")
	categories := &itemCategoryServiceStub{created: itemcategory.Category{
		ID: categoryID, Code: "medicine", Name: "药品", PocketID: pocketID, SortOrder: 20, Enabled: true, Version: 1,
	}}
	service := gameapi.NewKratosService(gameapi.NativeServices{ItemCategories: categories},
		slog.New(slog.NewTextHandler(io.Discard, nil)))
	ctx := authentication.WithPrincipal(context.Background(), authentication.Principal{AccountID: accountID})

	response, err := service.CreateGameItemCategory(ctx, &domainv1.CreateGameItemCategoryRequest{
		HeaderIdempotencyKey: "create-item-category",
		Body: &domainv1.CreateGameItemCategoryBody{
			Code: "medicine", Name: "药品", PocketId: pocketID.String(), SortOrder: 20, Enabled: true},
	})
	if err != nil {
		t.Fatalf("CreateGameItemCategory() error = %v", err)
	}
	if response.GetHttpStatusCode() != 201 || response.GetBody().GetId() != categoryID.String() {
		t.Fatalf("CreateGameItemCategory() = %#v", response)
	}
	if categories.createCommand.ActorAccountID != accountID {
		t.Fatalf("Create command = %+v", categories.createCommand)
	}
}

// TestKratosServiceCreatesStat 验证数值项的战斗专用标记不会被其他资料结构吞并。
func TestKratosServiceCreatesStat(t *testing.T) {
	t.Parallel()

	accountID := snowflake.MustParse("1048576123")
	statID := snowflake.MustParse("1048576124")
	stats := &statServiceStub{created: stat.Stat{
		ID: statID, Code: "attack", Name: "攻击", SortOrder: 10, BattleOnly: false, Enabled: true, Version: 1,
	}}
	service := gameapi.NewKratosService(gameapi.NativeServices{Stats: stats},
		slog.New(slog.NewTextHandler(io.Discard, nil)))
	ctx := authentication.WithPrincipal(context.Background(), authentication.Principal{AccountID: accountID})

	response, err := service.CreateGameStat(ctx, &domainv1.CreateGameStatRequest{
		HeaderIdempotencyKey: "create-stat",
		Body: &domainv1.CreateGameStatBody{
			Code: "attack", Name: "攻击", SortOrder: 10, BattleOnly: false, Enabled: true},
	})
	if err != nil {
		t.Fatalf("CreateGameStat() error = %v", err)
	}
	if response.GetHttpStatusCode() != 201 || response.GetBody().GetId() != statID.String() {
		t.Fatalf("CreateGameStat() = %#v", response)
	}
	if stats.createCommand.ActorAccountID != accountID || stats.createCommand.Code != "attack" {
		t.Fatalf("Create command = %+v", stats.createCommand)
	}
}

// nativeElementStub 只实现该公开契约切片需要观察的属性资料应用边界。
type nativeElementStub struct {
	create  element.CreateCommand
	created element.Element
}

func (stub *nativeElementStub) Create(_ context.Context, command element.CreateCommand) (element.Element, error) {
	stub.create = command
	return stub.created, nil
}

func (*nativeElementStub) Get(context.Context, snowflake.ID) (element.Element, error) {
	return element.Element{}, nil
}

func (*nativeElementStub) List(context.Context, element.ListQuery) (element.Page, error) {
	return element.Page{}, nil
}

func (*nativeElementStub) Update(context.Context, element.UpdateCommand) (element.Element, error) {
	return element.Element{}, nil
}

func (*nativeElementStub) Disable(context.Context, element.DisableCommand) error { return nil }

type abilityServiceStub struct {
	created       ability.Ability
	createCommand ability.CreateCommand
}

type skillServiceStub struct {
	created       skill.Skill
	createCommand skill.CreateCommand
}

func (stub *skillServiceStub) Create(_ context.Context, command skill.CreateCommand) (skill.Skill, error) {
	stub.createCommand = command
	return stub.created, nil
}
func (*skillServiceStub) Get(context.Context, snowflake.ID) (skill.Skill, error) {
	return skill.Skill{}, nil
}
func (*skillServiceStub) List(context.Context, skill.ListQuery) (skill.Page, error) {
	return skill.Page{}, nil
}
func (*skillServiceStub) Update(context.Context, skill.UpdateCommand) (skill.Skill, error) {
	return skill.Skill{}, nil
}

func (stub *abilityServiceStub) Create(_ context.Context, command ability.CreateCommand) (ability.Ability, error) {
	stub.createCommand = command
	return stub.created, nil
}
func (*abilityServiceStub) Get(context.Context, snowflake.ID) (ability.Ability, error) {
	return ability.Ability{}, nil
}
func (*abilityServiceStub) List(context.Context, ability.ListQuery) (ability.Page, error) {
	return ability.Page{}, nil
}
func (*abilityServiceStub) Update(context.Context, ability.UpdateCommand) (ability.Ability, error) {
	return ability.Ability{}, nil
}

type itemCategoryServiceStub struct {
	created       itemcategory.Category
	createCommand itemcategory.CreateCommand
}

func (stub *itemCategoryServiceStub) Create(_ context.Context, command itemcategory.CreateCommand) (itemcategory.Category, error) {
	stub.createCommand = command
	return stub.created, nil
}
func (*itemCategoryServiceStub) Get(context.Context, snowflake.ID) (itemcategory.Category, error) {
	return itemcategory.Category{}, nil
}
func (*itemCategoryServiceStub) List(context.Context, itemcategory.ListQuery) (itemcategory.Page, error) {
	return itemcategory.Page{}, nil
}
func (*itemCategoryServiceStub) Update(context.Context, itemcategory.UpdateCommand) (itemcategory.Category, error) {
	return itemcategory.Category{}, nil
}

type statServiceStub struct {
	created       stat.Stat
	createCommand stat.CreateCommand
}

func (stub *statServiceStub) Create(_ context.Context, command stat.CreateCommand) (stat.Stat, error) {
	stub.createCommand = command
	return stub.created, nil
}
func (*statServiceStub) Get(context.Context, snowflake.ID) (stat.Stat, error) {
	return stat.Stat{}, nil
}
func (*statServiceStub) List(context.Context, stat.ListQuery) (stat.Page, error) {
	return stat.Page{}, nil
}
func (*statServiceStub) Update(context.Context, stat.UpdateCommand) (stat.Stat, error) {
	return stat.Stat{}, nil
}
