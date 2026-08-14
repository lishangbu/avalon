package persistence

import (
	"context"
	"strings"
	"time"

	avalonent "github.com/lishangbu/avalon/ent"
	"github.com/lishangbu/avalon/ent/gamecurrency"
	"github.com/lishangbu/avalon/ent/gameitem"
	"github.com/lishangbu/avalon/ent/playercharactershoppurchase"
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
	rpg "github.com/lishangbu/avalon/internal/rpg"
)

func validAdminWrite(write rpg.AdminWriteContext) bool {
	return write.ActorAccountID.IsValid() && idempotency.ValidKey(write.IdempotencyKey) && strings.TrimSpace(write.RequestID) != ""
}
func validNamed(code, name string) bool {
	return stablecode.Valid(strings.TrimSpace(code)) && strings.TrimSpace(name) != "" && len([]rune(strings.TrimSpace(name))) <= 120
}

// ListNPCs 返回全部 NPC 维护资料。
func (adapter *Adapters) ListNPCs(ctx context.Context, size int) ([]rpg.AdminNPC, error) {
	rows, err := adapter.pool.Client(ctx).RpgNpc.Query().Order(rpgnpc.ByCode(), rpgnpc.ByID()).Limit(boundedPageSize(size, 200)).All(ctx)
	if err != nil {
		return nil, err
	}
	out := make([]rpg.AdminNPC, 0, len(rows))
	for _, row := range rows {
		description := ""
		if row.Description != nil {
			description = *row.Description
		}
		out = append(out, rpg.AdminNPC{ID: row.ID, LocationID: row.LocationID, Code: row.Code, Name: row.Name, NPCType: row.NpcType, Description: description, Enabled: row.Enabled, Version: row.Version})
	}
	return out, nil
}

// SaveNPC 创建或更新 NPC；空 ID 表示创建。
func (adapter *Adapters) SaveNPC(ctx context.Context, command rpg.SaveNPCCommand) (rpg.AdminNPC, error) {
	value := command.Value
	value.Code, value.Name, value.NPCType, value.Description = strings.TrimSpace(value.Code), strings.TrimSpace(value.Name), strings.TrimSpace(value.NPCType), strings.TrimSpace(value.Description)
	update := value.ID.IsValid()
	types := map[string]bool{"story": true, "merchant": true, "trainer": true, "service": true, "ambient": true}
	if !validAdminWrite(command.Write) || !value.LocationID.IsValid() || !validNamed(value.Code, value.Name) || !types[value.NPCType] || len([]rune(value.Description)) > 4000 || update && command.ExpectedVersion <= 0 {
		return rpg.AdminNPC{}, rpg.ErrInvalidAdminWorld
	}
	if !update {
		id, err := adapter.newID.Next(ctx)
		if err != nil {
			return rpg.AdminNPC{}, err
		}
		value.ID, value.Version = id, 1
	} else {
		value.Version = command.ExpectedVersion + 1
	}
	digest, err := idempotency.Digest(struct {
		Value    rpg.AdminNPC
		Expected int64
	}{value, command.ExpectedVersion})
	if err != nil {
		return value, err
	}
	now := time.Now().UTC()
	request := idempotency.Request{ActorAccountID: command.Write.ActorAccountID, OperationID: "rpg.npc.save", Key: command.Write.IdempotencyKey, RequestDigest: digest, CreatedAt: now}
	err = adapter.pool.WithinTransaction(ctx, func(txctx context.Context) error {
		client := adapter.pool.Client(txctx)
		writer := idempotency.NewPersistentWriter(idempotency.NewAdminEntRecords(client, adapter.newID))
		replay, e := idempotency.ClaimResponse(txctx, writer, request, &value)
		if e != nil || replay {
			return e
		}
		if _, e = client.RpgLocation.Query().Where(rpglocation.IDEQ(value.LocationID)).Only(txctx); e != nil {
			return adminWorldRepositoryError(e)
		}
		var before *rpg.AdminNPC
		if !update {
			b := client.RpgNpc.Create().SetID(value.ID).SetLocationID(value.LocationID).SetCode(value.Code).SetName(value.Name).SetNpcType(value.NPCType).SetEnabled(value.Enabled).SetVersion(1).SetCreatedAt(now).SetUpdatedAt(now)
			if value.Description != "" {
				b.SetDescription(value.Description)
			}
			if _, e = b.Save(txctx); e != nil {
				return adminWorldRepositoryError(e)
			}
		} else {
			row, e := client.RpgNpc.Query().Where(rpgnpc.IDEQ(value.ID)).Only(txctx)
			if e != nil {
				return adminWorldRepositoryError(e)
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
				return adminWorldRepositoryError(e)
			}
		}
		return adapter.auditAndComplete(txctx, writer, request, command.Write, "rpg.npc.saved", "rpg_npc", value.ID, before, value, now)
	})
	if err != nil {
		return rpg.AdminNPC{}, err
	}
	return value, nil
}
func npcRow(row *avalonent.RpgNpc) rpg.AdminNPC {
	d := ""
	if row.Description != nil {
		d = *row.Description
	}
	return rpg.AdminNPC{ID: row.ID, LocationID: row.LocationID, Code: row.Code, Name: row.Name, NPCType: row.NpcType, Description: d, Enabled: row.Enabled, Version: row.Version}
}

