package stablecode_test

import (
	"testing"

	"github.com/lishangbu/avalon/internal/gamedata/stablecode"
)

func TestValidAcceptsFrozenTwoCharacterGameDataCode(t *testing.T) {
	t.Parallel()

	if !stablecode.Valid("hp") {
		t.Fatal("Valid(\"hp\") = false, want true")
	}
	if stablecode.Valid("h") {
		t.Fatal("Valid(\"h\") = true, want false")
	}
}
