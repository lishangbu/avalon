package persistence

import (
	"context"
	"crypto/sha256"
	"encoding/json"
	"fmt"
	"math"
	"sort"
	"time"

	avalonent "github.com/lishangbu/avalon/ent"
	"github.com/lishangbu/avalon/ent/battleparticipantreservation"
	"github.com/lishangbu/avalon/ent/playercharacter"
	"github.com/lishangbu/avalon/ent/playercharacterequipmentinstance"
	"github.com/lishangbu/avalon/ent/playercharacterequipmentloadoutentry"
	"github.com/lishangbu/avalon/ent/playercharacterequipmentloadoutstate"
	"github.com/lishangbu/avalon/ent/playercharacterprofession"
	"github.com/lishangbu/avalon/ent/playercharacterwallet"
	"github.com/lishangbu/avalon/internal/platform/idempotency"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
	rpg "github.com/lishangbu/avalon/internal/rpg"
)

// ListEquipmentInstances 按稳定 ID 升序返回活动角色的一页未出售装备实例，并附带当前穿戴槽位。
func (adapter *Adapters) ListEquipmentInstances(ctx context.Context, accountID snowflake.ID, size int, cursor string) (rpg.EquipmentInstancePage, error) {
	client := adapter.pool.Client(ctx)
	playerID, err := activePlayerCharacterID(ctx, client, accountID)
	if err != nil {
		return rpg.EquipmentInstancePage{}, err
	}
	if size < 1 || size > 100 {
		size = 50
	}
	afterID, _, err := decodeEquipmentCursor(cursor, "player-instances", equipmentFilterHash(playerID.String()), false)
	if err != nil {
		return rpg.EquipmentInstancePage{}, err
	}
	query := client.PlayerCharacterEquipmentInstance.Query().Where(playercharacterequipmentinstance.PlayerCharacterIDEQ(playerID), playercharacterequipmentinstance.SoldAtIsNil())
	if afterID.IsValid() {
		query.Where(playercharacterequipmentinstance.IDGT(afterID))
	}
	rows, err := query.WithEquipment(func(q *avalonent.GameEquipmentQuery) { q.WithItem() }).WithLoadoutEntry().Order(avalonent.Asc(playercharacterequipmentinstance.FieldID)).Limit(size + 1).All(ctx)
	if err != nil {
		return rpg.EquipmentInstancePage{}, fmt.Errorf("查询 Equipment Instance: %w", err)
	}
	hasMore := len(rows) > size
	if hasMore {
		rows = rows[:size]
	}
	result := rpg.EquipmentInstancePage{Items: make([]rpg.EquipmentInstance, 0, len(rows))}
	for _, row := range rows {
		value, mapErr := equipmentInstanceView(row)
		if mapErr != nil {
			return rpg.EquipmentInstancePage{}, mapErr
		}
		result.Items = append(result.Items, value)
	}
	if hasMore {
		result.NextCursor, err = encodeEquipmentCursor("player-instances", equipmentFilterHash(playerID.String()), rows[len(rows)-1].ID, time.Time{})
		if err != nil {
			return rpg.EquipmentInstancePage{}, err
		}
	}
	return result, nil
}

// GetEquipmentInstance 返回活动角色拥有的一个实例；不泄露其它角色资产是否存在。
func (adapter *Adapters) GetEquipmentInstance(ctx context.Context, accountID, instanceID snowflake.ID) (rpg.EquipmentInstance, error) {
	client := adapter.pool.Client(ctx)
	playerID, err := activePlayerCharacterID(ctx, client, accountID)
	if err != nil {
		return rpg.EquipmentInstance{}, err
	}
	row, err := client.PlayerCharacterEquipmentInstance.Query().Where(playercharacterequipmentinstance.IDEQ(instanceID), playercharacterequipmentinstance.PlayerCharacterIDEQ(playerID), playercharacterequipmentinstance.SoldAtIsNil()).WithEquipment(func(q *avalonent.GameEquipmentQuery) { q.WithItem() }).WithLoadoutEntry().Only(ctx)
	if avalonent.IsNotFound(err) {
		return rpg.EquipmentInstance{}, rpg.ErrEquipmentNotOwned
	}
	if err != nil {
		return rpg.EquipmentInstance{}, err
	}
	return equipmentInstanceView(row)
}