// ListDialogues 返回对话及有序行。
func (adapter *Adapters) ListDialogues(ctx context.Context, size int) ([]rpg.AdminDialogue, error) {
	client := adapter.pool.Client(ctx)
	rows, e := client.RpgDialogue.Query().Order(rpgdialogue.ByCode(), rpgdialogue.ByID()).Limit(boundedPageSize(size, 200)).All(ctx)
	if e != nil {
		return nil, e
	}
	out := make([]rpg.AdminDialogue, 0, len(rows))
	idx := map[snowflake.ID]int{}
	for _, r := range rows {
		idx[r.ID] = len(out)
		out = append(out, rpg.AdminDialogue{ID: r.ID, NPCID: r.NpcID, Code: r.Code, Name: r.Name, Enabled: r.Enabled, Version: r.Version, Lines: []rpg.AdminDialogueLine{}})
	}
	lines, e := client.RpgDialogueLine.Query().Order(rpgdialogueline.ByDialogueID(), rpgdialogueline.ByPosition(), rpgdialogueline.ByID()).All(ctx)
	if e != nil {
		return nil, e
	}
	for _, r := range lines {
		if i, ok := idx[r.DialogueID]; ok {
			out[i].Lines = append(out[i].Lines, rpg.AdminDialogueLine{ID: r.ID, Position: r.Position, SpeakerName: r.SpeakerName, Content: r.Content})
		}
	}
	return out, nil
}

