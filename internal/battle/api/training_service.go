package api

import (
	"context"
	"strings"

	kratoserrors "github.com/go-kratos/kratos/v3/errors"
	battlev1 "github.com/lishangbu/avalon/api/gen/go/avalon/battle/v1"
	battle "github.com/lishangbu/avalon/internal/battle"
)

// CreateTrainingBattle 创建当前账号活动角色与指定固定版本 Bot 的 Preview Training Battle。
func (service *KratosService) CreateTrainingBattle(
	ctx context.Context,
	request *battlev1.CreateTrainingBattleRequest,
) (*battlev1.CreateTrainingBattleResponse, error) {
	principal, err := battlePrincipal(ctx)
	if err != nil {
		return nil, err
	}
	if service.training == nil || request.GetBody() == nil {
		return nil, kratoserrors.ServiceUnavailable("PRACTICE_UNAVAILABLE", "练习战服务当前不可用")
	}
	teamID, err := parseBattleIdentifier(request.GetBody().GetTeamId(), "INVALID_TEAM_ID")
	if err != nil {
		return nil, err
	}
	formatID, err := parseBattleIdentifier(request.GetBody().GetBattleFormatId(), "INVALID_BATTLE_FORMAT_ID")
	if err != nil {
		return nil, err
	}
	if strings.TrimSpace(request.GetBody().GetBotCode()) == "" {
		return nil, kratoserrors.BadRequest("INVALID_BOT_CODE", "Bot 代码不能为空")
	}
	session, err := service.training.Create(ctx, principal.AccountID, battle.CreateTrainingApplicationCommand{
		TeamID: teamID, BattleFormatID: formatID, BotCode: request.GetBody().GetBotCode(),
	})
	if err != nil {
		return nil, service.challengeError(ctx, "PRACTICE_CREATE_FAILED", err)
	}
	service.publishDisclosure(ctx, session.ID)
	return &battlev1.CreateTrainingBattleResponse{HttpStatusCode: 201, Body: battleView(session, principal.AccountID)}, nil
}
