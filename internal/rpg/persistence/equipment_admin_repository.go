package persistence

import (
	"context"
	"encoding/json"
	"sort"
	"strings"
	"time"

	avalonent "github.com/lishangbu/avalon/ent"
	"github.com/lishangbu/avalon/ent/gamecurrency"
	"github.com/lishangbu/avalon/ent/gameequipment"
	"github.com/lishangbu/avalon/ent/gameequipmentprofession"
	"github.com/lishangbu/avalon/ent/gameequipmentstatmodifier"
	"github.com/lishangbu/avalon/ent/gameitem"
	"github.com/lishangbu/avalon/ent/gamestat"
	"github.com/lishangbu/avalon/ent/playercharacter"
	"github.com/lishangbu/avalon/ent/playercharacterequipmentinstance"
	"github.com/lishangbu/avalon/ent/playercharacterequipmenttransaction"
	"github.com/lishangbu/avalon/ent/rpgprofession"
	platformaudit "github.com/lishangbu/avalon/internal/platform/audit"
	"github.com/lishangbu/avalon/internal/platform/database"
	"github.com/lishangbu/avalon/internal/platform/idempotency"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
	rpg "github.com/lishangbu/avalon/internal/rpg"
)

// ListEquipments 按稳定 ID 升序读取一页完整装备资料聚合和实例统计。
func (adapter *Adapters) ListEquipments(ctx context.Context, size int, cursor string) (rpg.AdminEquipmentPage, error) {
	if size < 1 || size > 100 {
		size = 50
	}
	afterID, _, err := decodeEquipmentCursor(cursor, "admin-equipments", "", false)
	if err != nil {
		return rpg.AdminEquipmentPage{}, err
	}
	query := adapter.pool.Client(ctx).GameEquipment.Query()
	if afterID.IsValid() {
		query.Where(gameequipment.IDGT(afterID))
	}
	rows, err := query.WithItem().WithProfessions().WithStatModifiers().Order(avalonent.Asc(gameequipment.FieldID)).Limit(size + 1).All(ctx)
	if err != nil {
		return rpg.AdminEquipmentPage{}, err
	}
	hasMore := len(rows) > size
	if hasMore {
		rows = rows[:size]
	}
	instanceCounts := make(map[snowflake.ID]int64, len(rows))
	if len(rows) > 0 {
		ids := make([]snowflake.ID, 0, len(rows))
		for _, row := range rows {
			ids = append(ids, row.ID)
		}
		var counts []struct {
			EquipmentID snowflake.ID `json:"equipment_id"`
			Count       int64        `json:"count"`
		}
		if err = adapter.pool.Client(ctx).PlayerCharacterEquipmentInstance.Query().Where(playercharacterequipmentinstance.EquipmentIDIn(ids...)).GroupBy(playercharacterequipmentinstance.FieldEquipmentID).Aggregate(avalonent.Count()).Scan(ctx, &counts); err != nil {
			return rpg.AdminEquipmentPage{}, err
		}
		for _, count := range counts {
			instanceCounts[count.EquipmentID] = count.Count
		}
	}
	result := rpg.AdminEquipmentPage{Items: make([]rpg.AdminEquipment, 0, len(rows))}
	for _, row := range rows {
		value := adminEquipmentView(row)
		value.InstanceCount = instanceCounts[row.ID]
		result.Items = append(result.Items, value)
	}
	if hasMore {
		result.NextCursor, err = encodeEquipmentCursor("admin-equipments", "", rows[len(rows)-1].ID, time.Time{})
		if err != nil {
			return rpg.AdminEquipmentPage{}, err
		}
	}
	return result, nil
}

