package persistence

import (
	"context"
	"fmt"
	"math"

	avalonent "github.com/lishangbu/avalon/ent"
	"github.com/lishangbu/avalon/ent/rpgencounterentry"
	"github.com/lishangbu/avalon/ent/rpglootentry"
	"github.com/lishangbu/avalon/internal/battle"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
	rpg "github.com/lishangbu/avalon/internal/rpg"
)

const (
	encounterLootEntryDrawNumber    = 3
	encounterLootQuantityDrawNumber = 4
)

type encounterLootCandidate struct {
	lootEntryID, itemID                      snowflake.ID
	minimumQuantity, maximumQuantity, weight int32
}

// freezeEncounterLoot 使用 Pending Encounter 已持久化 seed 的独立 draw 域冻结一次胜利掉落。
func freezeEncounterLoot(ctx context.Context, client *avalonent.Client, encounterEntryID snowflake.ID, seed []byte) (*battle.EncounterLootSnapshot, error) {
	encounter, err := client.RpgEncounterEntry.Query().Where(rpgencounterentry.IDEQ(encounterEntryID)).Only(ctx)
	if err != nil {
		return nil, fmt.Errorf("读取 Encounter Loot Table: %w", err)
	}
	if encounter.LootTableID == nil {
		return nil, nil
	}
	table, err := client.RpgLootTable.Get(ctx, *encounter.LootTableID)
	if err != nil || !table.Enabled {
		return nil, fmt.Errorf("读取启用 Loot Table: %w", err)
	}
	entries, err := client.RpgLootEntry.Query().Where(rpglootentry.LootTableIDEQ(table.ID), rpglootentry.EnabledEQ(true)).Order(rpglootentry.ByID()).All(ctx)
	if err != nil {
		return nil, fmt.Errorf("读取 Loot Entries: %w", err)
	}
	if len(entries) == 0 {
		return nil, fmt.Errorf("Loot Table 没有可抽样条目")
	}
	candidates := make([]encounterLootCandidate, 0, len(entries))
	for _, entry := range entries {
		candidates = append(candidates, encounterLootCandidate{lootEntryID: entry.ID, itemID: entry.ItemID, minimumQuantity: entry.MinimumQuantity, maximumQuantity: entry.MaximumQuantity, weight: entry.Weight})
	}
	selected, quantity, algorithm, err := drawEncounterLoot(seed, candidates)
	if err != nil {
		return nil, err
	}
	return &battle.EncounterLootSnapshot{LootTableID: table.ID, LootEntryID: selected.lootEntryID, ItemID: selected.itemID, Quantity: quantity, RandomAlgorithm: algorithm, EntryDrawNumber: encounterLootEntryDrawNumber, QuantityDrawNumber: encounterLootQuantityDrawNumber}, nil
}

func drawEncounterLoot(seed []byte, entries []encounterLootCandidate) (encounterLootCandidate, int32, string, error) {
	var total uint64
	for _, entry := range entries {
		if !entry.lootEntryID.IsValid() || !entry.itemID.IsValid() || entry.minimumQuantity <= 0 || entry.maximumQuantity < entry.minimumQuantity || entry.weight <= 0 {
			return encounterLootCandidate{}, 0, "", fmt.Errorf("Loot Entry 无效")
		}
		total += uint64(entry.weight)
	}
	if len(entries) == 0 || total == 0 || total > math.MaxUint32 {
		return encounterLootCandidate{}, 0, "", fmt.Errorf("Loot Table 权重总和无效")
	}
	source, err := rpg.NewRandomSourceFromSeed(seed)
	if err != nil {
		return encounterLootCandidate{}, 0, "", err
	}
	entryRoll, err := source.DrawUint32("encounter-loot-entry", encounterLootEntryDrawNumber, uint32(total))
	if err != nil {
		return encounterLootCandidate{}, 0, "", err
	}
	selected, err := selectEncounterLootEntry(entries, entryRoll)
	if err != nil {
		return encounterLootCandidate{}, 0, "", err
	}
	span := uint32(selected.maximumQuantity-selected.minimumQuantity) + 1
	quantityRoll, err := source.DrawUint32("encounter-loot-quantity", encounterLootQuantityDrawNumber, span)
	if err != nil {
		return encounterLootCandidate{}, 0, "", err
	}
	quantity, err := encounterLootQuantity(selected, quantityRoll)
	return selected, quantity, source.Algorithm(), err
}

func selectEncounterLootEntry(entries []encounterLootCandidate, roll uint32) (encounterLootCandidate, error) {
	cursor := uint64(0)
	for _, entry := range entries {
		cursor += uint64(entry.weight)
		if uint64(roll) < cursor {
			return entry, nil
		}
	}
	return encounterLootCandidate{}, fmt.Errorf("Loot Entry 抽样结果越界")
}

func encounterLootQuantity(entry encounterLootCandidate, roll uint32) (int32, error) {
	span := uint32(entry.maximumQuantity-entry.minimumQuantity) + 1
	if roll >= span {
		return 0, fmt.Errorf("Loot 数量抽样结果越界")
	}
	return entry.minimumQuantity + int32(roll), nil
}
