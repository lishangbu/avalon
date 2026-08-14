package persistence

import (
	"context"
	"crypto/sha256"
	"fmt"
	"math"
	"sort"
	"time"

	avalonent "github.com/lishangbu/avalon/ent"
	"github.com/lishangbu/avalon/ent/gameequipment"
	"github.com/lishangbu/avalon/ent/gameitem"
	"github.com/lishangbu/avalon/ent/playercharacter"
	"github.com/lishangbu/avalon/ent/playercharacterinventoryitem"
	"github.com/lishangbu/avalon/ent/playercharacterlootsettlement"
	"github.com/lishangbu/avalon/ent/playercharacterposition"
	"github.com/lishangbu/avalon/ent/playercharacterquest"
	"github.com/lishangbu/avalon/ent/playercharacterwallet"
	"github.com/lishangbu/avalon/ent/rpgshopitem"
	"github.com/lishangbu/avalon/internal/platform/idempotency"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
	rpg "github.com/lishangbu/avalon/internal/rpg"
)

// acquisitionItem 是事务内已经由权威来源解析的一种 Item 与数量。
// 它不是公开命令，调用方必须先锁定并验证 Shop、Quest 或 Loot 来源事实。
type acquisitionItem struct {
	itemID   snowflake.ID
	quantity int64
}

// PurchaseShopItem 锁定角色、位置、商品与钱包后原子完成支付和资产交付。
func (adapter *Adapters) PurchaseShopItem(ctx context.Context, command rpg.PurchaseShopItemCommand) (rpg.ItemAcquisitionResult, error) {
	var result rpg.ItemAcquisitionResult
	if !command.AccountID.IsValid() || !command.ShopItemID.IsValid() || command.Quantity < 1 || command.Quantity > 100 || !idempotency.ValidKey(command.IdempotencyKey) {
		return result, rpg.ErrShopItemUnavailable
	}
	now := command.Now.UTC()
	if command.Now.IsZero() {
		now = time.Now().UTC()
	}
	digest := sha256.Sum256([]byte(fmt.Sprintf("%s:%d", command.ShopItemID, command.Quantity)))
	err := adapter.pool.WithinTransaction(ctx, func(txctx context.Context) error {
		client := adapter.pool.Client(txctx)
		playerID, err := activePlayerCharacterID(txctx, client, command.AccountID)
		if err != nil {
			return err
		}
		replayed, err := adapter.claimPlayerResponse(txctx, client, playerID, "rpg.shop.purchase", command.IdempotencyKey, digest[:], &result, now)
		if err != nil || replayed {
			return err
		}
		product, err := client.RpgShopItem.Query().Where(rpgshopitem.IDEQ(command.ShopItemID), rpgshopitem.EnabledEQ(true)).WithItem().WithShop().ForUpdate().Only(txctx)
		if avalonent.IsNotFound(err) {
			return rpg.ErrShopItemUnavailable
		}
		if err != nil || product.Edges.Item == nil || product.Edges.Shop == nil {
			if err != nil {
				return fmt.Errorf("读取商店商品: %w", err)
			}
			return rpg.ErrShopItemUnavailable
		}
		position, err := client.PlayerCharacterPosition.Query().Where(playercharacterposition.PlayerCharacterIDEQ(playerID)).Only(txctx)
		if err != nil || !product.Edges.Shop.Enabled || !product.Edges.Item.Enabled || position.LocationID != product.Edges.Shop.LocationID {
			return rpg.ErrShopItemUnavailable
		}
		if product.BuyPrice > 0 && int64(command.Quantity) > math.MaxInt64/product.BuyPrice {
			return rpg.ErrShopItemUnavailable
		}
		total := product.BuyPrice * int64(command.Quantity)
		operationID, err := adapter.newID.Next(txctx)
		if err != nil {
			return err
		}
		balance, err := adapter.changeCurrencyBalance(txctx, client, playerID, product.CurrencyID, -total, "shop-purchase", operationID, now)
		if err != nil {
			return err
		}
		purchaseID, err := adapter.newID.Next(txctx)
		if err != nil {
			return err
		}
		if _, err = client.PlayerCharacterShopPurchase.Create().SetID(purchaseID).SetOperationID(operationID).SetPlayerCharacterID(playerID).SetShopItemID(product.ID).SetItemID(product.ItemID).SetCurrencyID(product.CurrencyID).SetQuantity(command.Quantity).SetUnitPrice(product.BuyPrice).SetTotalPrice(total).SetBalanceAfter(balance).SetCreatedAt(now).Save(txctx); err != nil {
			return fmt.Errorf("记录商店购买事实: %w", err)
		}
		reward, err := adapter.acquireItems(txctx, client, playerID, operationID, "shop", purchaseID, []acquisitionItem{{itemID: product.ItemID, quantity: int64(command.Quantity)}}, now)
		if err != nil {
			return err
		}
		result = rpg.ItemAcquisitionResult{OperationID: operationID, EquipmentInstanceIDs: reward.EquipmentInstanceIDs, BalanceAfter: balance}
		if len(reward.InventoryStacks) == 1 {
			value := reward.InventoryStacks[0]
			result.InventoryStack = &value
		}
		if err = adapter.createEquipmentOutbox(txctx, client, "rpg.shop.purchased.v1", operationID, playerID, now); err != nil {
			return err
		}
		return adapter.completePlayerResponse(txctx, client, playerID, "rpg.shop.purchase", command.IdempotencyKey, result)
	})
	return result, err
}

