package rpg

import (
	"testing"

	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// TestDrawEncounterLootIsDeterministic 验证相同 seed、候选顺序与 draw 域始终产生相同掉落事实。
func TestDrawEncounterLootIsDeterministic(t *testing.T) {
	t.Parallel()
	entries := []encounterLootCandidate{
		{lootEntryID: snowflake.MustParse("1048576001"), itemID: snowflake.MustParse("1048576002"), minimumQuantity: 1, maximumQuantity: 3, weight: 2},
		{lootEntryID: snowflake.MustParse("1048576003"), itemID: snowflake.MustParse("1048576004"), minimumQuantity: 4, maximumQuantity: 8, weight: 5},
	}
	seed := make([]byte, 32)
	firstEntry, firstQuantity, firstAlgorithm, err := drawEncounterLoot(seed, entries)
	if err != nil {
		t.Fatalf("drawEncounterLoot(first) error = %v", err)
	}
	secondEntry, secondQuantity, secondAlgorithm, err := drawEncounterLoot(seed, entries)
	if err != nil {
		t.Fatalf("drawEncounterLoot(second) error = %v", err)
	}
	if firstEntry != secondEntry || firstQuantity != secondQuantity || firstAlgorithm != secondAlgorithm || firstAlgorithm != randomAlgorithm {
		t.Fatalf("相同 seed 的 Loot 不稳定: first=%+v/%d/%s second=%+v/%d/%s", firstEntry, firstQuantity, firstAlgorithm, secondEntry, secondQuantity, secondAlgorithm)
	}
}

// TestEncounterLootSelectionBoundaries 验证整数权重区间和数量区间的首尾边界均为闭合、无偏移映射。
func TestEncounterLootSelectionBoundaries(t *testing.T) {
	t.Parallel()
	first := encounterLootCandidate{lootEntryID: snowflake.MustParse("1048576011"), itemID: snowflake.MustParse("1048576012"), minimumQuantity: 2, maximumQuantity: 4, weight: 2}
	second := encounterLootCandidate{lootEntryID: snowflake.MustParse("1048576013"), itemID: snowflake.MustParse("1048576014"), minimumQuantity: 7, maximumQuantity: 9, weight: 3}
	entries := []encounterLootCandidate{first, second}
	for _, test := range []struct {
		roll uint32
		want encounterLootCandidate
	}{{0, first}, {1, first}, {2, second}, {4, second}} {
		got, err := selectEncounterLootEntry(entries, test.roll)
		if err != nil || got != test.want {
			t.Fatalf("selectEncounterLootEntry(roll=%d) = %+v, %v; want %+v", test.roll, got, err, test.want)
		}
	}
	minimum, err := encounterLootQuantity(first, 0)
	if err != nil || minimum != 2 {
		t.Fatalf("encounterLootQuantity(minimum) = %d, %v", minimum, err)
	}
	maximum, err := encounterLootQuantity(first, 2)
	if err != nil || maximum != 4 {
		t.Fatalf("encounterLootQuantity(maximum) = %d, %v", maximum, err)
	}
	if _, err = selectEncounterLootEntry(entries, 5); err == nil {
		t.Fatal("selectEncounterLootEntry() 接受了权重总和之外的 roll")
	}
	if _, err = encounterLootQuantity(first, 3); err == nil {
		t.Fatal("encounterLootQuantity() 接受了数量区间之外的 roll")
	}
}
