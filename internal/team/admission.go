package team

import (
	"context"

	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// AdmissionService 在 Challenge 接受或 Training Battle 创建前重新读取并校验权威 Team。
type AdmissionService struct {
	// queries 通过账号和 PlayerCharacter 双重所有权读取 Team。
	queries *QueryService
	// validator 按当前实时资料修订重新校验全部成员引用。
	validator *CatalogValidator
}

// NewAdmissionService 使用显式查询与实时资料校验边界创建对战入场服务。
func NewAdmissionService(queries *QueryService, validator *CatalogValidator) *AdmissionService {
	return &AdmissionService{queries: queries, validator: validator}
}

// ValidateOwned 返回可冻结为 Battle Team Snapshot 的完整 Team。
func (service *AdmissionService) ValidateOwned(
	ctx context.Context,
	accountID snowflake.ID,
	playerCharacterID snowflake.ID,
	teamID snowflake.ID,
) (Team, error) {
	if service == nil || service.queries == nil || service.validator == nil {
		return Team{}, ErrTeamCatalogUnavailable
	}
	value, err := service.queries.GetOwned(ctx, accountID, playerCharacterID, teamID)
	if err != nil {
		return Team{}, err
	}
	if err := service.validator.ValidateCurrent(ctx, value.Members); err != nil {
		return Team{}, err
	}
	return value, nil
}
