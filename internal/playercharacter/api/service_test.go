package api_test

import (
	"context"
	"log/slog"
	"testing"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	domainv1 "github.com/lishangbu/avalon/api/gen/go/avalon/domain/v1"
	"github.com/lishangbu/avalon/internal/playercharacter"
	playerapi "github.com/lishangbu/avalon/internal/playercharacter/api"
	"github.com/lishangbu/avalon/internal/security/authentication"
)

// TestKratosServiceCreatesPlayerCharacter 验证原生 Kratos 服务直接把权威 Proto 请求映射到领域命令，
// 不经过进程内 HTTP 路由。
func TestKratosServiceCreatesPlayerCharacter(t *testing.T) {
	t.Parallel()

	accountID := snowflake.MustParse("1048576095")
	characterID := snowflake.MustParse("1048576096")
	now := time.Date(2026, time.July, 30, 1, 0, 0, 0, time.UTC)
	lifecycle := &lifecycleStub{created: playercharacter.PlayerCharacter{
		ID: characterID, AccountID: accountID, DisplayName: "星界旅人", Version: 1,
		CreatedAt: now, UpdatedAt: now,
	}}
	service := playerapi.NewKratosService(lifecycle, &queryStub{}, &activeStub{}, &presenceStub{}, slog.Default())
	ctx := authentication.WithPrincipal(context.Background(), authentication.Principal{AccountID: accountID})

	response, err := service.CreatePlayerCharacter(ctx, &domainv1.CreatePlayerCharacterRequest{
		HeaderIdempotencyKey: "create-player-character",
		Body:                 &domainv1.CreatePlayerCharacterBody{DisplayName: "星界旅人"},
	})
	if err != nil {
		t.Fatalf("CreatePlayerCharacter() error = %v", err)
	}
	if response.GetHttpStatusCode() != 201 || response.GetBody().GetId() != characterID.String() ||
		response.GetBody().GetVersion() != "1" {
		t.Fatalf("CreatePlayerCharacter() = %#v", response)
	}
	if lifecycle.create.AccountID != accountID || lifecycle.create.IdempotencyKey != "create-player-character" {
		t.Fatalf("Create command = %+v", lifecycle.create)
	}
}

type lifecycleStub struct {
	create  playercharacter.CreateCommand
	created playercharacter.PlayerCharacter
}

func (stub *lifecycleStub) Create(_ context.Context, command playercharacter.CreateCommand) (playercharacter.PlayerCharacter, error) {
	stub.create = command
	return stub.created, nil
}
func (*lifecycleStub) Rename(context.Context, playercharacter.RenameCommand) (playercharacter.PlayerCharacter, error) {
	return playercharacter.PlayerCharacter{}, nil
}
func (*lifecycleStub) Archive(context.Context, playercharacter.ArchiveCommand) (playercharacter.PlayerCharacter, error) {
	return playercharacter.PlayerCharacter{}, nil
}
func (*lifecycleStub) Restore(context.Context, playercharacter.RestoreCommand) (playercharacter.PlayerCharacter, error) {
	return playercharacter.PlayerCharacter{}, nil
}

type queryStub struct{}

func (*queryStub) GetOwned(context.Context, snowflake.ID, snowflake.ID) (playercharacter.PlayerCharacter, error) {
	return playercharacter.PlayerCharacter{}, nil
}
func (*queryStub) ListOwned(context.Context, snowflake.ID, bool) ([]playercharacter.PlayerCharacter, error) {
	return nil, nil
}
func (*queryStub) GetActive(context.Context, snowflake.ID) (playercharacter.ActiveBinding, error) {
	return playercharacter.ActiveBinding{}, nil
}
func (*queryStub) FindPublicByDisplayName(context.Context, snowflake.ID, string) (playercharacter.PublicPlayerCharacter, error) {
	return playercharacter.PublicPlayerCharacter{}, nil
}

type activeStub struct{}

func (*activeStub) Switch(context.Context, playercharacter.SwitchActiveCommand) (playercharacter.ActiveBinding, error) {
	return playercharacter.ActiveBinding{}, nil
}

type presenceStub struct{}

func (*presenceStub) Heartbeat(context.Context, snowflake.ID, snowflake.ID) (playercharacter.ActiveBinding, error) {
	return playercharacter.ActiveBinding{}, nil
}
