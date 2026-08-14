package team_test

import (
	"context"
	"errors"
	"strings"
	"testing"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/lishangbu/avalon/internal/team"
)

// TestShareServiceRevalidatesFrozenSnapshotBeforeImport 确认分享快照虽然不可变，
// 但导入为新 Team 前仍必须按当前实时资料重新校验。
func TestShareServiceRevalidatesFrozenSnapshotBeforeImport(t *testing.T) {
	t.Parallel()

	repository := &shareAdaptersStub{snapshot: team.ShareSnapshot{
		SchemaVersion: team.TeamShareSchemaVersion,
		Members: []team.Member{{
			Position: 1, CreatureID: snowflake.NewTestID(), AbilityID: snowflake.NewTestID(), TeraElementID: snowflake.NewTestID(),
			Skills: []team.MemberSkill{{Position: 1, SkillID: snowflake.NewTestID()}},
		}},
	}}
	validator := team.NewCatalogValidator(&catalogReaderStub{})
	service := team.NewShareService(repository, repository, validator, &availableGameDataGateStub{}, snowflake.NewTestID, team.NewShareCode, time.Now, nil)

	_, err := service.Import(context.Background(), team.ImportShareCommand{
		AccountID: snowflake.NewTestID(), PlayerCharacterID: snowflake.NewTestID(), Code: strings.Repeat("A", 43), Name: "导入副本",
		IdempotencyKey: "revalidate-shared-team", RequestID: "revalidate-shared-team-request",
	})
	if !errors.Is(err, team.ErrTeamReferenceInvalid) {
		t.Fatalf("Import() error = %v, want ErrTeamReferenceInvalid", err)
	}
	if !repository.importAttempted {
		t.Fatal("Import() did not reach the transaction-bound share import")
	}
	if repository.persisted {
		t.Fatal("Import() persisted a Team whose frozen references are no longer valid")
	}
}

// TestNewShareServiceRejectsMissingRealtimeDependencies 确认生产路径不能因为遗漏资料校验器或维护锁门禁
// 而把分享快照绕过 Current Game Data 校验直接写入 Team。
func TestNewShareServiceRejectsMissingRealtimeDependencies(t *testing.T) {
	t.Parallel()

	t.Run("资料校验器", func(t *testing.T) {
		defer func() {
			if recover() == nil {
				t.Fatal("NewShareService() accepted a nil CurrentMemberValidator")
			}
		}()
		_ = team.NewShareService(&shareAdaptersStub{}, &shareAdaptersStub{}, nil, &availableGameDataGateStub{}, snowflake.NewTestID, team.NewShareCode, time.Now, nil)
	})
	t.Run("可用资料门禁", func(t *testing.T) {
		defer func() {
			if recover() == nil {
				t.Fatal("NewShareService() accepted a nil CurrentGameDataGate")
			}
		}()
		_ = team.NewShareService(&shareAdaptersStub{}, &shareAdaptersStub{}, &acceptingCurrentMemberValidator{}, nil, snowflake.NewTestID, team.NewShareCode, time.Now, nil)
	})
}

// TestShareServiceRejectsUnavailableCurrentGameDataBeforeImport 验证维护状态打开时不解析、校验或写入分享快照。
func TestShareServiceRejectsUnavailableCurrentGameDataBeforeImport(t *testing.T) {
	t.Parallel()

	repository := &shareAdaptersStub{}
	gate := &availableGameDataGateStub{err: team.ErrTeamCatalogUnavailable}
	service := team.NewShareService(repository, repository, &acceptingCurrentMemberValidator{}, gate, snowflake.NewTestID, team.NewShareCode, time.Now, nil)
	_, err := service.Import(context.Background(), team.ImportShareCommand{
		AccountID: snowflake.NewTestID(), PlayerCharacterID: snowflake.NewTestID(), Code: strings.Repeat("A", 43), Name: "维护中的导入",
		IdempotencyKey: "transaction-team-import", RequestID: "transaction-team-import-request",
	})
	if !errors.Is(err, team.ErrTeamCatalogUnavailable) {
		t.Fatalf("Import() error = %v, want ErrTeamCatalogUnavailable", err)
	}
	if repository.importAttempted {
		t.Fatal("Import() reached persistence while Current Game Data was unavailable")
	}
}