// SaveDialogue 使用父版本完整替换对话行。
func (adapter *Adapters) SaveDialogue(ctx context.Context, c rpg.SaveDialogueCommand) (rpg.AdminDialogue, error) {
	v := c.Value
	v.Code, v.Name = strings.TrimSpace(v.Code), strings.TrimSpace(v.Name)
	update := v.ID.IsValid()
	if !validAdminWrite(c.Write) || !v.NPCID.IsValid() || !validNamed(v.Code, v.Name) || update && c.ExpectedVersion <= 0 {
		return rpg.AdminDialogue{}, rpg.ErrInvalidAdminWorld
	}
	seen := map[int32]bool{}
	for i := range v.Lines {
		v.Lines[i].SpeakerName = strings.TrimSpace(v.Lines[i].SpeakerName)
		v.Lines[i].Content = strings.TrimSpace(v.Lines[i].Content)
		x := v.Lines[i]
		if x.Position <= 0 || seen[x.Position] || x.SpeakerName == "" || len([]rune(x.SpeakerName)) > 120 || x.Content == "" || len([]rune(x.Content)) > 8000 {
			return rpg.AdminDialogue{}, rpg.ErrInvalidAdminWorld
		}
		seen[x.Position] = true
	}
	if !update {
		id, e := adapter.newID.Next(ctx)
		if e != nil {
			return rpg.AdminDialogue{}, e
		}
		v.ID, v.Version = id, 1
	} else {
		v.Version = c.ExpectedVersion + 1
	}
	return adapter.saveDialogue(ctx, c, v, update)
}
func (adapter *Adapters) saveDialogue(ctx context.Context, c rpg.SaveDialogueCommand, v rpg.AdminDialogue, update bool) (rpg.AdminDialogue, error) {
	digest, e := idempotency.Digest(struct {
		Value    rpg.AdminDialogue
		Expected int64
	}{v, c.ExpectedVersion})
	if e != nil {
		return v, e
	}
	now := time.Now().UTC()
	req := idempotency.Request{ActorAccountID: c.Write.ActorAccountID, OperationID: "rpg.dialogue.save", Key: c.Write.IdempotencyKey, RequestDigest: digest, CreatedAt: now}
	e = adapter.pool.WithinTransaction(ctx, func(tx context.Context) error {
		client := adapter.pool.Client(tx)
		w := idempotency.NewPersistentWriter(idempotency.NewAdminEntRecords(client, adapter.newID))
		replay, e := idempotency.ClaimResponse(tx, w, req, &v)
		if e != nil || replay {
			return e
		}
		if _, e = client.RpgNpc.Query().Where(rpgnpc.IDEQ(v.NPCID)).Only(tx); e != nil {
			return adminWorldRepositoryError(e)
		}
		var before *rpg.AdminDialogue
		if !update {
			if _, e = client.RpgDialogue.Create().SetID(v.ID).SetNpcID(v.NPCID).SetCode(v.Code).SetName(v.Name).SetEnabled(v.Enabled).SetVersion(1).SetCreatedAt(now).SetUpdatedAt(now).Save(tx); e != nil {
				return adminWorldRepositoryError(e)
			}
		} else {
			row, e := client.RpgDialogue.Query().Where(rpgdialogue.IDEQ(v.ID)).Only(tx)
			if e != nil {
				return adminWorldRepositoryError(e)
			}
			old := rpg.AdminDialogue{ID: row.ID, NPCID: row.NpcID, Code: row.Code, Name: row.Name, Enabled: row.Enabled, Version: row.Version}
			before = &old
			if _, e = client.RpgDialogue.UpdateOne(row).Where(rpgdialogue.VersionEQ(c.ExpectedVersion)).SetNpcID(v.NPCID).SetCode(v.Code).SetName(v.Name).SetEnabled(v.Enabled).SetVersion(v.Version).SetUpdatedAt(now).Save(tx); e != nil {
				return adminWorldRepositoryError(e)
			}
			if _, e = client.RpgDialogueLine.Delete().Where(rpgdialogueline.DialogueIDEQ(v.ID)).Exec(tx); e != nil {
				return e
			}
		}
		for i := range v.Lines {
			id, e := adapter.newID.Next(tx)
			if e != nil {
				return e
			}
			v.Lines[i].ID = id
			x := v.Lines[i]
			if _, e = client.RpgDialogueLine.Create().SetID(id).SetDialogueID(v.ID).SetPosition(x.Position).SetSpeakerName(x.SpeakerName).SetContent(x.Content).Save(tx); e != nil {
				return adminWorldRepositoryError(e)
			}
		}
		return adapter.auditAndComplete(tx, w, req, c.Write, "rpg.dialogue.saved", "rpg_dialogue", v.ID, before, v, now)
	})
	if e != nil {
		return rpg.AdminDialogue{}, e
	}
	return v, nil
}

// ListLootTables 返回掉落表聚合。
func (adapter *Adapters) ListLootTables(ctx context.Context, size int) ([]rpg.AdminLootTable, error) {
	client := adapter.pool.Client(ctx)
	rows, e := client.RpgLootTable.Query().Order(rpgloottable.ByCode(), rpgloottable.ByID()).Limit(boundedPageSize(size, 200)).All(ctx)
	if e != nil {
		return nil, e
	}
	out := make([]rpg.AdminLootTable, 0, len(rows))
	idx := map[snowflake.ID]int{}
	for _, r := range rows {
		idx[r.ID] = len(out)
		out = append(out, rpg.AdminLootTable{ID: r.ID, Code: r.Code, Name: r.Name, Enabled: r.Enabled, Version: r.Version, Entries: []rpg.AdminLootEntry{}})
	}
	items, e := client.RpgLootEntry.Query().Where(rpglootentry.EnabledEQ(true)).Order(rpglootentry.ByLootTableID(), rpglootentry.ByID()).All(ctx)
	if e != nil {
		return nil, e
	}
	for _, r := range items {
		if i, ok := idx[r.LootTableID]; ok {
			out[i].Entries = append(out[i].Entries, rpg.AdminLootEntry{ID: r.ID, ItemID: r.ItemID, MinimumQuantity: r.MinimumQuantity, MaximumQuantity: r.MaximumQuantity, Weight: r.Weight})
		}
	}
	return out, nil
}

