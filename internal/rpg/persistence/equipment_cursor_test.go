package persistence

import (
	"errors"
	"testing"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"
	rpg "github.com/lishangbu/avalon/internal/rpg"
)

// TestEquipmentCursorRoundTrip 验证装备游标完整保留类型、筛选摘要、时间和稳定身份。
func TestEquipmentCursorRoundTrip(t *testing.T) {
	t.Parallel()
	id := snowflake.MustParse("1048576001")
	timestamp := time.Date(2026, time.August, 14, 1, 2, 3, 456, time.UTC)
	cursor, err := encodeEquipmentCursor("admin-instances", "filter", id, timestamp)
	if err != nil {
		t.Fatalf("encodeEquipmentCursor() error = %v", err)
	}
	gotID, gotTime, err := decodeEquipmentCursor(cursor, "admin-instances", "filter", true)
	if err != nil || gotID != id || !gotTime.Equal(timestamp) {
		t.Fatalf("decodeEquipmentCursor() = %s, %s, %v", gotID, gotTime, err)
	}
}

// TestEquipmentCursorRejectsDifferentQuery 验证游标不能跨资源类型或筛选条件复用。
func TestEquipmentCursorRejectsDifferentQuery(t *testing.T) {
	t.Parallel()
	cursor, err := encodeEquipmentCursor("admin-instances", "first-filter", snowflake.MustParse("1048576001"), time.Now().UTC())
	if err != nil {
		t.Fatal(err)
	}
	for _, test := range []struct{ kind, filter string }{{"admin-transactions", "first-filter"}, {"admin-instances", "second-filter"}} {
		if _, _, decodeErr := decodeEquipmentCursor(cursor, test.kind, test.filter, true); !errors.Is(decodeErr, rpg.ErrInvalidEquipmentCursor) {
			t.Fatalf("decodeEquipmentCursor(%q, %q) error = %v", test.kind, test.filter, decodeErr)
		}
	}
}
