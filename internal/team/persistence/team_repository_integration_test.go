//go:build integration

package persistence_test

import (
	"bytes"
	"context"
	"errors"
	"fmt"
	"strings"
	"sync"
	"testing"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/lishangbu/avalon/internal/gamedata/teamcatalog"
	"github.com/lishangbu/avalon/internal/platform/database"
	"github.com/lishangbu/avalon/internal/platform/idempotency"
	"github.com/lishangbu/avalon/internal/platform/persistence"
	"github.com/lishangbu/avalon/internal/team"
	teampersistence "github.com/lishangbu/avalon/internal/team/persistence"
	"github.com/testcontainers/testcontainers-go/modules/postgres"
)

// teamPostgresImage 固定 Team 持久层集成测试使用的 PostgreSQL 镜像摘要，避免标签漂移改变约束行为。
const teamPostgresImage = "postgres:18.4@sha256:311136771dca6826c3b6e691ebf8cb6e896e165074bc57a728f9619f25f0c4c7"

func TestRepositoryKeepsTheFirstTeamActiveAndReplaysCreation(t *testing.T) {
	ctx, cancel := context.WithTimeout(context.Background(), 2*time.Minute)
	defer cancel()

	pool := startTeamDatabase(t, ctx)
	accountID := snowflake.MustParse("1048576131")
	playerCharacterID := snowflake.MustParse("1048576132")
	insertTeamOwner(t, ctx, pool, accountID, playerCharacterID)

	teamIDs := []snowflake.ID{
		snowflake.MustParse("1048576133"),
		snowflake.MustParse("1048576134"),
		snowflake.MustParse("1048576135"),
	}
	nextID := 0
	adapters := teampersistence.NewAdapters(pool, snowflake.NewTestID)
	service := team.NewService(adapters, acceptingCurrentMemberValidator{}, teamAvailabilityGate(pool), snowflake.TestSource(func() snowflake.ID {
		id := teamIDs[nextID]
		nextID++
		return id
	}), time.Now, pool)

	first, err := service.Create(ctx, validCreateCommand(accountID, playerCharacterID, "首发队伍", "create-first-team"))
	if err != nil || !first.Active {
		t.Fatalf("first Create() = %+v, error = %v", first, err)
	}
	replayed, err := service.Create(ctx, validCreateCommand(accountID, playerCharacterID, "首发队伍", "create-first-team"))
	if err != nil || replayed.ID != first.ID || !replayed.Active {
		t.Fatalf("replayed Create() = %+v, error = %v", replayed, err)
	}
	second, err := service.Create(ctx, validCreateCommand(accountID, playerCharacterID, "替补队伍", "create-second-team"))
	if err != nil || second.Active {
		t.Fatalf("second Create() = %+v, error = %v", second, err)
	}
	query := team.NewQueryService(adapters, adapters)
	stored, err := query.GetOwned(ctx, accountID, playerCharacterID, first.ID)
	if err != nil || stored.ID != first.ID || !stored.Active || len(stored.Members) != 1 || len(stored.Members[0].Skills) != 1 {
		t.Fatalf("GetOwned() = %+v, error = %v", stored, err)
	}
	listed, err := query.ListOwned(ctx, accountID, playerCharacterID)
	if err != nil || len(listed) != 2 || listed[0].ID != first.ID || listed[1].ID != second.ID {
		t.Fatalf("ListOwned() = %+v, error = %v", listed, err)
	}
	_, err = query.GetOwned(ctx, snowflake.NewTestID(), playerCharacterID, first.ID)
	if !errors.Is(err, team.ErrTeamNotFound) {
		t.Fatalf("other account GetOwned() error = %v, want ErrTeamNotFound", err)
	}
	_, err = query.ListOwned(ctx, snowflake.NewTestID(), playerCharacterID)
	if !errors.Is(err, team.ErrPlayerCharacterUnavailable) {
		t.Fatalf("other account ListOwned() error = %v, want ErrPlayerCharacterUnavailable", err)
	}

	updated, err := service.Update(ctx, team.UpdateCommand{
		AccountID: accountID, PlayerCharacterID: playerCharacterID, TeamID: second.ID, ExpectedVersion: second.Version,
		Name: "更新后的替补队伍", Members: validCreateCommand(
			accountID, playerCharacterID, "ignored", "ignored",
		).Members,
		IdempotencyKey: "update-second-team", RequestID: snowflake.NewTestID().String(),
	})
	if err != nil || updated.Version != 2 || updated.Name != "更新后的替补队伍" || updated.Active {
		t.Fatalf("Update() = %+v, error = %v", updated, err)
	}
	binding, err := query.GetActive(ctx, accountID, playerCharacterID)
	if err != nil || binding.TeamID != first.ID || binding.Version != 1 {
		t.Fatalf("initial GetActive() = %+v, error = %v", binding, err)
	}
	switched, err := service.SwitchActive(ctx, team.SwitchActiveCommand{
		AccountID: accountID, PlayerCharacterID: playerCharacterID, TeamID: second.ID,
		ExpectedVersion: binding.Version, IdempotencyKey: "switch-second-team", RequestID: snowflake.NewTestID().String(),
	})
	if err != nil || switched.TeamID != second.ID || switched.Version != 2 {
		t.Fatalf("SwitchActive() = %+v, error = %v", switched, err)
	}
	replayedSwitch, err := service.SwitchActive(ctx, team.SwitchActiveCommand{
		AccountID: accountID, PlayerCharacterID: playerCharacterID, TeamID: second.ID,
		ExpectedVersion: binding.Version, IdempotencyKey: "switch-second-team", RequestID: snowflake.NewTestID().String(),
	})
	if err != nil || replayedSwitch != switched {
		t.Fatalf("replayed SwitchActive() = %+v, error = %v", replayedSwitch, err)
	}
	deleted, err := service.Delete(ctx, team.DeleteCommand{
		AccountID: accountID, PlayerCharacterID: playerCharacterID, TeamID: second.ID,
		ExpectedVersion: updated.Version, IdempotencyKey: "delete-second-team", RequestID: snowflake.NewTestID().String(),
	})
	if err != nil || deleted.DeletedTeamID != second.ID || deleted.Active == nil ||
		deleted.Active.TeamID != first.ID || deleted.Active.Version != 3 {
		t.Fatalf("Delete(active second) = %+v, error = %v", deleted, err)
	}
	last, err := service.Delete(ctx, team.DeleteCommand{
		AccountID: accountID, PlayerCharacterID: playerCharacterID, TeamID: first.ID,
		ExpectedVersion: first.Version, IdempotencyKey: "delete-last-team", RequestID: snowflake.NewTestID().String(),
	})
	if err != nil || last.DeletedTeamID != first.ID || last.Active != nil {
		t.Fatalf("Delete(last) = %+v, error = %v", last, err)
	}
	_, err = query.GetActive(ctx, accountID, playerCharacterID)
	if !errors.Is(err, team.ErrTeamNotFound) {
		t.Fatalf("GetActive() after deleting last error = %v, want ErrTeamNotFound", err)
	}
}