// ListEquipmentOptions 按 Item 名称和装备身份返回全部启用装备的轻量引用。
// 该查询专供管理表单选择，不加载职业、属性修正、实例统计或规则聚合。
func (adapter *Adapters) ListEquipmentOptions(ctx context.Context) ([]rpg.EquipmentOption, error) {
	rows, err := adapter.pool.Client(ctx).GameEquipment.Query().Where(gameequipment.EnabledEQ(true)).WithItem(func(query *avalonent.GameItemQuery) {
		query.Where(gameitem.EnabledEQ(true))
	}).All(ctx)
	if err != nil {
		return nil, err
	}
	result := make([]rpg.EquipmentOption, 0, len(rows))
	for _, row := range rows {
		if row.Edges.Item == nil {
			continue
		}
		result = append(result, rpg.EquipmentOption{ID: row.ID, ItemName: row.Edges.Item.Name})
	}
	sort.Slice(result, func(left, right int) bool {
		if result[left].ItemName != result[right].ItemName {
			return result[left].ItemName < result[right].ItemName
		}
		return result[left].ID < result[right].ID
	})
	return result, nil
}

// SaveEquipment 在单一事务内创建或乐观替换主资料与全部规范关系。
func (adapter *Adapters) SaveEquipment(ctx context.Context, command rpg.SaveEquipmentCommand) (rpg.AdminEquipment, error) {
	value := command.Value
	update := value.ID.IsValid()
	if !validAdminWrite(command.Write) || !value.ItemID.IsValid() || !value.SellCurrencyID.IsValid() || value.MinimumLevel <= 0 || value.SellPrice < 0 || !validEquipmentCatalogShape(value) || !validAdminEquipmentRelations(value) || update && command.ExpectedVersion <= 0 {
		return rpg.AdminEquipment{}, rpg.ErrInvalidAdminWorld
	}
	// 幂等摘要只编码客户端提交的权威字段。创建时主键和关系主键尚未生成，不能把零值 Snowflake
	// Identifier 或查询投影字段纳入摘要，否则合法创建命令会在进入事务前编码失败。
	digestModifiers := make([]struct {
		StatID        snowflake.ID
		FlatValue     int64
		PercentageBPS int32
	}, 0, len(value.StatModifiers))
	for _, modifier := range value.StatModifiers {
		digestModifiers = append(digestModifiers, struct {
			StatID        snowflake.ID
			FlatValue     int64
			PercentageBPS int32
		}{modifier.StatID, modifier.FlatValue, modifier.PercentageBPS})
	}
	digest, err := idempotency.Digest(struct {
		ID                     string
		ItemID, SellCurrencyID snowflake.ID
		SlotType               rpg.EquipmentSlotType
		Handedness             rpg.EquipmentHandedness
		MinimumLevel           int32
		SellPrice              int64
		Enabled                bool
		ProfessionIDs          []snowflake.ID
		StatModifiers          any
		RuleTimings            []string
		ExpectedVersion        int64
	}{identifierString(value.ID), value.ItemID, value.SellCurrencyID, value.SlotType, value.Handedness, value.MinimumLevel, value.SellPrice, value.Enabled, value.ProfessionIDs, digestModifiers, value.RuleTimings, command.ExpectedVersion})
	if err != nil {
		return rpg.AdminEquipment{}, err
	}
	now := time.Now().UTC()
	request := idempotency.Request{ActorAccountID: command.Write.ActorAccountID, OperationID: "rpg.equipment.save", Key: command.Write.IdempotencyKey, RequestDigest: digest, CreatedAt: now}
	result := value
	err = adapter.pool.WithinTransaction(ctx, func(txctx context.Context) error {
		client := adapter.pool.Client(txctx)
		writer := idempotency.NewPersistentWriter(idempotency.NewAdminEntRecords(client, adapter.newID))
		replay, claimErr := idempotency.ClaimResponse(txctx, writer, request, &result)
		if claimErr != nil || replay {
			return claimErr
		}
		if !update {
			id, idErr := adapter.newID.Next(txctx)
			if idErr != nil {
				return idErr
			}
			value.ID = id
		}
		item, err := client.GameItem.Query().Where(gameitem.IDEQ(value.ItemID), gameitem.UsageTypeEQ("equipment")).Only(txctx)
		if avalonent.IsNotFound(err) {
			return rpg.ErrEquipmentNotFound
		}
		if err != nil {
			return err
		}
		if exists, queryErr := client.GameCurrency.Query().Where(gamecurrency.IDEQ(value.SellCurrencyID), gamecurrency.EnabledEQ(true)).Exist(txctx); queryErr != nil {
			return queryErr
		} else if !exists {
			return rpg.ErrAdminWorldNotFound
		}
		if len(value.ProfessionIDs) > 0 {
			count, queryErr := client.RpgProfession.Query().Where(rpgprofession.IDIn(value.ProfessionIDs...)).Count(txctx)
			if queryErr != nil {
				return queryErr
			}
			if count != len(value.ProfessionIDs) {
				return rpg.ErrAdminWorldNotFound
			}
		}
		statIDs := make([]snowflake.ID, 0, len(value.StatModifiers))
		for _, modifier := range value.StatModifiers {
			statIDs = append(statIDs, modifier.StatID)
		}
		if len(statIDs) > 0 {
			count, queryErr := client.GameStat.Query().Where(gamestat.IDIn(statIDs...), gamestat.EnabledEQ(true)).Count(txctx)
			if queryErr != nil {
				return queryErr
			}
			if count != len(statIDs) {
				return rpg.ErrAdminWorldNotFound
			}
		}
		rulesJSON, _ := json.Marshal(ruleTimingDocument(value.RuleTimings))
		compiled, err := rpg.CompileEquipmentRules(rulesJSON)
		if err != nil || value.Enabled && string(compiled) != "{}" {
			return rpg.ErrEquipmentRulesInvalid
		}
		var before *rpg.AdminEquipment
		var row *avalonent.GameEquipment
		if update {
			row, err = client.GameEquipment.Query().Where(gameequipment.IDEQ(value.ID)).WithInstances().ForUpdate().Only(txctx)
			if avalonent.IsNotFound(err) {
				return rpg.ErrEquipmentNotFound
			}
			if err != nil {
				return err
			}
			if row.Version != command.ExpectedVersion {
				return rpg.ErrAdminWorldConflict
			}
			if len(row.Edges.Instances) > 0 && (row.ItemID != value.ItemID || row.SlotType != string(value.SlotType) || optionalString(row.Handedness) != string(value.Handedness)) {
				return rpg.ErrAdminWorldConflict
			}
			oldRow, queryErr := client.GameEquipment.Query().Where(gameequipment.IDEQ(value.ID)).WithItem().WithProfessions().WithStatModifiers().WithInstances().Only(txctx)
			if queryErr != nil {
				return queryErr
			}
			old := adminEquipmentView(oldRow)
			before = &old
			builder := client.GameEquipment.UpdateOne(row).Where(gameequipment.VersionEQ(command.ExpectedVersion)).SetItemID(value.ItemID).SetSellCurrencyID(value.SellCurrencyID).SetSlotType(string(value.SlotType)).SetMinimumLevel(value.MinimumLevel).SetSellPrice(value.SellPrice).SetRules(compiled).SetEnabled(value.Enabled).SetVersion(row.Version + 1).SetUpdatedAt(now)
			if value.Handedness == "" {
				builder.ClearHandedness()
			} else {
				builder.SetHandedness(string(value.Handedness))
			}
			row, err = builder.Save(txctx)
		} else {
			builder := client.GameEquipment.Create().SetID(value.ID).SetItemID(value.ItemID).SetSellCurrencyID(value.SellCurrencyID).SetSlotType(string(value.SlotType)).SetMinimumLevel(value.MinimumLevel).SetSellPrice(value.SellPrice).SetRules(compiled).SetEnabled(value.Enabled).SetVersion(1).SetCreatedAt(now).SetUpdatedAt(now)
			if value.Handedness != "" {
				builder.SetHandedness(string(value.Handedness))
			}
			row, err = builder.Save(txctx)
		}
		if err != nil {
			return err
		}
		if err = adapter.syncEquipmentProfessions(txctx, client, row.ID, value.ProfessionIDs); err != nil {
			return err
		}
		if err = adapter.syncEquipmentStatModifiers(txctx, client, row.ID, value.StatModifiers); err != nil {
			return err
		}
		row, err = client.GameEquipment.Query().Where(gameequipment.IDEQ(row.ID)).WithItem().WithProfessions().WithStatModifiers().WithInstances().Only(txctx)
		if err != nil {
			return err
		}
		result = adminEquipmentView(row)
		result.ItemName = item.Name
		return adapter.auditAndComplete(txctx, writer, request, command.Write, "rpg.equipment.saved", "game_equipment", row.ID, before, result, now)
	})
	return result, adminWorldRepositoryError(err)
}

