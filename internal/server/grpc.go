package server

import (
	"context"
	"fmt"
	"io"
	"log/slog"
	"net/http"
	"strings"

	"connectrpc.com/connect"
	"github.com/go-kratos/kratos/v3/middleware"
	kratoslogging "github.com/go-kratos/kratos/v3/middleware/logging"
	"github.com/go-kratos/kratos/v3/middleware/metadata"
	"github.com/go-kratos/kratos/v3/middleware/ratelimit"
	"github.com/go-kratos/kratos/v3/middleware/recovery"
	"github.com/go-kratos/kratos/v3/middleware/selector"
	"github.com/go-kratos/kratos/v3/transport"
	kratosgrpc "github.com/go-kratos/kratos/v3/transport/grpc"
	kratoshttp "github.com/go-kratos/kratos/v3/transport/http"
	adminv1 "github.com/lishangbu/avalon/api/gen/go/avalon/admin/v1"
	adminv1connect "github.com/lishangbu/avalon/api/gen/go/avalon/admin/v1/adminv1connect"
	battlev1 "github.com/lishangbu/avalon/api/gen/go/avalon/battle/v1"
	battlev1connect "github.com/lishangbu/avalon/api/gen/go/avalon/battle/v1/v1connect"
	domainv1 "github.com/lishangbu/avalon/api/gen/go/avalon/domain/v1"
	domainv1connect "github.com/lishangbu/avalon/api/gen/go/avalon/domain/v1/v1connect"
	rpgv1 "github.com/lishangbu/avalon/api/gen/go/avalon/rpg/v1"
	rpgv1connect "github.com/lishangbu/avalon/api/gen/go/avalon/rpg/v1/rpgv1connect"
	securityv1 "github.com/lishangbu/avalon/api/gen/go/avalon/security/v1"
	securityv1connect "github.com/lishangbu/avalon/api/gen/go/avalon/security/v1/securityv1connect"
	systemv1 "github.com/lishangbu/avalon/api/gen/go/avalon/system/v1"
	systemv1connect "github.com/lishangbu/avalon/api/gen/go/avalon/system/v1/systemv1connect"
	"github.com/lishangbu/avalon/internal/platform/httpapi"
	"github.com/lishangbu/avalon/internal/platform/requestid"
	"golang.org/x/sync/errgroup"
	"google.golang.org/grpc"
	grpcmetadata "google.golang.org/grpc/metadata"
)

// connectTransport 将 Connect Handler 作为 Kratos HTTP Transport 暴露。
// Connect Handler 提供浏览器 Connect 协议，并与原生 gRPC 服务共用 Proto 契约。
type connectTransport struct{ server *kratoshttp.Server }

// kratosConnectInterceptor 将 Kratos Middleware 链接入 Connect Handler。
// Kratos HTTP 路由的 NotFoundHandler 不会自动执行注册到 Server 的 RPC Middleware，
// 因此 Connect Handler 必须显式使用同一条链，确保 Bearer、限流、日志和恢复行为一致。
type kratosConnectInterceptor struct{ middlewares []middleware.Middleware }

func (interceptor *kratosConnectInterceptor) WrapUnary(next connect.UnaryFunc) connect.UnaryFunc {
	return func(ctx context.Context, request connect.AnyRequest) (connect.AnyResponse, error) {
		currentRequestID := httpapi.EnsureRequestID(request.Header(), requestid.New)
		ctx = transport.NewServerContext(ctx, newConnectRequestTransport(request.Spec().Procedure, request.Header()))
		handler := middleware.Chain(interceptor.middlewares...)(func(ctx context.Context, value any) (any, error) {
			return next(ctx, value.(connect.AnyRequest))
		})
		response, err := handler(ctx, request)
		if response == nil {
			return nil, err
		}
		result := response.(connect.AnyResponse)
		result.Header().Set("X-Request-ID", currentRequestID)
		if current, ok := transport.FromServerContext(ctx); ok {
			for _, key := range current.ReplyHeader().Keys() {
				for _, value := range current.ReplyHeader().Values(key) {
					result.Header().Add(key, value)
				}
			}
		}
		return result, err
	}
}