// TestRepositoryTreatsCaseDistinctTeamNamesAsDistinctIdempotencyPayloads 验证创建、更新和分享导入的幂等请求摘要
// 必须保留规范化后的展示名称。名称唯一性使用 NameKey，但大小写不同仍是不同的可见命令输入，复用幂等键
// 时必须返回冲突，而不能把第一次成功响应误重放给第二次请求。
func TestRepositoryTreatsCaseDistinctTeamNamesAsDistinctIdempotencyPayloads(t *testing.T) {
	ctx, cancel := context.WithTimeout(context.Background(), 2*time.Minute)
	defer cancel()

	pool := startTeamDatabase(t, ctx)
	accountID := snowflake.MustParse("1048576154")
	sourceCharacterID := snowflake.MustParse("1048576155")
	targetCharacterID := snowflake.MustParse("1048576156")
	insertTeamOwner(t, ctx, pool, accountID, sourceCharacterID)
	if _, err := pool.Exec(ctx, `
        INSERT INTO player_character (
            id, account_id, display_name, display_name_key, version, created_at, updated_at
        ) VALUES ($2, $1, '幂等导入角色', '幂等导入角色', 1, now(), now())
    `, accountID, targetCharacterID); err != nil {
		t.Fatalf("创建幂等导入目标角色: %v", err)
	}

	adapters := teampersistence.NewAdapters(pool, snowflake.NewTestID)
	lifecycle := team.NewService(
		adapters,
		acceptingCurrentMemberValidator{},
		teamAvailabilityGate(pool),
		snowflake.NewTestID,
		time.Now,
		pool,
	)
	createCommand := validCreateCommand(accountID, sourceCharacterID, "Alpha", "case-distinct-team-create")
	created, err := lifecycle.Create(ctx, createCommand)
	if err != nil {
		t.Fatalf("Create() first request error = %v", err)
	}
	caseDistinctCreate := createCommand
	caseDistinctCreate.Name = "alpha"
	if _, err := lifecycle.Create(ctx, caseDistinctCreate); !errors.Is(err, idempotency.ErrConflict) {
		t.Fatalf("Create() with case-distinct name and reused idempotency key error = %v, want ErrConflict", err)
	}

	updateCommand := team.UpdateCommand{
		AccountID: accountID, PlayerCharacterID: sourceCharacterID, TeamID: created.ID, ExpectedVersion: created.Version,
		Name: "Bravo", Members: validCreateCommand(accountID, sourceCharacterID, "ignored", "ignored").Members,
		IdempotencyKey: "case-distinct-team-update", RequestID: snowflake.NewTestID().String(),
	}
	updated, err := lifecycle.Update(ctx, updateCommand)
	if err != nil {
		t.Fatalf("Update() first request error = %v", err)
	}
	caseDistinctUpdate := updateCommand
	caseDistinctUpdate.Name = "bravo"
	if _, err := lifecycle.Update(ctx, caseDistinctUpdate); !errors.Is(err, idempotency.ErrConflict) {
		t.Fatalf("Update() with case-distinct name and reused idempotency key error = %v, want ErrConflict", err)
	}

	shareCode := strings.Repeat("C", 43)
	shares := team.NewShareService(
		adapters, adapters,
		acceptingCurrentMemberValidator{},
		teamAvailabilityGate(pool),
		snowflake.NewTestID,
		func() (string, error) { return shareCode, nil },
		time.Now,
		pool)

	shared, err := shares.Create(ctx, team.CreateShareCommand{
		AccountID: accountID, PlayerCharacterID: sourceCharacterID, TeamID: updated.ID,
		ExpectedVersion: updated.Version, IdempotencyKey: "case-distinct-team-share", RequestID: snowflake.NewTestID().String(),
	})
	if err != nil {
		t.Fatalf("CreateShare() error = %v", err)
	}
	if shared.Code != shareCode {
		t.Fatalf("CreateShare() code = %q, want %q", shared.Code, shareCode)
	}
	importCommand := team.ImportShareCommand{
		AccountID: accountID, PlayerCharacterID: targetCharacterID, Code: shareCode, Name: "Charlie",
		IdempotencyKey: "case-distinct-team-import", RequestID: snowflake.NewTestID().String(),
	}
	if _, err := shares.Import(ctx, importCommand); err != nil {
		t.Fatalf("Import() first request error = %v", err)
	}
	caseDistinctImport := importCommand
	caseDistinctImport.Name = "charlie"
	if _, err := shares.Import(ctx, caseDistinctImport); !errors.Is(err, idempotency.ErrConflict) {
		t.Fatalf("Import() with case-distinct name and reused idempotency key error = %v, want ErrConflict", err)
	}
}

