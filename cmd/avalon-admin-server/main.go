// Command avalon-admin-server 启动与玩家入口隔离的 Avalon 管理 API。
package main

import (
	"context"
	"crypto/rand"
	"errors"
	"flag"
	"fmt"
	"log/slog"
	"os"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/go-kratos/kratos/v3/middleware"
	adminv1 "github.com/lishangbu/avalon/api/gen/go/avalon/admin/v1"
	domainv1 "github.com/lishangbu/avalon/api/gen/go/avalon/domain/v1"
	rpgv1 "github.com/lishangbu/avalon/api/gen/go/avalon/rpg/v1"
	systemv1 "github.com/lishangbu/avalon/api/gen/go/avalon/system/v1"
	"github.com/lishangbu/avalon/internal/admin"
	adminapi "github.com/lishangbu/avalon/internal/admin/api"
	adminauth "github.com/lishangbu/avalon/internal/admin/auth"
	adminpersistence "github.com/lishangbu/avalon/internal/admin/persistence"
	appruntime "github.com/lishangbu/avalon/internal/app/runtime"
	"github.com/lishangbu/avalon/internal/asset"
	assetapi "github.com/lishangbu/avalon/internal/asset/api"
	assetpersistence "github.com/lishangbu/avalon/internal/asset/persistence"
	"github.com/lishangbu/avalon/internal/asset/s3store"
	gameapi "github.com/lishangbu/avalon/internal/gamedata/api"
	"github.com/lishangbu/avalon/internal/platform/config"
	"github.com/lishangbu/avalon/internal/platform/database"
	"github.com/lishangbu/avalon/internal/platform/health"
	platformlogging "github.com/lishangbu/avalon/internal/platform/logging"
	platformsessionstore "github.com/lishangbu/avalon/internal/platform/sessionstore"
	rpgapi "github.com/lishangbu/avalon/internal/rpg/api"
	rpgpersistence "github.com/lishangbu/avalon/internal/rpg/persistence"
	"github.com/lishangbu/avalon/internal/security/access"
	"github.com/lishangbu/avalon/internal/security/account"
	"github.com/lishangbu/avalon/internal/security/authentication"
	"github.com/lishangbu/avalon/internal/security/session"
	"github.com/lishangbu/avalon/internal/server"
	"github.com/lishangbu/avalon/internal/systemapi"
)

var (
	// version 由发布构建注入，标识当前管理服务版本。
	version = "dev"
	// commit 由发布构建注入，关联可审计源码提交。
	commit = "unknown"
	// apiVersion 标识当前 Proto-first 管理契约版本。
	apiVersion = "unpublished"
)

func main() {
	logger := slog.New(slog.NewJSONHandler(os.Stdout, nil))
	if err := runServer(os.Args[1:]); err != nil {
		logger.Error("Avalon admin server 退出", "error", err.Error())
		os.Exit(1)
	}
}

