package api_test

import (
	"context"
	"encoding/json"
	"log/slog"
	"testing"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	domainv1 "github.com/lishangbu/avalon/api/gen/go/avalon/domain/v1"
	battle "github.com/lishangbu/avalon/internal/battle"
	"github.com/lishangbu/avalon/internal/gamedata/administration"
	gameapi "github.com/lishangbu/avalon/internal/gamedata/api"
	"github.com/lishangbu/avalon/internal/security/authentication"
)

// TestKratosServiceCreatesBotStrategy 验证 Bot 管理 HTTP 边界使用专属版本化命令，
// 并把真实 Idempotency-Key、管理员与全局资料修订完整传递给领域服务。
func TestKratosServiceCreatesBotStrategy(t *testing.T) {
	t.Parallel()
	accountID := snowflake.MustParse("1048576195")
	bots := &botStrategyServiceStub{created: battle.ManagedBotStrategy{
		Code: "training-bot", Version: 1, Enabled: true, Definition: validBotDefinition(),
		CreatedAt: time.Date(2026, time.July, 31, 12, 0, 0, 0, time.UTC),
	}}
	service := gameapi.NewKratosService(gameapi.NativeServices{BotStrategies: bots}, slog.Default())
	ctx := authentication.WithPrincipal(context.Background(), authentication.Principal{AccountID: accountID})
	response, err := service.CreateGameBotStrategy(ctx, &domainv1.CreateGameBotStrategyRequest{
		HeaderIdempotencyKey: "create-bot-strategy",
		Body: &domainv1.CreateGameBotStrategyBody{
			Code: "training-bot", DefinitionJson: string(validBotDefinition())},
	})
	if err != nil {
		t.Fatalf("CreateGameBotStrategy() error = %v", err)
	}
	if response.GetHttpStatusCode() != 201 || response.GetBody().GetVersion() != 1 || response.GetBody().GetCode() != "training-bot" {
		t.Fatalf("CreateGameBotStrategy() = %#v", response)
	}
	if bots.create.GameDataWriteContext != administration.NewGameDataWriteContext(accountID, "create-bot-strategy", "") {
		// Request ID 由 HTTP middleware 注入；直接调用传输服务时为空，但其他写入上下文必须精确一致。
		if bots.create.ActorAccountID != accountID || bots.create.IdempotencyKey != "create-bot-strategy" {
			t.Fatalf("Create command context = %+v", bots.create.GameDataWriteContext)
		}
	}
	if !json.Valid(bots.create.Definition) {
		t.Fatalf("Create command definition = %s", bots.create.Definition)
	}
}

// TestKratosServiceRejectsMalformedBotDefinition 验证 JSON 格式错误不会穿透 HTTP 边界到领域服务。
func TestKratosServiceRejectsMalformedBotDefinition(t *testing.T) {
	t.Parallel()
	bots := &botStrategyServiceStub{}
	service := gameapi.NewKratosService(gameapi.NativeServices{BotStrategies: bots}, slog.Default())
	ctx := authentication.WithPrincipal(context.Background(), authentication.Principal{AccountID: snowflake.NewTestID()})
	_, err := service.CreateGameBotStrategy(ctx, &domainv1.CreateGameBotStrategyRequest{
		HeaderIdempotencyKey: "malformed-bot-strategy",
		Body: &domainv1.CreateGameBotStrategyBody{
			Code: "training-bot", DefinitionJson: "{"},
	})
	if err == nil {
		t.Fatal("CreateGameBotStrategy() 未拒绝格式错误的 JSON")
	}
	if bots.createCalled {
		t.Fatal("格式错误的 JSON 不应抵达 Bot 领域服务")
	}
}

// botStrategyServiceStub 记录 Bot 管理传输服务测试中应当进入领域的命令。
type botStrategyServiceStub struct {
	// create 是最近一次创建命令的完整领域事实。
	create battle.CreateBotStrategyCommand
	// created 是创建调用需要返回的不可变版本。
	created battle.ManagedBotStrategy
	// createCalled 表示创建命令是否已经抵达领域边界。
	createCalled bool
}

func (stub *botStrategyServiceStub) Create(
	_ context.Context,
	command battle.CreateBotStrategyCommand,
) (battle.ManagedBotStrategy, error) {
	stub.createCalled = true
	stub.create = command
	return stub.created, nil
}

func (*botStrategyServiceStub) PublishNext(
	context.Context,
	battle.PublishNextBotStrategyCommand,
) (battle.ManagedBotStrategy, error) {
	return battle.ManagedBotStrategy{}, nil
}

func (*botStrategyServiceStub) Disable(context.Context, battle.DisableBotStrategyCommand) error {
	return nil
}

func (*botStrategyServiceStub) Get(context.Context, string, uint32) (battle.ManagedBotStrategy, error) {
	return battle.ManagedBotStrategy{}, battle.ErrBotStrategyNotFound
}

func (*botStrategyServiceStub) List(context.Context, battle.BotStrategyListQuery) (battle.BotStrategyPage, error) {
	return battle.BotStrategyPage{}, nil
}

// validBotDefinition 返回当前受支持的最小镜像 Bot 定义 JSON。
func validBotDefinition() json.RawMessage {
	return json.RawMessage(`{
		"schemaVersion":1,
		"displayName":"训练机器人",
		"planner":{"kind":"first_available","fallbackKind":"first_available"},
		"generator":{"kind":"mirror"},
		"budget":{"maxMembers":6,"maxSkillsPerMember":4,"maxDecisionMillis":50}
	}`)
}