func TestRepositorySerializesTheTwentyTeamLimit(t *testing.T) {
	ctx, cancel := context.WithTimeout(context.Background(), 2*time.Minute)
	defer cancel()

	pool := startTeamDatabase(t, ctx)
	accountID := snowflake.MustParse("1048576141")
	playerCharacterID := snowflake.MustParse("1048576142")
	insertTeamOwner(t, ctx, pool, accountID, playerCharacterID)
	service := team.NewService(
		teampersistence.NewAdapters(pool, snowflake.NewTestID), acceptingCurrentMemberValidator{}, teamAvailabilityGate(pool), snowflake.NewTestID, time.Now, pool,
	)

	const attempts = 21
	errorsByAttempt := make(chan error, attempts)
	activeByAttempt := make(chan bool, attempts)
	var wait sync.WaitGroup
	for index := 0; index < attempts; index++ {
		wait.Add(1)
		go func(index int) {
			defer wait.Done()
			created, err := service.Create(ctx, validCreateCommand(
				accountID, playerCharacterID,
				fmt.Sprintf("并发队伍%02d", index), fmt.Sprintf("create-team-%02d", index),
			))
			errorsByAttempt <- err
			if err == nil {
				activeByAttempt <- created.Active
			}
		}(index)
	}
	wait.Wait()
	close(errorsByAttempt)
	close(activeByAttempt)

	succeeded := 0
	limited := 0
	for err := range errorsByAttempt {
		switch {
		case err == nil:
			succeeded++
		case errors.Is(err, team.ErrTeamLimitExceeded):
			limited++
		default:
			t.Fatalf("concurrent Create() unexpected error = %v", err)
		}
	}
	active := 0
	for value := range activeByAttempt {
		if value {
			active++
		}
	}
	if succeeded != 20 || limited != 1 || active != 1 {
		t.Fatalf("concurrent Create(): succeeded=%d limited=%d active=%d", succeeded, limited, active)
	}
}

func TestRepositoryEnforcesTeamOwnershipAndNameBoundaries(t *testing.T) {
	ctx, cancel := context.WithTimeout(context.Background(), 2*time.Minute)
	defer cancel()

	pool := startTeamDatabase(t, ctx)
	accountID := snowflake.MustParse("1048576143")
	firstCharacterID := snowflake.MustParse("1048576144")
	secondCharacterID := snowflake.MustParse("1048576145")
	archivedCharacterID := snowflake.MustParse("1048576146")
	insertTeamOwner(t, ctx, pool, accountID, firstCharacterID)
	_, err := pool.Exec(ctx, `
        INSERT INTO player_character (
            id, account_id, display_name, display_name_key, version, archived_at, created_at, updated_at
        ) VALUES
            ($2, $1, '第二队伍角色', '第二队伍角色', 1, NULL, now(), now()),
            ($3, $1, '归档队伍角色', '归档队伍角色', 2, now(), now(), now())
    `, accountID, secondCharacterID, archivedCharacterID)
	if err != nil {
		t.Fatalf("创建额外 Team 测试角色: %v", err)
	}
	service := team.NewService(
		teampersistence.NewAdapters(pool, snowflake.NewTestID), acceptingCurrentMemberValidator{}, teamAvailabilityGate(pool), snowflake.NewTestID, time.Now, pool,
	)
	if _, err := service.Create(ctx, validCreateCommand(
		accountID, firstCharacterID, "共享名称", "create-first-shared-name",
	)); err != nil {
		t.Fatalf("first Create() error = %v", err)
	}
	_, err = service.Create(ctx, validCreateCommand(
		accountID, firstCharacterID, "共享名称", "duplicate-name-same-character",
	))
	if !errors.Is(err, team.ErrTeamConflict) {
		t.Fatalf("same character duplicate Create() error = %v, want ErrTeamConflict", err)
	}
	if _, err := service.Create(ctx, validCreateCommand(
		accountID, secondCharacterID, "共享名称", "same-name-other-character",
	)); err != nil {
		t.Fatalf("other character same name Create() error = %v", err)
	}
	_, err = service.Create(ctx, validCreateCommand(
		accountID, archivedCharacterID, "归档角色队伍", "archived-character-team",
	))
	if !errors.Is(err, team.ErrPlayerCharacterUnavailable) {
		t.Fatalf("archived character Create() error = %v, want ErrPlayerCharacterUnavailable", err)
	}
	_, err = service.Create(ctx, validCreateCommand(
		snowflake.NewTestID(), firstCharacterID, "越权队伍", "foreign-account-team",
	))
	if !errors.Is(err, team.ErrPlayerCharacterUnavailable) {
		t.Fatalf("foreign account Create() error = %v, want ErrPlayerCharacterUnavailable", err)
	}
}

