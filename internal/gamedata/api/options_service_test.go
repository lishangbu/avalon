package api_test

import (
	"context"
	"fmt"
	"log/slog"
	"testing"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	domainv1 "github.com/lishangbu/avalon/api/gen/go/avalon/domain/v1"
	gameapi "github.com/lishangbu/avalon/internal/gamedata/api"
	"github.com/lishangbu/avalon/internal/gamedata/battleformat"
	"github.com/lishangbu/avalon/internal/gamedata/element"
	"github.com/lishangbu/avalon/internal/gamedata/stat"
	"github.com/lishangbu/avalon/internal/security/authentication"
)

// TestKratosServiceListsAllEnabledElementOptions 验证原生属性选项接口没有外部分页，且会读取
// 超过单次领域分页上限的全部启用资料，而不是静默截断第 101 条记录。
func TestKratosServiceListsAllEnabledElementOptions(t *testing.T) {
	t.Parallel()
	pages := make([]element.Element, 100)
	for index := range pages {
		pages[index] = element.Element{ID: snowflake.NewTestID(), Code: fmt.Sprintf("element-%03d", index), Name: "属性", Enabled: true}
	}
	elements := &optionElementServiceStub{pages: map[int32]element.Page{
		1: {Items: pages, Total: 101, Page: 1, PageSize: 100},
		2: {Items: []element.Element{{ID: snowflake.NewTestID(), Code: "element-100", Name: "末尾属性", Enabled: true}}, Total: 101, Page: 2, PageSize: 100},
	}}
	service := gameapi.NewKratosService(gameapi.NativeServices{Elements: elements}, slog.Default())

	response, err := service.ListGameElementOptions(optionContext(), &domainv1.ListGameElementOptionsRequest{})
	if err != nil {
		t.Fatalf("ListGameElementOptions() error = %v", err)
	}
	if len(response.GetBody().GetItems()) != 101 || response.GetBody().GetItems()[100].GetCode() != "element-100" {
		t.Fatalf("ListGameElementOptions() items = %d", len(response.GetBody().GetItems()))
	}
	assertEnabledOptionQueries(t, elements.queries)
}

// TestKratosServiceListsEnabledStatOptions 验证能力值选项只公开稳定身份、编码和名称。
func TestKratosServiceListsEnabledStatOptions(t *testing.T) {
	t.Parallel()
	statID := snowflake.NewTestID()
	stats := &optionStatServiceStub{page: stat.Page{Items: []stat.Stat{{
		ID: statID, Code: "attack", Name: "攻击", Enabled: true,
	}}, Total: 1, Page: 1, PageSize: 100}}
	service := gameapi.NewKratosService(gameapi.NativeServices{Stats: stats}, slog.Default())

	response, err := service.ListGameStatOptions(optionContext(), &domainv1.ListGameStatOptionsRequest{})
	if err != nil {
		t.Fatalf("ListGameStatOptions() error = %v", err)
	}
	items := response.GetBody().GetItems()
	if len(items) != 1 || items[0].GetId() != statID.String() || items[0].GetCode() != "attack" || items[0].GetName() != "攻击" {
		t.Fatalf("ListGameStatOptions() = %#v", response)
	}
	assertEnabledOptionQueries(t, stats.queries)
}