// syncEquipmentProfessions 按 Profession Identifier 差量同步白名单关系。
// 未变化关系保留原 Snowflake Identifier，只有真正新增或移除的领域关系发生持久化变化。
func (adapter *Adapters) syncEquipmentProfessions(ctx context.Context, client *avalonent.Client, equipmentID snowflake.ID, desired []snowflake.ID) error {
	rows, err := client.GameEquipmentProfession.Query().Where(gameequipmentprofession.EquipmentIDEQ(equipmentID)).All(ctx)
	if err != nil {
		return err
	}
	desiredSet := make(map[snowflake.ID]struct{}, len(desired))
	for _, professionID := range desired {
		desiredSet[professionID] = struct{}{}
	}
	existing := make(map[snowflake.ID]struct{}, len(rows))
	for _, row := range rows {
		if _, keep := desiredSet[row.ProfessionID]; keep {
			existing[row.ProfessionID] = struct{}{}
			continue
		}
		if err = client.GameEquipmentProfession.DeleteOne(row).Exec(ctx); err != nil {
			return err
		}
	}
	for _, professionID := range desired {
		if _, keep := existing[professionID]; keep {
			continue
		}
		id, idErr := adapter.newID.Next(ctx)
		if idErr != nil {
			return idErr
		}
		if _, err = client.GameEquipmentProfession.Create().SetID(id).SetEquipmentID(equipmentID).SetProfessionID(professionID).Save(ctx); err != nil {
			return err
		}
	}
	return nil
}

