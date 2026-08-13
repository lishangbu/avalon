package rpg

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
)

// acquisitionItem 是事务内已经由权威来源解析的一种 Item 与数量。
// 它不是公开命令，调用方必须先锁定并验证 Shop、Quest 或 Loot 来源事实。
type acquisitionItem struct {
	itemID   snowflake.ID
	quantity int64
}

// PurchaseShopItem 锁定角色、位置、商品与钱包后原子完成支付和资产交付。
func (store *EntWorldStore) PurchaseShopItem(ctx context.Context, command PurchaseShopItemCommand) (ItemAcquisitionResult, error) {
	var result ItemAcquisitionResult
	if !command.AccountID.IsValid() || !command.ShopItemID.IsValid() || command.Quantity < 1 || command.Quantity > 100 || !idempotency.ValidKey(command.IdempotencyKey) {
		return result, ErrShopItemUnavailable
	}
	now := command.Now.UTC()
	if command.Now.IsZero() {
		now = time.Now().UTC()
	}
	digest := sha256.Sum256([]byte(fmt.Sprintf("%s:%d", command.ShopItemID, command.Quantity)))
	err := store.pool.WithinTransaction(ctx, func(txctx context.Context) error {
		client := store.pool.Client(txctx)
		playerID, err := activePlayerCharacterID(txctx, client, command.AccountID)
		if err != nil {
			return err
		}
		replayed, err := store.claimPlayerResponse(txctx, client, playerID, "rpg.shop.purchase", command.IdempotencyKey, digest[:], &result, now)
		if err != nil || replayed {
			return err
		}
		product, err := client.RpgShopItem.Query().Where(rpgshopitem.IDEQ(command.ShopItemID), rpgshopitem.EnabledEQ(true)).WithItem().WithShop().ForUpdate().Only(txctx)
		if avalonent.IsNotFound(err) {
			return ErrShopItemUnavailable
		}
		if err != nil || product.Edges.Item == nil || product.Edges.Shop == nil {
			if err != nil {
				return fmt.Errorf("读取商店商品: %w", err)
			}
			return ErrShopItemUnavailable
		}
		position, err := client.PlayerCharacterPosition.Query().Where(playercharacterposition.PlayerCharacterIDEQ(playerID)).Only(txctx)
		if err != nil || !product.Edges.Shop.Enabled || !product.Edges.Item.Enabled || position.LocationID != product.Edges.Shop.LocationID {
			return ErrShopItemUnavailable
		}
		if product.BuyPrice > 0 && int64(command.Quantity) > math.MaxInt64/product.BuyPrice {
			return ErrShopItemUnavailable
		}
		total := product.BuyPrice * int64(command.Quantity)
		operationID, err := store.newID.Next(txctx)
		if err != nil {
			return err
		}
		balance, err := store.changeCurrencyBalance(txctx, client, playerID, product.CurrencyID, -total, "shop-purchase", operationID, now)
		if err != nil {
			return err
		}
		purchaseID, err := store.newID.Next(txctx)
		if err != nil {
			return err
		}
		if _, err = client.PlayerCharacterShopPurchase.Create().SetID(purchaseID).SetOperationID(operationID).SetPlayerCharacterID(playerID).SetShopItemID(product.ID).SetItemID(product.ItemID).SetCurrencyID(product.CurrencyID).SetQuantity(command.Quantity).SetUnitPrice(product.BuyPrice).SetTotalPrice(total).SetBalanceAfter(balance).SetCreatedAt(now).Save(txctx); err != nil {
			return fmt.Errorf("记录商店购买事实: %w", err)
		}
		reward, err := store.acquireItems(txctx, client, playerID, operationID, "shop", purchaseID, []acquisitionItem{{itemID: product.ItemID, quantity: int64(command.Quantity)}}, now)
		if err != nil {
			return err
		}
		result = ItemAcquisitionResult{OperationID: operationID, EquipmentInstanceIDs: reward.EquipmentInstanceIDs, BalanceAfter: balance}
		if len(reward.InventoryStacks) == 1 {
			value := reward.InventoryStacks[0]
			result.InventoryStack = &value
		}
		if err = store.createEquipmentOutbox(txctx, client, "rpg.shop.purchased.v1", operationID, playerID, now); err != nil {
			return err
		}
		return store.completePlayerResponse(txctx, client, playerID, "rpg.shop.purchase", command.IdempotencyKey, result)
	})
	return result, err
}