func (interceptor *kratosConnectInterceptor) WrapStreamingHandler(next connect.StreamingHandlerFunc) connect.StreamingHandlerFunc {
	return func(ctx context.Context, conn connect.StreamingHandlerConn) error {
		httpapi.EnsureRequestID(conn.RequestHeader(), requestid.New)
		ctx = transport.NewServerContext(ctx, newConnectRequestTransport(conn.Spec().Procedure, conn.RequestHeader()))
		handler := middleware.Chain(interceptor.middlewares...)(func(ctx context.Context, value any) (any, error) {
			return nil, next(ctx, value.(connect.StreamingHandlerConn))
		})
		_, err := handler(ctx, conn)
		return err
	}
}

// connectRequestTransport 将 Connect 请求的 procedure 和 HTTP Header 映射为 Kratos Transport Context。
type connectRequestTransport struct {
	operation     string
	requestHeader connectHeader
	replyHeader   connectHeader
}

func newConnectRequestTransport(operation string, requestHeader http.Header) *connectRequestTransport {
	return &connectRequestTransport{
		operation:     operation,
		requestHeader: connectHeader(requestHeader),
		replyHeader:   connectHeader(make(http.Header)),
	}
}

func (*connectRequestTransport) Kind() transport.Kind { return transport.KindHTTP }
func (*connectRequestTransport) Endpoint() string     { return "connect://avalon" }
func (value *connectRequestTransport) Operation() string {
	return value.operation
}
func (value *connectRequestTransport) RequestHeader() transport.Header { return value.requestHeader }
func (value *connectRequestTransport) ReplyHeader() transport.Header   { return value.replyHeader }

type connectHeader http.Header

func (header connectHeader) Get(key string) string      { return http.Header(header).Get(key) }
func (header connectHeader) Set(key, value string)      { http.Header(header).Set(key, value) }
func (header connectHeader) Add(key, value string)      { http.Header(header).Add(key, value) }
func (header connectHeader) Values(key string) []string { return http.Header(header).Values(key) }
func (header connectHeader) Keys() []string {
	keys := make([]string, 0, len(header))
	for key := range header {
		keys = append(keys, key)
	}
	return keys
}

func (interceptor *kratosConnectInterceptor) WrapStreamingClient(next connect.StreamingClientFunc) connect.StreamingClientFunc {
	return next
}

func connectHandlerOptions(middlewares []middleware.Middleware) []connect.HandlerOption {
	return []connect.HandlerOption{connect.WithInterceptors(&kratosConnectInterceptor{middlewares: middlewares})}
}

// battleConnectHandler 复用现有 Battle unary 方法，并仅桥接原生 gRPC 与 Connect 的服务端流接口。
type battleConnectHandler struct{ battlev1.BattleServiceServer }

func (handler *battleConnectHandler) WatchBattleDisclosure(ctx context.Context, request *battlev1.WatchBattleDisclosureRequest, stream *connect.ServerStream[battlev1.WatchBattleDisclosureResponse]) error {
	return handler.BattleServiceServer.WatchBattleDisclosure(request, &connectServerStream[battlev1.WatchBattleDisclosureResponse]{ctx: ctx, stream: stream})
}

type connectServerStream[T any] struct {
	ctx    context.Context
	stream *connect.ServerStream[T]
}

func (stream *connectServerStream[T]) SetHeader(values grpcmetadata.MD) error {
	copyMetadata(stream.stream.ResponseHeader(), values)
	return nil
}

func (stream *connectServerStream[T]) SendHeader(values grpcmetadata.MD) error {
	return stream.SetHeader(values)
}

func (stream *connectServerStream[T]) SetTrailer(values grpcmetadata.MD) {
	copyMetadata(stream.stream.ResponseTrailer(), values)
}

func (stream *connectServerStream[T]) Context() context.Context { return stream.ctx }

func (stream *connectServerStream[T]) SendMsg(message any) error {
	value, ok := message.(*T)
	if !ok {
		return fmt.Errorf("Connect 服务端流消息类型错误：%T", message)
	}
	return stream.Send(value)
}

func (stream *connectServerStream[T]) RecvMsg(any) error { return io.EOF }
func (stream *connectServerStream[T]) Send(message *T) error {
	return stream.stream.Send(message)
}

func copyMetadata(header http.Header, values grpcmetadata.MD) {
	for key, items := range values {
		for _, item := range items {
			header.Add(key, item)
		}
	}
}

func newConnectTransport(address string, middlewares []middleware.Middleware, handlers ...func(*http.ServeMux)) *connectTransport {
	mux := http.NewServeMux()
	for _, register := range handlers {
		register(mux)
	}
	return &connectTransport{server: kratoshttp.NewServer(
		kratoshttp.Address(address), kratoshttp.Middleware(middlewares...), kratoshttp.NotFoundHandler(mux),
	)}
}