// TestShareServiceReadsImportTimeAfterEnteringCurrentGameDataGate 确认分享首次导入使用取得 Current Game Data
// 可用锁后的时间。这样等待维护窗口释放期间过期的分享，持久化层会按实际导入时刻拒绝它。
func TestShareServiceReadsImportTimeAfterEnteringCurrentGameDataGate(t *testing.T) {
	t.Parallel()

	requestedAt := time.Date(2026, time.August, 2, 10, 0, 0, 0, time.UTC)
	availableAt := requestedAt.Add(time.Minute)
	currentTime := requestedAt
	repository := &shareAdaptersStub{}
	gate := &clockAdvancingCurrentGameDataGateStub{
		advance: func() {
			currentTime = availableAt
		},
	}
	service := team.NewShareService(
		repository, repository,
		&acceptingCurrentMemberValidator{},
		gate,
		snowflake.NewTestID,
		team.NewShareCode,
		func() time.Time { return currentTime },
		nil)

	_, err := service.Import(context.Background(), team.ImportShareCommand{
		AccountID: snowflake.NewTestID(), PlayerCharacterID: snowflake.NewTestID(), Code: strings.Repeat("A", 43), Name: "门禁后的导入",
		IdempotencyKey: "import-time-after-current-game-data-gate", RequestID: "import-time-after-current-game-data-gate-request",
	})
	if err != nil {
		t.Fatalf("Import() error = %v", err)
	}
	if got, want := repository.importedAt, availableAt; !got.Equal(want) {
		t.Fatalf("ImportShareRecord.ImportedAt = %s, want Current Game Data gate time %s", got, want)
	}
}

// shareAdaptersStub 在领域服务测试中模拟读写适配器，并在快照解析后执行生产代码传入的校验器。
type shareAdaptersStub struct {
	// snapshot 是 Reader 从不可变分享记录解析出的冻结事实。
	snapshot team.ShareSnapshot
	// importAttempted 标记导入服务是否已进入存储事务边界。
	importAttempted bool
	// persisted 标记快照通过资料校验后是否会被写为独立 Team。
	persisted bool
	// importedAt 记录应用服务交给持久化层的首次导入时间。
	importedAt time.Time
}

func (*shareAdaptersStub) CreateShare(context.Context, team.CreateShareRecord) (team.CreateShareResult, error) {
	return team.CreateShareResult{}, nil
}

func (s *shareAdaptersStub) ResolveShare(context.Context, []byte, time.Time) (team.ShareSnapshot, error) {
	return s.snapshot, nil
}

func (*shareAdaptersStub) RevokeShare(context.Context, team.RevokeShareRecord) (team.Share, error) {
	return team.Share{}, nil
}

func (s *shareAdaptersStub) ImportShare(ctx context.Context, record team.ImportShareRecord) (team.Team, error) {
	s.importAttempted = true
	s.importedAt = record.ImportedAt
	if err := record.ValidateCurrentSnapshot(ctx, s.snapshot.Members); err != nil {
		return team.Team{}, err
	}
	s.persisted = true
	return record.Team, nil
}

// clockAdvancingCurrentGameDataGateStub 模拟等待维护窗口释放后才取得 Current Game Data 可用行锁的场景。
type clockAdvancingCurrentGameDataGateStub struct {
	// advance 在把执行权交给 Team 导入回调前推进受控时钟。
	advance func()
}

// WithinAvailable 在模拟门禁取得可用行锁后才执行导入工作。
func (stub *clockAdvancingCurrentGameDataGateStub) WithinAvailable(ctx context.Context, work func(context.Context) error) error {
	stub.advance()
	return work(ctx)
}
