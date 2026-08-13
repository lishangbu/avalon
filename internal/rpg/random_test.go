package rpg

import "testing"

func TestRandomSourceIsDeterministicAndBounded(t *testing.T) {
	first, err := NewRandomSourceFromSeed(make([]byte, 32))
	if err != nil {
		t.Fatal(err)
	}
	second, err := NewRandomSourceFromSeed(make([]byte, 32))
	if err != nil {
		t.Fatal(err)
	}
	for i := uint64(0); i < 100; i++ {
		a, err := first.DrawUint32("walk-encounter", i, 7)
		if err != nil {
			t.Fatal(err)
		}
		b, err := second.DrawUint32("walk-encounter", i, 7)
		if err != nil {
			t.Fatal(err)
		}
		if a != b || a >= 7 {
			t.Fatalf("draw %d = %d/%d", i, a, b)
		}
	}
}

func TestRandomSourceRejectsInvalidInput(t *testing.T) {
	source, _ := NewRandomSourceFromSeed(make([]byte, 32))
	if _, err := source.DrawUint32("", 0, 1); err == nil {
		t.Fatal("expected empty purpose error")
	}
	if _, err := source.DrawUint32("x", 0, 0); err == nil {
		t.Fatal("expected zero bound error")
	}
}