// GetEquipmentLoadout 返回活动角色当前整套 Loadout；首次读取没有状态时返回版本一的空视图。
func (adapter *Adapters) GetEquipmentLoadout(ctx context.Context, accountID snowflake.ID) (rpg.EquipmentLoadout, error) {
	client := adapter.pool.Client(ctx)
	playerID, err := activePlayerCharacterID(ctx, client, accountID)
	if err != nil {
		return rpg.EquipmentLoadout{}, err
	}
	return readEquipmentLoadout(ctx, client, playerID)
}

// ReplaceEquipmentLoadout 锁定单一状态版本，校验最终完整状态并原子替换全部槽位与流水。
func (adapter *Adapters) ReplaceEquipmentLoadout(ctx context.Context, command rpg.ReplaceEquipmentLoadoutCommand) (rpg.EquipmentLoadout, error) {
	var result rpg.EquipmentLoadout
	if !command.AccountID.IsValid() || command.ExpectedVersion <= 0 || !idempotency.ValidKey(command.IdempotencyKey) || command.Now.IsZero() {
		return result, rpg.ErrEquipmentLoadoutConflict
	}
	err := adapter.pool.WithinTransaction(ctx, func(txctx context.Context) error {
		client := adapter.pool.Client(txctx)
		playerID, err := activePlayerCharacterID(txctx, client, command.AccountID)
		if err != nil {
			return err
		}
		payload, _ := json.Marshal(struct {
			Entries []rpg.EquipmentLoadoutEntry
			Version int64
		}{command.Entries, command.ExpectedVersion})
		digest := sha256.Sum256(payload)
		replayed, err := adapter.claimPlayerResponse(txctx, client, playerID, "rpg.equipment.loadout.replace", command.IdempotencyKey, digest[:], &result, command.Now.UTC())
		if err != nil || replayed {
			return err
		}
		reserved, err := client.BattleParticipantReservation.Query().Where(battleparticipantreservation.IDEQ(playerID)).Exist(txctx)
		if err != nil {
			return err
		}
		if reserved {
			return rpg.ErrEquipmentInBattle
		}
		state, err := client.PlayerCharacterEquipmentLoadoutState.Query().Where(playercharacterequipmentloadoutstate.IDEQ(playerID)).ForUpdate().Only(txctx)
		if avalonent.IsNotFound(err) {
			if command.ExpectedVersion != 1 {
				return rpg.ErrEquipmentLoadoutConflict
			}
			state, err = client.PlayerCharacterEquipmentLoadoutState.Create().SetID(playerID).SetVersion(1).SetUpdatedAt(command.Now.UTC()).Save(txctx)
		}
		if err != nil {
			return err
		}
		if state.Version != command.ExpectedVersion {
			return rpg.ErrEquipmentLoadoutConflict
		}
		character, err := client.PlayerCharacter.Query().Where(playercharacter.IDEQ(playerID)).Only(txctx)
		if err != nil {
			return err
		}
		profRows, err := client.PlayerCharacterProfession.Query().Where(playercharacterprofession.PlayerCharacterIDEQ(playerID), playercharacterprofession.ActiveEQ(true)).All(txctx)
		if err != nil {
			return err
		}
		professions := make([]snowflake.ID, 0, len(profRows))
		for _, row := range profRows {
			professions = append(professions, row.ProfessionID)
		}
		ids := make([]snowflake.ID, 0, len(command.Entries))
		for _, entry := range command.Entries {
			ids = append(ids, entry.InstanceID)
		}
		instances := []*avalonent.PlayerCharacterEquipmentInstance{}
		if len(ids) > 0 {
			instances, err = client.PlayerCharacterEquipmentInstance.Query().Where(playercharacterequipmentinstance.IDIn(ids...), playercharacterequipmentinstance.PlayerCharacterIDEQ(playerID), playercharacterequipmentinstance.SoldAtIsNil()).WithEquipment(func(q *avalonent.GameEquipmentQuery) { q.WithItem().WithProfessions() }).All(txctx)
			if err != nil {
				return err
			}
		}
		byID := make(map[snowflake.ID]*avalonent.PlayerCharacterEquipmentInstance, len(instances))
		for _, row := range instances {
			byID[row.ID] = row
		}
		candidates := make([]rpg.EquipmentLoadoutCandidate, 0, len(command.Entries))
		for _, entry := range command.Entries {
			row := byID[entry.InstanceID]
			if row == nil || row.Edges.Equipment == nil {
				return rpg.ErrEquipmentNotOwned
			}
			equipment := row.Edges.Equipment
			if !equipment.Enabled || equipment.Edges.Item == nil || !equipment.Edges.Item.Enabled {
				return rpg.ErrEquipmentNotFound
			}
			allowed := make([]snowflake.ID, 0, len(equipment.Edges.Professions))
			for _, relation := range equipment.Edges.Professions {
				allowed = append(allowed, relation.ProfessionID)
			}
			candidates = append(candidates, rpg.EquipmentLoadoutCandidate{Slot: entry.Slot, InstanceID: entry.InstanceID, SlotType: rpg.EquipmentSlotType(equipment.SlotType), Handedness: rpg.EquipmentHandedness(optionalString(equipment.Handedness)), MinimumLevel: equipment.MinimumLevel, ProfessionIDs: allowed})
		}
		if err := rpg.ValidateEquipmentLoadout(character.Level, professions, candidates); err != nil {
			return err
		}
		oldRows, err := client.PlayerCharacterEquipmentLoadoutEntry.Query().Where(playercharacterequipmentloadoutentry.PlayerCharacterIDEQ(playerID)).All(txctx)
		if err != nil {
			return err
		}
		operationID, err := adapter.newID.Next(txctx)
		if err != nil {
			return err
		}
		newBySlot := map[string]snowflake.ID{}
		for _, entry := range command.Entries {
			newBySlot[string(entry.Slot)] = entry.InstanceID
		}
		for _, old := range oldRows {
			if newBySlot[old.Slot] != old.EquipmentInstanceID {
				if err := adapter.createEquipmentTransaction(txctx, client, operationID, playerID, old.EquipmentInstanceID, "unequip", "", old.Slot, command.Now.UTC()); err != nil {
					return err
				}
			}
		}
		oldBySlot := make(map[string]*avalonent.PlayerCharacterEquipmentLoadoutEntry, len(oldRows))
		for _, old := range oldRows {
			oldBySlot[old.Slot] = old
			if newBySlot[old.Slot] == old.EquipmentInstanceID {
				continue
			}
			// 先删除全部变化槽位，确保实例换槽时不会短暂触发实例唯一约束。
			if err = client.PlayerCharacterEquipmentLoadoutEntry.DeleteOne(old).Exec(txctx); err != nil {
				return err
			}
		}
		for _, entry := range command.Entries {
			old := oldBySlot[string(entry.Slot)]
			if old != nil && old.EquipmentInstanceID == entry.InstanceID {
				continue
			}
			id, idErr := adapter.newID.Next(txctx)
			if idErr != nil {
				return idErr
			}
			if _, err = client.PlayerCharacterEquipmentLoadoutEntry.Create().SetID(id).SetPlayerCharacterID(playerID).SetSlot(string(entry.Slot)).SetEquipmentInstanceID(entry.InstanceID).SetEquippedAt(command.Now.UTC()).Save(txctx); err != nil {
				return err
			}
			if err := adapter.createEquipmentTransaction(txctx, client, operationID, playerID, entry.InstanceID, "equip", "", string(entry.Slot), command.Now.UTC()); err != nil {
				return err
			}
		}
		state, err = client.PlayerCharacterEquipmentLoadoutState.UpdateOne(state).SetVersion(state.Version + 1).SetUpdatedAt(command.Now.UTC()).Save(txctx)
		if err != nil {
			return err
		}
		result = rpg.EquipmentLoadout{Version: state.Version, Entries: append([]rpg.EquipmentLoadoutEntry(nil), command.Entries...), UpdatedAt: state.UpdatedAt}
		if err := adapter.createEquipmentOutbox(txctx, client, "rpg.equipment.loadout-replaced.v1", operationID, playerID, command.Now.UTC()); err != nil {
			return err
		}
		return adapter.completePlayerResponse(txctx, client, playerID, "rpg.equipment.loadout.replace", command.IdempotencyKey, result)
	})
	return result, err
}