// ClaimQuestRewards 锁定已完成进度，并以完成轮次唯一约束保证奖励只领取一次。
func (adapter *Adapters) ClaimQuestRewards(ctx context.Context, command rpg.ClaimQuestRewardsCommand) (rpg.RewardAcquisitionResult, error) {
	var result rpg.RewardAcquisitionResult
	if !command.AccountID.IsValid() || !command.QuestID.IsValid() || command.ExpectedProgressVersion <= 0 || !idempotency.ValidKey(command.IdempotencyKey) {
		return result, rpg.ErrQuestRewardUnavailable
	}
	now := command.Now.UTC()
	if command.Now.IsZero() {
		now = time.Now().UTC()
	}
	digest := sha256.Sum256([]byte(fmt.Sprintf("%s:%d", command.QuestID, command.ExpectedProgressVersion)))
	err := adapter.pool.WithinTransaction(ctx, func(txctx context.Context) error {
		client := adapter.pool.Client(txctx)
		playerID, err := activePlayerCharacterID(txctx, client, command.AccountID)
		if err != nil {
			return err
		}
		replayed, err := adapter.claimPlayerResponse(txctx, client, playerID, "rpg.quest.rewards.claim", command.IdempotencyKey, digest[:], &result, now)
		if err != nil || replayed {
			return err
		}
		progress, err := client.PlayerCharacterQuest.Query().Where(playercharacterquest.PlayerCharacterIDEQ(playerID), playercharacterquest.QuestIDEQ(command.QuestID)).WithQuest(func(query *avalonent.RpgQuestQuery) { query.WithRewards() }).ForUpdate().Only(txctx)
		if avalonent.IsNotFound(err) {
			return rpg.ErrQuestRewardUnavailable
		}
		if err != nil || progress.Version != command.ExpectedProgressVersion || progress.Status != "completed" || progress.CompletionCount < 1 || progress.Edges.Quest == nil || !progress.Edges.Quest.Enabled {
			if err != nil {
				return fmt.Errorf("读取任务奖励进度: %w", err)
			}
			return rpg.ErrQuestRewardUnavailable
		}
		operationID, claimID, err := adapter.nextTwoIDs(txctx)
		if err != nil {
			return err
		}
		if _, err = client.PlayerCharacterQuestRewardClaim.Create().SetID(claimID).SetOperationID(operationID).SetPlayerCharacterQuestID(progress.ID).SetPlayerCharacterID(playerID).SetQuestID(command.QuestID).SetCompletionCount(progress.CompletionCount).SetClaimedAt(now).Save(txctx); err != nil {
			if avalonent.IsConstraintError(err) {
				return rpg.ErrQuestRewardUnavailable
			}
			return fmt.Errorf("记录任务奖励领取事实: %w", err)
		}
		items := make([]acquisitionItem, 0, len(progress.Edges.Quest.Edges.Rewards))
		currencyAmounts := make(map[snowflake.ID]int64)
		result = rpg.RewardAcquisitionResult{OperationID: operationID, EquipmentInstanceIDs: []snowflake.ID{}, InventoryStacks: []rpg.InventoryAcquisition{}, CurrencyBalances: []rpg.CurrencyAcquisition{}}
		for _, reward := range progress.Edges.Quest.Edges.Rewards {
			if reward.ItemID != nil {
				items = append(items, acquisitionItem{itemID: *reward.ItemID, quantity: reward.Quantity})
			}
			if reward.CurrencyID != nil {
				current := currencyAmounts[*reward.CurrencyID]
				if reward.Quantity <= 0 || current > math.MaxInt64-reward.Quantity {
					return rpg.ErrQuestRewardUnavailable
				}
				currencyAmounts[*reward.CurrencyID] = current + reward.Quantity
			}
		}
		currencyIDs := make([]snowflake.ID, 0, len(currencyAmounts))
		for currencyID := range currencyAmounts {
			currencyIDs = append(currencyIDs, currencyID)
		}
		sort.Slice(currencyIDs, func(i, j int) bool { return currencyIDs[i] < currencyIDs[j] })
		for _, currencyID := range currencyIDs {
			amount := currencyAmounts[currencyID]
			balance, changeErr := adapter.changeCurrencyBalance(txctx, client, playerID, currencyID, amount, "quest-reward", claimID, now)
			if changeErr != nil {
				return changeErr
			}
			result.CurrencyBalances = append(result.CurrencyBalances, rpg.CurrencyAcquisition{CurrencyID: currencyID, AmountDelta: amount, BalanceAfter: balance})
		}
		assets, err := adapter.acquireItems(txctx, client, playerID, operationID, "quest", claimID, items, now)
		if err != nil {
			return err
		}
		result.EquipmentInstanceIDs, result.InventoryStacks = assets.EquipmentInstanceIDs, assets.InventoryStacks
		if err = adapter.createEquipmentOutbox(txctx, client, "rpg.quest.rewards-claimed.v1", operationID, playerID, now); err != nil {
			return err
		}
		return adapter.completePlayerResponse(txctx, client, playerID, "rpg.quest.rewards.claim", command.IdempotencyKey, result)
	})
	return result, err
}

