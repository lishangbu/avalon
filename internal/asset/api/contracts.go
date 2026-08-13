package api

import (
	"context"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/lishangbu/avalon/internal/asset"
)

// Service 是原生 Kratos 资产边界所需的最小应用能力。
type Service interface {
	List(context.Context, snowflake.ID, asset.ListQuery) (asset.Page, error)
	BeginUpload(context.Context, asset.BeginUploadCommand) (asset.UploadGrant, error)
	Confirm(context.Context, asset.ConfirmCommand) (asset.Asset, error)
	Download(context.Context, snowflake.ID, snowflake.ID) (asset.DownloadGrant, error)
}
