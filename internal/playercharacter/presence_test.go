package playercharacter_test

import (
	"context"
	"testing"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/lishangbu/avalon/internal/playercharacter"
)

func TestPresenceRegistryKeepsCharacterOnlineUntilEveryConnectionExpires(t *testing.T) {
	t.Parallel()

	characterID := snowflake.MustParse("1048576083")
	firstConnection := snowflake.MustParse("1048576084")
	secondConnection := snowflake.MustParse("1048576085")
	now := time.Date(2026, time.July, 29, 4, 15, 0, 0, time.UTC)
	registry := playercharacter.NewPresenceRegistry(time.Minute)

	registry.Open(characterID, firstConnection, now)
	registry.Open(characterID, secondConnection, now.Add(30*time.Second))
	registry.Close(characterID, firstConnection)
	if !registry.Online(characterID, now.Add(70*time.Second)) {
		t.Fatal("仍有一个未超时连接时 PlayerCharacter 应保持在线")
	}
	if registry.Online(characterID, now.Add(91*time.Second)) {
		t.Fatal("全部连接超时后 PlayerCharacter 不应继续在线")
	}
}

func TestPresenceServiceOnlyRefreshesPersistedActiveCharacter(t *testing.T) {
	t.Parallel()

	accountID := snowflake.MustParse("1048576086")
	characterID := snowflake.MustParse("1048576087")
	connectionID := snowflake.MustParse("1048576088")
	now := time.Date(2026, time.July, 29, 4, 20, 0, 0, time.UTC)
	repository := &activeAdaptersStub{current: playercharacter.ActiveBinding{
		AccountID: accountID, PlayerCharacterID: characterID, Version: 1, UpdatedAt: now,
	}}
	registry := playercharacter.NewPresenceRegistry(time.Minute)
	service := playercharacter.NewPresenceService(repository, registry, func() time.Time { return now })

	binding, err := service.Heartbeat(context.Background(), accountID, connectionID)
	if err != nil {
		t.Fatalf("Heartbeat() error = %v", err)
	}
	if binding.PlayerCharacterID != characterID {
		t.Fatalf("Heartbeat() binding = %+v", binding)
	}
	if !registry.Online(characterID, now) {
		t.Fatal("心跳没有为持久活动角色建立 Presence")
	}
}
