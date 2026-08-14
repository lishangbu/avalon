package team_test

import (
	"context"
	"errors"
	"reflect"
	"testing"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/lishangbu/avalon/internal/team"
)

func TestServiceCreatesFirstTeamAsActiveWithNormalizedCompleteRoster(t *testing.T) {
	t.Parallel()

	characterID := snowflake.MustParse("1048576095")
	teamID := snowflake.MustParse("1048576096")
	creatureID := snowflake.MustParse("1048576097")
	abilityID := snowflake.MustParse("1048576098")
	elementID := snowflake.MustParse("1048576099")
	skillID := snowflake.MustParse("1048576100")
	statID := snowflake.MustParse("1048576101")
	natureID := snowflake.MustParse("1048576102")
	now := time.Date(2026, time.July, 29, 5, 30, 0, 0, time.UTC)
	repository := &teamRepositoryStub{}
	validator := &acceptingCurrentMemberValidator{}
	service := team.NewService(repository, validator, &availableGameDataGateStub{}, snowflake.TestSource(func() snowflake.ID { return teamID }), func() time.Time { return now }, nil)

	created, err := service.Create(context.Background(), team.CreateCommand{
		AccountID:         snowflake.NewTestID(),
		PlayerCharacterID: characterID,
		Name:              "  标准   单打  ",
		Members: []team.MemberInput{{
			CreatureID: creatureID, AbilityID: abilityID, TeraElementID: elementID, NatureID: natureID, Level: 50,
			SkillIDs: []snowflake.ID{skillID},
			Stats:    []team.MemberStatInput{{StatID: statID, IndividualValue: 31, EffortValue: 252}},
		}},
		IdempotencyKey: "create-first-team", RequestID: "create-first-team-request",
	})
	if err != nil {
		t.Fatalf("Create() error = %v", err)
	}
	if created.ID != teamID || created.PlayerCharacterID != characterID ||
		created.Name != "标准 单打" || created.NameKey != "标准 单打" || created.Version != 1 || !created.Active {
		t.Fatalf("Create() = %+v", created)
	}
	if len(created.Members) != 1 || created.Members[0].Position != 1 ||
		created.Members[0].Skills[0].Position != 1 || created.Members[0].Stats[0].StatID != statID {
		t.Fatalf("Members = %+v", created.Members)
	}
	if !reflect.DeepEqual(repository.record.Team, created) || repository.record.IdempotencyKey != "create-first-team" {
		t.Fatalf("Create record = %+v", repository.record)
	}
	if validator.calls != 1 {
		t.Fatalf("ValidateCurrent() calls = %d, want 1", validator.calls)
	}
}

func TestServiceRejectsDuplicateMemberSkillsBeforePersistence(t *testing.T) {
	t.Parallel()

	skillID := snowflake.NewTestID()
	service := team.NewService(&teamRepositoryStub{}, &acceptingCurrentMemberValidator{}, &availableGameDataGateStub{}, snowflake.NewTestID, time.Now, nil)
	_, err := service.Create(context.Background(), team.CreateCommand{
		AccountID: snowflake.NewTestID(), PlayerCharacterID: snowflake.NewTestID(), Name: "重复技能队伍",
		Members: []team.MemberInput{{
			CreatureID: snowflake.NewTestID(), AbilityID: snowflake.NewTestID(), TeraElementID: snowflake.NewTestID(), NatureID: snowflake.NewTestID(),
			SkillIDs: []snowflake.ID{skillID, skillID},
		}},
		IdempotencyKey: "duplicate-skill", RequestID: "duplicate-skill-request",
	})
	if err != team.ErrInvalidTeam {
		t.Fatalf("Create() error = %v, want ErrInvalidTeam", err)
	}
}