// SaveLootTable 使用父版本和稳定关系身份同步当前掉落项，移除项仅禁用以保留历史结算引用。
func (adapter *Adapters) SaveLootTable(ctx context.Context, c rpg.SaveLootTableCommand) (rpg.AdminLootTable, error) {
	v := c.Value
	v.Code, v.Name = strings.TrimSpace(v.Code), strings.TrimSpace(v.Name)
	update := v.ID.IsValid()
	if !validAdminWrite(c.Write) || !validNamed(v.Code, v.Name) || update && c.ExpectedVersion <= 0 {
		return rpg.AdminLootTable{}, rpg.ErrInvalidAdminWorld
	}
	for _, x := range v.Entries {
		if !x.ItemID.IsValid() || x.MinimumQuantity <= 0 || x.MaximumQuantity < x.MinimumQuantity || x.Weight <= 0 || !update && x.ID.IsValid() {
			return rpg.AdminLootTable{}, rpg.ErrInvalidAdminWorld
		}
	}
	if !update {
		id, e := adapter.newID.Next(ctx)
		if e != nil {
			return v, e
		}
		v.ID, v.Version = id, 1
	} else {
		v.Version = c.ExpectedVersion + 1
	}
	for i := range v.Entries {
		if v.Entries[i].ID.IsValid() {
			continue
		}
		id, err := adapter.newID.Next(ctx)
		if err != nil {
			return rpg.AdminLootTable{}, err
		}
		v.Entries[i].ID, v.Entries[i].NewRelation = id, true
	}
	return adapter.saveLoot(ctx, c, v, update)
}
func (adapter *Adapters) saveLoot(ctx context.Context, c rpg.SaveLootTableCommand, v rpg.AdminLootTable, update bool) (rpg.AdminLootTable, error) {
	digest, e := idempotency.Digest(struct {
		V rpg.AdminLootTable
		E int64
	}{v, c.ExpectedVersion})
	if e != nil {
		return v, e
	}
	now := time.Now().UTC()
	req := idempotency.Request{ActorAccountID: c.Write.ActorAccountID, OperationID: "rpg.loot_table.save", Key: c.Write.IdempotencyKey, RequestDigest: digest, CreatedAt: now}
	e = adapter.pool.WithinTransaction(ctx, func(tx context.Context) error {
		client := adapter.pool.Client(tx)
		w := idempotency.NewPersistentWriter(idempotency.NewAdminEntRecords(client, adapter.newID))
		replay, e := idempotency.ClaimResponse(tx, w, req, &v)
		if e != nil || replay {
			return e
		}
		for _, x := range v.Entries {
			if _, e = client.GameItem.Query().Where(gameitem.IDEQ(x.ItemID)).Only(tx); e != nil {
				return adminWorldRepositoryError(e)
			}
		}
		var before *rpg.AdminLootTable
		if !update {
			if _, e = client.RpgLootTable.Create().SetID(v.ID).SetCode(v.Code).SetName(v.Name).SetEnabled(v.Enabled).SetVersion(1).SetCreatedAt(now).SetUpdatedAt(now).Save(tx); e != nil {
				return adminWorldRepositoryError(e)
			}
		} else {
			row, e := client.RpgLootTable.Query().Where(rpgloottable.IDEQ(v.ID)).Only(tx)
			if e != nil {
				return adminWorldRepositoryError(e)
			}
			old := rpg.AdminLootTable{ID: row.ID, Code: row.Code, Name: row.Name, Enabled: row.Enabled, Version: row.Version}
			before = &old
			if _, e = client.RpgLootTable.UpdateOne(row).Where(rpgloottable.VersionEQ(c.ExpectedVersion)).SetCode(v.Code).SetName(v.Name).SetEnabled(v.Enabled).SetVersion(v.Version).SetUpdatedAt(now).Save(tx); e != nil {
				return adminWorldRepositoryError(e)
			}
		}
		existing, e := client.RpgLootEntry.Query().Where(rpglootentry.LootTableIDEQ(v.ID)).All(tx)
		if e != nil {
			return e
		}
		byID := make(map[snowflake.ID]*avalonent.RpgLootEntry, len(existing))
		retained := make(map[snowflake.ID]struct{}, len(v.Entries))
		for _, row := range existing {
			byID[row.ID] = row
		}
		for i := range v.Entries {
			x := v.Entries[i]
			retained[x.ID] = struct{}{}
			if x.NewRelation {
				_, e = client.RpgLootEntry.Create().SetID(x.ID).SetLootTableID(v.ID).SetItemID(x.ItemID).SetMinimumQuantity(x.MinimumQuantity).SetMaximumQuantity(x.MaximumQuantity).SetWeight(x.Weight).SetEnabled(true).Save(tx)
			} else if row, ok := byID[x.ID]; ok {
				_, e = client.RpgLootEntry.UpdateOne(row).SetItemID(x.ItemID).SetMinimumQuantity(x.MinimumQuantity).SetMaximumQuantity(x.MaximumQuantity).SetWeight(x.Weight).SetEnabled(true).Save(tx)
			} else {
				return rpg.ErrInvalidAdminWorld
			}
			if e != nil {
				return adminWorldRepositoryError(e)
			}
		}
		for _, row := range existing {
			if _, ok := retained[row.ID]; !ok {
				if _, e = client.RpgLootEntry.UpdateOne(row).SetEnabled(false).Save(tx); e != nil {
					return e
				}
			}
		}
		return adapter.auditAndComplete(tx, w, req, c.Write, "rpg.loot_table.saved", "rpg_loot_table", v.ID, before, v, now)
	})
	if e != nil {
		return rpg.AdminLootTable{}, e
	}
	return v, nil
}