// ClaimLootSettlement 只领取服务端已冻结的条目，并以结算终态阻止重复交付。
func (adapter *Adapters) ClaimLootSettlement(ctx context.Context, command rpg.ClaimLootSettlementCommand) (rpg.RewardAcquisitionResult, error) {
	var result rpg.RewardAcquisitionResult
	if !command.AccountID.IsValid() || !command.LootSettlementID.IsValid() || !idempotency.ValidKey(command.IdempotencyKey) {
		return result, rpg.ErrLootSettlementUnavailable
	}
	now := command.Now.UTC()
	if command.Now.IsZero() {
		now = time.Now().UTC()
	}
	digest := sha256.Sum256([]byte(command.LootSettlementID.String()))
	err := adapter.pool.WithinTransaction(ctx, func(txctx context.Context) error {
		client := adapter.pool.Client(txctx)
		playerID, err := activePlayerCharacterID(txctx, client, command.AccountID)
		if err != nil {
			return err
		}
		replayed, err := adapter.claimPlayerResponse(txctx, client, playerID, "rpg.loot.claim", command.IdempotencyKey, digest[:], &result, now)
		if err != nil || replayed {
			return err
		}
		settlement, err := client.PlayerCharacterLootSettlement.Query().Where(playercharacterlootsettlement.IDEQ(command.LootSettlementID), playercharacterlootsettlement.PlayerCharacterIDEQ(playerID)).WithEntries().ForUpdate().Only(txctx)
		if avalonent.IsNotFound(err) || err == nil && settlement.State != "pending" {
			return rpg.ErrLootSettlementUnavailable
		}
		if err != nil {
			return fmt.Errorf("读取权威掉落结算: %w", err)
		}
		operationID, err := adapter.newID.Next(txctx)
		if err != nil {
			return err
		}
		items := make([]acquisitionItem, 0, len(settlement.Edges.Entries))
		for _, entry := range settlement.Edges.Entries {
			items = append(items, acquisitionItem{itemID: entry.ItemID, quantity: int64(entry.Quantity)})
		}
		result, err = adapter.acquireItems(txctx, client, playerID, operationID, "loot", settlement.ID, items, now)
		if err != nil {
			return err
		}
		if _, err = client.PlayerCharacterLootSettlement.UpdateOne(settlement).SetState("claimed").SetOperationID(operationID).SetClaimedAt(now).Save(txctx); err != nil {
			return fmt.Errorf("完成掉落结算: %w", err)
		}
		if err = adapter.createEquipmentOutbox(txctx, client, "rpg.loot.claimed.v1", operationID, playerID, now); err != nil {
			return err
		}
		return adapter.completePlayerResponse(txctx, client, playerID, "rpg.loot.claim", command.IdempotencyKey, result)
	})
	return result, err
}