func TestServiceRejectsCurrentMembersBeforeCreatePersistence(t *testing.T) {
	t.Parallel()

	repository := &teamRepositoryStub{}
	validator := &rejectingCurrentMemberValidator{}
	service := team.NewService(repository, validator, &availableGameDataGateStub{}, snowflake.NewTestID, time.Now, nil)
	_, err := service.Create(context.Background(), validServiceCreateCommand(snowflake.NewTestID(), snowflake.NewTestID(), "拒绝创建"))
	if err != team.ErrTeamReferenceInvalid {
		t.Fatalf("Create() error = %v, want ErrTeamReferenceInvalid", err)
	}
	if validator.calls != 1 {
		t.Fatalf("ValidateCurrent() calls = %d, want 1", validator.calls)
	}
	if repository.createCalls != 0 {
		t.Fatalf("Create() persistence calls = %d, want 0", repository.createCalls)
	}
}

func TestServiceRevalidatesCurrentMembersDuringFirstUpdatePersistence(t *testing.T) {
	t.Parallel()

	accountID := snowflake.NewTestID()
	playerCharacterID := snowflake.NewTestID()
	teamID := snowflake.NewTestID()
	repository := &teamRepositoryStub{record: team.CreateRecord{Team: team.Team{
		ID: teamID, PlayerCharacterID: playerCharacterID, Name: "原始队伍", NameKey: "原始队伍", Version: 1,
	}}}
	validator := &acceptingCurrentMemberValidator{}
	service := team.NewService(repository, validator, &availableGameDataGateStub{}, snowflake.NewTestID, time.Now, nil)

	updated, err := service.Update(context.Background(), team.UpdateCommand{
		AccountID: accountID, PlayerCharacterID: playerCharacterID, TeamID: teamID, ExpectedVersion: 1,
		Name: "更新队伍", Members: validServiceMemberInputs(),
		IdempotencyKey: "update-valid-team", RequestID: "update-valid-team-request",
	})
	if err != nil {
		t.Fatalf("Update() error = %v", err)
	}
	if updated.Version != 2 || updated.Name != "更新队伍" {
		t.Fatalf("Update() = %+v", updated)
	}
	if validator.calls != 1 {
		t.Fatalf("ValidateCurrent() calls = %d, want 1", validator.calls)
	}
	if repository.updateCalls != 1 {
		t.Fatalf("Update() persistence calls = %d, want 1", repository.updateCalls)
	}
}

func TestServiceRejectsCurrentMembersBeforeUpdatePersistence(t *testing.T) {
	t.Parallel()

	accountID := snowflake.NewTestID()
	playerCharacterID := snowflake.NewTestID()
	teamID := snowflake.NewTestID()
	repository := &teamRepositoryStub{record: team.CreateRecord{Team: team.Team{
		ID: teamID, PlayerCharacterID: playerCharacterID, Name: "原始队伍", NameKey: "原始队伍", Version: 1,
	}}}
	validator := &rejectingCurrentMemberValidator{}
	service := team.NewService(repository, validator, &availableGameDataGateStub{}, snowflake.NewTestID, time.Now, nil)

	_, err := service.Update(context.Background(), team.UpdateCommand{
		AccountID: accountID, PlayerCharacterID: playerCharacterID, TeamID: teamID, ExpectedVersion: 1,
		Name: "拒绝更新", Members: validServiceMemberInputs(),
		IdempotencyKey: "update-rejected-team", RequestID: "update-rejected-team-request",
	})
	if err != team.ErrTeamReferenceInvalid {
		t.Fatalf("Update() error = %v, want ErrTeamReferenceInvalid", err)
	}
	if validator.calls != 1 {
		t.Fatalf("ValidateCurrent() calls = %d, want 1", validator.calls)
	}
	if repository.updateCalls != 0 {
		t.Fatalf("Update() persistence calls = %d, want 0", repository.updateCalls)
	}
}

func TestNewServiceRejectsNilCurrentMemberValidator(t *testing.T) {
	t.Parallel()

	var validator *acceptingCurrentMemberValidator
	defer func() {
		if recover() == nil {
			t.Fatal("NewService() accepted a nil CurrentMemberValidator")
		}
	}()
	_ = team.NewService(&teamRepositoryStub{}, validator, &availableGameDataGateStub{}, snowflake.NewTestID, time.Now, nil)
}

