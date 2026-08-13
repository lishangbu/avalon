package account_test

import (
	"errors"
	"strings"
	"testing"

	"github.com/lishangbu/avalon/internal/security/account"
)

func TestParseDisplayNameKeepsAccountDisplayIdentitySeparateFromUsername(t *testing.T) {
	t.Parallel()

	displayName, err := account.ParseDisplayName("  Avalon 管理员  ")
	if err != nil {
		t.Fatalf("ParseDisplayName() error = %v", err)
	}
	if displayName.String() != "Avalon 管理员" {
		t.Fatalf("DisplayName = %q", displayName.String())
	}
}

func TestParseDisplayNameRejectsInvalidValues(t *testing.T) {
	t.Parallel()

	for _, input := range []string{"", "\n", "管\u0000理员", strings.Repeat("管", 65)} {
		if _, err := account.ParseDisplayName(input); !errors.Is(err, account.ErrInvalidDisplayName) {
			t.Errorf("ParseDisplayName(%q) error = %v", input, err)
		}
	}
}
