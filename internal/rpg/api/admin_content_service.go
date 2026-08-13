package api

import (
	"context"
	kratoserrors "github.com/go-kratos/kratos/v3/errors"
	rpgv1 "github.com/lishangbu/avalon/api/gen/go/avalon/rpg/v1"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
	"github.com/lishangbu/avalon/internal/rpg"
)

func optionalID(raw, reason, message string) (snowflake.ID, error) {
	if raw == "" {
		return 0, nil
	}
	id, e := snowflake.Parse(raw)
	if e != nil {
		return 0, kratoserrors.BadRequest(reason, message)
	}
	return id, nil
}
func requiredID(raw, reason, message string) (snowflake.ID, error) {
	id, e := snowflake.Parse(raw)
	if e != nil {
		return 0, kratoserrors.BadRequest(reason, message)
	}
	return id, nil
}

// ListNpcs 返回 NPC 维护资料。
func (s *AdminWorldService) ListNpcs(ctx context.Context, q *rpgv1.ListNpcsRequest) (*rpgv1.ListNpcsResponse, error) {
	rows, e := s.store.ListNPCs(ctx, int(q.GetPageSize()))
	if e != nil {
		return nil, adminError(e)
	}
	out := &rpgv1.ListNpcsResponse{Npcs: make([]*rpgv1.AdminNpc, 0, len(rows))}
	for _, v := range rows {
		out.Npcs = append(out.Npcs, npcMessage(v))
	}
	return out, nil
}

// SaveNpc 创建或更新 NPC。
func (s *AdminWorldService) SaveNpc(ctx context.Context, q *rpgv1.SaveNpcRequest) (*rpgv1.SaveNpcResponse, error) {
	if q.GetBody() == nil {
		return nil, kratoserrors.BadRequest("INVALID_REQUEST", "请求格式无效")
	}
	w, e := adminWriteContext(ctx, q.GetIdempotencyKey())
	if e != nil {
		return nil, e
	}
	b := q.GetBody()
	location, e := requiredID(b.GetLocationId(), "INVALID_LOCATION_ID", "Location 标识无效")
	if e != nil {
		return nil, e
	}
	id, e := optionalID(q.GetNpcId(), "INVALID_NPC_ID", "NPC 标识无效")
	if e != nil {
		return nil, e
	}
	v, e := s.store.SaveNPC(ctx, rpg.SaveNPCCommand{Write: w, Value: rpg.AdminNPC{ID: id, LocationID: location, Code: b.GetCode(), Name: b.GetName(), NPCType: b.GetNpcType(), Description: b.GetDescription(), Enabled: b.GetEnabled()}, ExpectedVersion: q.GetExpectedVersion()})
	if e != nil {
		return nil, adminError(e)
	}
	return &rpgv1.SaveNpcResponse{Npc: npcMessage(v)}, nil
}
func npcMessage(v rpg.AdminNPC) *rpgv1.AdminNpc {
	return &rpgv1.AdminNpc{Id: v.ID.String(), LocationId: v.LocationID.String(), Code: v.Code, Name: v.Name, NpcType: v.NPCType, Description: v.Description, Enabled: v.Enabled, Version: v.Version}
}

// ListDialogues 返回对话聚合。
func (s *AdminWorldService) ListDialogues(ctx context.Context, q *rpgv1.ListDialoguesRequest) (*rpgv1.ListDialoguesResponse, error) {
	rows, e := s.store.ListDialogues(ctx, int(q.GetPageSize()))
	if e != nil {
		return nil, adminError(e)
	}
	out := &rpgv1.ListDialoguesResponse{Dialogues: make([]*rpgv1.AdminDialogue, 0, len(rows))}
	for _, v := range rows {
		out.Dialogues = append(out.Dialogues, dialogueMessage(v))
	}
	return out, nil
}