// TestNewServiceRejectsNilCurrentGameDataGate 验证不能构造缺少维护锁协调能力的 Team 保存服务。
func TestNewServiceRejectsNilCurrentGameDataGate(t *testing.T) {
	t.Parallel()

	var gate *availableGameDataGateStub
	defer func() {
		if recover() == nil {
			t.Fatal("NewService() accepted a nil CurrentGameDataGate")
		}
	}()
	_ = team.NewService(&teamRepositoryStub{}, &acceptingCurrentMemberValidator{}, gate, snowflake.NewTestID, time.Now, nil)
}

// TestServiceValidatesAndPersistsInsideCurrentGameDataGate 验证 Team 创建先取得 Current Game Data
// 可用事务，再在同一事务 Context 内完成资料校验与持久化。
func TestServiceValidatesAndPersistsInsideCurrentGameDataGate(t *testing.T) {
	t.Parallel()

	repository := &teamRepositoryStub{}
	validator := &contextRecordingMemberValidator{}
	gate := &availableGameDataGateStub{}
	service := team.NewService(repository, validator, gate, snowflake.NewTestID, time.Now, nil)

	if _, err := service.Create(context.Background(), validServiceCreateCommand(snowflake.NewTestID(), snowflake.NewTestID(), "事务校验队伍")); err != nil {
		t.Fatalf("Create() error = %v", err)
	}
	if !gate.called || !validator.sawTransaction || !repository.sawTransaction {
		t.Fatalf("Create() gate = %t, validator transaction = %t, repository transaction = %t, want all true",
			gate.called, validator.sawTransaction, repository.sawTransaction)
	}
}

// TestServiceRejectsUnavailableCurrentGameDataBeforePersistence 验证维护状态拒绝后既不校验也不写入 Team。
func TestServiceRejectsUnavailableCurrentGameDataBeforePersistence(t *testing.T) {
	t.Parallel()

	repository := &teamRepositoryStub{}
	validator := &acceptingCurrentMemberValidator{}
	gate := &availableGameDataGateStub{err: team.ErrTeamCatalogUnavailable}
	service := team.NewService(repository, validator, gate, snowflake.NewTestID, time.Now, nil)

	_, err := service.Create(context.Background(), validServiceCreateCommand(snowflake.NewTestID(), snowflake.NewTestID(), "维护中的队伍"))
	if !errors.Is(err, team.ErrTeamCatalogUnavailable) {
		t.Fatalf("Create() error = %v, want ErrTeamCatalogUnavailable", err)
	}
	if validator.calls != 0 || repository.createCalls != 0 {
		t.Fatalf("Create() validator calls = %d, persistence calls = %d, want both 0", validator.calls, repository.createCalls)
	}
}

// TestServiceCreateReplaysFirstResultAfterCurrentGameDataBecomesInvalid 验证已经成功创建的 Team
// 即使其资料引用随后被禁用，同一幂等键和同一载荷仍返回首次结果，而不会重新校验当前资料。
func TestServiceCreateReplaysFirstResultAfterCurrentGameDataBecomesInvalid(t *testing.T) {
	t.Parallel()

	validator := &changingCurrentMemberValidator{}
	repository := &replayingTeamRepositoryStub{}
	service := team.NewService(repository, validator, &availableGameDataGateStub{}, snowflake.NewTestID, time.Now, nil)
	command := validServiceCreateCommand(snowflake.NewTestID(), snowflake.NewTestID(), "可重放创建队伍")
	command.IdempotencyKey = "create-replay-after-reference-disabled"
	command.RequestID = "create-replay-after-reference-disabled-request"

	first, err := service.Create(context.Background(), command)
	if err != nil {
		t.Fatalf("首次 Create() error = %v", err)
	}
	validator.err = team.ErrTeamReferenceInvalid
	replayed, err := service.Create(context.Background(), command)
	if err != nil {
		t.Fatalf("重放 Create() error = %v", err)
	}
	if !reflect.DeepEqual(replayed, first) {
		t.Fatalf("重放 Create() = %+v，期望首次结果 %+v", replayed, first)
	}
	if validator.calls != 1 || repository.createExecutions != 1 {
		t.Fatalf("Create() 校验次数 = %d、首次执行次数 = %d，期望均为 1", validator.calls, repository.createExecutions)
	}
}

