package battle_test

import (
	"context"
	"encoding/json"
	"testing"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	battle "github.com/lishangbu/avalon/internal/battle"
	"github.com/lishangbu/avalon/internal/gamedata/administration"
)

// TestBotStrategyAdministrationServiceCanonicalizesDefinition 验证管理服务只会将严格解析后的定义交给 Repository，
// 从而使幂等摘要、审计和未来冻结 Battle 使用同一份确定 JSON。
func TestBotStrategyAdministrationServiceCanonicalizesDefinition(t *testing.T) {
	t.Parallel()
	repository := &botStrategyAdaptersStub{}
	service := battle.NewBotStrategyAdministrationService(repository, repository, repository, func() time.Time {
		return time.Date(2026, time.July, 31, 12, 0, 0, 0, time.UTC)
	})
	definition := json.RawMessage(`{
		"budget":{"maxDecisionMillis":50,"maxSkillsPerMember":4,"maxMembers":6},
		"generator":{"kind":"mirror"},
		"planner":{"fallbackKind":"first_available","kind":"first_available"},
		"displayName":"训练机器人",
		"schemaVersion":1
	}`)
	created, err := service.Create(context.Background(), battle.CreateBotStrategyCommand{
		GameDataWriteContext: administration.NewGameDataWriteContext(snowflake.MustParse("1048576193"), "bot-create-1", "request-bot-create-1"),
		Code:                 "training-bot", Definition: definition,
	})
	if err != nil {
		t.Fatalf("Create() error = %v", err)
	}
	if created.Code != "training-bot" || created.Version != 1 || !created.Enabled {
		t.Fatalf("Create() = %+v", created)
	}
	if !json.Valid(repository.createDefinition) || string(repository.createDefinition) == string(definition) {
		t.Fatalf("传给 Repository 的定义必须是规范化 JSON，得到 %s", repository.createDefinition)
	}
	if repository.createCommand.ActorAccountID == snowflake.ID(0) || repository.createCommand.IdempotencyKey != "bot-create-1" {
		t.Fatalf("管理写入上下文未完整传给 Repository：%+v", repository.createCommand.GameDataWriteContext)
	}
}

// TestBotStrategyAdministrationServiceRejectsUnsafeDefinition 验证未实现的 Planner 不会绕过管理入口进入资料表。
func TestBotStrategyAdministrationServiceRejectsUnsafeDefinition(t *testing.T) {
	t.Parallel()
	repository := &botStrategyAdaptersStub{}
	service := battle.NewBotStrategyAdministrationService(repository, repository, repository, time.Now)
	_, err := service.Create(context.Background(), battle.CreateBotStrategyCommand{
		GameDataWriteContext: administration.NewGameDataWriteContext(snowflake.MustParse("1048576194"), "bot-create-2", "request-bot-create-2"),
		Code:                 "unsafe-bot",
		Definition: json.RawMessage(`{
			"schemaVersion":1,"displayName":"不安全机器人",
			"planner":{"kind":"random","fallbackKind":"first_available"},
			"generator":{"kind":"mirror"},
			"budget":{"maxMembers":6,"maxSkillsPerMember":4,"maxDecisionMillis":50}
		}`),
	})
	if err == nil {
		t.Fatal("Create() 未拒绝未实现的 Planner")
	}
	if repository.createCalled {
		t.Fatal("非法定义不应抵达 Repository")
	}
}

// botStrategyAdaptersStub 为管理服务测试提供读取、查询与写入替身。
type botStrategyAdaptersStub struct {
	// createCommand 是服务传给创建操作的已规范化命令。
	createCommand battle.CreateBotStrategyCommand
	// createDefinition 是服务传给创建操作的规范 JSON。
	createDefinition json.RawMessage
	// createCalled 表示测试命令是否已经抵达持久化边界。
	createCalled bool
}

func (stub *botStrategyAdaptersStub) GetBotStrategy(
	context.Context,
	string,
	uint32,
) (battle.ManagedBotStrategy, error) {
	return battle.ManagedBotStrategy{}, battle.ErrBotStrategyNotFound
}

func (stub *botStrategyAdaptersStub) ListBotStrategies(
	context.Context,
	battle.BotStrategyListQuery,
) (battle.BotStrategyPage, error) {
	return battle.BotStrategyPage{}, nil
}

func (stub *botStrategyAdaptersStub) CreateBotStrategy(
	_ context.Context,
	command battle.CreateBotStrategyCommand,
	definition json.RawMessage,
	createdAt time.Time,
) (battle.ManagedBotStrategy, error) {
	stub.createCalled = true
	stub.createCommand = command
	stub.createDefinition = append(json.RawMessage(nil), definition...)
	return battle.ManagedBotStrategy{
		Code: command.Code, Version: 1, Enabled: true, Definition: append(json.RawMessage(nil), definition...), CreatedAt: createdAt,
	}, nil
}

func (stub *botStrategyAdaptersStub) PublishNextBotStrategy(
	context.Context,
	battle.PublishNextBotStrategyCommand,
	json.RawMessage,
	time.Time,
) (battle.ManagedBotStrategy, error) {
	return battle.ManagedBotStrategy{}, battle.ErrBotStrategyNotFound
}

func (stub *botStrategyAdaptersStub) DisableBotStrategy(
	context.Context,
	battle.DisableBotStrategyCommand,
	time.Time,
) error {
	return nil
}
