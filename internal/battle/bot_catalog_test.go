package battle_test

import (
	"context"
	"encoding/json"
	"errors"
	"testing"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	battle "github.com/lishangbu/avalon/internal/battle"
	"github.com/lishangbu/avalon/internal/gamedata/battleformat"
	"github.com/lishangbu/avalon/internal/team"
)

// TestPersistentTrainingBotCatalogFreezesEnabledMirrorDefinition 验证目录只使用当前启用定义，并把规范化
// JSON 和与真人 Team 隔离的镜像快照一并冻结到 Training Profile。
func TestPersistentTrainingBotCatalogFreezesEnabledMirrorDefinition(t *testing.T) {
	t.Parallel()
	playerTeam := team.Team{
		ID: snowflake.MustParse("1048576186"), Version: 2,
		Members: []team.Member{{Position: 1}, {Position: 2}},
	}
	catalog := battle.NewPersistentTrainingBotCatalog(botDefinitionReaderStub{record: battle.BotStrategyRecord{
		Code: "training-mirror", Version: 4, Definition: botDefinitionJSON(t, map[string]any{
			"schemaVersion": 1, "displayName": "镜像训练机器人",
			"planner":   map[string]any{"kind": "first_available", "fallbackKind": "first_available"},
			"generator": map[string]any{"kind": "mirror"},
			"budget":    map[string]any{"maxMembers": 6, "maxSkillsPerMember": 4, "maxDecisionMillis": 50},
		}),
	}}, snowflake.NewTestID)
	profile, err := catalog.ResolveTrainingBot(context.Background(), "training-mirror", playerTeam, botCatalogFormat(2))
	if err != nil {
		t.Fatalf("ResolveTrainingBot() error = %v", err)
	}
	if profile.Code != "training-mirror" || profile.StrategyVersion != 4 || profile.DisplayName != "镜像训练机器人" ||
		len(profile.Definition) == 0 || profile.Team.SourceTeamID != playerTeam.ID || profile.Team.SourceTeamVersion != 2 {
		t.Fatalf("ResolveTrainingBot() profile = %+v", profile)
	}
	playerTeam.Members[0].Position = 6
	if profile.Team.Members[0].Position != 1 {
		t.Fatalf("Bot Team 必须与调用者 Team 隔离，得到 %+v", profile.Team)
	}
	strategy, err := battle.NewBotStrategyFromFrozenDefinition(battle.Participant{
		IsBot: true, BotCode: profile.Code, BotStrategyVersion: profile.StrategyVersion, BotDefinition: profile.Definition,
	})
	if err != nil || strategy.Code() != profile.Code || strategy.Version() != profile.StrategyVersion {
		t.Fatalf("NewBotStrategyFromFrozenDefinition() = %#v, %v", strategy, err)
	}
}

// TestPersistentTrainingBotCatalogBuildsTemplateTeam 验证资料中的 template 生成器创建没有真人来源、但具有
// 稳定衍生身份的完整独立 Team Snapshot。
func TestPersistentTrainingBotCatalogBuildsTemplateTeam(t *testing.T) {
	t.Parallel()
	templateMember := validTemplateMember()
	catalog := battle.NewPersistentTrainingBotCatalog(botDefinitionReaderStub{record: battle.BotStrategyRecord{
		Code: "training-template", Version: 2, Definition: botDefinitionJSON(t, map[string]any{
			"schemaVersion": 1, "displayName": "模板训练机器人",
			"planner":   map[string]any{"kind": "first_available", "fallbackKind": "first_available"},
			"generator": map[string]any{"kind": "template", "members": []team.Member{templateMember}},
			"budget":    map[string]any{"maxMembers": 1, "maxSkillsPerMember": 1, "maxDecisionMillis": 20},
		}),
	}}, snowflake.NewTestID)
	profile, err := catalog.ResolveTrainingBot(context.Background(), "training-template", team.Team{
		ID: snowflake.MustParse("1048576187"), Version: 1, Members: []team.Member{{Position: 1}},
	}, botCatalogFormat(1))
	if err != nil {
		t.Fatalf("ResolveTrainingBot() error = %v", err)
	}
	if profile.Team.SourceTeamID == snowflake.ID(0) || profile.Team.SourceTeamVersion != 2 || len(profile.Team.Members) != 1 ||
		profile.Team.Members[0].CreatureID != templateMember.CreatureID {
		t.Fatalf("模板 Bot Team = %+v", profile.Team)
	}
}

// TestDecodeBotStrategyDefinitionRejectsUnknownOrUnsafeConfiguration 验证未知字段和未实现的策略不能以默认
// 行为悄悄进入生产 Battle。
func TestDecodeBotStrategyDefinitionRejectsUnknownOrUnsafeConfiguration(t *testing.T) {
	t.Parallel()
	_, _, err := battle.DecodeBotStrategyDefinition(json.RawMessage(`{
		"schemaVersion": 1,
		"displayName": "错误机器人",
		"planner": {"kind": "random", "fallbackKind": "first_available"},
		"generator": {"kind": "mirror"},
		"budget": {"maxMembers": 6, "maxSkillsPerMember": 4, "maxDecisionMillis": 50},
		"unexpected": true
	}`))
	if !errors.Is(err, battle.ErrBotDefinitionInvalid) {
		t.Fatalf("DecodeBotStrategyDefinition() error = %v，期望 ErrBotDefinitionInvalid", err)
	}
}

// botDefinitionReaderStub 为资料化 Bot 目录测试提供单条按 Code 查询的启用定义。
type botDefinitionReaderStub struct {
	// record 是测试期间唯一允许返回的启用策略定义。
	record battle.BotStrategyRecord
}

// GetEnabledBotStrategy 只对匹配 Code 返回预设定义，其他代码模拟未启用资料。
func (stub botDefinitionReaderStub) GetEnabledBotStrategy(_ context.Context, code string) (battle.BotStrategyRecord, error) {
	if code != stub.record.Code {
		return battle.BotStrategyRecord{}, battle.ErrBotStrategyUnavailable
	}
	return stub.record, nil
}

func botCatalogFormat(rosterCount int32) battleformat.Format {
	return battleformat.Format{RosterCount: rosterCount}
}

func botDefinitionJSON(t *testing.T, definition map[string]any) json.RawMessage {
	t.Helper()
	encoded, err := json.Marshal(definition)
	if err != nil {
		t.Fatalf("编码 Bot 定义: %v", err)
	}
	return encoded
}

func validTemplateMember() team.Member {
	return team.Member{
		Position: 1, CreatureID: snowflake.MustParse("1048576188"),
		AbilityID:     snowflake.MustParse("1048576189"),
		TeraElementID: snowflake.MustParse("1048576190"), Level: 50,
		NatureID: snowflake.MustParse("1048576193"),
		Skills:   []team.MemberSkill{{Position: 1, SkillID: snowflake.MustParse("1048576191")}},
		Stats:    []team.MemberStat{{StatID: snowflake.MustParse("1048576192"), IndividualValue: 31, EffortValue: 252}},
	}
}