// TestServiceUpdateReplaysFirstResultAfterTeamIsDeleted 验证更新首次成功后，即使 Team 随后被删除，
// 同一幂等键和同一载荷仍返回首次结果；重放不得因重新读取已删除 Team 或重新校验资料而失败。
func TestServiceUpdateReplaysFirstResultAfterTeamIsDeleted(t *testing.T) {
	t.Parallel()

	accountID := snowflake.NewTestID()
	playerCharacterID := snowflake.NewTestID()
	teamID := snowflake.NewTestID()
	validator := &changingCurrentMemberValidator{}
	repository := &replayingTeamRepositoryStub{owned: team.Team{
		ID: teamID, PlayerCharacterID: playerCharacterID, Name: "待更新队伍", NameKey: "待更新队伍", Version: 1,
		CreatedAt: time.Date(2026, time.August, 2, 10, 0, 0, 0, time.UTC),
	}}
	service := team.NewService(repository, validator, &availableGameDataGateStub{}, snowflake.NewTestID, time.Now, nil)
	command := team.UpdateCommand{
		AccountID: accountID, PlayerCharacterID: playerCharacterID, TeamID: teamID, ExpectedVersion: 1,
		Name: "已更新队伍", Members: validServiceMemberInputs(),
		IdempotencyKey: "update-replay-after-team-deleted", RequestID: "update-replay-after-team-deleted-request",
	}

	first, err := service.Update(context.Background(), command)
	if err != nil {
		t.Fatalf("首次 Update() error = %v", err)
	}
	repository.teamDeleted = true
	validator.err = team.ErrTeamReferenceInvalid
	replayed, err := service.Update(context.Background(), command)
	if err != nil {
		t.Fatalf("重放 Update() error = %v", err)
	}
	if !reflect.DeepEqual(replayed, first) {
		t.Fatalf("重放 Update() = %+v，期望首次结果 %+v", replayed, first)
	}
	if validator.calls != 1 || repository.updateExecutions != 1 {
		t.Fatalf("Update() 校验次数 = %d、首次执行次数 = %d，期望均为 1", validator.calls, repository.updateExecutions)
	}
}

// acceptingCurrentMemberValidator 模拟可通过当前实时资料校验的完整 Team 成员集合。
type acceptingCurrentMemberValidator struct {
	calls int
}

func (validator *acceptingCurrentMemberValidator) ValidateCurrent(context.Context, []team.Member) error {
	validator.calls++
	return nil
}

// rejectingCurrentMemberValidator 模拟当前实时资料已禁用 Team 成员引用的场景。
type rejectingCurrentMemberValidator struct {
	calls int
}

// changingCurrentMemberValidator 模拟首次校验通过、随后因 Current Game Data 变化而拒绝同一成员集合。
type changingCurrentMemberValidator struct {
	// calls 记录实际进入当前资料校验器的次数。
	calls int
	// err 是资料变化后校验器应返回的确定性错误。
	err error
}

// ValidateCurrent 在测试控制的资料状态下校验成员，并记录每次真实校验。
func (validator *changingCurrentMemberValidator) ValidateCurrent(context.Context, []team.Member) error {
	validator.calls++
	return validator.err
}

// contextRecordingMemberValidator 确认 Team 资料校验收到由 Current Game Data Gate 传播的事务 Context。
type contextRecordingMemberValidator struct {
	// sawTransaction 记录校验发生在门禁创建的事务 Context 内。
	sawTransaction bool
}

func (validator *contextRecordingMemberValidator) ValidateCurrent(ctx context.Context, _ []team.Member) error {
	validator.sawTransaction = ctx.Value(teamTransactionContextKey{}) == true
	return nil
}