// ListShops 返回商店及商品关系。
func (adapter *Adapters) ListShops(ctx context.Context, size int) ([]rpg.AdminShop, error) {
	client := adapter.pool.Client(ctx)
	rows, e := client.RpgShop.Query().Order(rpgshop.ByCode(), rpgshop.ByID()).Limit(boundedPageSize(size, 200)).All(ctx)
	if e != nil {
		return nil, e
	}
	out := make([]rpg.AdminShop, 0, len(rows))
	idx := map[snowflake.ID]int{}
	for _, r := range rows {
		npc := snowflake.ID(0)
		if r.NpcID != nil {
			npc = *r.NpcID
		}
		idx[r.ID] = len(out)
		out = append(out, rpg.AdminShop{ID: r.ID, NPCID: npc, LocationID: r.LocationID, Code: r.Code, Name: r.Name, Enabled: r.Enabled, Version: r.Version, Items: []rpg.AdminShopItem{}})
	}
	items, e := client.RpgShopItem.Query().Order(rpgshopitem.ByShopID(), rpgshopitem.ByID()).All(ctx)
	if e != nil {
		return nil, e
	}
	for _, r := range items {
		if i, ok := idx[r.ShopID]; ok {
			out[i].Items = append(out[i].Items, rpg.AdminShopItem{ID: r.ID, ItemID: r.ItemID, CurrencyID: r.CurrencyID, BuyPrice: r.BuyPrice, SellPrice: r.SellPrice, Enabled: r.Enabled})
		}
	}
	return out, nil
}