// syncEquipmentStatModifiers 按 Stat Identifier 差量同步属性修正关系。
// 数值变化原位更新关系，避免完整保存操作无意义地替换稳定关系身份。
func (adapter *Adapters) syncEquipmentStatModifiers(ctx context.Context, client *avalonent.Client, equipmentID snowflake.ID, desired []rpg.AdminEquipmentStatModifier) error {
	rows, err := client.GameEquipmentStatModifier.Query().Where(gameequipmentstatmodifier.EquipmentIDEQ(equipmentID)).All(ctx)
	if err != nil {
		return err
	}
	desiredByStat := make(map[snowflake.ID]rpg.AdminEquipmentStatModifier, len(desired))
	for _, modifier := range desired {
		desiredByStat[modifier.StatID] = modifier
	}
	existing := make(map[snowflake.ID]struct{}, len(rows))
	for _, row := range rows {
		modifier, keep := desiredByStat[row.StatID]
		if !keep {
			if err = client.GameEquipmentStatModifier.DeleteOne(row).Exec(ctx); err != nil {
				return err
			}
			continue
		}
		existing[row.StatID] = struct{}{}
		if row.FlatValue != modifier.FlatValue || row.PercentageBps != modifier.PercentageBPS {
			if _, err = client.GameEquipmentStatModifier.UpdateOne(row).SetFlatValue(modifier.FlatValue).SetPercentageBps(modifier.PercentageBPS).Save(ctx); err != nil {
				return err
			}
		}
	}
	for _, modifier := range desired {
		if _, keep := existing[modifier.StatID]; keep {
			continue
		}
		id, idErr := adapter.newID.Next(ctx)
		if idErr != nil {
			return idErr
		}
		if _, err = client.GameEquipmentStatModifier.Create().SetID(id).SetEquipmentID(equipmentID).SetStatID(modifier.StatID).SetFlatValue(modifier.FlatValue).SetPercentageBps(modifier.PercentageBPS).Save(ctx); err != nil {
			return err
		}
	}
	return nil
}