// teamTransactionContextKey 是测试门禁传播事务 Context 的私有标记，避免测试依赖数据库实现细节。
type teamTransactionContextKey struct{}

// availableGameDataGateStub 模拟向 Team 校验与写入传播同一事务 Context 的 seam。
type availableGameDataGateStub struct {
	// err 是维护窗口或状态锁定失败时返回的确定性错误。
	err error
	// called 记录 Team 命令是否请求通过可用资料门禁执行。
	called bool
}

// WithinAvailable 在可用状态下为回调附加事务标记；失败时不执行回调。
func (stub *availableGameDataGateStub) WithinAvailable(ctx context.Context, work func(context.Context) error) error {
	stub.called = true
	if stub.err != nil {
		return stub.err
	}
	return work(context.WithValue(ctx, teamTransactionContextKey{}, true))
}

func (validator *rejectingCurrentMemberValidator) ValidateCurrent(context.Context, []team.Member) error {
	validator.calls++
	return team.ErrTeamReferenceInvalid
}

// validServiceCreateCommand 构造满足 Team 成员形状校验的最小创建命令。
func validServiceCreateCommand(accountID, playerCharacterID snowflake.ID, name string) team.CreateCommand {
	return team.CreateCommand{
		AccountID: accountID, PlayerCharacterID: playerCharacterID, Name: name, Members: validServiceMemberInputs(),
		IdempotencyKey: "create-valid-team", RequestID: "create-valid-team-request",
	}
}

// validServiceMemberInputs 构造可交由实时资料校验器判断的最小成员输入。
func validServiceMemberInputs() []team.MemberInput {
	return []team.MemberInput{{
		CreatureID: snowflake.NewTestID(), AbilityID: snowflake.NewTestID(), TeraElementID: snowflake.NewTestID(), NatureID: snowflake.NewTestID(), Level: 50,
		SkillIDs: []snowflake.ID{snowflake.NewTestID()},
	}}
}

type teamRepositoryStub struct {
	record      team.CreateRecord
	createCalls int
	updateCalls int
	// sawTransaction 记录持久化收到与校验器相同的事务 Context。
	sawTransaction bool
}

func (s *teamRepositoryStub) Create(ctx context.Context, record team.CreateRecord) (team.Team, error) {
	if err := record.ValidateCurrentMembers(ctx); err != nil {
		return team.Team{}, err
	}
	s.createCalls++
	s.sawTransaction = ctx.Value(teamTransactionContextKey{}) == true
	s.record = record
	value := record.Team
	value.Active = true
	s.record.Team = value
	return value, nil
}

func (s *teamRepositoryStub) GetOwned(context.Context, snowflake.ID, snowflake.ID, snowflake.ID) (team.Team, error) {
	return s.record.Team, nil
}

func (s *teamRepositoryStub) ListOwned(context.Context, snowflake.ID, snowflake.ID) ([]team.Team, error) {
	return []team.Team{s.record.Team}, nil
}

func (s *teamRepositoryStub) GetActive(context.Context, snowflake.ID, snowflake.ID) (team.ActiveBinding, error) {
	return team.ActiveBinding{PlayerCharacterID: s.record.Team.PlayerCharacterID, TeamID: s.record.Team.ID, Version: 1}, nil
}

func (s *teamRepositoryStub) Update(ctx context.Context, record team.UpdateRecord) (team.Team, error) {
	if err := record.ValidateCurrentMembers(ctx); err != nil {
		return team.Team{}, err
	}
	s.updateCalls++
	value := record.Team
	value.Version = record.ExpectedVersion + 1
	return value, nil
}

func (s *teamRepositoryStub) Delete(_ context.Context, record team.DeleteRecord) (team.DeleteResult, error) {
	return team.DeleteResult{DeletedTeamID: record.TeamID}, nil
}

func (s *teamRepositoryStub) SwitchActive(_ context.Context, record team.SwitchActiveRecord) (team.ActiveBinding, error) {
	return team.ActiveBinding{
		PlayerCharacterID: record.PlayerCharacterID, TeamID: record.TeamID,
		Version: record.ExpectedVersion + 1, UpdatedAt: record.UpdatedAt,
	}, nil
}