// acquireItems 把来源已经验证的 Item 按资料类型分流为聚合背包或独立 Equipment Instance。
func (adapter *Adapters) acquireItems(ctx context.Context, client *avalonent.Client, playerID, operationID snowflake.ID, sourceType string, sourceReferenceID snowflake.ID, values []acquisitionItem, now time.Time) (rpg.RewardAcquisitionResult, error) {
	result := rpg.RewardAcquisitionResult{OperationID: operationID, EquipmentInstanceIDs: []snowflake.ID{}, InventoryStacks: []rpg.InventoryAcquisition{}, CurrencyBalances: []rpg.CurrencyAcquisition{}}
	merged := make(map[snowflake.ID]int64, len(values))
	for _, value := range values {
		if !value.itemID.IsValid() || value.quantity <= 0 || merged[value.itemID] > math.MaxInt64-value.quantity {
			return result, rpg.ErrEquipmentNotFound
		}
		merged[value.itemID] += value.quantity
	}
	ids := make([]snowflake.ID, 0, len(merged))
	for id := range merged {
		ids = append(ids, id)
	}
	sort.Slice(ids, func(i, j int) bool { return ids[i] < ids[j] })
	for _, itemID := range ids {
		item, err := client.GameItem.Query().Where(gameitem.IDEQ(itemID), gameitem.EnabledEQ(true)).Only(ctx)
		if avalonent.IsNotFound(err) {
			return result, rpg.ErrEquipmentNotFound
		}
		if err != nil {
			return result, fmt.Errorf("读取获取道具资料: %w", err)
		}
		quantity := merged[itemID]
		if item.UsageType == "equipment" {
			if quantity > 100 {
				return result, rpg.ErrEquipmentLoadoutConflict
			}
			equipment, err := client.GameEquipment.Query().Where(gameequipment.ItemIDEQ(itemID), gameequipment.EnabledEQ(true)).Only(ctx)
			if avalonent.IsNotFound(err) {
				return result, rpg.ErrEquipmentNotFound
			}
			if err != nil {
				return result, fmt.Errorf("读取 Equipment Catalog Entry: %w", err)
			}
			for range quantity {
				instanceID, idErr := adapter.newID.Next(ctx)
				if idErr != nil {
					return result, idErr
				}
				if _, err = client.PlayerCharacterEquipmentInstance.Create().SetID(instanceID).SetPlayerCharacterID(playerID).SetEquipmentID(equipment.ID).SetSourceType(sourceType).SetSourceReferenceID(sourceReferenceID).SetVersion(1).SetAcquiredAt(now).SetUpdatedAt(now).Save(ctx); err != nil {
					return result, fmt.Errorf("建立 Equipment Instance: %w", err)
				}
				if err = adapter.createEquipmentTransaction(ctx, client, operationID, playerID, instanceID, "acquire", sourceType, "", now); err != nil {
					return result, err
				}
				result.EquipmentInstanceIDs = append(result.EquipmentInstanceIDs, instanceID)
			}
			continue
		}
		balance, err := adapter.creditInventoryStack(ctx, client, playerID, itemID, quantity, sourceType, sourceReferenceID, now)
		if err != nil {
			return result, err
		}
		result.InventoryStacks = append(result.InventoryStacks, rpg.InventoryAcquisition{ItemID: itemID, QuantityDelta: quantity, BalanceAfter: balance})
	}
	return result, nil
}

