package api

import (
	"testing"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/lishangbu/avalon/internal/gamedata/item"
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
