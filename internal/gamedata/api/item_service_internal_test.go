package api

import (
	"testing"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/lishangbu/avalon/internal/gamedata/item"
	"github.com/lishangbu/avalon/internal/gamedata/itemrules"
)

// TestGameItemMessageExposesReadyAsset 验证管理端 Item 响应返回目录图标的 Ready Asset，
// 使客户端可以通过现有公开 Asset 下载链路渲染已初始化图片。
func TestGameItemMessageExposesReadyAsset(t *testing.T) {
	assetID := snowflake.MustParse("1048576000")
	message := gameItemMessage(item.Item{
		ID: snowflake.MustParse("1048576001"), Code: "master-ball", Name: "大师球",
		UsageType: item.UsageCapture, Version: 1, AssetID: &assetID,
	})
	if message.GetAssetId() != assetID.String() {
		t.Fatalf("GameItem.assetId = %q, want %q", message.GetAssetId(), assetID)
	}
}

// TestGameItemRulesMessagePreservesNormalizedFields 验证关系表规则经过 Protobuf 映射后保持 Identifier、布尔和整数值。
func TestGameItemRulesMessagePreservesNormalizedFields(t *testing.T) {
	itemID := snowflake.MustParse("1048576001")
	elementID := snowflake.MustParse("1048576002")
	message, err := gameItemRulesMessage(item.Rules{ItemID: itemID, Version: 7, Rules: itemrules.Detail{
		ItemID: itemID, EndTurnHealDenominator: 16, CuresPoison: true,
		ElementDamageBoostElementID: &elementID,
	}})
	if err != nil {
		t.Fatalf("gameItemRulesMessage() error = %v", err)
	}
	if message.GetItemId() != itemID.String() || message.GetVersion() != "7" ||
		message.GetEndTurnHealDenominator() != 16 || !message.GetCuresPoison() ||
		message.GetElementDamageBoostElementId() != elementID.String() {
		t.Fatalf("GameItemRules = %+v", message)
	}
}
