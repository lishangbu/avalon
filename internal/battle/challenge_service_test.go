package battle_test

import (
	"context"
	"encoding/json"
	"testing"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	battle "github.com/lishangbu/avalon/internal/battle"
	"github.com/lishangbu/avalon/internal/gamedata/battleformat"
	"github.com/lishangbu/avalon/internal/playercharacter"
	"github.com/lishangbu/avalon/internal/team"
)

// TestChallengeApplicationCreateFreezesConsistentRuntimeFacts 验证创建用例只在同一可用资料修订下冻结事实。
func TestChallengeApplicationCreateFreezesConsistentRuntimeFacts(t *testing.T) {
	t.Parallel()
	accountID := snowflake.MustParse("1048576178")
	characterID := snowflake.MustParse("1048576179")
	targetAccountID := snowflake.MustParse("1048576180")
	targetCharacterID := snowflake.MustParse("1048576181")
	teamID := snowflake.MustParse("1048576182")
	formatID := snowflake.MustParse("1048576183")
	format := testChallengeFormat(formatID)
	repository := &challengeRepositoryStub{}
	service := battle.NewChallengeApplicationServiceWithRules(
		repository, repository,
		activeCharacterStub{active: playercharacter.ActiveBinding{AccountID: accountID, PlayerCharacterID: characterID}, character: playercharacter.PlayerCharacter{ID: characterID, AccountID: accountID, DisplayName: "发起者"}},
		targetQueryStub{target: playercharacter.ChallengeTarget{AccountID: targetAccountID, PlayerCharacterID: targetCharacterID, DisplayName: "接收者"}},
		teamAdmissionStub{team: team.Team{ID: teamID, PlayerCharacterID: characterID, Version: 1, Members: []team.Member{{Position: 1}, {Position: 2}}}},
		formatQueryStub{format: format},
		nil,
		snowflake.TestSource(func() snowflake.ID { return snowflake.MustParse("1048576184") }),
		func() time.Time { return time.Date(2026, time.July, 30, 8, 0, 0, 0, time.UTC) })

	challenge, err := service.Create(context.Background(), accountID, battle.CreateChallengeApplicationCommand{
		TeamID: teamID, TargetDisplayName: "接收者", BattleFormatID: formatID,
	})
	if err != nil {
		t.Fatalf("Create() error = %v", err)
	}
	if repository.created.ID != challenge.ID || challenge.Status != battle.ChallengePending {
		t.Fatalf("Create() challenge = %+v, persisted = %+v", challenge, repository.created)
	}
	var frozen battleformat.Format
	if err := json.Unmarshal(challenge.BattleFormatSnapshot, &frozen); err != nil || frozen.ID != formatID {
		t.Fatalf("Create() frozen format = %+v, error = %v", frozen, err)
	}
}