func (transport *connectTransport) Start(ctx context.Context) error {
	return transport.server.Start(ctx)
}
func (transport *connectTransport) Stop(ctx context.Context) error { return transport.server.Stop(ctx) }

// AdminGRPCServer 是管理 Proto 服务的 Kratos gRPC 与 Connect HTTP Transport 组合。
type AdminGRPCServer struct {
	grpcServer    *kratosgrpc.Server
	connectServer *connectTransport
}

// PlayerGRPCServer 是玩家 Proto 服务的 Kratos gRPC 与 Connect HTTP Transport 组合。
type PlayerGRPCServer struct {
	grpcServer    *kratosgrpc.Server
	connectServer *connectTransport
}

// NewPlayerGRPCServer 创建玩家服务的 Kratos Transport。
func NewPlayerGRPCServer(grpcAddress, connectAddress string, systemService systemv1.SystemServiceServer, securityService securityv1.PlayerSecurityServiceServer, playerCharacterService domainv1.PlayerCharacterServiceServer, teamService domainv1.TeamServiceServer, playerService domainv1.PlayerServiceServer, battleService battlev1.BattleServiceServer, challengeService battlev1.ChallengeServiceServer, pveService battlev1.PvEServiceServer, rpgWorldService rpgv1.RpgWorldServiceServer, logger *slog.Logger, middlewares []middleware.Middleware, options ...grpc.ServerOption) *PlayerGRPCServer {
	base := serverMiddleware(logger, middlewares...)
	grpcServer := kratosgrpc.NewServer(kratosgrpc.Address(grpcAddress), kratosgrpc.Middleware(base...), kratosgrpc.Options(options...))
	systemv1.RegisterSystemServiceServer(grpcServer, systemService)
	securityv1.RegisterPlayerSecurityServiceServer(grpcServer, securityService)
	domainv1.RegisterPlayerCharacterServiceServer(grpcServer, playerCharacterService)
	domainv1.RegisterTeamServiceServer(grpcServer, teamService)
	domainv1.RegisterPlayerServiceServer(grpcServer, playerService)
	battlev1.RegisterBattleServiceServer(grpcServer, battleService)
	battlev1.RegisterChallengeServiceServer(grpcServer, challengeService)
	battlev1.RegisterPvEServiceServer(grpcServer, pveService)
	rpgv1.RegisterRpgWorldServiceServer(grpcServer, rpgWorldService)
	connectServer := newConnectTransport(connectAddress, serverMiddleware(logger, middlewares...), func(mux *http.ServeMux) {
		opts := connectHandlerOptions(serverMiddleware(logger, middlewares...))
		p, h := systemv1connect.NewSystemServiceHandler(systemService, opts...)
		mux.Handle(p, h)
		p, h = securityv1connect.NewPlayerSecurityServiceHandler(securityService, opts...)
		mux.Handle(p, h)
		p, h = domainv1connect.NewPlayerCharacterServiceHandler(playerCharacterService, opts...)
		mux.Handle(p, h)
		p, h = domainv1connect.NewTeamServiceHandler(teamService, opts...)
		mux.Handle(p, h)
		p, h = domainv1connect.NewPlayerServiceHandler(playerService, opts...)
		mux.Handle(p, h)
		p, h = battlev1connect.NewBattleServiceHandler(&battleConnectHandler{BattleServiceServer: battleService}, opts...)
		mux.Handle(p, h)
		p, h = battlev1connect.NewChallengeServiceHandler(challengeService, opts...)
		mux.Handle(p, h)
		p, h = battlev1connect.NewPvEServiceHandler(pveService, opts...)
		mux.Handle(p, h)
		p, h = rpgv1connect.NewRpgWorldServiceHandler(rpgWorldService, opts...)
		mux.Handle(p, h)
	})
	return &PlayerGRPCServer{grpcServer: grpcServer, connectServer: connectServer}
}

