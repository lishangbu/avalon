package rpg

import (
	"context"
	"strings"
	"time"

	avalonent "github.com/lishangbu/avalon/ent"
	"github.com/lishangbu/avalon/ent/gamecurrency"
	"github.com/lishangbu/avalon/ent/gameitem"
	"github.com/lishangbu/avalon/ent/rpgdialogue"
	"github.com/lishangbu/avalon/ent/rpgdialogueline"
	"github.com/lishangbu/avalon/ent/rpglocation"
	"github.com/lishangbu/avalon/ent/rpglootentry"
	"github.com/lishangbu/avalon/ent/rpgloottable"
	"github.com/lishangbu/avalon/ent/rpgnpc"
	"github.com/lishangbu/avalon/ent/rpgshop"
	"github.com/lishangbu/avalon/ent/rpgshopitem"
	"github.com/lishangbu/avalon/internal/gamedata/stablecode"
	"github.com/lishangbu/avalon/internal/platform/idempotency"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

func validAdminWrite(write AdminWriteContext) bool {
	return write.ActorAccountID.IsValid() && idempotency.ValidKey(write.IdempotencyKey) && strings.TrimSpace(write.RequestID) != ""
}
func validNamed(code, name string) bool {
	return stablecode.Valid(strings.TrimSpace(code)) && strings.TrimSpace(name) != "" && len([]rune(strings.TrimSpace(name))) <= 120
}

// ListNPCs 返回全部 NPC 维护资料。
func (store *EntWorldStore) ListNPCs(ctx context.Context, size int) ([]AdminNPC, error) {
	rows, err := store.pool.Client(ctx).RpgNpc.Query().Order(rpgnpc.ByCode(), rpgnpc.ByID()).Limit(boundedPageSize(size, 200)).All(ctx)
	if err != nil {
		return nil, err
	}
	out := make([]AdminNPC, 0, len(rows))
	for _, row := range rows {
		description := ""
		if row.Description != nil {
			description = *row.Description
		}
		out = append(out, AdminNPC{ID: row.ID, LocationID: row.LocationID, Code: row.Code, Name: row.Name, NPCType: row.NpcType, Description: description, Enabled: row.Enabled, Version: row.Version})
	}
	return out, nil
}

// SaveNPC 创建或更新 NPC；空 ID 表示创建。
func (store *EntWorldStore) SaveNPC(ctx context.Context, command SaveNPCCommand) (AdminNPC, error) {
	value := command.Value
	value.Code, value.Name, value.NPCType, value.Description = strings.TrimSpace(value.Code), strings.TrimSpace(value.Name), strings.TrimSpace(value.NPCType), strings.TrimSpace(value.Description)
	update := value.ID.IsValid()
	types := map[string]bool{"story": true, "merchant": true, "trainer": true, "service": true, "ambient": true}
	if !validAdminWrite(command.Write) || !value.LocationID.IsValid() || !validNamed(value.Code, value.Name) || !types[value.NPCType] || len([]rune(value.Description)) > 4000 || update && command.ExpectedVersion <= 0 {
		return AdminNPC{}, ErrInvalidAdminWorld
	}
	if !update {
		id, err := store.newID.Next(ctx)
		if err != nil {
			return AdminNPC{}, err
		}
		value.ID, value.Version = id, 1
	} else {
		value.Version = command.ExpectedVersion + 1
	}
	digest, err := idempotency.Digest(struct {
		Value    AdminNPC
		Expected int64
	}{value, command.ExpectedVersion})
	if err != nil {
		return value, err
	}
	now := time.Now().UTC()
	request := idempotency.Request{ActorAccountID: command.Write.ActorAccountID, OperationID: "rpg.npc.save", Key: command.Write.IdempotencyKey, RequestDigest: digest, CreatedAt: now}
	err = store.pool.WithinTransaction(ctx, func(txctx context.Context) error {
		client := store.pool.Client(txctx)
		writer := idempotency.NewPersistentWriter(idempotency.NewAdminEntRecords(client, store.newID))
		replay, e := idempotency.ClaimResponse(txctx, writer, request, &value)
		if e != nil || replay {
			return e
		}
		if _, e = client.RpgLocation.Query().Where(rpglocation.IDEQ(value.LocationID)).Only(txctx); e != nil {
			return adminWorldStoreError(e)
		}
		var before *AdminNPC
		if !update {
			b := client.RpgNpc.Create().SetID(value.ID).SetLocationID(value.LocationID).SetCode(value.Code).SetName(value.Name).SetNpcType(value.NPCType).SetEnabled(value.Enabled).SetVersion(1).SetCreatedAt(now).SetUpdatedAt(now)
			if value.Description != "" {
				b.SetDescription(value.Description)
			}
			if _, e = b.Save(txctx); e != nil {
				return adminWorldStoreError(e)
			}
		} else {
			row, e := client.RpgNpc.Query().Where(rpgnpc.IDEQ(value.ID)).Only(txctx)
			if e != nil {
				return adminWorldStoreError(e)
			}
			old := npcRow(row)
			before = &old
			b := client.RpgNpc.UpdateOne(row).Where(rpgnpc.VersionEQ(command.ExpectedVersion)).SetLocationID(value.LocationID).SetCode(value.Code).SetName(value.Name).SetNpcType(value.NPCType).SetEnabled(value.Enabled).SetVersion(value.Version).SetUpdatedAt(now)
			if value.Description == "" {
				b.ClearDescription()
			} else {
				b.SetDescription(value.Description)
			}
			if _, e = b.Save(txctx); e != nil {
				return adminWorldStoreError(e)
			}
		}
		return store.auditAndComplete(txctx, writer, request, command.Write, "rpg.npc.saved", "rpg_npc", value.ID, before, value, now)
	})
	if err != nil {
		return AdminNPC{}, err
	}
	return value, nil
}
func npcRow(row *avalonent.RpgNpc) AdminNPC {
	d := ""
	if row.Description != nil {
		d = *row.Description
	}
	return AdminNPC{ID: row.ID, LocationID: row.LocationID, Code: row.Code, Name: row.Name, NPCType: row.NpcType, Description: d, Enabled: row.Enabled, Version: row.Version}
}

// ListDialogues 返回对话及有序行。
func (store *EntWorldStore) ListDialogues(ctx context.Context, size int) ([]AdminDialogue, error) {
	client := store.pool.Client(ctx)
	rows, e := client.RpgDialogue.Query().Order(rpgdialogue.ByCode(), rpgdialogue.ByID()).Limit(boundedPageSize(size, 200)).All(ctx)
	if e != nil {
		return nil, e
	}
	out := make([]AdminDialogue, 0, len(rows))
	idx := map[snowflake.ID]int{}
	for _, r := range rows {
		idx[r.ID] = len(out)
		out = append(out, AdminDialogue{ID: r.ID, NPCID: r.NpcID, Code: r.Code, Name: r.Name, Enabled: r.Enabled, Version: r.Version, Lines: []AdminDialogueLine{}})
	}
	lines, e := client.RpgDialogueLine.Query().Order(rpgdialogueline.ByDialogueID(), rpgdialogueline.ByPosition(), rpgdialogueline.ByID()).All(ctx)
	if e != nil {
		return nil, e
	}
	for _, r := range lines {
		if i, ok := idx[r.DialogueID]; ok {
			out[i].Lines = append(out[i].Lines, AdminDialogueLine{ID: r.ID, Position: r.Position, SpeakerName: r.SpeakerName, Content: r.Content})
		}
	}
	return out, nil
}

// SaveDialogue 使用父版本完整替换对话行。
func (store *EntWorldStore) SaveDialogue(ctx context.Context, c SaveDialogueCommand) (AdminDialogue, error) {
	v := c.Value
	v.Code, v.Name = strings.TrimSpace(v.Code), strings.TrimSpace(v.Name)
	update := v.ID.IsValid()
	if !validAdminWrite(c.Write) || !v.NPCID.IsValid() || !validNamed(v.Code, v.Name) || update && c.ExpectedVersion <= 0 {
		return AdminDialogue{}, ErrInvalidAdminWorld
	}
	seen := map[int32]bool{}
	for i := range v.Lines {
		v.Lines[i].SpeakerName = strings.TrimSpace(v.Lines[i].SpeakerName)
		v.Lines[i].Content = strings.TrimSpace(v.Lines[i].Content)
		x := v.Lines[i]
		if x.Position <= 0 || seen[x.Position] || x.SpeakerName == "" || len([]rune(x.SpeakerName)) > 120 || x.Content == "" || len([]rune(x.Content)) > 8000 {
			return AdminDialogue{}, ErrInvalidAdminWorld
		}
		seen[x.Position] = true
	}
	if !update {
		id, e := store.newID.Next(ctx)
		if e != nil {
			return AdminDialogue{}, e
		}
		v.ID, v.Version = id, 1
	} else {
		v.Version = c.ExpectedVersion + 1
	}
	return store.saveDialogue(ctx, c, v, update)
}
func (store *EntWorldStore) saveDialogue(ctx context.Context, c SaveDialogueCommand, v AdminDialogue, update bool) (AdminDialogue, error) {
	digest, e := idempotency.Digest(struct {
		Value    AdminDialogue
		Expected int64
	}{v, c.ExpectedVersion})
	if e != nil {
		return v, e
	}
	now := time.Now().UTC()
	req := idempotency.Request{ActorAccountID: c.Write.ActorAccountID, OperationID: "rpg.dialogue.save", Key: c.Write.IdempotencyKey, RequestDigest: digest, CreatedAt: now}
	e = store.pool.WithinTransaction(ctx, func(tx context.Context) error {
		client := store.pool.Client(tx)
		w := idempotency.NewPersistentWriter(idempotency.NewAdminEntRecords(client, store.newID))
		replay, e := idempotency.ClaimResponse(tx, w, req, &v)
		if e != nil || replay {
			return e
		}
		if _, e = client.RpgNpc.Query().Where(rpgnpc.IDEQ(v.NPCID)).Only(tx); e != nil {
			return adminWorldStoreError(e)
		}
		var before *AdminDialogue
		if !update {
			if _, e = client.RpgDialogue.Create().SetID(v.ID).SetNpcID(v.NPCID).SetCode(v.Code).SetName(v.Name).SetEnabled(v.Enabled).SetVersion(1).SetCreatedAt(now).SetUpdatedAt(now).Save(tx); e != nil {
				return adminWorldStoreError(e)
			}
		} else {
			row, e := client.RpgDialogue.Query().Where(rpgdialogue.IDEQ(v.ID)).Only(tx)
			if e != nil {
				return adminWorldStoreError(e)
			}
			old := AdminDialogue{ID: row.ID, NPCID: row.NpcID, Code: row.Code, Name: row.Name, Enabled: row.Enabled, Version: row.Version}
			before = &old
			if _, e = client.RpgDialogue.UpdateOne(row).Where(rpgdialogue.VersionEQ(c.ExpectedVersion)).SetNpcID(v.NPCID).SetCode(v.Code).SetName(v.Name).SetEnabled(v.Enabled).SetVersion(v.Version).SetUpdatedAt(now).Save(tx); e != nil {
				return adminWorldStoreError(e)
			}
			if _, e = client.RpgDialogueLine.Delete().Where(rpgdialogueline.DialogueIDEQ(v.ID)).Exec(tx); e != nil {
				return e
			}
		}
		for i := range v.Lines {
			id, e := store.newID.Next(tx)
			if e != nil {
				return e
			}
			v.Lines[i].ID = id
			x := v.Lines[i]
			if _, e = client.RpgDialogueLine.Create().SetID(id).SetDialogueID(v.ID).SetPosition(x.Position).SetSpeakerName(x.SpeakerName).SetContent(x.Content).Save(tx); e != nil {
				return adminWorldStoreError(e)
			}
		}
		return store.auditAndComplete(tx, w, req, c.Write, "rpg.dialogue.saved", "rpg_dialogue", v.ID, before, v, now)
	})
	if e != nil {
		return AdminDialogue{}, e
	}
	return v, nil
}

// ListLootTables 返回掉落表聚合。
func (store *EntWorldStore) ListLootTables(ctx context.Context, size int) ([]AdminLootTable, error) {
	client := store.pool.Client(ctx)
	rows, e := client.RpgLootTable.Query().Order(rpgloottable.ByCode(), rpgloottable.ByID()).Limit(boundedPageSize(size, 200)).All(ctx)
	if e != nil {
		return nil, e
	}
	out := make([]AdminLootTable, 0, len(rows))
	idx := map[snowflake.ID]int{}
	for _, r := range rows {
		idx[r.ID] = len(out)
		out = append(out, AdminLootTable{ID: r.ID, Code: r.Code, Name: r.Name, Enabled: r.Enabled, Version: r.Version, Entries: []AdminLootEntry{}})
	}
	items, e := client.RpgLootEntry.Query().Order(rpglootentry.ByLootTableID(), rpglootentry.ByID()).All(ctx)
	if e != nil {
		return nil, e
	}
	for _, r := range items {
		if i, ok := idx[r.LootTableID]; ok {
			out[i].Entries = append(out[i].Entries, AdminLootEntry{ID: r.ID, ItemID: r.ItemID, MinimumQuantity: r.MinimumQuantity, MaximumQuantity: r.MaximumQuantity, Weight: r.Weight})
		}
	}
	return out, nil
}

// SaveLootTable 使用父版本完整替换掉落项。
func (store *EntWorldStore) SaveLootTable(ctx context.Context, c SaveLootTableCommand) (AdminLootTable, error) {
	v := c.Value
	v.Code, v.Name = strings.TrimSpace(v.Code), strings.TrimSpace(v.Name)
	update := v.ID.IsValid()
	if !validAdminWrite(c.Write) || !validNamed(v.Code, v.Name) || update && c.ExpectedVersion <= 0 {
		return AdminLootTable{}, ErrInvalidAdminWorld
	}
	for _, x := range v.Entries {
		if !x.ItemID.IsValid() || x.MinimumQuantity <= 0 || x.MaximumQuantity < x.MinimumQuantity || x.Weight <= 0 {
			return AdminLootTable{}, ErrInvalidAdminWorld
		}
	}
	if !update {
		id, e := store.newID.Next(ctx)
		if e != nil {
			return v, e
		}
		v.ID, v.Version = id, 1
	} else {
		v.Version = c.ExpectedVersion + 1
	}
	return store.saveLoot(ctx, c, v, update)
}
func (store *EntWorldStore) saveLoot(ctx context.Context, c SaveLootTableCommand, v AdminLootTable, update bool) (AdminLootTable, error) {
	digest, e := idempotency.Digest(struct {
		V AdminLootTable
		E int64
	}{v, c.ExpectedVersion})
	if e != nil {
		return v, e
	}
	now := time.Now().UTC()
	req := idempotency.Request{ActorAccountID: c.Write.ActorAccountID, OperationID: "rpg.loot_table.save", Key: c.Write.IdempotencyKey, RequestDigest: digest, CreatedAt: now}
	e = store.pool.WithinTransaction(ctx, func(tx context.Context) error {
		client := store.pool.Client(tx)
		w := idempotency.NewPersistentWriter(idempotency.NewAdminEntRecords(client, store.newID))
		replay, e := idempotency.ClaimResponse(tx, w, req, &v)
		if e != nil || replay {
			return e
		}
		for _, x := range v.Entries {
			if _, e = client.GameItem.Query().Where(gameitem.IDEQ(x.ItemID)).Only(tx); e != nil {
				return adminWorldStoreError(e)
			}
		}
		var before *AdminLootTable
		if !update {
			if _, e = client.RpgLootTable.Create().SetID(v.ID).SetCode(v.Code).SetName(v.Name).SetEnabled(v.Enabled).SetVersion(1).SetCreatedAt(now).SetUpdatedAt(now).Save(tx); e != nil {
				return adminWorldStoreError(e)
			}
		} else {
			row, e := client.RpgLootTable.Query().Where(rpgloottable.IDEQ(v.ID)).Only(tx)
			if e != nil {
				return adminWorldStoreError(e)
			}
			old := AdminLootTable{ID: row.ID, Code: row.Code, Name: row.Name, Enabled: row.Enabled, Version: row.Version}
			before = &old
			if _, e = client.RpgLootTable.UpdateOne(row).Where(rpgloottable.VersionEQ(c.ExpectedVersion)).SetCode(v.Code).SetName(v.Name).SetEnabled(v.Enabled).SetVersion(v.Version).SetUpdatedAt(now).Save(tx); e != nil {
				return adminWorldStoreError(e)
			}
			if _, e = client.RpgLootEntry.Delete().Where(rpglootentry.LootTableIDEQ(v.ID)).Exec(tx); e != nil {
				return e
			}
		}
		for i := range v.Entries {
			id, e := store.newID.Next(tx)
			if e != nil {
				return e
			}
			v.Entries[i].ID = id
			x := v.Entries[i]
			if _, e = client.RpgLootEntry.Create().SetID(id).SetLootTableID(v.ID).SetItemID(x.ItemID).SetMinimumQuantity(x.MinimumQuantity).SetMaximumQuantity(x.MaximumQuantity).SetWeight(x.Weight).Save(tx); e != nil {
				return adminWorldStoreError(e)
			}
		}
		return store.auditAndComplete(tx, w, req, c.Write, "rpg.loot_table.saved", "rpg_loot_table", v.ID, before, v, now)
	})
	if e != nil {
		return AdminLootTable{}, e
	}
	return v, nil
}

// ListShops 返回商店及商品关系。
func (store *EntWorldStore) ListShops(ctx context.Context, size int) ([]AdminShop, error) {
	client := store.pool.Client(ctx)
	rows, e := client.RpgShop.Query().Order(rpgshop.ByCode(), rpgshop.ByID()).Limit(boundedPageSize(size, 200)).All(ctx)
	if e != nil {
		return nil, e
	}
	out := make([]AdminShop, 0, len(rows))
	idx := map[snowflake.ID]int{}
	for _, r := range rows {
		npc := snowflake.ID(0)
		if r.NpcID != nil {
			npc = *r.NpcID
		}
		idx[r.ID] = len(out)
		out = append(out, AdminShop{ID: r.ID, NPCID: npc, LocationID: r.LocationID, Code: r.Code, Name: r.Name, Enabled: r.Enabled, Version: r.Version, Items: []AdminShopItem{}})
	}
	items, e := client.RpgShopItem.Query().Order(rpgshopitem.ByShopID(), rpgshopitem.ByID()).All(ctx)
	if e != nil {
		return nil, e
	}
	for _, r := range items {
		if i, ok := idx[r.ShopID]; ok {
			out[i].Items = append(out[i].Items, AdminShopItem{ID: r.ID, ItemID: r.ItemID, CurrencyID: r.CurrencyID, BuyPrice: r.BuyPrice, SellPrice: r.SellPrice, StockLimit: r.StockLimit, Enabled: r.Enabled})
		}
	}
	return out, nil
}

// SaveShop 使用父版本完整替换商品关系。
func (store *EntWorldStore) SaveShop(ctx context.Context, c SaveShopCommand) (AdminShop, error) {
	v := c.Value
	v.Code, v.Name = strings.TrimSpace(v.Code), strings.TrimSpace(v.Name)
	update := v.ID.IsValid()
	if !validAdminWrite(c.Write) || !v.LocationID.IsValid() || !validNamed(v.Code, v.Name) || update && c.ExpectedVersion <= 0 {
		return AdminShop{}, ErrInvalidAdminWorld
	}
	for _, x := range v.Items {
		if !x.ItemID.IsValid() || !x.CurrencyID.IsValid() || x.BuyPrice < 0 || x.SellPrice != nil && *x.SellPrice < 0 || x.StockLimit != nil && *x.StockLimit <= 0 {
			return AdminShop{}, ErrInvalidAdminWorld
		}
	}
	if !update {
		id, e := store.newID.Next(ctx)
		if e != nil {
			return v, e
		}
		v.ID, v.Version = id, 1
	} else {
		v.Version = c.ExpectedVersion + 1
	}
	return store.saveShop(ctx, c, v, update)
}
func (store *EntWorldStore) saveShop(ctx context.Context, c SaveShopCommand, v AdminShop, update bool) (AdminShop, error) {
	digest, e := idempotency.Digest(struct {
		V AdminShop
		E int64
	}{v, c.ExpectedVersion})
	if e != nil {
		return v, e
	}
	now := time.Now().UTC()
	req := idempotency.Request{ActorAccountID: c.Write.ActorAccountID, OperationID: "rpg.shop.save", Key: c.Write.IdempotencyKey, RequestDigest: digest, CreatedAt: now}
	e = store.pool.WithinTransaction(ctx, func(tx context.Context) error {
		client := store.pool.Client(tx)
		w := idempotency.NewPersistentWriter(idempotency.NewAdminEntRecords(client, store.newID))
		replay, e := idempotency.ClaimResponse(tx, w, req, &v)
		if e != nil || replay {
			return e
		}
		if _, e = client.RpgLocation.Query().Where(rpglocation.IDEQ(v.LocationID)).Only(tx); e != nil {
			return adminWorldStoreError(e)
		}
		if v.NPCID.IsValid() {
			if _, e = client.RpgNpc.Query().Where(rpgnpc.IDEQ(v.NPCID)).Only(tx); e != nil {
				return adminWorldStoreError(e)
			}
		}
		for _, x := range v.Items {
			if _, e = client.GameItem.Query().Where(gameitem.IDEQ(x.ItemID)).Only(tx); e != nil {
				return adminWorldStoreError(e)
			}
			if _, e = client.GameCurrency.Query().Where(gamecurrency.IDEQ(x.CurrencyID)).Only(tx); e != nil {
				return adminWorldStoreError(e)
			}
		}
		var before *AdminShop
		if !update {
			b := client.RpgShop.Create().SetID(v.ID).SetLocationID(v.LocationID).SetCode(v.Code).SetName(v.Name).SetEnabled(v.Enabled).SetVersion(1).SetCreatedAt(now).SetUpdatedAt(now)
			if v.NPCID.IsValid() {
				b.SetNpcID(v.NPCID)
			}
			if _, e = b.Save(tx); e != nil {
				return adminWorldStoreError(e)
			}
		} else {
			row, e := client.RpgShop.Query().Where(rpgshop.IDEQ(v.ID)).Only(tx)
			if e != nil {
				return adminWorldStoreError(e)
			}
			old := AdminShop{ID: row.ID, LocationID: row.LocationID, Code: row.Code, Name: row.Name, Enabled: row.Enabled, Version: row.Version}
			before = &old
			b := client.RpgShop.UpdateOne(row).Where(rpgshop.VersionEQ(c.ExpectedVersion)).SetLocationID(v.LocationID).SetCode(v.Code).SetName(v.Name).SetEnabled(v.Enabled).SetVersion(v.Version).SetUpdatedAt(now)
			if v.NPCID.IsValid() {
				b.SetNpcID(v.NPCID)
			} else {
				b.ClearNpcID()
			}
			if _, e = b.Save(tx); e != nil {
				return adminWorldStoreError(e)
			}
			if _, e = client.RpgShopItem.Delete().Where(rpgshopitem.ShopIDEQ(v.ID)).Exec(tx); e != nil {
				return e
			}
		}
		for i := range v.Items {
			id, e := store.newID.Next(tx)
			if e != nil {
				return e
			}
			v.Items[i].ID = id
			x := v.Items[i]
			if _, e = client.RpgShopItem.Create().SetID(id).SetShopID(v.ID).SetItemID(x.ItemID).SetCurrencyID(x.CurrencyID).SetBuyPrice(x.BuyPrice).SetNillableSellPrice(x.SellPrice).SetNillableStockLimit(x.StockLimit).SetEnabled(x.Enabled).Save(tx); e != nil {
				return adminWorldStoreError(e)
			}
		}
		return store.auditAndComplete(tx, w, req, c.Write, "rpg.shop.saved", "rpg_shop", v.ID, before, v, now)
	})
	if e != nil {
		return AdminShop{}, e
	}
	return v, nil
}