// TestChallengeApplicationAcceptUsesFrozenFormatAndCurrentTeams 验证接受邀请时使用已冻结赛制与重新校验的当前队伍。
func TestChallengeApplicationAcceptUsesFrozenFormatAndCurrentTeams(t *testing.T) {
	t.Parallel()
	challengerAccountID := snowflake.NewTestID()
	challengerCharacterID := snowflake.NewTestID()
	targetAccountID := snowflake.NewTestID()
	targetCharacterID := snowflake.NewTestID()
	teamID := snowflake.NewTestID()
	formatID := snowflake.NewTestID()
	createdAt := time.Date(2026, time.July, 30, 9, 0, 0, 0, time.UTC)
	challenge, err := battle.NewChallenge(context.Background(), battle.CreateChallengeCommand{
		ChallengerAccountID: challengerAccountID, ChallengerPlayerCharacterID: challengerCharacterID, ChallengerDisplayName: "发起者",
		ChallengerTeam:  team.Team{ID: snowflake.NewTestID(), PlayerCharacterID: challengerCharacterID, Version: 1, Members: []team.Member{{Position: 1}, {Position: 2}}},
		TargetAccountID: targetAccountID, TargetPlayerCharacterID: targetCharacterID, TargetDisplayName: "接收者",
		BattleFormatID: formatID, BattleFormatSnapshot: mustJSON(testChallengeFormat(formatID)),
	}, snowflake.NewTestID, func() time.Time { return createdAt })
	if err != nil {
		t.Fatalf("NewChallenge() error = %v", err)
	}
	repository := &challengeRepositoryStub{challenge: challenge}
	service := battle.NewChallengeApplicationServiceWithRules(
		repository, repository,
		activeCharacterStub{active: playercharacter.ActiveBinding{AccountID: targetAccountID, PlayerCharacterID: targetCharacterID}, character: playercharacter.PlayerCharacter{ID: targetCharacterID, AccountID: targetAccountID, DisplayName: "接收者"}},
		targetQueryStub{},
		teamAdmissionStub{team: team.Team{ID: teamID, PlayerCharacterID: targetCharacterID, Version: 1, Members: []team.Member{{Position: 1}, {Position: 2}}}},
		formatQueryStub{format: testChallengeFormat(formatID)},
		nil,
		snowflake.NewTestID,
		func() time.Time { return createdAt.Add(time.Minute) })

	_, err = service.Accept(context.Background(), targetAccountID, battle.AcceptChallengeApplicationCommand{
		ChallengeID: challenge.ID, TeamID: teamID,
	})
	if err != nil {
		t.Fatalf("Accept() error = %v", err)
	}
	if !repository.accepted {
		t.Fatal("通过当前资料重新校验后应调用持久化接受操作")
	}
}

func mustJSON(value any) json.RawMessage {
	encoded, err := json.Marshal(value)
	if err != nil {
		panic(err)
	}
	return encoded
}

// TestTrainingApplicationCreatesFrozenDatabaseBot 验证训练战会冻结资料化 Bot Team、定义和自动 Preview。
func TestTrainingApplicationCreatesFrozenDatabaseBot(t *testing.T) {
	t.Parallel()
	accountID := snowflake.NewTestID()
	characterID := snowflake.NewTestID()
	teamID := snowflake.NewTestID()
	formatID := snowflake.NewTestID()
	format := testChallengeFormat(formatID)
	format.Availability = battleformat.Availability{Training: true}
	playerTeam := team.Team{
		ID: teamID, PlayerCharacterID: characterID, Version: 1,
		Members: []team.Member{{Position: 1}, {Position: 2}},
	}
	repository := &practiceRepositoryStub{}
	service := battle.NewTrainingApplicationServiceWithRules(
		repository,
		activeCharacterStub{active: playercharacter.ActiveBinding{AccountID: accountID, PlayerCharacterID: characterID}, character: playercharacter.PlayerCharacter{ID: characterID, AccountID: accountID, DisplayName: "练习者"}},
		teamAdmissionStub{team: playerTeam},
		formatQueryStub{format: format},
		battle.NewPersistentTrainingBotCatalog(enabledBotStrategyStub{record: battle.BotStrategyRecord{
			Code: "training-mirror", Version: 3, Definition: testMirrorBotDefinition("资料训练机器人"),
		}}, snowflake.NewTestID),
		nil,
		snowflake.NewTestID,
		func() time.Time { return time.Date(2026, time.July, 30, 8, 0, 0, 0, time.UTC) },
	)

	session, err := service.Create(context.Background(), accountID, battle.CreateTrainingApplicationCommand{
		TeamID: teamID, BattleFormatID: formatID, BotCode: "training-mirror",
	})
	if err != nil {
		t.Fatalf("Create() error = %v", err)
	}
	if session.Mode != battle.BattleModePvE || session.SourceType != battle.BattleSourceTraining || len(session.PreviewSubmissions) != 1 || !session.Participants[1].IsBot {
		t.Fatalf("Create() session = %+v", session)
	}
	playerTeam.Members[0].Position = 6
	if session.Participants[1].Team.Members[0].Position != 1 || repository.created.ID != session.ID ||
		len(session.Participants[1].BotDefinition) == 0 || session.Participants[1].BotStrategyVersion != 3 {
		t.Fatalf("Create() must persist an isolated Bot snapshot, session=%+v persisted=%+v", session, repository.created)
	}
}