// NewAdminGRPCServer 创建管理服务的 Kratos Transport。
func NewAdminGRPCServer(grpcAddress, connectAddress string, systemService systemv1.SystemServiceServer, securityService adminv1.AdminSecurityServiceServer, managementService adminv1.AdminManagementServiceServer, operationsService adminv1.AdminOperationsServiceServer, gameDataService domainv1.GameDataServiceServer, rpgWorldAdminService rpgv1.RpgWorldAdminServiceServer, logger *slog.Logger, middlewares []middleware.Middleware, options ...grpc.ServerOption) *AdminGRPCServer {
	base := serverMiddleware(logger, middlewares...)
	grpcServer := kratosgrpc.NewServer(kratosgrpc.Address(grpcAddress), kratosgrpc.Middleware(base...), kratosgrpc.Options(options...))
	systemv1.RegisterSystemServiceServer(grpcServer, systemService)
	adminv1.RegisterAdminSecurityServiceServer(grpcServer, securityService)
	adminv1.RegisterAdminManagementServiceServer(grpcServer, managementService)
	adminv1.RegisterAdminOperationsServiceServer(grpcServer, operationsService)
	domainv1.RegisterGameDataServiceServer(grpcServer, gameDataService)
	rpgv1.RegisterRpgWorldAdminServiceServer(grpcServer, rpgWorldAdminService)
	connectServer := newConnectTransport(connectAddress, serverMiddleware(logger, middlewares...), func(mux *http.ServeMux) {
		opts := connectHandlerOptions(serverMiddleware(logger, middlewares...))
		p, h := systemv1connect.NewSystemServiceHandler(systemService, opts...)
		mux.Handle(p, h)
		p, h = adminv1connect.NewAdminSecurityServiceHandler(securityService, opts...)
		mux.Handle(p, h)
		p, h = adminv1connect.NewAdminManagementServiceHandler(managementService, opts...)
		mux.Handle(p, h)
		p, h = adminv1connect.NewAdminOperationsServiceHandler(operationsService, opts...)
		mux.Handle(p, h)
		p, h = domainv1connect.NewGameDataServiceHandler(gameDataService, opts...)
		mux.Handle(p, h)
		p, h = rpgv1connect.NewRpgWorldAdminServiceHandler(rpgWorldAdminService, opts...)
		mux.Handle(p, h)
	})
	return &AdminGRPCServer{grpcServer: grpcServer, connectServer: connectServer}
}

func serverMiddleware(logger *slog.Logger, custom ...middleware.Middleware) []middleware.Middleware {
	chain := observabilityMiddleware(logger)
	chain = append(chain, selector.Server(ratelimit.Server()).Match(func(_ context.Context, operation string) bool {
		return strings.HasSuffix(operation, "/Login") || strings.HasSuffix(operation, "/Refresh")
	}).Build())
	return append(chain, custom...)
}

func observabilityMiddleware(logger *slog.Logger) []middleware.Middleware {
	if logger == nil {
		logger = slog.Default()
	}
	return []middleware.Middleware{metadata.Server(metadata.WithPropagatedPrefix("x-request-id", "x-trace-id", "x-client-version", "x-protocol-source")), kratoslogging.Server(logger), recovery.Recovery(recovery.WithLogger(logger))}
}

// Start 启动原生 gRPC 与浏览器 Connect Transport。
func (server *AdminGRPCServer) Start(ctx context.Context) error {
	return startTransports(ctx, server.grpcServer, server.connectServer)
}

// Stop 按逆序优雅停止管理 Transport。
func (server *AdminGRPCServer) Stop(ctx context.Context) error {
	connectErr := server.connectServer.Stop(ctx)
	grpcErr := server.grpcServer.Stop(ctx)
	if connectErr != nil {
		return connectErr
	}
	return grpcErr
}

// Start 启动原生 gRPC 与浏览器 Connect Transport。
func (server *PlayerGRPCServer) Start(ctx context.Context) error {
	return startTransports(ctx, server.grpcServer, server.connectServer)
}

// Stop 按逆序优雅停止玩家 Transport。
func (server *PlayerGRPCServer) Stop(ctx context.Context) error {
	connectErr := server.connectServer.Stop(ctx)
	grpcErr := server.grpcServer.Stop(ctx)
	if connectErr != nil {
		return connectErr
	}
	return grpcErr
}

// startTransports 并发运行一个业务入口的全部 Transport，避免前一个 Serve 循环阻塞后续监听器启动。
func startTransports(ctx context.Context, servers ...transport.Server) error {
	group, groupContext := errgroup.WithContext(ctx)
	for _, current := range servers {
		server := current
		group.Go(func() error {
			return server.Start(groupContext)
		})
	}
	return group.Wait()
}
