package api

import (
	"context"
	"errors"
	"strings"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	kratoserrors "github.com/go-kratos/kratos/v3/errors"
	battlev1 "github.com/lishangbu/avalon/api/gen/go/avalon/battle/v1"
	battle "github.com/lishangbu/avalon/internal/battle"
	battlestore "github.com/lishangbu/avalon/internal/battle/store"
)

// CreateChallenge 创建当前账号活动角色发起的短期对战邀请。
func (service *KratosService) CreateChallenge(
	ctx context.Context,
	request *battlev1.CreateChallengeRequest,
) (*battlev1.CreateChallengeResponse, error) {
	principal, err := battlePrincipal(ctx)
	if err != nil {
		return nil, err
	}
	if service.challenges == nil || request.GetBody() == nil {
		return nil, kratoserrors.ServiceUnavailable("CHALLENGE_UNAVAILABLE", "挑战服务当前不可用")
	}
	teamID, err := parseBattleIdentifier(request.GetBody().GetTeamId(), "INVALID_TEAM_ID")
	if err != nil {
		return nil, err
	}
	formatID, err := parseBattleIdentifier(request.GetBody().GetBattleFormatId(), "INVALID_BATTLE_FORMAT_ID")
	if err != nil {
		return nil, err
	}
	if strings.TrimSpace(request.GetBody().GetTargetDisplayName()) == "" {
		return nil, kratoserrors.BadRequest("INVALID_TARGET_DISPLAY_NAME", "目标展示名称不能为空")
	}
	challenge, err := service.challenges.Create(ctx, principal.AccountID, battle.CreateChallengeApplicationCommand{
		TeamID: teamID, TargetDisplayName: request.GetBody().GetTargetDisplayName(), BattleFormatID: formatID,
	})
	if err != nil {
		return nil, service.challengeError(ctx, "CHALLENGE_CREATE_FAILED", err)
	}
	return &battlev1.CreateChallengeResponse{HttpStatusCode: 201, Body: challengeView(challenge)}, nil
}

// GetChallenge 查询当前账号作为发起方或接收方参与的单个 Challenge。
func (service *KratosService) GetChallenge(
	ctx context.Context,
	request *battlev1.GetChallengeRequest,
) (*battlev1.GetChallengeResponse, error) {
	principal, err := battlePrincipal(ctx)
	if err != nil {
		return nil, err
	}
	if service.challenges == nil {
		return nil, kratoserrors.ServiceUnavailable("CHALLENGE_UNAVAILABLE", "挑战服务当前不可用")
	}
	challengeID, err := parseBattleIdentifier(request.GetChallengeId(), "INVALID_CHALLENGE_ID")
	if err != nil {
		return nil, err
	}
	challenge, err := service.challenges.Get(ctx, principal.AccountID, challengeID)
	if err != nil {
		return nil, service.challengeError(ctx, "CHALLENGE_QUERY_FAILED", err)
	}
	return &battlev1.GetChallengeResponse{HttpStatusCode: 200, Body: challengeView(challenge)}, nil
}

// AcceptChallenge 接受 Challenge，并返回同一事务创建的 Preview Battle 参与者视图。
func (service *KratosService) AcceptChallenge(
	ctx context.Context,
	request *battlev1.AcceptChallengeRequest,
) (*battlev1.AcceptChallengeResponse, error) {
	principal, err := battlePrincipal(ctx)
	if err != nil {
		return nil, err
	}
	if service.challenges == nil || request.GetBody() == nil {
		return nil, kratoserrors.ServiceUnavailable("CHALLENGE_UNAVAILABLE", "挑战服务当前不可用")
	}
	challengeID, err := parseBattleIdentifier(request.GetChallengeId(), "INVALID_CHALLENGE_ID")
	if err != nil {
		return nil, err
	}
	teamID, err := parseBattleIdentifier(request.GetBody().GetTeamId(), "INVALID_TEAM_ID")
	if err != nil {
		return nil, err
	}
	session, err := service.challenges.Accept(ctx, principal.AccountID, battle.AcceptChallengeApplicationCommand{
		ChallengeID: challengeID, TeamID: teamID,
	})
	if err != nil {
		return nil, service.challengeError(ctx, "CHALLENGE_ACCEPT_FAILED", err)
	}
	service.publishDisclosure(ctx, session.ID)
	return &battlev1.AcceptChallengeResponse{HttpStatusCode: 201, Body: battleView(session, principal.AccountID)}, nil
}