// TestKratosServiceListsEnabledBattleRuleOptions 验证三类 BattleFormat 组成项保持独立选项契约。
func TestKratosServiceListsEnabledBattleRuleOptions(t *testing.T) {
	t.Parallel()
	clauses := &optionBattleRuleServiceStub{
		clausePage:      battleformat.ClausePage{Items: []battleformat.Clause{{ID: snowflake.NewTestID(), Code: "sleep", Name: "睡眠条款", Enabled: true}}, Total: 1, Page: 1, PageSize: 100},
		restrictionPage: battleformat.RestrictionPage{Items: []battleformat.Restriction{{ID: snowflake.NewTestID(), Code: "species", Name: "物种限制", Enabled: true}}, Total: 1, Page: 1, PageSize: 100},
		mechanicPage:    battleformat.MechanicPage{Items: []battleformat.Mechanic{{ID: snowflake.NewTestID(), Code: "mega", Name: "超级进化", Enabled: true}}, Total: 1, Page: 1, PageSize: 100},
	}
	service := gameapi.NewKratosService(gameapi.NativeServices{BattleRules: clauses}, slog.Default())
	ctx := optionContext()

	clauseResponse, clauseErr := service.ListBattleClauseOptions(ctx, &domainv1.ListBattleClauseOptionsRequest{})
	restrictionResponse, restrictionErr := service.ListBattleRestrictionOptions(ctx, &domainv1.ListBattleRestrictionOptionsRequest{})
	mechanicResponse, mechanicErr := service.ListBattleMechanicOptions(ctx, &domainv1.ListBattleMechanicOptionsRequest{})
	if clauseErr != nil || restrictionErr != nil || mechanicErr != nil {
		t.Fatalf("option errors = clause:%v restriction:%v mechanic:%v", clauseErr, restrictionErr, mechanicErr)
	}
	if clauseResponse.GetBody().GetItems()[0].GetCode() != "sleep" ||
		restrictionResponse.GetBody().GetItems()[0].GetCode() != "species" ||
		mechanicResponse.GetBody().GetItems()[0].GetCode() != "mega" {
		t.Fatalf("battle option responses = %#v %#v %#v", clauseResponse, restrictionResponse, mechanicResponse)
	}
	assertEnabledOptionQueries(t, clauses.clauseQueries)
	assertEnabledOptionQueries(t, clauses.restrictionQueries)
	assertEnabledOptionQueries(t, clauses.mechanicQueries)
}

func optionContext() context.Context {
	return authentication.WithPrincipal(context.Background(), authentication.Principal{AccountID: snowflake.NewTestID()})
}

type optionQuery interface {
	element.ListQuery | stat.ListQuery | battleformat.ClauseListQuery | battleformat.RestrictionListQuery | battleformat.MechanicListQuery
}

func assertEnabledOptionQueries[T optionQuery](t *testing.T, queries []T) {
	t.Helper()
	if len(queries) == 0 {
		t.Fatal("选项接口没有调用领域列表")
	}
	for index := range queries {
		page, pageSize, enabled := optionQueryFields(queries[index])
		if page != int32(index+1) || pageSize != 100 || enabled == nil || !*enabled {
			t.Fatalf("选项查询[%d] = page:%d pageSize:%d enabled:%v", index, page, pageSize, enabled)
		}
	}
}

func optionQueryFields[T optionQuery](query T) (int32, int32, *bool) {
	switch value := any(query).(type) {
	case element.ListQuery:
		return value.Page, value.PageSize, value.Enabled
	case stat.ListQuery:
		return value.Page, value.PageSize, value.Enabled
	case battleformat.ClauseListQuery:
		return value.Page, value.PageSize, value.Enabled
	case battleformat.RestrictionListQuery:
		return value.Page, value.PageSize, value.Enabled
	case battleformat.MechanicListQuery:
		return value.Page, value.PageSize, value.Enabled
	default:
		return 0, 0, nil
	}
}

// optionElementServiceStub 按页返回属性资料并记录选项接口发出的全部领域查询。
type optionElementServiceStub struct {
	// pages 按页码保存独立测试数据。
	pages map[int32]element.Page
	// queries 保存接口为收集全部选项发出的查询。
	queries []element.ListQuery
}

func (*optionElementServiceStub) Create(context.Context, element.CreateCommand) (element.Element, error) {
	return element.Element{}, nil
}
func (*optionElementServiceStub) Get(context.Context, snowflake.ID) (element.Element, error) {
	return element.Element{}, nil
}
func (stub *optionElementServiceStub) List(_ context.Context, query element.ListQuery) (element.Page, error) {
	stub.queries = append(stub.queries, query)
	return stub.pages[query.Page], nil
}
func (*optionElementServiceStub) Update(context.Context, element.UpdateCommand) (element.Element, error) {
	return element.Element{}, nil
}

// optionStatServiceStub 返回能力值资料并记录选项接口使用的领域查询。
type optionStatServiceStub struct {
	// page 是唯一一页能力值测试资料。
	page stat.Page
	// queries 保存接口发出的查询。
	queries []stat.ListQuery
}

func (*optionStatServiceStub) Create(context.Context, stat.CreateCommand) (stat.Stat, error) {
	return stat.Stat{}, nil
}
func (*optionStatServiceStub) Get(context.Context, snowflake.ID) (stat.Stat, error) {
	return stat.Stat{}, nil
}
func (stub *optionStatServiceStub) List(_ context.Context, query stat.ListQuery) (stat.Page, error) {
	stub.queries = append(stub.queries, query)
	return stub.page, nil
}
func (*optionStatServiceStub) Update(context.Context, stat.UpdateCommand) (stat.Stat, error) {
	return stat.Stat{}, nil
}

