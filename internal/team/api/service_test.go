package api_test

import (
	"context"
	"encoding/json"
	"errors"
	"log/slog"
	"testing"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	kratoserrors "github.com/go-kratos/kratos/v3/errors"
	domainv1 "github.com/lishangbu/avalon/api/gen/go/avalon/domain/v1"
	"github.com/lishangbu/avalon/internal/security/authentication"
	"github.com/lishangbu/avalon/internal/team"
	teamapi "github.com/lishangbu/avalon/internal/team/api"
)

// TestKratosServiceCreatesTeam 验证 Team 的生成 Proto 请求直接进入领域命令，且响应保持权威契约字段。
func TestKratosServiceCreatesTeam(t *testing.T) {
	t.Parallel()

	accountID := snowflake.MustParse("1048576103")
	characterID := snowflake.MustParse("1048576104")
	teamID := snowflake.MustParse("1048576105")
	now := time.Date(2026, time.July, 30, 2, 0, 0, 0, time.UTC)
	lifecycle := &lifecycleStub{created: team.Team{
		ID: teamID, PlayerCharacterID: characterID, Name: "首发阵容", Version: 1,
		CreatedAt: now, UpdatedAt: now,
	}}
	service := teamapi.NewKratosService(lifecycle, &queryStub{}, &shareStub{}, slog.Default())
	ctx := authentication.WithPrincipal(context.Background(), authentication.Principal{AccountID: accountID})

	response, err := service.CreateTeam(ctx, &domainv1.CreateTeamRequest{
		PlayerCharacterId: characterID.String(), HeaderIdempotencyKey: "create-team",
		Body: &domainv1.CreateTeamBody{Name: "首发阵容"},
	})
	if err != nil {
		t.Fatalf("CreateTeam() error = %v", err)
	}
	if response.GetHttpStatusCode() != 200 || response.GetBody().GetId() != teamID.String() {
		t.Fatalf("CreateTeam() = %#v", response)
	}
	if lifecycle.create.AccountID != accountID || lifecycle.create.PlayerCharacterID != characterID ||
		lifecycle.create.IdempotencyKey != "create-team" {
		t.Fatalf("Create command = %+v", lifecycle.create)
	}
}

// TestKratosServiceDoesNotReissueShareCodeOnIdempotentReplay 验证生成的 Team 契约不会把持久化响应中不存在的分享码重新暴露给客户端。
func TestKratosServiceDoesNotReissueShareCodeOnIdempotentReplay(t *testing.T) {
	t.Parallel()

	accountID := snowflake.MustParse("1048576106")
	characterID := snowflake.MustParse("1048576107")
	teamID := snowflake.MustParse("1048576108")
	shareID := snowflake.MustParse("1048576109")
	shares := &shareStub{created: team.CreateShareResult{Share: team.Share{
		ID: shareID, SourceTeamID: teamID, OwnerPlayerCharacterID: characterID,
		SourceTeamVersion: 1, SchemaVersion: team.TeamShareSchemaVersion, Version: 1,
	}}}
	service := teamapi.NewKratosService(&lifecycleStub{}, &queryStub{}, shares, slog.Default())
	ctx := authentication.WithPrincipal(context.Background(), authentication.Principal{AccountID: accountID})

	response, err := service.CreateTeamShare(ctx, &domainv1.CreateTeamShareRequest{
		PlayerCharacterId: characterID.String(), TeamId: teamID.String(), HeaderIdempotencyKey: "replay-team-share",
		Body: &domainv1.CreateTeamShareBody{ExpectedVersion: "1"},
	})
	if err != nil {
		t.Fatalf("CreateTeamShare() error = %v", err)
	}
	if response.GetHttpStatusCode() != 200 || response.GetBody().GetCode() != "" {
		t.Fatalf("CreateTeamShare() = %#v, want an empty replayed share code", response)
	}
	if shares.create.AccountID != accountID || shares.create.PlayerCharacterID != characterID || shares.create.TeamID != teamID {
		t.Fatalf("CreateShare command = %+v", shares.create)
	}
}

// TestKratosServiceImportsTeamWithStableHTTPStatus 验证导入分享的包装响应与生成 HTTP 路由、
// Protobuf 中已经固定的 200 成功状态保持一致。
func TestKratosServiceImportsTeamWithStableHTTPStatus(t *testing.T) {
	t.Parallel()

	accountID := snowflake.MustParse("1048576116")
	characterID := snowflake.MustParse("1048576117")
	teamID := snowflake.MustParse("1048576118")
	shares := &shareStub{imported: team.Team{
		ID: teamID, PlayerCharacterID: characterID, Name: "导入阵容", Version: 1,
	}}
	service := teamapi.NewKratosService(&lifecycleStub{}, &queryStub{}, shares, slog.Default())
	ctx := authentication.WithPrincipal(context.Background(), authentication.Principal{AccountID: accountID})

	response, err := service.ImportTeamShare(ctx, &domainv1.ImportTeamShareRequest{
		PlayerCharacterId: characterID.String(), HeaderIdempotencyKey: "import-team-share",
		Body: &domainv1.ImportTeamShareBody{Code: "有效分享码", Name: "导入阵容"},
	})
	if err != nil {
		t.Fatalf("ImportTeamShare() error = %v", err)
	}
	if response.GetHttpStatusCode() != 200 || response.GetBody().GetId() != teamID.String() {
		t.Fatalf("ImportTeamShare() = %#v，期望包装响应状态码为 200", response)
	}
}