// SaveShop 使用父版本同步商品关系，并保留已经被购买事实引用的稳定关系身份。
func (adapter *Adapters) SaveShop(ctx context.Context, c rpg.SaveShopCommand) (rpg.AdminShop, error) {
	v := c.Value
	v.Code, v.Name = strings.TrimSpace(v.Code), strings.TrimSpace(v.Name)
	update := v.ID.IsValid()
	if !validAdminWrite(c.Write) || !v.LocationID.IsValid() || !validNamed(v.Code, v.Name) || update && c.ExpectedVersion <= 0 {
		return rpg.AdminShop{}, rpg.ErrInvalidAdminWorld
	}
	identities := make(map[[2]snowflake.ID]struct{}, len(v.Items))
	ids := make(map[snowflake.ID]struct{}, len(v.Items))
	for _, x := range v.Items {
		if !x.ItemID.IsValid() || !x.CurrencyID.IsValid() || x.BuyPrice < 0 || x.SellPrice != nil && *x.SellPrice < 0 || !update && x.ID.IsValid() {
			return rpg.AdminShop{}, rpg.ErrInvalidAdminWorld
		}
		identity := [2]snowflake.ID{x.ItemID, x.CurrencyID}
		if _, exists := identities[identity]; exists {
			return rpg.AdminShop{}, rpg.ErrInvalidAdminWorld
		}
		identities[identity] = struct{}{}
		if x.ID.IsValid() {
			if _, exists := ids[x.ID]; exists {
				return rpg.AdminShop{}, rpg.ErrInvalidAdminWorld
			}
			ids[x.ID] = struct{}{}
		}
	}
	if !update {
		id, e := adapter.newID.Next(ctx)
		if e != nil {
			return v, e
		}
		v.ID, v.Version = id, 1
	} else {
		v.Version = c.ExpectedVersion + 1
	}
	for i := range v.Items {
		if v.Items[i].ID.IsValid() {
			continue
		}
		id, e := adapter.newID.Next(ctx)
		if e != nil {
			return rpg.AdminShop{}, e
		}
		v.Items[i].ID = id
		v.Items[i].NewRelation = true
	}
	return adapter.saveShop(ctx, c, v, update)
}
func (adapter *Adapters) saveShop(ctx context.Context, c rpg.SaveShopCommand, v rpg.AdminShop, update bool) (rpg.AdminShop, error) {
	digest, e := idempotency.Digest(struct {
		V rpg.AdminShop
		E int64
	}{v, c.ExpectedVersion})
	if e != nil {
		return v, e
	}
	now := time.Now().UTC()
	req := idempotency.Request{ActorAccountID: c.Write.ActorAccountID, OperationID: "rpg.shop.save", Key: c.Write.IdempotencyKey, RequestDigest: digest, CreatedAt: now}
	e = adapter.pool.WithinTransaction(ctx, func(tx context.Context) error {
		client := adapter.pool.Client(tx)
		w := idempotency.NewPersistentWriter(idempotency.NewAdminEntRecords(client, adapter.newID))
		replay, e := idempotency.ClaimResponse(tx, w, req, &v)
		if e != nil || replay {
			return e
		}
		if _, e = client.RpgLocation.Query().Where(rpglocation.IDEQ(v.LocationID)).Only(tx); e != nil {
			return adminWorldRepositoryError(e)
		}
		if v.NPCID.IsValid() {
			if _, e = client.RpgNpc.Query().Where(rpgnpc.IDEQ(v.NPCID)).Only(tx); e != nil {
				return adminWorldRepositoryError(e)
			}
		}
		for _, x := range v.Items {
			if _, e = client.GameItem.Query().Where(gameitem.IDEQ(x.ItemID)).Only(tx); e != nil {
				return adminWorldRepositoryError(e)
			}
			if _, e = client.GameCurrency.Query().Where(gamecurrency.IDEQ(x.CurrencyID)).Only(tx); e != nil {
				return adminWorldRepositoryError(e)
			}
		}
		var before *rpg.AdminShop
		if !update {
			b := client.RpgShop.Create().SetID(v.ID).SetLocationID(v.LocationID).SetCode(v.Code).SetName(v.Name).SetEnabled(v.Enabled).SetVersion(1).SetCreatedAt(now).SetUpdatedAt(now)
			if v.NPCID.IsValid() {
				b.SetNpcID(v.NPCID)
			}
			if _, e = b.Save(tx); e != nil {
				return adminWorldRepositoryError(e)
			}
		} else {
			row, e := client.RpgShop.Query().Where(rpgshop.IDEQ(v.ID)).Only(tx)
			if e != nil {
				return adminWorldRepositoryError(e)
			}
			existing, queryErr := client.RpgShopItem.Query().Where(rpgshopitem.ShopIDEQ(v.ID)).Order(rpgshopitem.ByID()).All(tx)
			if queryErr != nil {
				return adminWorldRepositoryError(queryErr)
			}
			old := rpg.AdminShop{ID: row.ID, LocationID: row.LocationID, Code: row.Code, Name: row.Name, Enabled: row.Enabled, Version: row.Version, Items: make([]rpg.AdminShopItem, 0, len(existing))}
			if row.NpcID != nil {
				old.NPCID = *row.NpcID
			}
			for _, item := range existing {
				old.Items = append(old.Items, rpg.AdminShopItem{ID: item.ID, ItemID: item.ItemID, CurrencyID: item.CurrencyID, BuyPrice: item.BuyPrice, SellPrice: item.SellPrice, Enabled: item.Enabled})
			}
			before = &old
			b := client.RpgShop.UpdateOne(row).Where(rpgshop.VersionEQ(c.ExpectedVersion)).SetLocationID(v.LocationID).SetCode(v.Code).SetName(v.Name).SetEnabled(v.Enabled).SetVersion(v.Version).SetUpdatedAt(now)
			if v.NPCID.IsValid() {
				b.SetNpcID(v.NPCID)
			} else {
				b.ClearNpcID()
			}
			if _, e = b.Save(tx); e != nil {
				return adminWorldRepositoryError(e)
			}
			if e = adapter.syncShopItems(tx, client, &v, existing); e != nil {
				return e
			}
		}
		if !update {
			for i := range v.Items {
				x := v.Items[i]
				if _, e = client.RpgShopItem.Create().SetID(x.ID).SetShopID(v.ID).SetItemID(x.ItemID).SetCurrencyID(x.CurrencyID).SetBuyPrice(x.BuyPrice).SetNillableSellPrice(x.SellPrice).SetEnabled(x.Enabled).Save(tx); e != nil {
					return adminWorldRepositoryError(e)
				}
			}
		}
		return adapter.auditAndComplete(tx, w, req, c.Write, "rpg.shop.saved", "rpg_shop", v.ID, before, v, now)
	})
	if e != nil {
		return rpg.AdminShop{}, e
	}
	return v, nil
}