// TestRepositoryFreezesRevokesAndImportsIndependentTeamShares 验证 Team 分享快照独立冻结、撤销和导入。
func TestRepositoryFreezesRevokesAndImportsIndependentTeamShares(t *testing.T) {
	ctx, cancel := context.WithTimeout(context.Background(), 2*time.Minute)
	defer cancel()

	pool := startTeamDatabase(t, ctx)
	accountID := snowflake.MustParse("1048576147")
	sourceCharacterID := snowflake.MustParse("1048576148")
	targetCharacterID := snowflake.MustParse("1048576149")
	otherTargetCharacterID := snowflake.MustParse("1048576150")
	insertTeamOwner(t, ctx, pool, accountID, sourceCharacterID)
	_, err := pool.Exec(ctx, `
        INSERT INTO player_character (
            id, account_id, display_name, display_name_key, version, created_at, updated_at
        ) VALUES
            ($2, $1, '分享导入角色', '分享导入角色', 1, now(), now()),
            ($3, $1, '另一分享导入角色', '另一分享导入角色', 1, now(), now())
    `, accountID, targetCharacterID, otherTargetCharacterID)
	if err != nil {
		t.Fatalf("创建 Team 分享导入角色: %v", err)
	}
	adapters := teampersistence.NewAdapters(pool, snowflake.NewTestID)
	clock := time.Date(2026, time.July, 29, 8, 0, 0, 0, time.UTC)
	now := func() time.Time { return clock }
	lifecycle := team.NewService(adapters, acceptingCurrentMemberValidator{}, teamAvailabilityGate(pool), snowflake.NewTestID, now, pool)
	source, err := lifecycle.Create(ctx, validCreateCommand(
		accountID, sourceCharacterID, "冻结来源队伍", "create-share-source-team",
	))
	if err != nil {
		t.Fatalf("source Create() error = %v", err)
	}
	shareCode := strings.Repeat("A", 43)
	shares := team.NewShareService(adapters, adapters, acceptingCurrentMemberValidator{}, teamAvailabilityGate(pool), snowflake.NewTestID, func() (string, error) { return shareCode, nil }, now, pool)
	created, err := shares.Create(ctx, team.CreateShareCommand{
		AccountID: accountID, PlayerCharacterID: sourceCharacterID, TeamID: source.ID,
		ExpectedVersion: source.Version, IdempotencyKey: "create-team-share", RequestID: snowflake.NewTestID().String(),
	})
	if err != nil || created.Code != shareCode || created.Share.SourceTeamVersion != source.Version {
		t.Fatalf("share Create() = %+v, error = %v", created, err)
	}
	assertTeamShareCodeIsNotPersisted(t, ctx, pool, accountID, shareCode)
	if _, err := pool.Exec(ctx, `
        UPDATE player_character_team_share SET snapshot = '{}'::jsonb WHERE id = $1
    `, created.Share.ID); err == nil {
		t.Fatal("直接修改冻结 Team 分享快照未被数据库拒绝")
	}
	assertFrozenTeamShareFieldsAreImmutable(t, ctx, pool, created.Share.ID)
	snapshot, err := shares.Resolve(ctx, shareCode)
	if err != nil || snapshot.Name != source.Name || len(snapshot.Members) != 1 {
		t.Fatalf("Resolve() = %+v, error = %v", snapshot, err)
	}
	updated, err := lifecycle.Update(ctx, team.UpdateCommand{
		AccountID: accountID, PlayerCharacterID: sourceCharacterID, TeamID: source.ID,
		ExpectedVersion: source.Version, Name: "来源已经修改",
		Members:        validCreateCommand(accountID, sourceCharacterID, "ignored", "ignored").Members,
		IdempotencyKey: "update-share-source", RequestID: snowflake.NewTestID().String(),
	})
	if err != nil || updated.Version != 2 {
		t.Fatalf("source Update() = %+v, error = %v", updated, err)
	}
	replayedCreate, err := shares.Create(ctx, team.CreateShareCommand{
		AccountID: accountID, PlayerCharacterID: sourceCharacterID, TeamID: source.ID,
		ExpectedVersion: source.Version, IdempotencyKey: "create-team-share", RequestID: snowflake.NewTestID().String(),
	})
	if err != nil || replayedCreate.Code != "" || replayedCreate.Share.ID != created.Share.ID {
		t.Fatalf("replayed share Create() = %+v, error = %v", replayedCreate, err)
	}
	frozen, err := shares.Resolve(ctx, shareCode)
	if err != nil || frozen.Name != "冻结来源队伍" {
		t.Fatalf("Resolve() after source update = %+v, error = %v", frozen, err)
	}
	rejectedImport := team.NewShareService(adapters, adapters, rejectingCurrentMemberValidator{}, teamAvailabilityGate(pool), snowflake.NewTestID, team.NewShareCode, now, pool)
	_, err = rejectedImport.Import(ctx, team.ImportShareCommand{
		AccountID: accountID, PlayerCharacterID: targetCharacterID, Code: shareCode, Name: "失效资料副本",
		IdempotencyKey: "reject-invalid-shared-team", RequestID: snowflake.NewTestID().String(),
	})
	if !errors.Is(err, team.ErrTeamReferenceInvalid) {
		t.Fatalf("Import() with invalid current references error = %v, want ErrTeamReferenceInvalid", err)
	}
	targetTeams, err := team.NewQueryService(adapters, adapters).ListOwned(ctx, accountID, targetCharacterID)
	if err != nil || len(targetTeams) != 0 {
		t.Fatalf("ListOwned() after rejected import = %+v, error = %v", targetTeams, err)
	}
	imported, err := shares.Import(ctx, team.ImportShareCommand{
		AccountID: accountID, PlayerCharacterID: targetCharacterID, Code: shareCode, Name: "独立导入副本",
		IdempotencyKey: "import-team-share", RequestID: snowflake.NewTestID().String(),
	})
	if err != nil || imported.ID == source.ID || imported.PlayerCharacterID != targetCharacterID ||
		imported.Name != "独立导入副本" || !imported.Active {
		t.Fatalf("Import() = %+v, error = %v", imported, err)
	}
	_, err = shares.Import(ctx, team.ImportShareCommand{
		AccountID: accountID, PlayerCharacterID: otherTargetCharacterID, Code: shareCode, Name: "独立导入副本",
		IdempotencyKey: "import-team-share", RequestID: snowflake.NewTestID().String(),
	})
	if !errors.Is(err, idempotency.ErrConflict) {
		t.Fatalf("跨角色复用导入幂等键的 Import() error = %v, want ErrConflict", err)
	}
	otherTargetTeams, err := team.NewQueryService(adapters, adapters).ListOwned(ctx, accountID, otherTargetCharacterID)
	if err != nil || len(otherTargetTeams) != 0 {
		t.Fatalf("跨角色幂等冲突后的 ListOwned() = %+v, error = %v", otherTargetTeams, err)
	}
	revoked, err := shares.Revoke(ctx, team.RevokeShareCommand{
		AccountID: accountID, PlayerCharacterID: sourceCharacterID, ShareID: created.Share.ID,
		ExpectedVersion: created.Share.Version, IdempotencyKey: "revoke-team-share", RequestID: snowflake.NewTestID().String(),
	})
	if err != nil || revoked.RevokedAt == nil || revoked.Version != 2 {
		t.Fatalf("Revoke() = %+v, error = %v", revoked, err)
	}
	if _, err := pool.Exec(ctx, `
		UPDATE player_character_team_share
		SET revoked_at = revoked_at + interval '1 minute'
		WHERE id = $1
	`, created.Share.ID); err == nil {
		t.Fatal("第二次修改已撤销 Team 分享的生命周期未被数据库拒绝")
	}
	_, err = shares.Resolve(ctx, shareCode)
	if !errors.Is(err, team.ErrTeamShareNotFound) {
		t.Fatalf("Resolve() after revoke error = %v, want ErrTeamShareNotFound", err)
	}
	replayedImport, err := shares.Import(ctx, team.ImportShareCommand{
		AccountID: accountID, PlayerCharacterID: targetCharacterID, Code: shareCode, Name: "独立导入副本",
		IdempotencyKey: "import-team-share", RequestID: snowflake.NewTestID().String(),
	})
	if err != nil || replayedImport.ID != imported.ID {
		t.Fatalf("replayed Import() after revoke = %+v, error = %v", replayedImport, err)
	}
	_, err = shares.Import(ctx, team.ImportShareCommand{
		AccountID: accountID, PlayerCharacterID: targetCharacterID, Code: shareCode, Name: "撤销后新副本",
		IdempotencyKey: "import-revoked-team-share", RequestID: snowflake.NewTestID().String(),
	})
	if !errors.Is(err, team.ErrTeamShareNotFound) {
		t.Fatalf("new Import() after revoke error = %v, want ErrTeamShareNotFound", err)
	}
	expiringCode := strings.Repeat("B", 43)
	expiringShares := team.NewShareService(adapters, adapters, acceptingCurrentMemberValidator{}, teamAvailabilityGate(pool), snowflake.NewTestID, func() (string, error) { return expiringCode, nil }, now, pool)
	expiresAt := clock.Add(2 * time.Minute)
	if _, err := expiringShares.Create(ctx, team.CreateShareCommand{
		AccountID: accountID, PlayerCharacterID: sourceCharacterID, TeamID: source.ID,
		ExpectedVersion: updated.Version, ExpiresAt: &expiresAt,
		IdempotencyKey: "create-expiring-team-share", RequestID: snowflake.NewTestID().String(),
	}); err != nil {
		t.Fatalf("expiring share Create() error = %v", err)
	}
	if _, err := lifecycle.Delete(ctx, team.DeleteCommand{
		AccountID: accountID, PlayerCharacterID: sourceCharacterID, TeamID: source.ID,
		ExpectedVersion: updated.Version, IdempotencyKey: "delete-shared-source-team", RequestID: snowflake.NewTestID().String(),
	}); err != nil {
		t.Fatalf("Delete(shared source) error = %v", err)
	}
	preserved, err := expiringShares.Resolve(ctx, expiringCode)
	if err != nil || preserved.Name != "来源已经修改" {
		t.Fatalf("Resolve() after deleting source = %+v, error = %v", preserved, err)
	}
	expiringImported, err := expiringShares.Import(ctx, team.ImportShareCommand{
		AccountID: accountID, PlayerCharacterID: targetCharacterID, Code: expiringCode, Name: "到期后重放副本",
		IdempotencyKey: "import-expiring-team-share", RequestID: snowflake.NewTestID().String(),
	})
	if err != nil {
		t.Fatalf("Import() before expiry error = %v", err)
	}
	clock = clock.Add(3 * time.Minute)
	_, err = expiringShares.Resolve(ctx, expiringCode)
	if !errors.Is(err, team.ErrTeamShareNotFound) {
		t.Fatalf("Resolve() after expiry error = %v, want ErrTeamShareNotFound", err)
	}
	replayedExpiredImport, err := expiringShares.Import(ctx, team.ImportShareCommand{
		AccountID: accountID, PlayerCharacterID: targetCharacterID, Code: expiringCode, Name: "到期后重放副本",
		IdempotencyKey: "import-expiring-team-share", RequestID: snowflake.NewTestID().String(),
	})
	if err != nil || replayedExpiredImport.ID != expiringImported.ID {
		t.Fatalf("replayed Import() after expiry = %+v, error = %v", replayedExpiredImport, err)
	}
}