// ListAdminEquipmentInstances 按获得时间和稳定 ID 倒序返回一页管理诊断实例。
func (adapter *Adapters) ListAdminEquipmentInstances(ctx context.Context, command rpg.AdminEquipmentInstanceQuery) (rpg.AdminEquipmentInstancePage, error) {
	if command.SourceType != "" && !validEquipmentSourceType(command.SourceType) {
		return rpg.AdminEquipmentInstancePage{}, rpg.ErrInvalidEquipmentFilter
	}
	if command.PageSize < 1 || command.PageSize > 100 {
		command.PageSize = 50
	}
	filterHash := equipmentFilterHash(equipmentOptionalIDText(command.PlayerCharacterID), equipmentOptionalIDText(command.EquipmentID), equipmentOptionalBoolText(command.Equipped), command.SourceType)
	afterID, afterTime, err := decodeEquipmentCursor(command.Cursor, "admin-instances", filterHash, true)
	if err != nil {
		return rpg.AdminEquipmentInstancePage{}, err
	}
	query := adapter.pool.Client(ctx).PlayerCharacterEquipmentInstance.Query().WithEquipment(func(q *avalonent.GameEquipmentQuery) { q.WithItem() }).WithLoadoutEntry()
	if command.PlayerCharacterID.IsValid() {
		query.Where(playercharacterequipmentinstance.PlayerCharacterIDEQ(command.PlayerCharacterID))
	}
	if command.EquipmentID.IsValid() {
		query.Where(playercharacterequipmentinstance.EquipmentIDEQ(command.EquipmentID))
	}
	if command.SourceType != "" {
		query.Where(playercharacterequipmentinstance.SourceTypeEQ(command.SourceType))
	}
	if command.Equipped != nil {
		if *command.Equipped {
			query.Where(playercharacterequipmentinstance.HasLoadoutEntry())
		} else {
			query.Where(playercharacterequipmentinstance.Not(playercharacterequipmentinstance.HasLoadoutEntry()))
		}
	}
	if afterID.IsValid() {
		query.Where(playercharacterequipmentinstance.Or(playercharacterequipmentinstance.AcquiredAtLT(afterTime), playercharacterequipmentinstance.And(playercharacterequipmentinstance.AcquiredAtEQ(afterTime), playercharacterequipmentinstance.IDLT(afterID))))
	}
	rows, err := query.Order(avalonent.Desc(playercharacterequipmentinstance.FieldAcquiredAt), avalonent.Desc(playercharacterequipmentinstance.FieldID)).Limit(command.PageSize + 1).All(ctx)
	if err != nil {
		return rpg.AdminEquipmentInstancePage{}, err
	}
	hasMore := len(rows) > command.PageSize
	if hasMore {
		rows = rows[:command.PageSize]
	}
	result := rpg.AdminEquipmentInstancePage{Items: make([]rpg.AdminEquipmentInstance, 0, len(rows))}
	for _, row := range rows {
		value := rpg.AdminEquipmentInstance{ID: row.ID, PlayerCharacterID: row.PlayerCharacterID, EquipmentID: row.EquipmentID, SourceReferenceID: optionalIdentifier(row.SourceReferenceID), SourceType: row.SourceType, Version: row.Version, AcquiredAt: row.AcquiredAt}
		if row.SoldAt != nil {
			value.SoldAt = row.SoldAt.UTC()
		}
		if row.Edges.Equipment != nil && row.Edges.Equipment.Edges.Item != nil {
			value.ItemName = row.Edges.Equipment.Edges.Item.Name
		}
		if len(row.Edges.LoadoutEntry) > 0 {
			value.EquippedSlot = row.Edges.LoadoutEntry[0].Slot
		}
		result.Items = append(result.Items, value)
	}
	if hasMore {
		last := rows[len(rows)-1]
		result.NextCursor, err = encodeEquipmentCursor("admin-instances", filterHash, last.ID, last.AcquiredAt)
		if err != nil {
			return rpg.AdminEquipmentInstancePage{}, err
		}
	}
	return result, nil
}

