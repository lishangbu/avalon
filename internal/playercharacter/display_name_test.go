package playercharacter_test

import (
	"errors"
	"testing"

	"github.com/lishangbu/avalon/internal/playercharacter"
)

func TestParseDisplayNameNormalizesWidthCaseAndSurroundingWhitespace(t *testing.T) {
	t.Parallel()

	displayName, err := playercharacter.ParseDisplayName("  Ａｖａｌｏｎ_一号  ")
	if err != nil {
		t.Fatalf("ParseDisplayName() error = %v", err)
	}
	if displayName.String() != "Avalon_一号" {
		t.Errorf("String() = %q, want %q", displayName.String(), "Avalon_一号")
	}
	if displayName.Key() != "avalon_一号" {
		t.Errorf("Key() = %q, want %q", displayName.Key(), "avalon_一号")
	}
	if displayName.ModerationKey() != "avalon一号" {
		t.Errorf("ModerationKey() = %q, want %q", displayName.ModerationKey(), "avalon一号")
	}
}

func TestParseDisplayNameRejectsInvalidLengthAndPunctuation(t *testing.T) {
	t.Parallel()

	for _, input := range []string{"a", "valid.name", "角色\n名称", "12345678901234567"} {
		if _, err := playercharacter.ParseDisplayName(input); !errors.Is(err, playercharacter.ErrInvalidDisplayName) {
			t.Errorf("ParseDisplayName(%q) error = %v, want ErrInvalidDisplayName", input, err)
		}
	}
}
