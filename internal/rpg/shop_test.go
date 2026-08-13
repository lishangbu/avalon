package rpg

import (
	"encoding/json"
	"testing"

	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// TestAdminShopJSONRoundTrip 验证可选 NPC 不会破坏商店幂等响应，商品稳定身份可完整恢复。
func TestAdminShopJSONRoundTrip(t *testing.T) {
	shopID, locationID, shopItemID, itemID, currencyID := snowflake.NewTestID(), snowflake.NewTestID(), snowflake.NewTestID(), snowflake.NewTestID(), snowflake.NewTestID()
	want := AdminShop{ID: shopID, LocationID: locationID, Code: "general-shop", Name: "杂货店", Enabled: true, Version: 2, Items: []AdminShopItem{{ID: shopItemID, ItemID: itemID, CurrencyID: currencyID, BuyPrice: 10, Enabled: true}}}
	raw, err := json.Marshal(want)
	if err != nil {
		t.Fatalf("Marshal(AdminShop) error = %v", err)
	}
	var got AdminShop
	if err = json.Unmarshal(raw, &got); err != nil {
		t.Fatalf("Unmarshal(AdminShop) error = %v", err)
	}
	if got.ID != want.ID || got.NPCID.IsValid() || got.LocationID != want.LocationID || len(got.Items) != 1 || got.Items[0].ID != shopItemID {
		t.Fatalf("AdminShop round trip = %+v", got)
	}
}