// ListEquipmentTransactions 按提交时间和稳定 ID 倒序返回一页不可变资产流水。
func (adapter *Adapters) ListEquipmentTransactions(ctx context.Context, command rpg.EquipmentTransactionQuery) (rpg.AdminEquipmentTransactionPage, error) {
	if command.Action != "" && !validEquipmentTransactionAction(command.Action) {
		return rpg.AdminEquipmentTransactionPage{}, rpg.ErrInvalidEquipmentFilter
	}
	if command.PageSize < 1 || command.PageSize > 100 {
		command.PageSize = 50
	}
	filterHash := equipmentFilterHash(equipmentOptionalIDText(command.PlayerCharacterID), equipmentOptionalIDText(command.EquipmentInstanceID), command.Action)
	afterID, afterTime, err := decodeEquipmentCursor(command.Cursor, "admin-transactions", filterHash, true)
	if err != nil {
		return rpg.AdminEquipmentTransactionPage{}, err
	}
	query := adapter.pool.Client(ctx).PlayerCharacterEquipmentTransaction.Query()
	if command.PlayerCharacterID.IsValid() {
		query.Where(playercharacterequipmenttransaction.PlayerCharacterIDEQ(command.PlayerCharacterID))
	}
	if command.EquipmentInstanceID.IsValid() {
		query.Where(playercharacterequipmenttransaction.EquipmentInstanceIDEQ(command.EquipmentInstanceID))
	}
	if command.Action != "" {
		query.Where(playercharacterequipmenttransaction.ActionEQ(command.Action))
	}
	if afterID.IsValid() {
		query.Where(playercharacterequipmenttransaction.Or(playercharacterequipmenttransaction.CreatedAtLT(afterTime), playercharacterequipmenttransaction.And(playercharacterequipmenttransaction.CreatedAtEQ(afterTime), playercharacterequipmenttransaction.IDLT(afterID))))
	}
	rows, err := query.Order(avalonent.Desc(playercharacterequipmenttransaction.FieldCreatedAt), avalonent.Desc(playercharacterequipmenttransaction.FieldID)).Limit(command.PageSize + 1).All(ctx)
	if err != nil {
		return rpg.AdminEquipmentTransactionPage{}, err
	}
	hasMore := len(rows) > command.PageSize
	if hasMore {
		rows = rows[:command.PageSize]
	}
	result := rpg.AdminEquipmentTransactionPage{Items: make([]rpg.AdminEquipmentTransaction, 0, len(rows))}
	for _, row := range rows {
		result.Items = append(result.Items, rpg.AdminEquipmentTransaction{ID: row.ID, OperationID: row.OperationID, PlayerCharacterID: row.PlayerCharacterID, InstanceID: row.EquipmentInstanceID, Action: row.Action, SourceType: optionalString(row.SourceType), Slot: optionalString(row.Slot), CreatedAt: row.CreatedAt})
	}
	if hasMore {
		last := rows[len(rows)-1]
		result.NextCursor, err = encodeEquipmentCursor("admin-transactions", filterHash, last.ID, last.CreatedAt)
		if err != nil {
			return rpg.AdminEquipmentTransactionPage{}, err
		}
	}
	return result, nil
}

