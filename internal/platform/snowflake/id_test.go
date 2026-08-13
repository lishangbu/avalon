package snowflake

import (
	"context"
	"encoding/json"
	"errors"
	"testing"
	"time"
)

func TestParseRequiresCanonicalPositiveDecimal(t *testing.T) {
	for _, raw := range []string{"", "0", "01", "+1", "-1", "1.0", "9223372036854775808"} {
		if _, err := Parse(raw); !errors.Is(err, ErrInvalidID) {
			t.Fatalf("Parse(%q) error = %v, want ErrInvalidID", raw, err)
		}
	}
	id, err := Parse("123456789")
	if err != nil || id.String() != "123456789" {
		t.Fatalf("Parse() = %v, %v", id, err)
	}
}

// TestIDJSONUsesCanonicalDecimalString 验证 JSON 边界始终使用不透明十进制字符串，
// 并拒绝会在 JavaScript 中丢失精度的数值表示和旧 UUID 表示。
func TestIDJSONUsesCanonicalDecimalString(t *testing.T) {
	t.Parallel()

	id := MustParse("9007199254740993")
	encoded, err := json.Marshal(id)
	if err != nil {
		t.Fatalf("json.Marshal() error = %v", err)
	}
	if got, want := string(encoded), `"9007199254740993"`; got != want {
		t.Fatalf("JSON Identifier = %s，期望 %s", got, want)
	}
	for _, raw := range []string{`9007199254740993`, `"019fbfc5-3400-79f7-9deb-725f066b35e8"`, `"09007199254740993"`} {
		var decoded ID
		if err := json.Unmarshal([]byte(raw), &decoded); !errors.Is(err, ErrInvalidID) {
			t.Fatalf("json.Unmarshal(%s) error = %v，期望 ErrInvalidID", raw, err)
		}
	}
	var decoded ID
	if err := json.Unmarshal(encoded, &decoded); err != nil || decoded != id {
		t.Fatalf("JSON 往返结果 = %s, %v", decoded, err)
	}
}

type fakeClock struct{ current time.Time }

func (clock *fakeClock) now() time.Time { return clock.current }
func (clock *fakeClock) wait(ctx context.Context, duration time.Duration) error {
	if err := ctx.Err(); err != nil {
		return err
	}
	clock.current = clock.current.Add(duration)
	return nil
}

func TestGeneratorUsesFixedLayoutAndWaitsAfterSequenceExhaustion(t *testing.T) {
	clock := &fakeClock{current: time.UnixMilli(Epoch + 7)}
	generator, err := newGenerator(9, func() bool { return true }, clock)
	if err != nil {
		t.Fatal(err)
	}
	var last ID
	for range int(maximumSequence) + 2 {
		last, err = generator.Next(context.Background())
		if err != nil {
			t.Fatal(err)
		}
	}
	want := ID((8 << timestampShift) | (9 << nodeShift))
	if last != want {
		t.Fatalf("last ID = %d, want %d", last, want)
	}
}

func TestGeneratorRejectsExpiredLeaseAndLargeRollback(t *testing.T) {
	valid := true
	clock := &fakeClock{current: time.UnixMilli(Epoch + 10000)}
	generator, err := newGenerator(1, func() bool { return valid }, clock)
	if err != nil {
		t.Fatal(err)
	}
	if _, err := generator.Next(context.Background()); err != nil {
		t.Fatal(err)
	}
	valid = false
	if _, err := generator.Next(context.Background()); !errors.Is(err, ErrLeaseInvalid) {
		t.Fatalf("expired lease error = %v", err)
	}
	valid = true
	clock.current = time.UnixMilli(Epoch)
	if _, err := generator.Next(context.Background()); !errors.Is(err, ErrClockRollback) {
		t.Fatalf("rollback error = %v", err)
	}
}