// TestKratosServiceExposesCurrentGameDataCompatibilityIssues 验证 Team API 会把当前实时资料
// 校验产生的结构化问题放入 Kratos metadata，同时保留领域错误链供上层测试与适配器判定。
func TestKratosServiceExposesCurrentGameDataCompatibilityIssues(t *testing.T) {
	t.Parallel()

	accountID := snowflake.MustParse("1048576126")
	characterID := snowflake.MustParse("1048576127")
	missingSkillID := snowflake.MustParse("1048576128")
	wantIssues := []team.CompatibilityIssue{{
		MemberPosition: 2, Field: "skillIds", Code: "reference_unavailable", ReferenceID: missingSkillID,
	}}
	lifecycle := &lifecycleStub{createErr: team.NewCompatibilityError(wantIssues)}
	service := teamapi.NewKratosService(lifecycle, &queryStub{}, &shareStub{}, slog.Default())
	ctx := authentication.WithPrincipal(context.Background(), authentication.Principal{AccountID: accountID})

	_, err := service.CreateTeam(ctx, &domainv1.CreateTeamRequest{
		PlayerCharacterId: characterID.String(), HeaderIdempotencyKey: "invalid-current-game-data",
		Body: &domainv1.CreateTeamBody{Name: "资料失效阵容"},
	})
	if !errors.Is(err, team.ErrTeamReferenceInvalid) {
		t.Fatalf("CreateTeam() error = %v，期望保持 ErrTeamReferenceInvalid", err)
	}
	transportError := kratoserrors.FromError(err)
	if transportError.GetCode() != 400 || transportError.GetReason() != "INVALID_TEAM" {
		t.Fatalf("CreateTeam() Kratos error = %+v，期望 400 INVALID_TEAM", transportError)
	}
	var gotIssues []team.CompatibilityIssue
	if decodeErr := json.Unmarshal([]byte(transportError.GetMetadata()["issues"]), &gotIssues); decodeErr != nil {
		t.Fatalf("解码 Kratos metadata.issues 失败：%v；metadata = %#v", decodeErr, transportError.GetMetadata())
	}
	if len(gotIssues) != 1 || gotIssues[0] != wantIssues[0] {
		t.Fatalf("Kratos metadata.issues = %+v，期望 %+v", gotIssues, wantIssues)
	}
}

type lifecycleStub struct {
	create  team.CreateCommand
	created team.Team
	// createErr 是模拟创建 Team 时返回的领域错误。
	createErr error
}

func (stub *lifecycleStub) Create(_ context.Context, command team.CreateCommand) (team.Team, error) {
	stub.create = command
	return stub.created, stub.createErr
}
func (*lifecycleStub) Update(context.Context, team.UpdateCommand) (team.Team, error) {
	return team.Team{}, nil
}
func (*lifecycleStub) Delete(context.Context, team.DeleteCommand) (team.DeleteResult, error) {
	return team.DeleteResult{}, nil
}
func (*lifecycleStub) SwitchActive(context.Context, team.SwitchActiveCommand) (team.ActiveBinding, error) {
	return team.ActiveBinding{}, nil
}

type queryStub struct{}

func (*queryStub) GetOwned(context.Context, snowflake.ID, snowflake.ID, snowflake.ID) (team.Team, error) {
	return team.Team{}, nil
}
func (*queryStub) ListOwned(context.Context, snowflake.ID, snowflake.ID) ([]team.Team, error) {
	return nil, nil
}
func (*queryStub) GetActive(context.Context, snowflake.ID, snowflake.ID) (team.ActiveBinding, error) {
	return team.ActiveBinding{}, nil
}

type shareStub struct {
	// create 记录生成契约映射后的分享创建命令。
	create team.CreateShareCommand
	// created 是模拟领域服务返回的创建或幂等重放结果。
	created team.CreateShareResult
	// imported 是模拟领域服务返回的独立导入 Team。
	imported team.Team
	// importErr 是模拟导入分享时返回的领域错误。
	importErr error
}

func (stub *shareStub) Create(_ context.Context, command team.CreateShareCommand) (team.CreateShareResult, error) {
	stub.create = command
	return stub.created, nil
}
func (*shareStub) Resolve(context.Context, string) (team.ShareSnapshot, error) {
	return team.ShareSnapshot{}, nil
}
func (*shareStub) Revoke(context.Context, team.RevokeShareCommand) (team.Share, error) {
	return team.Share{}, nil
}
func (stub *shareStub) Import(context.Context, team.ImportShareCommand) (team.Team, error) {
	return stub.imported, stub.importErr
}