// replayingTeamRepositoryStub 模拟持久化 adapter：先识别已完成的幂等请求，再仅对首次执行校验当前资料。
//
// 它让领域服务测试通过公开 Create 和 Update seam 验证重放语义，而不依赖 PostgreSQL 具体实现。
type replayingTeamRepositoryStub struct {
	// owned 是首次 Update 读取到的持久 Team；重放前可被标记为删除。
	owned team.Team
	// teamDeleted 表示首次更新完成后，原 Team 已被其他生命周期操作删除。
	teamDeleted bool
	// created 和 updated 保存已提交命令的可重放结果。
	created team.Team
	updated team.Team
	// createCompleted 和 updateCompleted 表示相应幂等响应已经可以直接重放。
	createCompleted bool
	updateCompleted bool
	// createExecutions 和 updateExecutions 仅统计真正的首次持久化执行次数。
	createExecutions int
	updateExecutions int
}

// Create 先重放已完成结果；仅首次执行通过 Record 中可信校验器验证资料。
func (stub *replayingTeamRepositoryStub) Create(ctx context.Context, record team.CreateRecord) (team.Team, error) {
	if stub.createCompleted {
		return stub.created, nil
	}
	if err := record.ValidateCurrentMembers(ctx); err != nil {
		return team.Team{}, err
	}
	stub.createExecutions++
	stub.created = record.Team
	stub.created.Active = true
	stub.createCompleted = true
	return stub.created, nil
}

// GetOwned 模拟首次 Update 的 Team 所有权读取；删除后不得用于成功命令的幂等重放。
func (stub *replayingTeamRepositoryStub) GetOwned(context.Context, snowflake.ID, snowflake.ID, snowflake.ID) (team.Team, error) {
	if stub.teamDeleted {
		return team.Team{}, team.ErrTeamNotFound
	}
	return stub.owned, nil
}

// ListOwned 满足 Team Repository 查询接口；本回归测试不依赖列表结果。
func (*replayingTeamRepositoryStub) ListOwned(context.Context, snowflake.ID, snowflake.ID) ([]team.Team, error) {
	return nil, nil
}

// GetActive 满足 Team Repository 查询接口；本回归测试不依赖默认 Team 绑定。
func (*replayingTeamRepositoryStub) GetActive(context.Context, snowflake.ID, snowflake.ID) (team.ActiveBinding, error) {
	return team.ActiveBinding{}, nil
}

// Update 先重放已完成结果；首次执行才读取现存 Team 并验证新成员引用。
func (stub *replayingTeamRepositoryStub) Update(ctx context.Context, record team.UpdateRecord) (team.Team, error) {
	if stub.updateCompleted {
		return stub.updated, nil
	}
	if stub.teamDeleted {
		return team.Team{}, team.ErrTeamNotFound
	}
	if err := record.ValidateCurrentMembers(ctx); err != nil {
		return team.Team{}, err
	}
	stub.updateExecutions++
	stub.updated = stub.owned
	stub.updated.Name = record.Team.Name
	stub.updated.NameKey = record.Team.NameKey
	stub.updated.Members = record.Team.Members
	stub.updated.UpdatedAt = record.Team.UpdatedAt
	stub.updated.Version = record.ExpectedVersion + 1
	stub.updateCompleted = true
	return stub.updated, nil
}

// Delete 满足 Team Repository 写接口；本回归测试不经由该入口删除 Team。
func (*replayingTeamRepositoryStub) Delete(context.Context, team.DeleteRecord) (team.DeleteResult, error) {
	return team.DeleteResult{}, nil
}

// SwitchActive 满足 Team Repository 写接口；本回归测试不依赖默认 Team 切换。
func (*replayingTeamRepositoryStub) SwitchActive(context.Context, team.SwitchActiveRecord) (team.ActiveBinding, error) {
	return team.ActiveBinding{}, nil
}