// TestRepositoryBlocksFirstShareImportUntilConcurrentRevocationCommits 验证首次分享导入必须锁定分享记录：
// 当撤销事务已经修改但尚未提交时，导入不能读取旧快照并创建新 Team；撤销提交后首次导入应按已失效
// 分享返回 ErrTeamShareNotFound。这个场景使用真实 PostgreSQL 事务覆盖分享撤销与跨角色导入的并发边界。
func TestRepositoryBlocksFirstShareImportUntilConcurrentRevocationCommits(t *testing.T) {
	ctx, cancel := context.WithTimeout(context.Background(), 2*time.Minute)
	defer cancel()

	pool := startTeamDatabase(t, ctx)
	accountID := snowflake.MustParse("1048576151")
	sourceCharacterID := snowflake.MustParse("1048576152")
	targetCharacterID := snowflake.MustParse("1048576153")
	insertTeamOwner(t, ctx, pool, accountID, sourceCharacterID)
	if _, err := pool.Exec(ctx, `
		INSERT INTO player_character (
			id, account_id, display_name, display_name_key, version, created_at, updated_at
		) VALUES ($2, $1, '并发导入角色', '并发导入角色', 1, now(), now())
	`, accountID, targetCharacterID); err != nil {
		t.Fatalf("创建并发导入目标角色: %v", err)
	}

	clock := time.Date(2026, time.July, 29, 9, 0, 0, 0, time.UTC)
	now := func() time.Time { return clock }
	adapters := teampersistence.NewAdapters(pool, snowflake.NewTestID)
	lifecycle := team.NewService(adapters, acceptingCurrentMemberValidator{}, teamAvailabilityGate(pool), snowflake.NewTestID, now, pool)
	source, err := lifecycle.Create(ctx, validCreateCommand(
		accountID, sourceCharacterID, "并发撤销来源", "create-concurrent-revoke-source",
	))
	if err != nil {
		t.Fatalf("创建分享来源 Team: %v", err)
	}
	shareCode := strings.Repeat("C", 43)
	shares := team.NewShareService(
		adapters, adapters,
		acceptingCurrentMemberValidator{},
		teamAvailabilityGate(pool),
		snowflake.NewTestID,
		func() (string, error) { return shareCode, nil },
		now,
		pool)

	created, err := shares.Create(ctx, team.CreateShareCommand{
		AccountID: accountID, PlayerCharacterID: sourceCharacterID, TeamID: source.ID,
		ExpectedVersion: source.Version, IdempotencyKey: "create-concurrent-revoke-share", RequestID: snowflake.NewTestID().String(),
	})
	if err != nil {
		t.Fatalf("创建并发撤销分享: %v", err)
	}

	revocationLocked := make(chan struct{})
	allowRevocationCommit := make(chan struct{})
	var allowCommitOnce sync.Once
	allowCommit := func() { allowCommitOnce.Do(func() { close(allowRevocationCommit) }) }
	defer allowCommit()
	revocationFinished := make(chan error, 1)
	go func() {
		revocationFinished <- pool.WithTx(ctx, func(tx database.Transaction) error {
			// 该更新与生产撤销命令使用相同的一次性生命周期转换，并在提交前持有分享行锁。
			tag, updateErr := tx.Exec(ctx, `
				UPDATE player_character_team_share
				SET revoked_at = $1, version = version + 1, updated_at = $1
				WHERE id = $2 AND revoked_at IS NULL
			`, clock.Add(time.Minute), created.Share.ID)
			if updateErr != nil {
				return updateErr
			}
			if tag.RowsAffected() != 1 {
				return fmt.Errorf("并发撤销没有命中分享记录")
			}
			close(revocationLocked)
			select {
			case <-allowRevocationCommit:
				return nil
			case <-ctx.Done():
				return ctx.Err()
			}
		})
	}()
	select {
	case <-revocationLocked:
	case <-ctx.Done():
		t.Fatalf("等待未提交分享撤销: %v", ctx.Err())
	}

	importFinished := make(chan error, 1)
	go func() {
		_, importErr := shares.Import(ctx, team.ImportShareCommand{
			AccountID: accountID, PlayerCharacterID: targetCharacterID, Code: shareCode, Name: "不应导入的副本",
			IdempotencyKey: "import-during-concurrent-revoke", RequestID: snowflake.NewTestID().String(),
		})
		importFinished <- importErr
	}()
	waitForFirstShareImportToBlock(t, ctx, pool, importFinished)

	allowCommit()
	if err := <-revocationFinished; err != nil {
		t.Fatalf("提交并发撤销: %v", err)
	}
	if err := <-importFinished; !errors.Is(err, team.ErrTeamShareNotFound) {
		t.Fatalf("Import() after revoke commit error = %v, want ErrTeamShareNotFound", err)
	}
	targetTeams, err := team.NewQueryService(adapters, adapters).ListOwned(ctx, accountID, targetCharacterID)
	if err != nil {
		t.Fatalf("读取撤销后的目标 Team: %v", err)
	}
	if len(targetTeams) != 0 {
		t.Fatalf("撤销提交后首次 Import() 新增了目标 Team: %+v", targetTeams)
	}
}

