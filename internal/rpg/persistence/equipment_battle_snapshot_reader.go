package persistence

import (
	"context"
	"encoding/json"
	"fmt"

	avalonent "github.com/lishangbu/avalon/ent"
	"github.com/lishangbu/avalon/ent/playercharacterequipmentloadoutentry"
	"github.com/lishangbu/avalon/ent/playercharacterequipmentloadoutstate"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
	rpg "github.com/lishangbu/avalon/internal/rpg"
)

// EquipmentBattleSnapshotDocument 是写入 Battle Participant 输入的版本化规范装备文档。
type EquipmentBattleSnapshotDocument struct {
	// SchemaVersion 固定当前文档结构；读取方不得猜测未知版本。
	SchemaVersion int32 `json:"schemaVersion"`
	// LoadoutVersion 是 Battle 创建时整套 Loadout 的乐观版本。
	LoadoutVersion int64 `json:"loadoutVersion"`
	// Entries 按固定槽位保存全部装备来源事实。
	Entries []EquipmentBattleSnapshotDocumentEntry `json:"entries"`
}

// EquipmentBattleSnapshotDocumentEntry 是一件冻结装备的 JSON 边界表示。
type EquipmentBattleSnapshotDocumentEntry struct {
	// InstanceID、EquipmentID 与 ItemID 分别标识资产、规则资料和展示资料。
	InstanceID  snowflake.ID `json:"instanceId"`
	EquipmentID snowflake.ID `json:"equipmentId"`
	ItemID      snowflake.ID `json:"itemId"`
	// Slot 是 Battle 创建时该实例占用的固定槽位。
	Slot rpg.EquipmentSlot `json:"slot"`
	// CatalogVersion 是 Equipment Catalog Entry 的冻结乐观版本。
	CatalogVersion int64 `json:"catalogVersion"`
	// StatModifiers 是本场唯一可读取的完整属性修正集合。
	StatModifiers []rpg.EquipmentStatModifier `json:"statModifiers"`
	// CompiledRules 是本场唯一可读取的规范规则文档。
	CompiledRules json.RawMessage `json:"compiledRules"`
}

// FreezePlayerCharacterEquipmentForBattle 在当前事务内读取 Loadout，并返回不依赖实时资料的规范 JSON。
func (adapter *Adapters) FreezePlayerCharacterEquipmentForBattle(ctx context.Context, playerID snowflake.ID) (json.RawMessage, error) {
	return FreezePlayerCharacterEquipmentWithEnt(ctx, adapter.pool.Client(ctx), playerID)
}

// FreezePlayerCharacterEquipmentWithEnt 使用调用方事务 Client 冻结 PlayerCharacter Equipment Loadout。
func FreezePlayerCharacterEquipmentWithEnt(ctx context.Context, client *avalonent.Client, playerID snowflake.ID) (json.RawMessage, error) {
	if client == nil || !playerID.IsValid() {
		return nil, rpg.ErrEquipmentLoadoutConflict
	}
	loadoutVersion := int64(1)
	state, err := client.PlayerCharacterEquipmentLoadoutState.Query().Where(playercharacterequipmentloadoutstate.IDEQ(playerID)).Only(ctx)
	if err == nil {
		loadoutVersion = state.Version
	} else if !avalonent.IsNotFound(err) {
		return nil, fmt.Errorf("读取 Battle Equipment Loadout 版本: %w", err)
	}
	rows, err := client.PlayerCharacterEquipmentLoadoutEntry.Query().Where(playercharacterequipmentloadoutentry.PlayerCharacterIDEQ(playerID)).WithEquipmentInstance(func(query *avalonent.PlayerCharacterEquipmentInstanceQuery) {
		query.WithEquipment(func(equipment *avalonent.GameEquipmentQuery) { equipment.WithStatModifiers() })
	}).All(ctx)
	if err != nil {
		return nil, fmt.Errorf("读取 Battle Equipment Loadout: %w", err)
	}
	entries := make([]rpg.EquipmentBattleSnapshotEntry, 0, len(rows))
	for _, row := range rows {
		instance := row.Edges.EquipmentInstance
		if instance == nil || instance.Edges.Equipment == nil || instance.SoldAt != nil {
			return nil, rpg.ErrEquipmentLoadoutConflict
		}
		equipment := instance.Edges.Equipment
		modifiers := make([]rpg.EquipmentStatModifier, 0, len(equipment.Edges.StatModifiers))
		for _, modifier := range equipment.Edges.StatModifiers {
			modifiers = append(modifiers, rpg.EquipmentStatModifier{StatID: modifier.StatID, FlatValue: modifier.FlatValue, PercentageBPS: modifier.PercentageBps})
		}
		entries = append(entries, rpg.EquipmentBattleSnapshotEntry{InstanceID: instance.ID, EquipmentID: equipment.ID, ItemID: equipment.ItemID, Slot: rpg.EquipmentSlot(row.Slot), CatalogVersion: equipment.Version, StatModifiers: modifiers, CompiledRules: equipment.Rules})
	}
	snapshot, err := rpg.FreezeEquipmentBattleSnapshot(loadoutVersion, entries)
	if err != nil {
		return nil, err
	}
	document := EquipmentBattleSnapshotDocument{SchemaVersion: 1, LoadoutVersion: snapshot.LoadoutVersion, Entries: make([]EquipmentBattleSnapshotDocumentEntry, 0, len(snapshot.Entries))}
	for _, entry := range snapshot.Entries {
		document.Entries = append(document.Entries, EquipmentBattleSnapshotDocumentEntry{InstanceID: entry.InstanceID, EquipmentID: entry.EquipmentID, ItemID: entry.ItemID, Slot: entry.Slot, CatalogVersion: entry.CatalogVersion, StatModifiers: entry.StatModifiers, CompiledRules: entry.CompiledRules})
	}
	payload, err := json.Marshal(document)
	if err != nil {
		return nil, fmt.Errorf("编码 Battle Equipment Snapshot: %w", err)
	}
	return payload, nil
}