// SellEquipmentInstance 原子写入未穿戴实例出售终态、钱包余额、资产流水、幂等响应与 Outbox。
func (adapter *Adapters) SellEquipmentInstance(ctx context.Context, command rpg.SellEquipmentCommand) (rpg.SellEquipmentResult, error) {
	var result rpg.SellEquipmentResult
	if !command.AccountID.IsValid() || !command.InstanceID.IsValid() || command.ExpectedVersion <= 0 || !idempotency.ValidKey(command.IdempotencyKey) || command.Now.IsZero() {
		return result, rpg.ErrEquipmentLoadoutConflict
	}
	err := adapter.pool.WithinTransaction(ctx, func(txctx context.Context) error {
		client := adapter.pool.Client(txctx)
		playerID, err := activePlayerCharacterID(txctx, client, command.AccountID)
		if err != nil {
			return err
		}
		payload, _ := json.Marshal(struct {
			ID      snowflake.ID
			Version int64
		}{command.InstanceID, command.ExpectedVersion})
		digest := sha256.Sum256(payload)
		replayed, err := adapter.claimPlayerResponse(txctx, client, playerID, "rpg.equipment.sell", command.IdempotencyKey, digest[:], &result, command.Now.UTC())
		if err != nil || replayed {
			return err
		}
		row, err := client.PlayerCharacterEquipmentInstance.Query().Where(playercharacterequipmentinstance.IDEQ(command.InstanceID), playercharacterequipmentinstance.PlayerCharacterIDEQ(playerID), playercharacterequipmentinstance.SoldAtIsNil()).WithEquipment().WithLoadoutEntry().ForUpdate().Only(txctx)
		if avalonent.IsNotFound(err) {
			return rpg.ErrEquipmentNotOwned
		}
		if err != nil {
			return err
		}
		if row.Version != command.ExpectedVersion || len(row.Edges.LoadoutEntry) > 0 {
			return rpg.ErrEquipmentLoadoutConflict
		}
		operationID, err := adapter.newID.Next(txctx)
		if err != nil {
			return err
		}
		if err = adapter.createEquipmentTransaction(txctx, client, operationID, playerID, row.ID, "sell", "", "", command.Now.UTC()); err != nil {
			return err
		}
		equipment := row.Edges.Equipment
		result = rpg.SellEquipmentResult{OperationID: operationID, CurrencyID: equipment.SellCurrencyID, SellPrice: equipment.SellPrice}
		balanceAfter, err := adapter.creditEquipmentSale(txctx, client, playerID, equipment.SellCurrencyID, equipment.SellPrice, operationID, command.Now.UTC())
		if err != nil {
			return err
		}
		result.BalanceAfter = balanceAfter
		if _, err = client.PlayerCharacterEquipmentInstance.UpdateOne(row).Where(playercharacterequipmentinstance.VersionEQ(command.ExpectedVersion)).SetSoldAt(command.Now.UTC()).SetUpdatedAt(command.Now.UTC()).SetVersion(row.Version + 1).Save(txctx); err != nil {
			return err
		}
		if err = adapter.createEquipmentOutbox(txctx, client, "rpg.equipment.sold.v1", operationID, playerID, command.Now.UTC()); err != nil {
			return err
		}
		return adapter.completePlayerResponse(txctx, client, playerID, "rpg.equipment.sell", command.IdempotencyKey, result)
	})
	return result, err
}