// creditInventoryStack 原子增加聚合背包，并记录同一来源的不可变数量流水。
func (adapter *Adapters) creditInventoryStack(ctx context.Context, client *avalonent.Client, playerID, itemID snowflake.ID, quantity int64, reason string, referenceID snowflake.ID, now time.Time) (int64, error) {
	row, err := client.PlayerCharacterInventoryItem.Query().Where(playercharacterinventoryitem.PlayerCharacterIDEQ(playerID), playercharacterinventoryitem.ItemIDEQ(itemID)).ForUpdate().Only(ctx)
	if avalonent.IsNotFound(err) {
		id, idErr := adapter.newID.Next(ctx)
		if idErr != nil {
			return 0, idErr
		}
		row, err = client.PlayerCharacterInventoryItem.Create().SetID(id).SetPlayerCharacterID(playerID).SetItemID(itemID).SetQuantity(quantity).SetVersion(1).SetUpdatedAt(now).Save(ctx)
	} else if err == nil {
		if row.Quantity > math.MaxInt64-quantity {
			return 0, rpg.ErrEquipmentLoadoutConflict
		}
		row, err = client.PlayerCharacterInventoryItem.UpdateOne(row).SetQuantity(row.Quantity + quantity).SetVersion(row.Version + 1).SetUpdatedAt(now).Save(ctx)
	}
	if err != nil {
		return 0, fmt.Errorf("更新获取道具背包: %w", err)
	}
	transactionID, err := adapter.newID.Next(ctx)
	if err != nil {
		return 0, err
	}
	if _, err = client.PlayerCharacterInventoryTransaction.Create().SetID(transactionID).SetPlayerCharacterID(playerID).SetItemID(itemID).SetQuantityDelta(quantity).SetBalanceAfter(row.Quantity).SetReasonCode(reason).SetReferenceID(referenceID).SetCreatedAt(now).Save(ctx); err != nil {
		return 0, fmt.Errorf("记录获取道具流水: %w", err)
	}
	return row.Quantity, nil
}

// changeCurrencyBalance 原子增加或扣减钱包并记录不可变货币流水。
func (adapter *Adapters) changeCurrencyBalance(ctx context.Context, client *avalonent.Client, playerID, currencyID snowflake.ID, delta int64, reason string, referenceID snowflake.ID, now time.Time) (int64, error) {
	if _, err := client.PlayerCharacter.Query().Where(playercharacter.IDEQ(playerID)).ForUpdate().Only(ctx); err != nil {
		return 0, fmt.Errorf("锁定钱包所属角色: %w", err)
	}
	wallet, err := client.PlayerCharacterWallet.Query().Where(playercharacterwallet.PlayerCharacterIDEQ(playerID), playercharacterwallet.CurrencyIDEQ(currencyID)).ForUpdate().Only(ctx)
	if avalonent.IsNotFound(err) {
		if delta < 0 {
			return 0, rpg.ErrInsufficientCurrency
		}
		id, idErr := adapter.newID.Next(ctx)
		if idErr != nil {
			return 0, idErr
		}
		wallet, err = client.PlayerCharacterWallet.Create().SetID(id).SetPlayerCharacterID(playerID).SetCurrencyID(currencyID).SetBalance(delta).SetVersion(1).SetUpdatedAt(now).Save(ctx)
	} else if err == nil {
		if delta < 0 && wallet.Balance < -delta {
			return 0, rpg.ErrInsufficientCurrency
		}
		if delta > 0 && wallet.Balance > math.MaxInt64-delta {
			return 0, rpg.ErrEquipmentLoadoutConflict
		}
		wallet, err = client.PlayerCharacterWallet.UpdateOne(wallet).SetBalance(wallet.Balance + delta).SetVersion(wallet.Version + 1).SetUpdatedAt(now).Save(ctx)
	}
	if err != nil {
		return 0, fmt.Errorf("更新角色钱包: %w", err)
	}
	if delta == 0 {
		return wallet.Balance, nil
	}
	transactionID, err := adapter.newID.Next(ctx)
	if err != nil {
		return 0, err
	}
	if _, err = client.PlayerCharacterCurrencyTransaction.Create().SetID(transactionID).SetPlayerCharacterID(playerID).SetCurrencyID(currencyID).SetAmountDelta(delta).SetBalanceAfter(wallet.Balance).SetReasonCode(reason).SetReferenceID(referenceID).SetCreatedAt(now).Save(ctx); err != nil {
		return 0, fmt.Errorf("记录角色货币流水: %w", err)
	}
	return wallet.Balance, nil
}

// nextTwoIDs 以固定顺序产生共同 Operation 与来源事实 Identifier。
func (adapter *Adapters) nextTwoIDs(ctx context.Context) (snowflake.ID, snowflake.ID, error) {
	first, err := adapter.newID.Next(ctx)
	if err != nil {
		return 0, 0, err
	}
	second, err := adapter.newID.Next(ctx)
	return first, second, err
}