// optionBattleRuleServiceStub 分别保存三类 BattleFormat 组成项及其查询证据。
type optionBattleRuleServiceStub struct {
	// clausePage 是条款选项数据。
	clausePage battleformat.ClausePage
	// restrictionPage 是限制选项数据。
	restrictionPage battleformat.RestrictionPage
	// mechanicPage 是机制选项数据。
	mechanicPage battleformat.MechanicPage
	// clauseQueries 保存条款选项查询。
	clauseQueries []battleformat.ClauseListQuery
	// restrictionQueries 保存限制选项查询。
	restrictionQueries []battleformat.RestrictionListQuery
	// mechanicQueries 保存机制选项查询。
	mechanicQueries []battleformat.MechanicListQuery
}

func (*optionBattleRuleServiceStub) CreateClause(context.Context, battleformat.CreateClauseCommand) (battleformat.Clause, error) {
	return battleformat.Clause{}, nil
}
func (*optionBattleRuleServiceStub) UpdateClause(context.Context, battleformat.UpdateClauseCommand) (battleformat.Clause, error) {
	return battleformat.Clause{}, nil
}
func (*optionBattleRuleServiceStub) GetClause(context.Context, snowflake.ID) (battleformat.Clause, error) {
	return battleformat.Clause{}, nil
}
func (stub *optionBattleRuleServiceStub) ListClauses(_ context.Context, query battleformat.ClauseListQuery) (battleformat.ClausePage, error) {
	stub.clauseQueries = append(stub.clauseQueries, query)
	return stub.clausePage, nil
}
func (*optionBattleRuleServiceStub) CreateRestriction(context.Context, battleformat.CreateRestrictionCommand) (battleformat.Restriction, error) {
	return battleformat.Restriction{}, nil
}
func (*optionBattleRuleServiceStub) UpdateRestriction(context.Context, battleformat.UpdateRestrictionCommand) (battleformat.Restriction, error) {
	return battleformat.Restriction{}, nil
}
func (*optionBattleRuleServiceStub) GetRestriction(context.Context, snowflake.ID) (battleformat.Restriction, error) {
	return battleformat.Restriction{}, nil
}
func (stub *optionBattleRuleServiceStub) ListRestrictions(_ context.Context, query battleformat.RestrictionListQuery) (battleformat.RestrictionPage, error) {
	stub.restrictionQueries = append(stub.restrictionQueries, query)
	return stub.restrictionPage, nil
}
func (*optionBattleRuleServiceStub) CreateMechanic(context.Context, battleformat.CreateMechanicCommand) (battleformat.Mechanic, error) {
	return battleformat.Mechanic{}, nil
}
func (*optionBattleRuleServiceStub) UpdateMechanic(context.Context, battleformat.UpdateMechanicCommand) (battleformat.Mechanic, error) {
	return battleformat.Mechanic{}, nil
}
func (*optionBattleRuleServiceStub) GetMechanic(context.Context, snowflake.ID) (battleformat.Mechanic, error) {
	return battleformat.Mechanic{}, nil
}
func (stub *optionBattleRuleServiceStub) ListMechanics(_ context.Context, query battleformat.MechanicListQuery) (battleformat.MechanicPage, error) {
	stub.mechanicQueries = append(stub.mechanicQueries, query)
	return stub.mechanicPage, nil
}
func (*optionBattleRuleServiceStub) CreateFormat(context.Context, battleformat.CreateFormatCommand) (battleformat.Format, error) {
	return battleformat.Format{}, nil
}
func (*optionBattleRuleServiceStub) UpdateFormat(context.Context, battleformat.UpdateFormatCommand) (battleformat.Format, error) {
	return battleformat.Format{}, nil
}
func (*optionBattleRuleServiceStub) GetFormat(context.Context, snowflake.ID) (battleformat.Format, error) {
	return battleformat.Format{}, nil
}
func (*optionBattleRuleServiceStub) ListFormats(context.Context, battleformat.FormatListQuery) (battleformat.FormatPage, error) {
	return battleformat.FormatPage{}, nil
}
