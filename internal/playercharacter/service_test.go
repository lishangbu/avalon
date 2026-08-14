package playercharacter_test

import (
	"context"
	"testing"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/lishangbu/avalon/internal/playercharacter"
)

func TestServiceCreatesFirstPlayerCharacterForAccount(t *testing.T) {
	t.Parallel()

	accountID := snowflake.MustParse("1048576060")
	characterID := snowflake.MustParse("1048576061")
	now := time.Date(2026, time.July, 29, 3, 0, 0, 0, time.UTC)
	repository := &playerCharacterRepositoryStub{}
	service := playercharacter.NewService(
		repository,
		snowflake.TestSource(func() snowflake.ID { return characterID }),
		func() time.Time { return now },
	)

	created, err := service.Create(context.Background(), playercharacter.CreateCommand{
		AccountID:      accountID,
		DisplayName:    "  Ａｖａｌｏｎ_一号  ",
		IdempotencyKey: "create-first-character",
		RequestID:      "create-first-character-request",
	})
	if err != nil {
		t.Fatalf("Create() error = %v", err)
	}
	if created.ID != characterID || created.AccountID != accountID || created.DisplayName != "Avalon_一号" ||
		created.DisplayNameKey != "avalon_一号" || created.Version != 1 || created.ArchivedAt != nil ||
		!created.CreatedAt.Equal(now) || !created.UpdatedAt.Equal(now) {
		t.Fatalf("Create() = %+v", created)
	}
	if repository.accountID != accountID || repository.created.PlayerCharacter != created ||
		repository.created.ModerationKey != "avalon一号" ||
		repository.created.IdempotencyKey != "create-first-character" ||
		repository.created.RequestID != "create-first-character-request" {
		t.Fatalf("Create record = %+v, accountID = %s", repository.created, repository.accountID)
	}
}

func TestServiceRenamesPlayerCharacterWithoutChangingStableIdentity(t *testing.T) {
	t.Parallel()

	accountID := snowflake.MustParse("1048576062")
	characterID := snowflake.MustParse("1048576063")
	now := time.Date(2026, time.July, 29, 3, 15, 0, 0, time.UTC)
	repository := &playerCharacterRepositoryStub{}
	service := playercharacter.NewService(repository, snowflake.NewTestID, func() time.Time { return now })

	renamed, err := service.Rename(context.Background(), playercharacter.RenameCommand{
		AccountID:         accountID,
		PlayerCharacterID: characterID,
		ExpectedVersion:   3,
		DisplayName:       "  星界_二号  ",
		IdempotencyKey:    "rename-character",
		RequestID:         "rename-character-request",
	})
	if err != nil {
		t.Fatalf("Rename() error = %v", err)
	}
	if renamed.ID != characterID || renamed.AccountID != accountID || renamed.DisplayName != "星界_二号" ||
		renamed.DisplayNameKey != "星界_二号" || renamed.Version != 4 || !renamed.UpdatedAt.Equal(now) {
		t.Fatalf("Rename() = %+v", renamed)
	}
	if repository.renamed.PlayerCharacterID != characterID || repository.renamed.ExpectedVersion != 3 ||
		repository.renamed.DisplayName != "星界_二号" || repository.renamed.DisplayNameKey != "星界_二号" ||
		repository.renamed.ModerationKey != "星界二号" || repository.renamed.IdempotencyKey != "rename-character" ||
		repository.renamed.RequestID != "rename-character-request" || !repository.renamed.UpdatedAt.Equal(now) {
		t.Fatalf("Rename record = %+v", repository.renamed)
	}
}

func TestServiceArchivesPlayerCharacterWithoutDeletingIdentity(t *testing.T) {
	t.Parallel()

	accountID := snowflake.MustParse("1048576064")
	characterID := snowflake.MustParse("1048576065")
	now := time.Date(2026, time.July, 29, 3, 30, 0, 0, time.UTC)
	repository := &playerCharacterRepositoryStub{}
	presence := playercharacter.NewPresenceRegistry(time.Minute)
	presence.Open(characterID, snowflake.NewTestID(), now)
	service := playercharacter.NewServiceWithPresence(repository, presence, snowflake.NewTestID, func() time.Time { return now })

	archived, err := service.Archive(context.Background(), playercharacter.ArchiveCommand{
		AccountID:         accountID,
		PlayerCharacterID: characterID,
		ExpectedVersion:   4,
		IdempotencyKey:    "archive-character",
		RequestID:         "archive-character-request",
	})
	if err != nil {
		t.Fatalf("Archive() error = %v", err)
	}
	if archived.ID != characterID || archived.AccountID != accountID || archived.Version != 5 ||
		archived.ArchivedAt == nil || !archived.ArchivedAt.Equal(now) || !archived.UpdatedAt.Equal(now) {
		t.Fatalf("Archive() = %+v", archived)
	}
	if repository.archived.PlayerCharacterID != characterID || repository.archived.ExpectedVersion != 4 ||
		repository.archived.IdempotencyKey != "archive-character" ||
		repository.archived.RequestID != "archive-character-request" || !repository.archived.ArchivedAt.Equal(now) {
		t.Fatalf("Archive record = %+v", repository.archived)
	}
	if presence.Online(characterID, now) {
		t.Fatal("归档后 PlayerCharacter Presence 未由应用服务清除")
	}
}