// waitForFirstShareImportToBlock 等待首次导入实际进入 PostgreSQL 的分享行锁等待。
//
// 仅用固定时间窗口观察 goroutine 未返回，会在调度较慢时让旧实现产生假阳性。这里通过 pg_stat_activity
// 确认导入连接正在等待 player_character_team_share 的 FOR UPDATE 锁；若旧实现无锁读取并提前返回，则立即
// 失败。该探针只读取当前测试数据库中本应用连接的活动状态，不触碰领域数据。
func waitForFirstShareImportToBlock(
	t *testing.T,
	ctx context.Context,
	pool *database.Pool,
	importFinished <-chan error,
) {
	t.Helper()
	deadline := time.NewTimer(5 * time.Second)
	defer deadline.Stop()
	poll := time.NewTicker(20 * time.Millisecond)
	defer poll.Stop()

	for {
		var waiting int
		err := pool.QueryRow(ctx, `
			SELECT count(*)
			FROM pg_stat_activity
			WHERE datname = current_database()
			  AND wait_event_type = 'Lock'
			  AND query LIKE '%player_character_team_share%'
			  AND query LIKE '%FOR UPDATE%'
		`).Scan(&waiting)
		if err != nil {
			t.Fatalf("读取首次分享导入锁等待状态: %v", err)
		}
		if waiting > 0 {
			return
		}

		select {
		case importErr := <-importFinished:
			t.Fatalf("撤销未提交时 Import() 提前返回 %v，期望等待分享行锁", importErr)
		case <-deadline.C:
			t.Fatal("首次 Import() 未进入分享行锁等待，无法证明撤销与导入已串行化")
		case <-poll.C:
		}
	}
}