func testMirrorBotDefinition(displayName string) json.RawMessage {
	return json.RawMessage(`{"schemaVersion":1,"displayName":"` + displayName + `","planner":{"kind":"first_available","fallbackKind":"first_available"},"generator":{"kind":"mirror"},"budget":{"maxMembers":6,"maxSkillsPerMember":4,"maxDecisionMillis":50}}`)
}

func testChallengeFormat(id snowflake.ID) battleformat.Format {
	return battleformat.Format{
		ID: id, Code: "test-practice", Name: "测试赛制", Enabled: true,
		RosterCount: 2, SelectCount: 2, ActiveParticipantsPerSide: 1,
		Deadlines:    battleformat.Deadlines{PreviewSeconds: 60, TurnSeconds: 30, BattleSeconds: 300},
		Availability: battleformat.Availability{Challenge: true},
	}
}

type activeCharacterStub struct {
	active    playercharacter.ActiveBinding
	character playercharacter.PlayerCharacter
}

func (stub activeCharacterStub) GetActive(context.Context, snowflake.ID) (playercharacter.ActiveBinding, error) {
	return stub.active, nil
}

func (stub activeCharacterStub) GetOwned(context.Context, snowflake.ID, snowflake.ID) (playercharacter.PlayerCharacter, error) {
	return stub.character, nil
}

type targetQueryStub struct {
	target playercharacter.ChallengeTarget
}

func (stub targetQueryStub) ResolveChallengeTarget(context.Context, snowflake.ID, string) (playercharacter.ChallengeTarget, error) {
	return stub.target, nil
}

type teamAdmissionStub struct{ team team.Team }

func (stub teamAdmissionStub) ValidateOwned(context.Context, snowflake.ID, snowflake.ID, snowflake.ID) (team.Team, error) {
	return stub.team, nil
}

type formatQueryStub struct{ format battleformat.Format }

func (stub formatQueryStub) GetFormat(context.Context, snowflake.ID) (battleformat.Format, error) {
	return stub.format, nil
}

type challengeRepositoryStub struct {
	created   battle.Challenge
	challenge battle.Challenge
	accepted  bool
}

func (stub *challengeRepositoryStub) CreateChallenge(_ context.Context, challenge battle.Challenge) error {
	stub.created = challenge
	return nil
}

func (stub *challengeRepositoryStub) GetChallenge(context.Context, snowflake.ID) (battle.Challenge, error) {
	return stub.challenge, nil
}

func (stub *challengeRepositoryStub) AcceptChallenge(
	context.Context,
	snowflake.ID,
	snowflake.ID,
	team.Team,
	battle.Format,
	time.Time,
) (battle.Battle, error) {
	stub.accepted = true
	return battle.Battle{}, nil
}

func (stub *challengeRepositoryStub) RejectChallenge(context.Context, snowflake.ID, snowflake.ID, time.Time) (battle.Challenge, error) {
	return battle.Challenge{}, nil
}

func (stub *challengeRepositoryStub) WithdrawChallenge(context.Context, snowflake.ID, snowflake.ID, time.Time) (battle.Challenge, error) {
	return battle.Challenge{}, nil
}

func (stub *challengeRepositoryStub) ExpireChallenge(context.Context, snowflake.ID, time.Time) (battle.Challenge, error) {
	return battle.Challenge{}, nil
}

type practiceRepositoryStub struct{ created battle.Battle }

func (stub *practiceRepositoryStub) Create(_ context.Context, session battle.Battle) error {
	stub.created = session
	return nil
}

// enabledBotStrategyStub 为 Training 应用测试提供一条已经启用的不可变 Bot 资料记录。
type enabledBotStrategyStub struct {
	// record 是按代码返回的已启用定义。
	record battle.BotStrategyRecord
}

// GetEnabledBotStrategy 返回预设资料记录，模拟同一 Bot Code 仅有一个当前启用版本的约束。
func (stub enabledBotStrategyStub) GetEnabledBotStrategy(_ context.Context, code string) (battle.BotStrategyRecord, error) {
	if code != stub.record.Code {
		return battle.BotStrategyRecord{}, battle.ErrBotStrategyUnavailable
	}
	return stub.record, nil
}