// SaveDialogue 创建或更新对话及有序行。
func (s *AdminWorldService) SaveDialogue(ctx context.Context, q *rpgv1.SaveDialogueRequest) (*rpgv1.SaveDialogueResponse, error) {
	if q.GetBody() == nil {
		return nil, kratoserrors.BadRequest("INVALID_REQUEST", "请求格式无效")
	}
	w, e := adminWriteContext(ctx, q.GetIdempotencyKey())
	if e != nil {
		return nil, e
	}
	b := q.GetBody()
	npc, e := requiredID(b.GetNpcId(), "INVALID_NPC_ID", "NPC 标识无效")
	if e != nil {
		return nil, e
	}
	id, e := optionalID(q.GetDialogueId(), "INVALID_DIALOGUE_ID", "Dialogue 标识无效")
	if e != nil {
		return nil, e
	}
	v := rpg.AdminDialogue{ID: id, NPCID: npc, Code: b.GetCode(), Name: b.GetName(), Enabled: b.GetEnabled(), Lines: make([]rpg.AdminDialogueLine, 0, len(b.GetLines()))}
	for _, x := range b.GetLines() {
		v.Lines = append(v.Lines, rpg.AdminDialogueLine{Position: x.GetPosition(), SpeakerName: x.GetSpeakerName(), Content: x.GetContent()})
	}
	saved, e := s.store.SaveDialogue(ctx, rpg.SaveDialogueCommand{Write: w, Value: v, ExpectedVersion: q.GetExpectedVersion()})
	if e != nil {
		return nil, adminError(e)
	}
	return &rpgv1.SaveDialogueResponse{Dialogue: dialogueMessage(saved)}, nil
}
func dialogueMessage(v rpg.AdminDialogue) *rpgv1.AdminDialogue {
	out := &rpgv1.AdminDialogue{Id: v.ID.String(), NpcId: v.NPCID.String(), Code: v.Code, Name: v.Name, Enabled: v.Enabled, Version: v.Version, Lines: make([]*rpgv1.AdminDialogueLine, 0, len(v.Lines))}
	for _, x := range v.Lines {
		out.Lines = append(out.Lines, &rpgv1.AdminDialogueLine{Id: x.ID.String(), Position: x.Position, SpeakerName: x.SpeakerName, Content: x.Content})
	}
	return out
}

// ListLootTables 返回掉落聚合。
func (s *AdminWorldService) ListLootTables(ctx context.Context, q *rpgv1.ListLootTablesRequest) (*rpgv1.ListLootTablesResponse, error) {
	rows, e := s.store.ListLootTables(ctx, int(q.GetPageSize()))
	if e != nil {
		return nil, adminError(e)
	}
	out := &rpgv1.ListLootTablesResponse{Tables: make([]*rpgv1.AdminLootTable, 0, len(rows))}
	for _, v := range rows {
		out.Tables = append(out.Tables, lootMessage(v))
	}
	return out, nil
}

// SaveLootTable 创建或更新掉落聚合。
func (s *AdminWorldService) SaveLootTable(ctx context.Context, q *rpgv1.SaveLootTableRequest) (*rpgv1.SaveLootTableResponse, error) {
	if q.GetBody() == nil {
		return nil, kratoserrors.BadRequest("INVALID_REQUEST", "请求格式无效")
	}
	w, e := adminWriteContext(ctx, q.GetIdempotencyKey())
	if e != nil {
		return nil, e
	}
	b := q.GetBody()
	id, e := optionalID(q.GetLootTableId(), "INVALID_LOOT_TABLE_ID", "Loot Table 标识无效")
	if e != nil {
		return nil, e
	}
	v := rpg.AdminLootTable{ID: id, Code: b.GetCode(), Name: b.GetName(), Enabled: b.GetEnabled(), Entries: make([]rpg.AdminLootEntry, 0, len(b.GetEntries()))}
	for _, x := range b.GetEntries() {
		entryID, e := optionalID(x.GetLootEntryId(), "INVALID_LOOT_ENTRY_ID", "Loot Entry 标识无效")
		if e != nil {
			return nil, e
		}
		item, e := requiredID(x.GetItemId(), "INVALID_ITEM_ID", "Item 标识无效")
		if e != nil {
			return nil, e
		}
		v.Entries = append(v.Entries, rpg.AdminLootEntry{ID: entryID, ItemID: item, MinimumQuantity: x.GetMinimumQuantity(), MaximumQuantity: x.GetMaximumQuantity(), Weight: x.GetWeight()})
	}
	saved, e := s.store.SaveLootTable(ctx, rpg.SaveLootTableCommand{Write: w, Value: v, ExpectedVersion: q.GetExpectedVersion()})
	if e != nil {
		return nil, adminError(e)
	}
	return &rpgv1.SaveLootTableResponse{Table: lootMessage(saved)}, nil
}
func lootMessage(v rpg.AdminLootTable) *rpgv1.AdminLootTable {
	out := &rpgv1.AdminLootTable{Id: v.ID.String(), Code: v.Code, Name: v.Name, Enabled: v.Enabled, Version: v.Version, Entries: make([]*rpgv1.AdminLootEntry, 0, len(v.Entries))}
	for _, x := range v.Entries {
		out.Entries = append(out.Entries, &rpgv1.AdminLootEntry{Id: x.ID.String(), ItemId: x.ItemID.String(), MinimumQuantity: x.MinimumQuantity, MaximumQuantity: x.MaximumQuantity, Weight: x.Weight})
	}
	return out
}