// acceptingCurrentMemberValidator 让存储集成测试聚焦 Team 事务语义，资料引用校验由领域单元测试覆盖。
type acceptingCurrentMemberValidator struct{}

func (acceptingCurrentMemberValidator) ValidateCurrent(context.Context, []team.Member) error {
	return nil
}

// rejectingCurrentMemberValidator 模拟当前实时资料已禁用 Team 成员引用的场景。
type rejectingCurrentMemberValidator struct{}

func (rejectingCurrentMemberValidator) ValidateCurrent(context.Context, []team.Member) error {
	return team.ErrTeamReferenceInvalid
}

// assertFrozenTeamShareFieldsAreImmutable 覆盖数据库触发器对删除和全部冻结字段篡改的拒绝行为。
func assertFrozenTeamShareFieldsAreImmutable(t *testing.T, ctx context.Context, pool *database.Pool, shareID snowflake.ID) {
	t.Helper()

	updates := []struct {
		name      string
		statement string
	}{
		{name: "删除", statement: `DELETE FROM player_character_team_share WHERE id = $1`},
		{name: "来源队伍", statement: `UPDATE player_character_team_share SET source_team_id = 1048576999 WHERE id = $1`},
		{name: "来源版本", statement: `UPDATE player_character_team_share SET source_team_version = source_team_version + 1 WHERE id = $1`},
		{name: "分享码摘要", statement: `UPDATE player_character_team_share SET code_digest = decode(repeat('00', 32), 'hex') WHERE id = $1`},
		{name: "快照结构版本", statement: `UPDATE player_character_team_share SET schema_version = schema_version + 1 WHERE id = $1`},
		{name: "到期时间", statement: `UPDATE player_character_team_share SET expires_at = expires_at + interval '1 minute' WHERE id = $1`},
		{name: "创建时间", statement: `UPDATE player_character_team_share SET created_at = created_at + interval '1 minute' WHERE id = $1`},
		{name: "乐观版本", statement: `UPDATE player_character_team_share SET version = version + 1 WHERE id = $1`},
		{name: "更新时间", statement: `UPDATE player_character_team_share SET updated_at = updated_at + interval '1 minute' WHERE id = $1`},
	}
	for _, update := range updates {
		t.Run(update.name, func(t *testing.T) {
			if _, err := pool.Exec(ctx, update.statement, shareID); err == nil {
				t.Fatalf("篡改 Team 分享%s未被数据库拒绝", update.name)
			}
		})
	}
}

// assertTeamShareCodeIsNotPersisted 证明分享码明文不会进入幂等响应或审计 JSON；数据库只保存其摘要。
func assertTeamShareCodeIsNotPersisted(
	t *testing.T,
	ctx context.Context,
	pool *database.Pool,
	accountID snowflake.ID,
	shareCode string,
) {
	t.Helper()

	var idempotencyResponse []byte
	if err := pool.QueryRow(ctx, `
		SELECT response::text
		FROM administration_idempotency_record
		WHERE actor_account_id = $1
		  AND operation_id = 'team.share.create'
		  AND idempotency_key = 'create-team-share'
	`, accountID).Scan(&idempotencyResponse); err != nil {
		t.Fatalf("读取 Team 分享创建幂等响应: %v", err)
	}
	if bytes.Contains(idempotencyResponse, []byte(shareCode)) {
		t.Fatal("Team 分享码明文进入了幂等响应")
	}

	var auditChanges []byte
	if err := pool.QueryRow(ctx, `
		SELECT changes::text
		FROM administration_audit_log
		WHERE action_code = 'team.share-created'
		ORDER BY sequence DESC
		LIMIT 1
	`).Scan(&auditChanges); err != nil {
		t.Fatalf("读取 Team 分享创建审计: %v", err)
	}
	if bytes.Contains(auditChanges, []byte(shareCode)) {
		t.Fatal("Team 分享码明文进入了审计记录")
	}
}

// validCreateCommand 构造满足成员形状约束的最小 Team 创建命令，供持久层事务测试复用。
func validCreateCommand(accountID, playerCharacterID snowflake.ID, name, key string) team.CreateCommand {
	return team.CreateCommand{
		AccountID:         accountID,
		PlayerCharacterID: playerCharacterID,
		Name:              name,
		Members: []team.MemberInput{{
			CreatureID:    snowflake.MustParse("1048576137"),
			AbilityID:     snowflake.MustParse("1048576138"),
			TeraElementID: snowflake.MustParse("1048576139"),
			NatureID:      snowflake.MustParse("1048576213"),
			Level:         50,
			SkillIDs:      []snowflake.ID{snowflake.MustParse("1048576140")},
		}},
		IdempotencyKey: key,
		RequestID:      snowflake.NewTestID().String(),
	}
}