// creditEquipmentSale 把 Equipment Catalog Entry 声明的售价计入对应钱包并保存不可变货币流水。
// 零售价不会创建零变动流水，但仍返回当前余额，使幂等响应具有完整确定结果。
func (adapter *Adapters) creditEquipmentSale(ctx context.Context, client *avalonent.Client, playerID, currencyID snowflake.ID, amount int64, operationID snowflake.ID, now time.Time) (int64, error) {
	// PlayerCharacter 行是钱包首次创建和后续入账的共同串行化锚点；不存在的钱包行自身无法使用 FOR UPDATE 锁定。
	if _, err := client.PlayerCharacter.Query().Where(playercharacter.IDEQ(playerID)).ForUpdate().Only(ctx); err != nil {
		return 0, fmt.Errorf("锁定 Equipment 出售角色: %w", err)
	}
	wallet, err := client.PlayerCharacterWallet.Query().Where(
		playercharacterwallet.PlayerCharacterIDEQ(playerID),
		playercharacterwallet.CurrencyIDEQ(currencyID),
	).ForUpdate().Only(ctx)
	if avalonent.IsNotFound(err) {
		walletID, idErr := adapter.newID.Next(ctx)
		if idErr != nil {
			return 0, idErr
		}
		wallet, err = client.PlayerCharacterWallet.Create().SetID(walletID).SetPlayerCharacterID(playerID).SetCurrencyID(currencyID).SetBalance(amount).SetVersion(1).SetUpdatedAt(now).Save(ctx)
	} else if err == nil && amount > 0 {
		if wallet.Balance > math.MaxInt64-amount {
			return 0, rpg.ErrEquipmentLoadoutConflict
		}
		wallet, err = client.PlayerCharacterWallet.UpdateOne(wallet).Where(playercharacterwallet.VersionEQ(wallet.Version)).SetBalance(wallet.Balance + amount).SetVersion(wallet.Version + 1).SetUpdatedAt(now).Save(ctx)
	}
	if err != nil {
		return 0, fmt.Errorf("更新 Equipment 出售钱包: %w", err)
	}
	if amount == 0 {
		return wallet.Balance, nil
	}
	transactionID, err := adapter.newID.Next(ctx)
	if err != nil {
		return 0, err
	}
	if _, err := client.PlayerCharacterCurrencyTransaction.Create().SetID(transactionID).SetPlayerCharacterID(playerID).SetCurrencyID(currencyID).SetAmountDelta(amount).SetBalanceAfter(wallet.Balance).SetReasonCode("equipment-sale").SetReferenceID(operationID).SetCreatedAt(now).Save(ctx); err != nil {
		return 0, fmt.Errorf("记录 Equipment 出售货币流水: %w", err)
	}
	return wallet.Balance, nil
}