// RejectChallenge 拒绝当前活动角色收到的待处理 Challenge。
func (service *KratosService) RejectChallenge(
	ctx context.Context,
	request *battlev1.RejectChallengeRequest,
) (*battlev1.RejectChallengeResponse, error) {
	challenge, err := service.resolveChallengeForCurrentAccount(ctx, request.GetChallengeId(), "CHALLENGE_REJECT_FAILED", func(accountID, challengeID snowflake.ID) (battle.Challenge, error) {
		return service.challenges.Reject(ctx, accountID, challengeID)
	})
	if err != nil {
		return nil, err
	}
	return &battlev1.RejectChallengeResponse{HttpStatusCode: 200, Body: challengeView(challenge)}, nil
}

// WithdrawChallenge 撤回当前活动角色发起的待处理 Challenge。
func (service *KratosService) WithdrawChallenge(
	ctx context.Context,
	request *battlev1.WithdrawChallengeRequest,
) (*battlev1.WithdrawChallengeResponse, error) {
	challenge, err := service.resolveChallengeForCurrentAccount(ctx, request.GetChallengeId(), "CHALLENGE_WITHDRAW_FAILED", func(accountID, challengeID snowflake.ID) (battle.Challenge, error) {
		return service.challenges.Withdraw(ctx, accountID, challengeID)
	})
	if err != nil {
		return nil, err
	}
	return &battlev1.WithdrawChallengeResponse{HttpStatusCode: 200, Body: challengeView(challenge)}, nil
}

func (service *KratosService) resolveChallengeForCurrentAccount(
	ctx context.Context,
	rawChallengeID string,
	reason string,
	resolve func(snowflake.ID, snowflake.ID) (battle.Challenge, error),
) (battle.Challenge, error) {
	principal, err := battlePrincipal(ctx)
	if err != nil {
		return battle.Challenge{}, err
	}
	if service.challenges == nil {
		return battle.Challenge{}, kratoserrors.ServiceUnavailable("CHALLENGE_UNAVAILABLE", "挑战服务当前不可用")
	}
	challengeID, err := parseBattleIdentifier(rawChallengeID, "INVALID_CHALLENGE_ID")
	if err != nil {
		return battle.Challenge{}, err
	}
	challenge, err := resolve(principal.AccountID, challengeID)
	if err != nil {
		return battle.Challenge{}, service.challengeError(ctx, reason, err)
	}
	return challenge, nil
}

func challengeView(challenge battle.Challenge) *battlev1.ChallengeView {
	return &battlev1.ChallengeView{
		Id: challenge.ID.String(), Status: string(challenge.Status), ChallengerDisplayName: challenge.ChallengerDisplayName,
		TargetDisplayName: challenge.TargetDisplayName, BattleFormatId: challenge.BattleFormatID.String(),
		ExpiresAt: timeString(challenge.ExpiresAt), TerminalReason: challenge.TerminalReason,
		ResolvedAt: timeString(challenge.ResolvedAt),
	}
}

func (service *KratosService) challengeError(ctx context.Context, reason string, err error) error {
	switch {
	case errors.Is(err, battlestore.ErrChallengeNotFound), errors.Is(err, battle.ErrChallengeActorMismatch):
		return kratoserrors.NotFound("CHALLENGE_NOT_FOUND", "挑战不存在")
	case errors.Is(err, battle.ErrChallengeCreationUnavailable):
		return kratoserrors.Conflict("CHALLENGE_UNAVAILABLE", "当前无法创建或接受挑战")
	case errors.Is(err, battle.ErrChallengeNotPending), errors.Is(err, battle.ErrChallengeExpired),
		errors.Is(err, battle.ErrChallengeRecipientMismatch), errors.Is(err, battlestore.ErrBattleConflict):
		return kratoserrors.Conflict("CHALLENGE_CONFLICT", "挑战状态已变化或操作不匹配")
	default:
		service.logger.ErrorContext(ctx, "Challenge Kratos 服务调用失败", "reason", reason, "error", err)
		return kratoserrors.InternalServer(reason, "服务端无法完成请求")
	}
}