// ClaimQuestRewards 锁定已完成进度，并以完成轮次唯一约束保证奖励只领取一次。
func (store *EntWorldStore) ClaimQuestRewards(ctx context.Context, command ClaimQuestRewardsCommand) (RewardAcquisitionResult, error) {
	var result RewardAcquisitionResult
	if !command.AccountID.IsValid() || !command.QuestID.IsValid() || command.ExpectedProgressVersion <= 0 || !idempotency.ValidKey(command.IdempotencyKey) {
		return result, ErrQuestRewardUnavailable
	}
	now := command.Now.UTC()
	if command.Now.IsZero() {
		now = time.Now().UTC()
	}
	digest := sha256.Sum256([]byte(fmt.Sprintf("%s:%d", command.QuestID, command.ExpectedProgressVersion)))
	err := store.pool.WithinTransaction(ctx, func(txctx context.Context) error {
		client := store.pool.Client(txctx)
		playerID, err := activePlayerCharacterID(txctx, client, command.AccountID)
		if err != nil {
			return err
		}
		replayed, err := store.claimPlayerResponse(txctx, client, playerID, "rpg.quest.rewards.claim", command.IdempotencyKey, digest[:], &result, now)
		if err != nil || replayed {
			return err
		}
		progress, err := client.PlayerCharacterQuest.Query().Where(playercharacterquest.PlayerCharacterIDEQ(playerID), playercharacterquest.QuestIDEQ(command.QuestID)).WithQuest(func(query *avalonent.RpgQuestQuery) { query.WithRewards() }).ForUpdate().Only(txctx)
		if avalonent.IsNotFound(err) {
			return ErrQuestRewardUnavailable
		}
		if err != nil || progress.Version != command.ExpectedProgressVersion || progress.Status != "completed" || progress.CompletionCount < 1 || progress.Edges.Quest == nil || !progress.Edges.Quest.Enabled {
			if err != nil {
				return fmt.Errorf("读取任务奖励进度: %w", err)
			}
			return ErrQuestRewardUnavailable
		}
		operationID, claimID, err := store.nextTwoIDs(txctx)
		if err != nil {
			return err
		}
		if _, err = client.PlayerCharacterQuestRewardClaim.Create().SetID(claimID).SetOperationID(operationID).SetPlayerCharacterQuestID(progress.ID).SetPlayerCharacterID(playerID).SetQuestID(command.QuestID).SetCompletionCount(progress.CompletionCount).SetClaimedAt(now).Save(txctx); err != nil {
			if avalonent.IsConstraintError(err) {
				return ErrQuestRewardUnavailable
			}
			return fmt.Errorf("记录任务奖励领取事实: %w", err)
		}
		items := make([]acquisitionItem, 0, len(progress.Edges.Quest.Edges.Rewards))
		currencyAmounts := make(map[snowflake.ID]int64)
		result = RewardAcquisitionResult{OperationID: operationID, EquipmentInstanceIDs: []snowflake.ID{}, InventoryStacks: []InventoryAcquisition{}, CurrencyBalances: []CurrencyAcquisition{}}
		for _, reward := range progress.Edges.Quest.Edges.Rewards {
			if reward.ItemID != nil {
				items = append(items, acquisitionItem{itemID: *reward.ItemID, quantity: reward.Quantity})
			}
			if reward.CurrencyID != nil {
				current := currencyAmounts[*reward.CurrencyID]
				if reward.Quantity <= 0 || current > math.MaxInt64-reward.Quantity {
					return ErrQuestRewardUnavailable
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
			balance, changeErr := store.changeCurrencyBalance(txctx, client, playerID, currencyID, amount, "quest-reward", claimID, now)
			if changeErr != nil {
				return changeErr
			}
			result.CurrencyBalances = append(result.CurrencyBalances, CurrencyAcquisition{CurrencyID: currencyID, AmountDelta: amount, BalanceAfter: balance})
		}
		assets, err := store.acquireItems(txctx, client, playerID, operationID, "quest", claimID, items, now)
		if err != nil {
			return err
		}
		result.EquipmentInstanceIDs, result.InventoryStacks = assets.EquipmentInstanceIDs, assets.InventoryStacks
		if err = store.createEquipmentOutbox(txctx, client, "rpg.quest.rewards-claimed.v1", operationID, playerID, now); err != nil {
			return err
		}
		return store.completePlayerResponse(txctx, client, playerID, "rpg.quest.rewards.claim", command.IdempotencyKey, result)
	})
	return result, err
}

// ClaimLootSettlement 只领取服务端已冻结的条目，并以结算终态阻止重复交付。
func (store *EntWorldStore) ClaimLootSettlement(ctx context.Context, command ClaimLootSettlementCommand) (RewardAcquisitionResult, error) {
	var result RewardAcquisitionResult
	if !command.AccountID.IsValid() || !command.LootSettlementID.IsValid() || !idempotency.ValidKey(command.IdempotencyKey) {
		return result, ErrLootSettlementUnavailable
	}
	now := command.Now.UTC()
	if command.Now.IsZero() {
		now = time.Now().UTC()
	}
	digest := sha256.Sum256([]byte(command.LootSettlementID.String()))
	err := store.pool.WithinTransaction(ctx, func(txctx context.Context) error {
		client := store.pool.Client(txctx)
		playerID, err := activePlayerCharacterID(txctx, client, command.AccountID)
		if err != nil {
			return err
		}
		replayed, err := store.claimPlayerResponse(txctx, client, playerID, "rpg.loot.claim", command.IdempotencyKey, digest[:], &result, now)
		if err != nil || replayed {
			return err
		}
		settlement, err := client.PlayerCharacterLootSettlement.Query().Where(playercharacterlootsettlement.IDEQ(command.LootSettlementID), playercharacterlootsettlement.PlayerCharacterIDEQ(playerID)).WithEntries().ForUpdate().Only(txctx)
		if avalonent.IsNotFound(err) || err == nil && settlement.State != "pending" {
			return ErrLootSettlementUnavailable
		}
		if err != nil {
			return fmt.Errorf("读取权威掉落结算: %w", err)
		}
		operationID, err := store.newID.Next(txctx)
		if err != nil {
			return err
		}
		items := make([]acquisitionItem, 0, len(settlement.Edges.Entries))
		for _, entry := range settlement.Edges.Entries {
			items = append(items, acquisitionItem{itemID: entry.ItemID, quantity: int64(entry.Quantity)})
		}
		result, err = store.acquireItems(txctx, client, playerID, operationID, "loot", settlement.ID, items, now)
		if err != nil {
			return err
		}
		if _, err = client.PlayerCharacterLootSettlement.UpdateOne(settlement).SetState("claimed").SetOperationID(operationID).SetClaimedAt(now).Save(txctx); err != nil {
			return fmt.Errorf("完成掉落结算: %w", err)
		}
		if err = store.createEquipmentOutbox(txctx, client, "rpg.loot.claimed.v1", operationID, playerID, now); err != nil {
			return err
		}
		return store.completePlayerResponse(txctx, client, playerID, "rpg.loot.claim", command.IdempotencyKey, result)
	})
	return result, err
}

// acquireItems 把来源已经验证的 Item 按资料类型分流为聚合背包或独立 Equipment Instance。
func (store *EntWorldStore) acquireItems(ctx context.Context, client *avalonent.Client, playerID, operationID snowflake.ID, sourceType string, sourceReferenceID snowflake.ID, values []acquisitionItem, now time.Time) (RewardAcquisitionResult, error) {
	result := RewardAcquisitionResult{OperationID: operationID, EquipmentInstanceIDs: []snowflake.ID{}, InventoryStacks: []InventoryAcquisition{}, CurrencyBalances: []CurrencyAcquisition{}}
	merged := make(map[snowflake.ID]int64, len(values))
	for _, value := range values {
		if !value.itemID.IsValid() || value.quantity <= 0 || merged[value.itemID] > math.MaxInt64-value.quantity {
			return result, ErrEquipmentNotFound
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
			return result, ErrEquipmentNotFound
		}
		if err != nil {
			return result, fmt.Errorf("读取获取道具资料: %w", err)
		}
		quantity := merged[itemID]
		if item.UsageType == "equipment" {
			if quantity > 100 {
				return result, ErrEquipmentLoadoutConflict
			}
			equipment, err := client.GameEquipment.Query().Where(gameequipment.ItemIDEQ(itemID), gameequipment.EnabledEQ(true)).Only(ctx)
			if avalonent.IsNotFound(err) {
				return result, ErrEquipmentNotFound
			}
			if err != nil {
				return result, fmt.Errorf("读取 Equipment Catalog Entry: %w", err)
			}
			for range quantity {
				instanceID, idErr := store.newID.Next(ctx)
				if idErr != nil {
					return result, idErr
				}
				if _, err = client.PlayerCharacterEquipmentInstance.Create().SetID(instanceID).SetPlayerCharacterID(playerID).SetEquipmentID(equipment.ID).SetSourceType(sourceType).SetSourceReferenceID(sourceReferenceID).SetVersion(1).SetAcquiredAt(now).SetUpdatedAt(now).Save(ctx); err != nil {
					return result, fmt.Errorf("建立 Equipment Instance: %w", err)
				}
				if err = store.createEquipmentTransaction(ctx, client, operationID, playerID, instanceID, "acquire", sourceType, "", now); err != nil {
					return result, err
				}
				result.EquipmentInstanceIDs = append(result.EquipmentInstanceIDs, instanceID)
			}
			continue
		}
		balance, err := store.creditInventoryStack(ctx, client, playerID, itemID, quantity, sourceType, sourceReferenceID, now)
		if err != nil {
			return result, err
		}
		result.InventoryStacks = append(result.InventoryStacks, InventoryAcquisition{ItemID: itemID, QuantityDelta: quantity, BalanceAfter: balance})
	}
	return result, nil
}

// creditInventoryStack 原子增加聚合背包，并记录同一来源的不可变数量流水。
func (store *EntWorldStore) creditInventoryStack(ctx context.Context, client *avalonent.Client, playerID, itemID snowflake.ID, quantity int64, reason string, referenceID snowflake.ID, now time.Time) (int64, error) {
	row, err := client.PlayerCharacterInventoryItem.Query().Where(playercharacterinventoryitem.PlayerCharacterIDEQ(playerID), playercharacterinventoryitem.ItemIDEQ(itemID)).ForUpdate().Only(ctx)
	if avalonent.IsNotFound(err) {
		id, idErr := store.newID.Next(ctx)
		if idErr != nil {
			return 0, idErr
		}
		row, err = client.PlayerCharacterInventoryItem.Create().SetID(id).SetPlayerCharacterID(playerID).SetItemID(itemID).SetQuantity(quantity).SetVersion(1).SetUpdatedAt(now).Save(ctx)
	} else if err == nil {
		if row.Quantity > math.MaxInt64-quantity {
			return 0, ErrEquipmentLoadoutConflict
		}
		row, err = client.PlayerCharacterInventoryItem.UpdateOne(row).SetQuantity(row.Quantity + quantity).SetVersion(row.Version + 1).SetUpdatedAt(now).Save(ctx)
	}
	if err != nil {
		return 0, fmt.Errorf("更新获取道具背包: %w", err)
	}
	transactionID, err := store.newID.Next(ctx)
	if err != nil {
		return 0, err
	}
	if _, err = client.PlayerCharacterInventoryTransaction.Create().SetID(transactionID).SetPlayerCharacterID(playerID).SetItemID(itemID).SetQuantityDelta(quantity).SetBalanceAfter(row.Quantity).SetReasonCode(reason).SetReferenceID(referenceID).SetCreatedAt(now).Save(ctx); err != nil {
		return 0, fmt.Errorf("记录获取道具流水: %w", err)
	}
	return row.Quantity, nil
}

// changeCurrencyBalance 原子增加或扣减钱包并记录不可变货币流水。
func (store *EntWorldStore) changeCurrencyBalance(ctx context.Context, client *avalonent.Client, playerID, currencyID snowflake.ID, delta int64, reason string, referenceID snowflake.ID, now time.Time) (int64, error) {
	if _, err := client.PlayerCharacter.Query().Where(playercharacter.IDEQ(playerID)).ForUpdate().Only(ctx); err != nil {
		return 0, fmt.Errorf("锁定钱包所属角色: %w", err)
	}
	wallet, err := client.PlayerCharacterWallet.Query().Where(playercharacterwallet.PlayerCharacterIDEQ(playerID), playercharacterwallet.CurrencyIDEQ(currencyID)).ForUpdate().Only(ctx)
	if avalonent.IsNotFound(err) {
		if delta < 0 {
			return 0, ErrInsufficientCurrency
		}
		id, idErr := store.newID.Next(ctx)
		if idErr != nil {
			return 0, idErr
		}
		wallet, err = client.PlayerCharacterWallet.Create().SetID(id).SetPlayerCharacterID(playerID).SetCurrencyID(currencyID).SetBalance(delta).SetVersion(1).SetUpdatedAt(now).Save(ctx)
	} else if err == nil {
		if delta < 0 && wallet.Balance < -delta {
			return 0, ErrInsufficientCurrency
		}
		if delta > 0 && wallet.Balance > math.MaxInt64-delta {
			return 0, ErrEquipmentLoadoutConflict
		}
		wallet, err = client.PlayerCharacterWallet.UpdateOne(wallet).SetBalance(wallet.Balance + delta).SetVersion(wallet.Version + 1).SetUpdatedAt(now).Save(ctx)
	}
	if err != nil {
		return 0, fmt.Errorf("更新角色钱包: %w", err)
	}
	if delta == 0 {
		return wallet.Balance, nil
	}
	transactionID, err := store.newID.Next(ctx)
	if err != nil {
		return 0, err
	}
	if _, err = client.PlayerCharacterCurrencyTransaction.Create().SetID(transactionID).SetPlayerCharacterID(playerID).SetCurrencyID(currencyID).SetAmountDelta(delta).SetBalanceAfter(wallet.Balance).SetReasonCode(reason).SetReferenceID(referenceID).SetCreatedAt(now).Save(ctx); err != nil {
		return 0, fmt.Errorf("记录角色货币流水: %w", err)
	}
	return wallet.Balance, nil
}

// nextTwoIDs 以固定顺序产生共同 Operation 与来源事实 Identifier。
func (store *EntWorldStore) nextTwoIDs(ctx context.Context) (snowflake.ID, snowflake.ID, error) {
	first, err := store.newID.Next(ctx)
	if err != nil {
		return 0, 0, err
	}
	second, err := store.newID.Next(ctx)
	return first, second, err
}