func TestServiceRestoresPlayerCharacterWithinActiveLimit(t *testing.T) {
	t.Parallel()

	accountID := snowflake.MustParse("1048576066")
	characterID := snowflake.MustParse("1048576067")
	now := time.Date(2026, time.July, 29, 3, 45, 0, 0, time.UTC)
	repository := &playerCharacterRepositoryStub{}
	service := playercharacter.NewService(repository, snowflake.NewTestID, func() time.Time { return now })

	restored, err := service.Restore(context.Background(), playercharacter.RestoreCommand{
		AccountID:         accountID,
		PlayerCharacterID: characterID,
		ExpectedVersion:   5,
		IdempotencyKey:    "restore-character",
		RequestID:         "restore-character-request",
	})
	if err != nil {
		t.Fatalf("Restore() error = %v", err)
	}
	if restored.ID != characterID || restored.AccountID != accountID || restored.Version != 6 ||
		restored.ArchivedAt != nil || !restored.UpdatedAt.Equal(now) {
		t.Fatalf("Restore() = %+v", restored)
	}
	if repository.restored.PlayerCharacterID != characterID || repository.restored.ExpectedVersion != 5 ||
		repository.restored.IdempotencyKey != "restore-character" ||
		repository.restored.RequestID != "restore-character-request" || !repository.restored.RestoredAt.Equal(now) {
		t.Fatalf("Restore record = %+v", repository.restored)
	}
}

type playerCharacterRepositoryStub struct {
	accountID snowflake.ID
	created   playercharacter.CreateRecord
	renamed   playercharacter.RenameRecord
	archived  playercharacter.ArchiveRecord
	restored  playercharacter.RestoreRecord
}

func (s *playerCharacterRepositoryStub) WithinAccount(
	_ context.Context,
	accountID snowflake.ID,
	work func(playercharacter.Writer) error,
) error {
	s.accountID = accountID
	return work(s)
}

func (s *playerCharacterRepositoryStub) Create(
	_ context.Context,
	record playercharacter.CreateRecord,
) (playercharacter.PlayerCharacter, error) {
	s.created = record
	return record.PlayerCharacter, nil
}

func (s *playerCharacterRepositoryStub) Rename(
	_ context.Context,
	record playercharacter.RenameRecord,
) (playercharacter.PlayerCharacter, error) {
	s.renamed = record
	return playercharacter.PlayerCharacter{
		ID: record.PlayerCharacterID, AccountID: record.AccountID,
		DisplayName: record.DisplayName, DisplayNameKey: record.DisplayNameKey,
		Version: record.ExpectedVersion + 1, UpdatedAt: record.UpdatedAt,
	}, nil
}

func (s *playerCharacterRepositoryStub) Archive(
	_ context.Context,
	record playercharacter.ArchiveRecord,
) (playercharacter.PlayerCharacter, error) {
	s.archived = record
	archivedAt := record.ArchivedAt
	return playercharacter.PlayerCharacter{
		ID: record.PlayerCharacterID, AccountID: record.AccountID,
		Version: record.ExpectedVersion + 1, ArchivedAt: &archivedAt, UpdatedAt: record.ArchivedAt,
	}, nil
}

func (s *playerCharacterRepositoryStub) Restore(
	_ context.Context,
	record playercharacter.RestoreRecord,
) (playercharacter.PlayerCharacter, error) {
	s.restored = record
	return playercharacter.PlayerCharacter{
		ID: record.PlayerCharacterID, AccountID: record.AccountID,
		Version: record.ExpectedVersion + 1, UpdatedAt: record.RestoredAt,
	}, nil
}