// ListShops 返回商店聚合。
func (s *AdminWorldService) ListShops(ctx context.Context, q *rpgv1.ListShopsRequest) (*rpgv1.ListShopsResponse, error) {
	rows, e := s.store.ListShops(ctx, int(q.GetPageSize()))
	if e != nil {
		return nil, adminError(e)
	}
	out := &rpgv1.ListShopsResponse{Shops: make([]*rpgv1.AdminShop, 0, len(rows))}
	for _, v := range rows {
		out.Shops = append(out.Shops, shopMessage(v))
	}
	return out, nil
}

// SaveShop 创建或更新商店聚合。
func (s *AdminWorldService) SaveShop(ctx context.Context, q *rpgv1.SaveShopRequest) (*rpgv1.SaveShopResponse, error) {
	if q.GetBody() == nil {
		return nil, kratoserrors.BadRequest("INVALID_REQUEST", "请求格式无效")
	}
	w, e := adminWriteContext(ctx, q.GetIdempotencyKey())
	if e != nil {
		return nil, e
	}
	b := q.GetBody()
	id, e := optionalID(q.GetShopId(), "INVALID_SHOP_ID", "Shop 标识无效")
	if e != nil {
		return nil, e
	}
	npc, e := optionalID(b.GetNpcId(), "INVALID_NPC_ID", "NPC 标识无效")
	if e != nil {
		return nil, e
	}
	loc, e := requiredID(b.GetLocationId(), "INVALID_LOCATION_ID", "Location 标识无效")
	if e != nil {
		return nil, e
	}
	v := rpg.AdminShop{ID: id, NPCID: npc, LocationID: loc, Code: b.GetCode(), Name: b.GetName(), Enabled: b.GetEnabled(), Items: make([]rpg.AdminShopItem, 0, len(b.GetItems()))}
	for _, x := range b.GetItems() {
		shopItem, e := optionalID(x.GetShopItemId(), "INVALID_SHOP_ITEM_ID", "Shop Item 标识无效")
		if e != nil {
			return nil, e
		}
		item, e := requiredID(x.GetItemId(), "INVALID_ITEM_ID", "Item 标识无效")
		if e != nil {
			return nil, e
		}
		currency, e := requiredID(x.GetCurrencyId(), "INVALID_CURRENCY_ID", "Currency 标识无效")
		if e != nil {
			return nil, e
		}
		v.Items = append(v.Items, rpg.AdminShopItem{ID: shopItem, ItemID: item, CurrencyID: currency, BuyPrice: x.GetBuyPrice(), SellPrice: x.SellPrice, Enabled: x.GetEnabled()})
	}
	saved, e := s.store.SaveShop(ctx, rpg.SaveShopCommand{Write: w, Value: v, ExpectedVersion: q.GetExpectedVersion()})
	if e != nil {
		return nil, adminError(e)
	}
	return &rpgv1.SaveShopResponse{Shop: shopMessage(saved)}, nil
}
func shopMessage(v rpg.AdminShop) *rpgv1.AdminShop {
	npc := ""
	if v.NPCID.IsValid() {
		npc = v.NPCID.String()
	}
	out := &rpgv1.AdminShop{Id: v.ID.String(), NpcId: npc, LocationId: v.LocationID.String(), Code: v.Code, Name: v.Name, Enabled: v.Enabled, Version: v.Version, Items: make([]*rpgv1.AdminShopItem, 0, len(v.Items))}
	for _, x := range v.Items {
		out.Items = append(out.Items, &rpgv1.AdminShopItem{Id: x.ID.String(), ItemId: x.ItemID.String(), CurrencyId: x.CurrencyID.String(), BuyPrice: x.BuyPrice, SellPrice: x.SellPrice, Enabled: x.Enabled})
	}
	return out
}