// GrantEquipment 为每个数量单位建立独立实例，并共享 Operation Identifier、提交时间和 Outbox。
func (adapter *Adapters) GrantEquipment(ctx context.Context, command rpg.GrantEquipmentCommand) (rpg.GrantEquipmentResult, error) {
	var result rpg.GrantEquipmentResult
	command.Reason = strings.TrimSpace(command.Reason)
	if !validGrantEquipmentCommand(command) {
		return result, rpg.ErrInvalidAdminWorld
	}
	digest, err := idempotency.Digest(struct {
		PlayerCharacterID snowflake.ID
		EquipmentID       snowflake.ID
		Quantity          int32
		Reason            string
	}{command.PlayerCharacterID, command.EquipmentID, command.Quantity, command.Reason})
	if err != nil {
		return result, err
	}
	now := command.Now.UTC()
	if command.Now.IsZero() {
		now = time.Now().UTC()
	}
	request := idempotency.Request{ActorAccountID: command.Write.ActorAccountID, OperationID: "rpg.equipment.grant", Key: command.Write.IdempotencyKey, RequestDigest: digest, CreatedAt: now}
	err = adapter.pool.WithinTransaction(ctx, func(txctx context.Context) error {
		client := adapter.pool.Client(txctx)
		writer := idempotency.NewPersistentWriter(idempotency.NewAdminEntRecords(client, adapter.newID))
		replay, claimErr := idempotency.ClaimResponse(txctx, writer, request, &result)
		if claimErr != nil || replay {
			return claimErr
		}
		if exists, queryErr := client.PlayerCharacter.Query().Where(playercharacter.IDEQ(command.PlayerCharacterID)).Exist(txctx); queryErr != nil {
			return queryErr
		} else if !exists {
			return rpg.ErrAdminWorldNotFound
		}
		exists, err := client.GameEquipment.Query().Where(
			gameequipment.IDEQ(command.EquipmentID),
			gameequipment.EnabledEQ(true),
			gameequipment.HasItemWith(gameitem.EnabledEQ(true)),
		).Exist(txctx)
		if err != nil {
			return err
		}
		if !exists {
			return rpg.ErrEquipmentNotFound
		}
		operationID, err := adapter.newID.Next(txctx)
		if err != nil {
			return err
		}
		result = rpg.GrantEquipmentResult{OperationID: operationID, InstanceIDs: make([]snowflake.ID, 0, command.Quantity)}
		for range command.Quantity {
			id, idErr := adapter.newID.Next(txctx)
			if idErr != nil {
				return idErr
			}
			if _, err = client.PlayerCharacterEquipmentInstance.Create().SetID(id).SetPlayerCharacterID(command.PlayerCharacterID).SetEquipmentID(command.EquipmentID).SetSourceType("admin").SetSourceReferenceID(operationID).SetVersion(1).SetAcquiredAt(now).SetUpdatedAt(now).Save(txctx); err != nil {
				return err
			}
			if err = adapter.createEquipmentTransaction(txctx, client, operationID, command.PlayerCharacterID, id, "acquire", "admin", "", now); err != nil {
				return err
			}
			result.InstanceIDs = append(result.InstanceIDs, id)
		}
		if err := adapter.createEquipmentOutbox(txctx, client, "rpg.equipment.acquired.v1", operationID, command.PlayerCharacterID, now); err != nil {
			return err
		}
		changes, err := json.Marshal(struct {
			PlayerCharacterID snowflake.ID   `json:"player_character_id"`
			EquipmentID       snowflake.ID   `json:"equipment_id"`
			Quantity          int32          `json:"quantity"`
			InstanceIDs       []snowflake.ID `json:"instance_ids"`
		}{command.PlayerCharacterID, command.EquipmentID, command.Quantity, result.InstanceIDs})
		if err != nil {
			return err
		}
		auditID, err := adapter.newID.Next(txctx)
		if err != nil {
			return err
		}
		objectID := operationID.String()
		if err := platformaudit.Append(txctx, database.Executor(txctx, adapter.pool), platformaudit.AdminLedger, platformaudit.Entry{ID: auditID, ActorAccountID: &command.Write.ActorAccountID, ActorKind: "admin", ActionCode: "rpg.equipment.granted", ObjectType: "player_character_equipment_grant", ObjectID: &objectID, RequestID: command.Write.RequestID, Reason: &command.Reason, Changes: changes, CreatedAt: now}); err != nil {
			return err
		}
		return idempotency.Complete(txctx, writer, request, result)
	})
	return result, adminWorldRepositoryError(err)
}

// validGrantEquipmentCommand 在进入事务前校验管理身份、目标、数量和审计原因边界。
func validGrantEquipmentCommand(command rpg.GrantEquipmentCommand) bool {
	return validAdminWrite(command.Write) && command.PlayerCharacterID.IsValid() && command.EquipmentID.IsValid() && command.Quantity >= 1 && command.Quantity <= 100 && command.Reason != "" && len([]rune(command.Reason)) <= 500
}

