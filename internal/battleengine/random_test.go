package battleengine_test

import (
	"errors"
	"testing"

	"github.com/lishangbu/avalon/internal/battleengine"
)

func TestSplitMix64V1ProducesStableLanguageIndependentTrace(t *testing.T) {
	t.Parallel()

	source, err := battleengine.NewRandomSource(battleengine.RandomAlgorithmSplitMix64V1, 1)
	if err != nil {
		t.Fatalf("NewRandomSource() error = %v", err)
	}
	first, source, firstTrace, err := source.Next(100, "accuracy")
	if err != nil {
		t.Fatalf("first Next() error = %v", err)
	}
	second, _, secondTrace, err := source.Next(100, "damage-roll")
	if err != nil {
		t.Fatalf("second Next() error = %v", err)
	}

	if first != 65 || second != 19 {
		t.Fatalf("values = %d, %d, want 65, 19", first, second)
	}
	if firstTrace != (battleengine.RandomTraceEntry{
		Sequence: 1, Bound: 100, Reason: "accuracy", Value: 65,
	}) || secondTrace.Sequence != 2 || secondTrace.Reason != "damage-roll" || secondTrace.Value != 19 {
		t.Fatalf("trace = %+v, %+v", firstTrace, secondTrace)
	}
}

func TestTracedRandomRejectsTheFirstReplayDivergence(t *testing.T) {
	t.Parallel()

	replay, err := battleengine.NewTracedRandom([]battleengine.RandomTraceEntry{{
		Sequence: 1, Bound: 100, Reason: "accuracy", Value: 42,
	}})
	if err != nil {
		t.Fatalf("NewTracedRandom() error = %v", err)
	}
	if _, _, _, err := replay.Next(16, "accuracy"); !errors.Is(err, battleengine.ErrRandomTraceDiverged) {
		t.Fatalf("Next() error = %v, want ErrRandomTraceDiverged", err)
	}
	if replay.FullyConsumed() {
		t.Fatal("FullyConsumed() = true after rejected replay consumption")
	}
}

func TestRandomContractsRejectInvalidVersionAndTrace(t *testing.T) {
	t.Parallel()

	if _, err := battleengine.NewRandomSource("future-random", 1); !errors.Is(err, battleengine.ErrUnsupportedRandomAlgorithm) {
		t.Fatalf("NewRandomSource() error = %v, want ErrUnsupportedRandomAlgorithm", err)
	}
	if _, err := battleengine.NewTracedRandom([]battleengine.RandomTraceEntry{{
		Sequence: 2, Bound: 1, Reason: "tie-break", Value: 0,
	}}); !errors.Is(err, battleengine.ErrInvalidRandomTrace) {
		t.Fatalf("NewTracedRandom() error = %v, want ErrInvalidRandomTrace", err)
	}
}
