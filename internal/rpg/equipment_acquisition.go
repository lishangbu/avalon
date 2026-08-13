package rpg

import (
	"errors"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

var (
	// ErrShopItemUnavailable 表示商品、商店、地点或关联资料不允许当前角色购买。
	ErrShopItemUnavailable = errors.New("商店商品当前不可购买")
	// ErrInsufficientCurrency 表示当前角色的钱包余额不足以原子完成购买。
	ErrInsufficientCurrency = errors.New("货币余额不足")
	// ErrQuestRewardUnavailable 表示任务未完成、版本冲突、已经领取或奖励资料不完整。
	ErrQuestRewardUnavailable = errors.New("任务奖励当前不可领取")
	// ErrLootSettlementUnavailable 表示掉落结算不存在、不属于当前角色或已经领取。
	ErrLootSettlementUnavailable = errors.New("掉落结算当前不可领取")
)

// InventoryAcquisition 是一次资产获取后普通道具的确定性结果。
type InventoryAcquisition struct {
	// ItemID 是进入聚合 Inventory Stack 的 Item Catalog Entry Identifier。
	ItemID snowflake.ID
	// QuantityDelta 是本次增加的正整数数量。
	QuantityDelta int64
	// BalanceAfter 是事务提交后的 Inventory Stack 数量。
	BalanceAfter int64
}

// CurrencyAcquisition 是一次奖励入账后的确定性货币结果。
type CurrencyAcquisition struct {
	// CurrencyID 是被增加余额的 Currency Identifier。
	CurrencyID snowflake.ID
	// AmountDelta 是本次增加的正整数金额。
	AmountDelta int64
	// BalanceAfter 是事务提交后的钱包余额。
	BalanceAfter int64
}

// ItemAcquisitionResult 返回同一命令产生的普通道具或 Equipment Instance。
type ItemAcquisitionResult struct {
	// OperationID 关联支付、资产流水、幂等响应与 Outbox。
	OperationID snowflake.ID
	// EquipmentInstanceIDs 按建立顺序保存不可堆叠装备资产身份。
	EquipmentInstanceIDs []snowflake.ID
	// InventoryStack 仅在商品是普通道具时存在。
	InventoryStack *InventoryAcquisition
	// BalanceAfter 是商店支付后的钱包余额。
	BalanceAfter int64
}

// RewardAcquisitionResult 返回一次任务或掉落领取产生的全部资产。
type RewardAcquisitionResult struct {
	// OperationID 关联本次领取的全部流水和 Outbox。
	OperationID snowflake.ID
	// EquipmentInstanceIDs 是本次建立的全部不可堆叠装备资产。
	EquipmentInstanceIDs []snowflake.ID
	// InventoryStacks 是按 Item Identifier 排序的普通道具结果。
	InventoryStacks []InventoryAcquisition
	// CurrencyBalances 是按 Currency Identifier 排序的货币结果。
	CurrencyBalances []CurrencyAcquisition
}

// PurchaseShopItemCommand 是读取服务端价格并原子支付、交付的玩家命令。
type PurchaseShopItemCommand struct {
	// AccountID 用于解析当前活动 PlayerCharacter。
	AccountID snowflake.ID
	// ShopItemID 是服务端已维护的商品关系身份。
	ShopItemID snowflake.ID
	// Quantity 是一次购买的一至一百件商品数量。
	Quantity int32
	// IdempotencyKey 使网络重试返回首次支付与交付结果。
	IdempotencyKey string
	// Now 是购买事实、流水和 Outbox 统一使用的 UTC 时间。
	Now time.Time
}

// ClaimQuestRewardsCommand 领取当前已完成任务轮次的全部奖励。
type ClaimQuestRewardsCommand struct {
	// AccountID 与 QuestID 定位当前角色的任务进度。
	AccountID, QuestID snowflake.ID
	// ExpectedProgressVersion 防止领取时覆盖并发任务进度变化。
	ExpectedProgressVersion int64
	// IdempotencyKey 使领取重试只产生一组资产。
	IdempotencyKey string
	// Now 是领取事实、流水和 Outbox 统一使用的 UTC 时间。
	Now time.Time
}

// ClaimLootSettlementCommand 领取服务端预先建立的权威掉落结算。
type ClaimLootSettlementCommand struct {
	// AccountID 用于解析当前活动 PlayerCharacter。
	AccountID snowflake.ID
	// LootSettlementID 由 Battle 或世界交互创建，客户端不能自行构造其内容。
	LootSettlementID snowflake.ID
	// IdempotencyKey 使领取重试只产生一组资产。
	IdempotencyKey string
	// Now 是领取事实、流水和 Outbox 统一使用的 UTC 时间。
	Now time.Time
}