// teamAvailabilityGate 为 Team 持久层集成测试提供与生产装配一致的事务边界。
func teamAvailabilityGate(pool *database.Pool) team.CurrentGameDataGate {
	return teamcatalog.NewAvailabilityGate(pool)
}

// startTeamDatabase 在独立 PostgreSQL 容器中创建 Ent Schema，并返回自动清理的应用连接池。
func startTeamDatabase(t *testing.T, ctx context.Context) *database.Pool {
	t.Helper()
	container, err := postgres.Run(
		ctx,
		teamPostgresImage,
		postgres.WithDatabase("avalon_team_test"),
		postgres.WithUsername("avalon"),
		postgres.WithPassword("avalon"),
		postgres.BasicWaitStrategies(),
	)
	if err != nil {
		t.Fatalf("启动 PostgreSQL: %v", err)
	}
	t.Cleanup(func() {
		if err := container.Terminate(context.Background()); err != nil {
			t.Errorf("停止 PostgreSQL: %v", err)
		}
	})
	databaseURL, err := container.ConnectionString(ctx, "sslmode=disable")
	if err != nil {
		t.Fatalf("读取 PostgreSQL 地址: %v", err)
	}
	pool, err := database.Open(database.Config{URL: databaseURL, MaxOpenConnections: 20, MaxIdleConnections: 10})
	if err != nil {
		t.Fatalf("打开数据库: %v", err)
	}
	t.Cleanup(pool.Close)
	if err := pool.Persistence().ApplySchema(ctx, persistence.SchemaModeCreate); err != nil {
		t.Fatalf("创建 Ent Schema: %v", err)
	}
	if _, err := pool.Exec(ctx, `INSERT INTO audit_hash_chain_state (id, ledger, latest_hash, updated_at) VALUES (1048577, 'admin_audit_log', ''::bytea, now()), (1048578, 'administration_audit_log', ''::bytea, now())`); err != nil {
		t.Fatalf("初始化 Team 审计哈希链: %v", err)
	}
	return pool
}

// insertTeamOwner 写入可拥有 Team 的最小玩家账号与 PlayerCharacter，不绕过 Team 事务本身的不变量。
func insertTeamOwner(
	t *testing.T,
	ctx context.Context,
	pool *database.Pool,
	accountID, playerCharacterID snowflake.ID,
) {
	t.Helper()
	_, err := pool.Exec(ctx, `
        INSERT INTO account (
            id, username, username_key, display_name, password_hash, password_algorithm,
            password_parameters, status, security_version, created_at, updated_at
        ) VALUES ($1, 'team-owner', 'team-owner', '队伍账号', 'test', 'argon2id', '{}',
                  'active', 1, now(), now())
    `, accountID)
	if err != nil {
		t.Fatalf("创建 Team 测试账号: %v", err)
	}
	_, err = pool.Exec(ctx, `
        INSERT INTO player_character (
            id, account_id, display_name, display_name_key, version, created_at, updated_at
        ) VALUES ($2, $1, '队伍角色', '队伍角色', 1, now(), now())
    `, accountID, playerCharacterID)
	if err != nil {
		t.Fatalf("创建 Team 测试角色: %v", err)
	}
	// Ent edge 迁移后，Team 夹具必须先建立完整的 Current Game Data 外键事实。
	elementID := snowflake.MustParse("1048576139")
	speciesID := snowflake.MustParse("1048576136")
	creatureID := snowflake.MustParse("1048576137")
	abilityID := snowflake.MustParse("1048576138")
	natureID := snowflake.MustParse("1048576213")
	skillID := snowflake.MustParse("1048576140")
	statements := []struct {
		query string
		args  []any
	}{
		{`INSERT INTO game_element (id, code, name, sort_order, enabled, version, created_at, updated_at) VALUES ($1, 'test-element', '测试属性', 1, true, 1, now(), now())`, []any{elementID}},
		{`INSERT INTO game_species (id, national_dex_number, code, name, gender_differences, forms_switchable, enabled, version, created_at, updated_at) VALUES ($1, 1, 'test-species', '测试物种', false, false, true, 1, now(), now())`, []any{speciesID}},
		{`INSERT INTO game_creature (id, code, name, species_id, male_eighths, female_eighths, default_form, enabled, version, created_at, updated_at) VALUES ($1, 'test-creature', '测试生物', $2, 0, 0, true, true, 1, now(), now())`, []any{creatureID, speciesID}},
		{`INSERT INTO game_ability (id, code, name, main_series, rules, enabled, version, created_at, updated_at) VALUES ($1, 'test-ability', '测试特性', true, '{}'::jsonb, true, 1, now(), now())`, []any{abilityID}},
		{`INSERT INTO game_skill (id, code, name, element_id, priority, rules, enabled, version, created_at, updated_at) VALUES ($1, 'test-skill', '测试技能', $2, 0, '{}'::jsonb, true, 1, now(), now())`, []any{skillID, elementID}},
		{`INSERT INTO game_nature (id, code, name, enabled, version, created_at, updated_at) VALUES ($1, 'test-nature', '测试性格', true, 1, now(), now())`, []any{natureID}},
	}
	for _, statement := range statements {
		if _, err := pool.Exec(ctx, statement.query, statement.args...); err != nil {
			t.Fatalf("创建 Team 游戏资料夹具: %v", err)
		}
	}
}