// validEquipmentSourceType 判断字符串是否属于权威装备获取来源闭集。
func validEquipmentSourceType(value string) bool {
	switch value {
	case "shop", "quest", "loot", "admin":
		return true
	default:
		return false
	}
}

// validEquipmentTransactionAction 判断字符串是否属于不可变装备流水动作闭集。
func validEquipmentTransactionAction(value string) bool {
	switch value {
	case "acquire", "equip", "unequip", "sell":
		return true
	default:
		return false
	}
}

func adminEquipmentView(row *avalonent.GameEquipment) rpg.AdminEquipment {
	value := rpg.AdminEquipment{ID: row.ID, ItemID: row.ItemID, SellCurrencyID: row.SellCurrencyID, SlotType: rpg.EquipmentSlotType(row.SlotType), Handedness: rpg.EquipmentHandedness(optionalString(row.Handedness)), MinimumLevel: row.MinimumLevel, SellPrice: row.SellPrice, Enabled: row.Enabled, Version: row.Version, InstanceCount: int64(len(row.Edges.Instances)), RuleTimings: equipmentRuleTimings(row.Rules)}
	if row.Edges.Item != nil {
		value.ItemName = row.Edges.Item.Name
	}
	for _, relation := range row.Edges.Professions {
		value.ProfessionIDs = append(value.ProfessionIDs, relation.ProfessionID)
	}
	for _, relation := range row.Edges.StatModifiers {
		value.StatModifiers = append(value.StatModifiers, rpg.AdminEquipmentStatModifier{ID: relation.ID, StatID: relation.StatID, FlatValue: relation.FlatValue, PercentageBPS: relation.PercentageBps})
	}
	return value
}
func ruleTimingDocument(timings []string) map[string][]any {
	value := map[string][]any{}
	for _, timing := range timings {
		value[timing] = []any{}
	}
	return value
}
func validEquipmentCatalogShape(value rpg.AdminEquipment) bool {
	candidate := rpg.EquipmentLoadoutCandidate{Slot: rpg.EquipmentSlot(value.SlotType), InstanceID: snowflake.ID(1), SlotType: value.SlotType, Handedness: value.Handedness, MinimumLevel: value.MinimumLevel}
	if value.SlotType == rpg.EquipmentSlotTypeAccessory {
		candidate.Slot = rpg.EquipmentSlotAccessory1
	}
	return rpg.EquipmentSlotMatches(candidate.Slot, candidate.SlotType) && ((value.SlotType == rpg.EquipmentSlotTypeMainHand || value.SlotType == rpg.EquipmentSlotTypeOffHand) == (value.Handedness != ""))
}

// validAdminEquipmentRelations 在持久化前校验职业与 Stat 关系的唯一性和领域数值边界。
func validAdminEquipmentRelations(value rpg.AdminEquipment) bool {
	professions := make(map[snowflake.ID]struct{}, len(value.ProfessionIDs))
	for _, professionID := range value.ProfessionIDs {
		if !professionID.IsValid() {
			return false
		}
		if _, exists := professions[professionID]; exists {
			return false
		}
		professions[professionID] = struct{}{}
	}
	stats := make(map[snowflake.ID]struct{}, len(value.StatModifiers))
	modifiers := make([]rpg.EquipmentStatModifier, 0, len(value.StatModifiers))
	for _, modifier := range value.StatModifiers {
		if !modifier.StatID.IsValid() {
			return false
		}
		if _, exists := stats[modifier.StatID]; exists {
			return false
		}
		stats[modifier.StatID] = struct{}{}
		modifiers = append(modifiers, rpg.EquipmentStatModifier{StatID: modifier.StatID, FlatValue: modifier.FlatValue, PercentageBPS: modifier.PercentageBPS})
	}
	_, err := rpg.ApplyEquipmentStatModifiers(0, 0, modifiers)
	return err == nil
}
