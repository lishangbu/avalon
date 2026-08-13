package authentication_test

import (
	"context"
	"testing"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/lishangbu/avalon/internal/security/authentication"
)

func TestLogoutServiceRevokesAuthenticatedSessionFamily(t *testing.T) {
	t.Parallel()

	familyID := snowflake.MustParse("1048576204")
	store := &recordingLogoutStore{}
	service := authentication.NewLogoutService(store, func() time.Time {
		return time.Unix(60, 0).UTC()
	})

	if err := service.Logout(context.Background(), authentication.Principal{
		SessionFamilyID: familyID,
	}); err != nil {
		t.Fatalf("Logout() error = %v", err)
	}
	if store.familyID != familyID {
		t.Errorf("family ID = %s, want %s", store.familyID, familyID)
	}
}

type recordingLogoutStore struct {
	familyID snowflake.ID
}

func (s *recordingLogoutStore) RevokeSessionFamily(
	_ context.Context,
	familyID snowflake.ID,
	_ time.Time,
) error {
	s.familyID = familyID
	return nil
}