func readEquipmentLoadout(ctx context.Context, client *avalonent.Client, playerID snowflake.ID) (rpg.EquipmentLoadout, error) {
	state, err := client.PlayerCharacterEquipmentLoadoutState.Query().Where(playercharacterequipmentloadoutstate.IDEQ(playerID)).Only(ctx)
	if avalonent.IsNotFound(err) {
		return rpg.EquipmentLoadout{Version: 1, Entries: []rpg.EquipmentLoadoutEntry{}, UpdatedAt: time.Unix(0, 0).UTC()}, nil
	}
	if err != nil {
		return rpg.EquipmentLoadout{}, err
	}
	rows, err := client.PlayerCharacterEquipmentLoadoutEntry.Query().Where(playercharacterequipmentloadoutentry.PlayerCharacterIDEQ(playerID)).Order(avalonent.Asc(playercharacterequipmentloadoutentry.FieldSlot)).All(ctx)
	if err != nil {
		return rpg.EquipmentLoadout{}, err
	}
	result := rpg.EquipmentLoadout{Version: state.Version, Entries: make([]rpg.EquipmentLoadoutEntry, 0, len(rows)), UpdatedAt: state.UpdatedAt}
	for _, row := range rows {
		result.Entries = append(result.Entries, rpg.EquipmentLoadoutEntry{Slot: rpg.EquipmentSlot(row.Slot), InstanceID: row.EquipmentInstanceID, EquippedAt: row.EquippedAt})
	}
	return result, nil
}

