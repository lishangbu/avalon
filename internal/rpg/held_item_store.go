package rpg

import (
	"context"
	"crypto/sha256"
	"encoding/json"
	"fmt"
	"time"

	"entgo.io/ent/dialect/sql"
	avalonent "github.com/lishangbu/avalon/ent"
	"github.com/lishangbu/avalon/ent/battleparticipantreservation"
	"github.com/lishangbu/avalon/ent/gameitem"
	"github.com/lishangbu/avalon/ent/playercharactercreature"
	"github.com/lishangbu/avalon/ent/playercharacterinventoryitem"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

const heldItemReplaceOperation = "rpg.creature-held-item.replace"

// GetInventory 返回活动角色的非零聚合背包与全部 Owned Creature 携带物。
func (store *EntWorldStore) GetInventory(ctx context.Context, accountID snowflake.ID) (Inventory, error) {
	client := store.pool.Client(ctx)
	playerID, err := activePlayerCharacterID(ctx, client, accountID)
	if err != nil {
		return Inventory{}, err
	}
	items, err := client.PlayerCharacterInventoryItem.Query().Where(playercharacterinventoryitem.PlayerCharacterIDEQ(playerID), playercharacterinventoryitem.QuantityGT(0)).WithItem().Order(avalonent.Asc(playercharacterinventoryitem.FieldItemID)).All(ctx)
	if err != nil {
		return Inventory{}, fmt.Errorf("查询 Inventory Stack: %w", err)
	}
	owned, err := client.PlayerCharacterCreature.Query().Where(playercharactercreature.PlayerCharacterIDEQ(playerID)).WithHeldItem().Order(avalonent.Asc(playercharactercreature.FieldID)).All(ctx)
	if err != nil {
		return Inventory{}, fmt.Errorf("查询 Owned Creature 携带物: %w", err)
	}
	result := Inventory{Items: make([]InventoryItem, 0, len(items)), OwnedCreatures: make([]OwnedCreatureHeldItem, 0, len(owned))}
	for _, row := range items {
		if row.Edges.Item == nil {
			return Inventory{}, fmt.Errorf("Inventory Stack %s 缺少 Item 关系", row.ID)
		}
		result.Items = append(result.Items, InventoryItem{ItemID: row.ItemID, ItemName: row.Edges.Item.Name, UsageType: row.Edges.Item.UsageType, Quantity: row.Quantity, Version: row.Version})
	}
	for _, row := range owned {
		result.OwnedCreatures = append(result.OwnedCreatures, heldItemView(row))
	}
	return result, nil
}

// ReplaceHeldItem 在一个 PostgreSQL 事务内锁定角色资产，原子扣还背包、写流水、更新 Creature、保存幂等响应并创建 Outbox。
func (store *EntWorldStore) ReplaceHeldItem(ctx context.Context, command ReplaceHeldItemCommand) (OwnedCreatureHeldItem, error) {
	var result OwnedCreatureHeldItem
	err := store.pool.WithinTransaction(ctx, func(transactionCtx context.Context) error {
		client := store.pool.Client(transactionCtx)
		playerID, err := activePlayerCharacterID(transactionCtx, client, command.AccountID)
		if err != nil {
			return err
		}
		requestBytes, err := json.Marshal(struct {
			OwnedCreatureID snowflake.ID  `json:"owned_creature_id"`
			ItemID          *snowflake.ID `json:"item_id,omitempty"`
			ExpectedVersion int64         `json:"expected_version"`
		}{command.OwnedCreatureID, command.ItemID, command.ExpectedCreatureVersion})
		if err != nil {
			return err
		}
		digest := sha256.Sum256(requestBytes)
		replayed, err := store.claimPlayerResponse(transactionCtx, client, playerID, heldItemReplaceOperation, command.IdempotencyKey, digest[:], &result, command.Now.UTC())
		if err != nil || replayed {
			return err
		}
		reserved, err := client.BattleParticipantReservation.Query().Where(battleparticipantreservation.IDEQ(playerID)).Exist(transactionCtx)
		if err != nil {
			return fmt.Errorf("查询 Battle Reservation: %w", err)
		}
		if reserved {
			return ErrCreatureInBattle
		}
		owned, err := client.PlayerCharacterCreature.Query().Where(playercharactercreature.IDEQ(command.OwnedCreatureID), playercharactercreature.PlayerCharacterIDEQ(playerID)).WithHeldItem().ForUpdate().Only(transactionCtx)
		if avalonent.IsNotFound(err) {
			return ErrOwnedCreatureConflict
		}
		if err != nil {
			return fmt.Errorf("锁定 Owned Creature: %w", err)
		}
		if owned.Version != command.ExpectedCreatureVersion {
			return ErrOwnedCreatureConflict
		}
		currentID := optionalIdentifier(owned.HeldItemID)
		targetID := optionalCommandIdentifier(command.ItemID)
		if currentID == targetID {
			result = heldItemView(owned)
			return store.completePlayerResponse(transactionCtx, client, playerID, heldItemReplaceOperation, command.IdempotencyKey, result)
		}
		if targetID.IsValid() {
			available, queryErr := client.GameItem.Query().Where(gameitem.IDEQ(targetID), gameitem.EnabledEQ(true), gameitem.UsageTypeEQ("held"), executableHeldItemPredicate()).Exist(transactionCtx)
			if queryErr != nil {
				return fmt.Errorf("校验 Held Item 资料: %w", queryErr)
			}
			if !available {
				return ErrHeldItemUnavailable
			}
		}
		operationID, err := store.newID.Next(transactionCtx)
		if err != nil {
			return err
		}
		if currentID.IsValid() {
			if err := store.changeInventoryQuantity(transactionCtx, client, playerID, currentID, 1, "held-item-returned", operationID, command.Now.UTC()); err != nil {
				return err
			}
		}
		if targetID.IsValid() {
			if err := store.changeInventoryQuantity(transactionCtx, client, playerID, targetID, -1, "held-item-equipped", operationID, command.Now.UTC()); err != nil {
				return err
			}
		}
		update := client.PlayerCharacterCreature.UpdateOne(owned).Where(playercharactercreature.VersionEQ(command.ExpectedCreatureVersion)).SetVersion(owned.Version + 1).SetUpdatedAt(command.Now.UTC())
		if targetID.IsValid() {
			update.SetHeldItemID(targetID)
		} else {
			update.ClearHeldItemID()
		}
		updated, err := update.Save(transactionCtx)
		if avalonent.IsNotFound(err) {
			return ErrOwnedCreatureConflict
		}
		if err != nil {
			return fmt.Errorf("更新 Owned Creature 携带物: %w", err)
		}
		result = heldItemView(updated)
		if targetID.IsValid() {
			item, itemErr := client.GameItem.Get(transactionCtx, targetID)
			if itemErr != nil {
				return itemErr
			}
			result.HeldItemName = item.Name
		}
		payload, err := json.Marshal(map[string]any{"operation_id": operationID.String(), "player_character_id": playerID.String(), "owned_creature_id": command.OwnedCreatureID.String(), "held_item_id": identifierString(targetID), "version": result.Version, "occurred_at": command.Now.UTC()})
		if err != nil {
			return err
		}
		outboxID, err := store.newID.Next(transactionCtx)
		if err != nil {
			return err
		}
		if _, err = client.OutboxMessage.Create().SetID(outboxID).SetTopic("rpg.creature-held-item-replaced.v1").SetAggregateID(operationID).SetPayload(payload).SetState("pending").SetAttemptCount(0).SetAvailableAt(command.Now.UTC()).SetCreatedAt(command.Now.UTC()).SetUpdatedAt(command.Now.UTC()).Save(transactionCtx); err != nil {
			return fmt.Errorf("创建 Held Item Outbox: %w", err)
		}
		return store.completePlayerResponse(transactionCtx, client, playerID, heldItemReplaceOperation, command.IdempotencyKey, result)
	})
	return result, err
}

// changeInventoryQuantity 锁定一种 Inventory Stack，提交非零数量变化和对应不可变流水。
func (store *EntWorldStore) changeInventoryQuantity(ctx context.Context, client *avalonent.Client, playerID, itemID snowflake.ID, delta int64, reason string, operationID snowflake.ID, now time.Time) error {
	row, err := client.PlayerCharacterInventoryItem.Query().Where(playercharacterinventoryitem.PlayerCharacterIDEQ(playerID), playercharacterinventoryitem.ItemIDEQ(itemID)).ForUpdate().Only(ctx)
	if avalonent.IsNotFound(err) && delta > 0 {
		rowID, idErr := store.newID.Next(ctx)
		if idErr != nil {
			return idErr
		}
		row, err = client.PlayerCharacterInventoryItem.Create().SetID(rowID).SetPlayerCharacterID(playerID).SetItemID(itemID).SetQuantity(0).SetVersion(1).SetUpdatedAt(now).Save(ctx)
	} else if avalonent.IsNotFound(err) {
		return ErrHeldItemUnavailable
	}
	if err != nil {
		return fmt.Errorf("锁定 Inventory Stack: %w", err)
	}
	quantity := row.Quantity + delta
	if quantity < 0 {
		return ErrHeldItemUnavailable
	}
	if _, err = client.PlayerCharacterInventoryItem.UpdateOne(row).SetQuantity(quantity).SetVersion(row.Version + 1).SetUpdatedAt(now).Save(ctx); err != nil {
		return fmt.Errorf("更新 Inventory Stack: %w", err)
	}
	transactionID, err := store.newID.Next(ctx)
	if err != nil {
		return err
	}
	if _, err = client.PlayerCharacterInventoryTransaction.Create().SetID(transactionID).SetPlayerCharacterID(playerID).SetItemID(itemID).SetQuantityDelta(delta).SetBalanceAfter(quantity).SetReasonCode(reason).SetReferenceID(operationID).SetCreatedAt(now).Save(ctx); err != nil {
		return fmt.Errorf("创建 Inventory Transaction: %w", err)
	}
	return nil
}

// executableHeldItemPredicate 要求 held 资料至少存在一类 Battle Engine 已读取的规范化规则。
func executableHeldItemPredicate() func(*sql.Selector) {
	return gameitem.Or(gameitem.HasStatusRules(), gameitem.HasDamageRules(), gameitem.HasStatBoosterAbilities(), gameitem.HasWeatherRules(), gameitem.HasSwitchRules(), gameitem.HasContactRules(), gameitem.HasRecoveryRules(), gameitem.HasStatRules(), gameitem.HasActionRules(), gameitem.HasMultiHitRules(), gameitem.HasWeightRules())
}

func optionalCommandIdentifier(value *snowflake.ID) snowflake.ID {
	if value == nil {
		return snowflake.ID(0)
	}
	return *value
}

func identifierString(value snowflake.ID) string {
	if !value.IsValid() {
		return ""
	}
	return value.String()
}

func heldItemView(row *avalonent.PlayerCharacterCreature) OwnedCreatureHeldItem {
	value := OwnedCreatureHeldItem{PlayerCharacterCreatureID: row.ID, CreatureID: row.CreatureID, HeldItemID: optionalIdentifier(row.HeldItemID), Version: row.Version}
	if row.Nickname != nil {
		value.Nickname = *row.Nickname
	}
	if row.Edges.HeldItem != nil {
		value.HeldItemName = row.Edges.HeldItem.Name
	}
	return value
}