// syncShopItems 按稳定关系身份同步商品；已有购买历史的移除项只禁用，以保持不可变购买事实的外键有效。
func (adapter *Adapters) syncShopItems(ctx context.Context, client *avalonent.Client, value *rpg.AdminShop, existing []*avalonent.RpgShopItem) error {
	byID := make(map[snowflake.ID]*avalonent.RpgShopItem, len(existing))
	retained := make(map[snowflake.ID]struct{}, len(value.Items))
	for _, row := range existing {
		byID[row.ID] = row
	}
	for i := range value.Items {
		x := &value.Items[i]
		if !x.NewRelation {
			row, ok := byID[x.ID]
			if !ok {
				return rpg.ErrInvalidAdminWorld
			}
			retained[x.ID] = struct{}{}
			if _, err := client.RpgShopItem.UpdateOne(row).SetItemID(x.ItemID).SetCurrencyID(x.CurrencyID).SetBuyPrice(x.BuyPrice).SetNillableSellPrice(x.SellPrice).SetEnabled(x.Enabled).Save(ctx); err != nil {
				return adminWorldRepositoryError(err)
			}
			continue
		}
		retained[x.ID] = struct{}{}
		if _, err := client.RpgShopItem.Create().SetID(x.ID).SetShopID(value.ID).SetItemID(x.ItemID).SetCurrencyID(x.CurrencyID).SetBuyPrice(x.BuyPrice).SetNillableSellPrice(x.SellPrice).SetEnabled(x.Enabled).Save(ctx); err != nil {
			return adminWorldRepositoryError(err)
		}
	}
	for _, row := range existing {
		if _, ok := retained[row.ID]; ok {
			continue
		}
		referenced, err := client.PlayerCharacterShopPurchase.Query().Where(playercharactershoppurchase.ShopItemIDEQ(row.ID)).Exist(ctx)
		if err != nil {
			return adminWorldRepositoryError(err)
		}
		if referenced {
			if _, err = client.RpgShopItem.UpdateOne(row).SetEnabled(false).Save(ctx); err != nil {
				return adminWorldRepositoryError(err)
			}
			value.Items = append(value.Items, rpg.AdminShopItem{ID: row.ID, ItemID: row.ItemID, CurrencyID: row.CurrencyID, BuyPrice: row.BuyPrice, SellPrice: row.SellPrice, Enabled: false})
			continue
		}
		if err = client.RpgShopItem.DeleteOne(row).Exec(ctx); err != nil {
			return adminWorldRepositoryError(err)
		}
	}
	return nil
}