func equipmentInstanceView(row *avalonent.PlayerCharacterEquipmentInstance) (rpg.EquipmentInstance, error) {
	equipment := row.Edges.Equipment
	if equipment == nil || equipment.Edges.Item == nil {
		return rpg.EquipmentInstance{}, fmt.Errorf("Equipment Instance %s 缺少资料关系", row.ID)
	}
	value := rpg.EquipmentInstance{ID: row.ID, EquipmentID: row.EquipmentID, ItemID: equipment.ItemID, Name: equipment.Edges.Item.Name, SlotType: rpg.EquipmentSlotType(equipment.SlotType), Handedness: rpg.EquipmentHandedness(optionalString(equipment.Handedness)), SourceType: row.SourceType, Version: row.Version, MinimumLevel: equipment.MinimumLevel, RuleTimings: equipmentRuleTimings(equipment.Rules), AcquiredAt: row.AcquiredAt}
	if len(row.Edges.LoadoutEntry) > 0 {
		value.EquippedSlot = row.Edges.LoadoutEntry[0].Slot
	}
	return value, nil
}
func optionalString(value *string) string {
	if value == nil {
		return ""
	}
	return *value
}
func equipmentRuleTimings(raw json.RawMessage) []string {
	var values map[string]json.RawMessage
	_ = json.Unmarshal(raw, &values)
	result := make([]string, 0, len(values))
	for key := range values {
		result = append(result, key)
	}
	sort.Strings(result)
	return result
}
func (adapter *Adapters) createEquipmentTransaction(ctx context.Context, client *avalonent.Client, operationID, playerID, instanceID snowflake.ID, action, source, slot string, now time.Time) error {
	id, err := adapter.newID.Next(ctx)
	if err != nil {
		return err
	}
	builder := client.PlayerCharacterEquipmentTransaction.Create().SetID(id).SetOperationID(operationID).SetPlayerCharacterID(playerID).SetEquipmentInstanceID(instanceID).SetAction(action).SetCreatedAt(now)
	if source != "" {
		builder.SetSourceType(source)
	}
	if slot != "" {
		builder.SetSlot(slot)
	}
	_, err = builder.Save(ctx)
	return err
}
func (adapter *Adapters) createEquipmentOutbox(ctx context.Context, client *avalonent.Client, topic string, operationID, playerID snowflake.ID, now time.Time) error {
	id, err := adapter.newID.Next(ctx)
	if err != nil {
		return err
	}
	payload, _ := json.Marshal(map[string]string{"operation_id": operationID.String(), "player_character_id": playerID.String(), "occurred_at": now.Format(time.RFC3339Nano)})
	_, err = client.OutboxMessage.Create().SetID(id).SetTopic(topic).SetAggregateID(operationID).SetPayload(payload).SetState("pending").SetAttemptCount(0).SetAvailableAt(now).SetCreatedAt(now).SetUpdatedAt(now).Save(ctx)
	return err
}