// runServer 加载独立管理配置、装配依赖并托管 Kratos 应用生命周期。
func runServer(args []string) error {
	flags := flag.NewFlagSet("avalon-admin-server", flag.ContinueOnError)
	flags.SetOutput(os.Stderr)
	configPath := flags.String("config", config.DefaultAdminServerPath, "avalon-admin-server YAML 配置文件路径")
	if err := flags.Parse(args); err != nil {
		return err
	}
	if flags.NArg() != 0 {
		return errors.New("不接受位置参数")
	}
	cfg, err := config.LoadAdminServer(*configPath)
	if err != nil {
		return err
	}
	logger, err := platformlogging.NewSlog(cfg.GetLog(), os.Stdout)
	if err != nil {
		return err
	}
	instanceID, err := os.Hostname()
	if err != nil || instanceID == "" {
		instanceID = "unknown"
	}
	logger = logger.With(
		"service.id", instanceID,
		"service.name", "avalon-admin-server",
		"service.version", version,
	)
	kratosLogger := logger
	if warning := config.PermissionWarning(*configPath); warning != "" {
		logger.Warn(warning)
	}

	startupContext, cancelStartup := context.WithTimeout(context.Background(), 15*time.Second)
	defer cancelStartup()
	pool, err := database.Open(config.PersistenceConfig(cfg.GetDatabase()))
	if err != nil {
		return errors.New("无法创建 PostgreSQL 连接池")
	}
	defer pool.Close()
	schemaMode, err := config.PersistenceSchemaMode(cfg.GetDatabase().GetSchemaMode())
	if err != nil {
		return err
	}
	if err := pool.Persistence().ApplySchema(startupContext, schemaMode); err != nil {
		return fmt.Errorf("应用 PostgreSQL Schema: %w", err)
	}
	identifierRuntime, err := snowflake.AcquireRuntime(startupContext, database.NewSnowflakeLeaseStore(pool))
	if err != nil {
		return fmt.Errorf("获取雪花节点租约: %w", err)
	}
	defer identifierRuntime.Close()
	identifierLeaseMonitor, err := snowflake.NewLeaseMonitor(identifierRuntime)
	if err != nil {
		return err
	}
	schema := database.NewSchemaChecker(pool, 0)

	storage := cfg.GetObjectStorage()
	assetBlobs, err := s3store.New(startupContext, s3store.Config{
		Endpoint: storage.GetEndpoint(), Region: storage.GetRegion(), Bucket: storage.GetBucket(),
		AccessKeyID: storage.GetAccessKeyId(), SecretAccessKey: storage.GetSecretAccessKey(),
		UsePathStyle: storage.GetUsePathStyle(),
	})
	if err != nil {
		return errors.New("无法初始化 RustFS 客户端")
	}
	if err := assetBlobs.Ready(startupContext); err != nil {
		return fmt.Errorf("RustFS Bucket 未就绪: %w", err)
	}
	readiness := health.NewGate(pool, schema, assetBlobs, identifierRuntime)

	valkey := cfg.GetValkey()
	sessionBackend := platformsessionstore.New(platformsessionstore.Config{Address: valkey.GetAddress(), Username: valkey.GetUsername(), Password: valkey.GetPassword(), Database: int(valkey.GetDatabase()), Prefix: "avalon:session", Domain: "admin"})
	if err := sessionBackend.Ready(startupContext); err != nil {
		return fmt.Errorf("Valkey Session Store 未就绪: %w", err)
	}
	defer sessionBackend.Close()
	authenticationRepository := adminpersistence.NewAuthenticationRepository(pool, sessionBackend)
	// 管理员会话使用领域隔离 SHA-256 摘要，数据库不保存令牌明文。
	tokens := session.NewTokenIssuer(session.TokenPurposeSession, rand.Reader)
	policy := authentication.SessionPolicy{
		AbsoluteTTL: time.Duration(cfg.GetSecurity().GetAbsoluteSessionSeconds()) * time.Second,
		IdleTTL:     time.Duration(cfg.GetSecurity().GetIdleSessionSeconds()) * time.Second,
	}
	loginService := authentication.NewService(
		authenticationRepository, account.NewPasswordHasher(rand.Reader),
		tokens, policy,
		authentication.LoginProtectionPolicy{
			LockThreshold: 5, BaseLock: time.Minute, MaximumLock: 15 * time.Minute,
		},
		identifierRuntime, time.Now,
	)
	logoutService := authentication.NewLogoutService(authenticationRepository, time.Now)
	sessionManager := authentication.NewSessionManager(
		authenticationRepository, authenticationRepository, identifierRuntime, time.Now,
	)
	identityQuery := admin.NewIdentityQuery(authenticationRepository)
	accessTokens, err := adminauth.NewEphemeralAccessTokenIssuer(10*time.Minute, time.Now)
	if err != nil {
		return err
	}
	refreshService := authentication.NewRefreshService(authenticationRepository, tokens, policy.IdleTTL, identifierRuntime, time.Now)
	refreshValidator := authentication.NewSessionAuthenticator(authenticationRepository, tokens, 0, 0, time.Now)
	adminSecurityService := adminapi.NewSecurityService(
		loginService, logoutService, identityQuery, accessTokens, refreshService, refreshValidator, sessionManager,
	)
	accessCatalog, err := access.NewOperationCatalog(adminv1.File_avalon_admin_v1_admin_proto, domainv1.File_avalon_domain_v1_domain_proto, rpgv1.File_avalon_rpg_v1_rpg_proto, systemv1.File_avalon_system_v1_system_proto)
	if err != nil {
		return fmt.Errorf("编译管理员 RPC 安全目录: %w", err)
	}

	assetApplication := asset.NewService(assetpersistence.NewRepository(pool, identifierRuntime), assetBlobs, identifierRuntime, time.Now)
	assetService := assetapi.NewKratosService(assetApplication, logger)
	gameDataServices, err := gameapi.NewAdministrationServices(pool, assetService, identifierRuntime, logger)
	if err != nil {
		return err
	}
	backgroundJobRepository := adminpersistence.NewBackgroundJobRepository(pool, identifierRuntime)
	backgroundJobApplication := admin.NewBackgroundJobService(
		backgroundJobRepository, backgroundJobRepository, backgroundJobRepository,
		backgroundJobRepository, backgroundJobRepository, time.Now,
	)
	backgroundJobService := adminapi.NewBackgroundJobService(backgroundJobApplication).
		WithBattleOperations(adminpersistence.NewBattleOperationsQuery(pool))
	rpgWorldAdapters := rpgpersistence.NewAdapters(pool, identifierRuntime)
	rpgWorldAdminService := rpgapi.NewAdminWorldService(rpgWorldAdapters, rpgWorldAdapters)
	adminManagementRepository := adminpersistence.NewManagementRepository(pool, identifierRuntime)
	adminManagementService := adminapi.NewManagementService(adminManagementRepository, adminManagementRepository)
	grpcServer := server.NewAdminGRPCServer(
		cfg.GetServer().GetGrpcAddress(), cfg.GetServer().GetConnectAddress(),
		systemapi.NewService(systemapi.BuildInfo{
			Version: version, Commit: commit, APIMajorVersion: "v1",
		}),
		adminSecurityService,
		adminManagementService,
		backgroundJobService,
		gameDataServices,
		rpgWorldAdminService,
		kratosLogger,
		[]middleware.Middleware{adminapi.NewBearerSecurityMiddleware(accessCatalog, accessTokens, identityQuery)},
	)
	application := appruntime.NewApplication(
		appruntime.ApplicationInfo{
			ID:          instanceID,
			Name:        "avalon-admin-server",
			Version:     version,
			Metadata:    map[string]string{"commit": commit, "api.version": apiVersion},
			StopTimeout: time.Duration(cfg.GetLifecycle().GetShutdownTimeoutSeconds()) * time.Second,
		},
		kratosLogger,
		grpcServer,
		identifierLeaseMonitor,
	)
	err = application.Run()
	readiness.BeginDrain()
	return err
}
