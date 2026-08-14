package main

import (
	"context"
	"crypto/rand"
	"encoding/binary"
	"errors"
	"flag"
	"fmt"
	"log/slog"
	"os"
	"sync"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/go-kratos/kratos/v3/middleware"
	battlev1 "github.com/lishangbu/avalon/api/gen/go/avalon/battle/v1"
	domainv1 "github.com/lishangbu/avalon/api/gen/go/avalon/domain/v1"
	rpgv1 "github.com/lishangbu/avalon/api/gen/go/avalon/rpg/v1"
	securityv1 "github.com/lishangbu/avalon/api/gen/go/avalon/security/v1"
	systemv1 "github.com/lishangbu/avalon/api/gen/go/avalon/system/v1"
	adminauth "github.com/lishangbu/avalon/internal/admin/auth"
	appruntime "github.com/lishangbu/avalon/internal/app/runtime"
	battle "github.com/lishangbu/avalon/internal/battle"
	battleapi "github.com/lishangbu/avalon/internal/battle/api"
	battlestore "github.com/lishangbu/avalon/internal/battle/store"
	"github.com/lishangbu/avalon/internal/battleengine"
	"github.com/lishangbu/avalon/internal/gamedata/ability"
	gameapi "github.com/lishangbu/avalon/internal/gamedata/api"
	"github.com/lishangbu/avalon/internal/gamedata/battleformat"
	"github.com/lishangbu/avalon/internal/gamedata/creaturemetadata"
	"github.com/lishangbu/avalon/internal/gamedata/effect"
	"github.com/lishangbu/avalon/internal/gamedata/element"
	"github.com/lishangbu/avalon/internal/gamedata/elementeffectiveness"
	"github.com/lishangbu/avalon/internal/gamedata/item"
	"github.com/lishangbu/avalon/internal/gamedata/nature"
	"github.com/lishangbu/avalon/internal/gamedata/skill"
	"github.com/lishangbu/avalon/internal/gamedata/skillailment"
	"github.com/lishangbu/avalon/internal/gamedata/skilldamageclass"
	"github.com/lishangbu/avalon/internal/gamedata/skillstatchange"
	"github.com/lishangbu/avalon/internal/gamedata/skilltarget"
	"github.com/lishangbu/avalon/internal/gamedata/stat"
	gamedatastore "github.com/lishangbu/avalon/internal/gamedata/store"
	"github.com/lishangbu/avalon/internal/gamedata/teamcatalog"
	"github.com/lishangbu/avalon/internal/platform/config"
	"github.com/lishangbu/avalon/internal/platform/database"
	"github.com/lishangbu/avalon/internal/platform/health"
	platformlogging "github.com/lishangbu/avalon/internal/platform/logging"
	platformsessionstore "github.com/lishangbu/avalon/internal/platform/sessionstore"
	"github.com/lishangbu/avalon/internal/playercharacter"
	playercharacterapi "github.com/lishangbu/avalon/internal/playercharacter/api"
	playercharacterstore "github.com/lishangbu/avalon/internal/playercharacter/store"
	"github.com/lishangbu/avalon/internal/rpg"
	rpgapi "github.com/lishangbu/avalon/internal/rpg/api"
	"github.com/lishangbu/avalon/internal/security/access"
	"github.com/lishangbu/avalon/internal/security/account"
	securityapi "github.com/lishangbu/avalon/internal/security/api"
	"github.com/lishangbu/avalon/internal/security/authentication"
	securitypersistence "github.com/lishangbu/avalon/internal/security/persistence"
	"github.com/lishangbu/avalon/internal/security/session"
	"github.com/lishangbu/avalon/internal/server"
	"github.com/lishangbu/avalon/internal/systemapi"
	"github.com/lishangbu/avalon/internal/team"
	teamapi "github.com/lishangbu/avalon/internal/team/api"
	teamstore "github.com/lishangbu/avalon/internal/team/store"
)

var (
	version = "dev"
	commit  = "unknown"
)

const (
	// defaultBattleRuntimeCapacity 限制单实例进程同时承载的活跃 Battle Runtime 数量。
	defaultBattleRuntimeCapacity = 128
	// runtimePanicInterruptTimeout 限制隔离 Runtime 故障时的持久化清理时间。
	runtimePanicInterruptTimeout = 5 * time.Second
	// runtimeRegistryReconcileInterval 是将 Worker 写入的终局状态同步到本机 Runtime Registry 的最大延迟。
	runtimeRegistryReconcileInterval = 5 * time.Second
)

func main() {
	logger := slog.New(slog.NewJSONHandler(os.Stdout, nil))
	if err := run(os.Args[1:]); err != nil {
		logger.Error("Avalon server 退出", "error", err.Error())
		os.Exit(1)
	}
}

func run(args []string) error {
	flags := flag.NewFlagSet("avalon-server", flag.ContinueOnError)
	flags.SetOutput(os.Stderr)
	configPath := flags.String("config", config.DefaultServerPath, "avalon-server YAML 配置文件路径")
	if err := flags.Parse(args); err != nil {
		return err
	}
	if flags.NArg() != 0 {
		return errors.New("不接受位置参数")
	}
	cfg, err := config.LoadServer(*configPath)
	if err != nil {
		return err
	}
	logger, err := platformlogging.NewSlog(cfg.GetLog(), os.Stdout)
	if err != nil {
		return err
	}
	if warning := config.PermissionWarning(*configPath); warning != "" {
		logger.Warn(warning)
	}
	instanceID, err := os.Hostname()
	if err != nil || instanceID == "" {
		instanceID = "unknown"
	}
	logger = logger.With(
		"service.id", instanceID,
		"service.name", "avalon-server",
		"service.version", version,
	)
	kratosLogger := logger

	startupCtx, cancelStartup := context.WithTimeout(context.Background(), 15*time.Second)
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
	if err := pool.Persistence().ApplySchema(startupCtx, schemaMode); err != nil {
		return fmt.Errorf("应用 PostgreSQL Schema: %w", err)
	}
	identifierRuntime, err := snowflake.AcquireRuntime(startupCtx, database.NewSnowflakeLeaseStore(pool))
	if err != nil {
		return fmt.Errorf("获取雪花节点租约: %w", err)
	}
	defer identifierRuntime.Close()
	identifierLeaseMonitor, err := snowflake.NewLeaseMonitor(identifierRuntime)
	if err != nil {
		return err
	}
	schema := database.NewSchemaChecker(pool, 0)
	lease, err := pool.AcquireLease(startupCtx, database.ServerLeaseKey)
	if err != nil {
		return errors.New("无法取得 Avalon server 单实例租约")
	}
	defer func() {
		closeCtx, cancel := context.WithTimeout(context.Background(), 2*time.Second)
		defer cancel()
		_ = lease.Close(closeCtx)
	}()
	// 玩家进程只读取当前实时资料目录，不装配管理端对象存储与资料写入依赖。
	// 这使进程依赖集合与其公开 RPC 集合保持一致，避免管理基础设施扩大玩家入口的故障面。
	readiness := health.NewGate(pool, schema, lease, identifierRuntime)

	valkey := cfg.GetValkey()
	sessionBackend := platformsessionstore.New(platformsessionstore.Config{Address: valkey.GetAddress(), Username: valkey.GetUsername(), Password: valkey.GetPassword(), Database: int(valkey.GetDatabase()), Prefix: "avalon:session", Domain: "player"})
	if err := sessionBackend.Ready(startupCtx); err != nil {
		return fmt.Errorf("Valkey Session Store 未就绪: %w", err)
	}
	defer sessionBackend.Close()
	authenticationRepository := securitypersistence.NewAuthenticationRepository(pool, sessionBackend)
	// 会话凭证使用领域标识生成不可逆摘要，数据库不保存令牌明文。
	sessionTokens := session.NewTokenIssuer(session.TokenPurposeSession, rand.Reader)
	policy := authentication.SessionPolicy{
		AbsoluteTTL: time.Duration(cfg.GetSecurity().GetAbsoluteSessionSeconds()) * time.Second,
		IdleTTL:     time.Duration(cfg.GetSecurity().GetIdleSessionSeconds()) * time.Second,
	}
	loginService := authentication.NewService(
		authenticationRepository,
		account.NewPasswordHasher(rand.Reader),
		sessionTokens,
		policy,
		authentication.LoginProtectionPolicy{
			LockThreshold: 5,
			BaseLock:      time.Minute,
			MaximumLock:   15 * time.Minute,
		},
		identifierRuntime,
		time.Now,
	)
	logoutService := authentication.NewLogoutService(authenticationRepository, time.Now)
	sessionManager := authentication.NewSessionManager(authenticationRepository, authenticationRepository, identifierRuntime, time.Now)
	currentSessionQuery := authentication.NewIdentityQuery(authenticationRepository)
	refreshService := authentication.NewRefreshService(authenticationRepository, sessionTokens, policy.IdleTTL, identifierRuntime, time.Now)
	playerAccessTokens, err := adminauth.NewEphemeralAccessTokenIssuer(10*time.Minute, time.Now)
	if err != nil {
		return errors.New("无法初始化玩家 access token 签发器")
	}
	bearerSecurityService := securityapi.NewBearerService(
		loginService, logoutService, currentSessionQuery, sessionManager, playerAccessTokens,
	)
	bearerSecurityService.SetRefreshService(refreshService)
	accessCatalog, err := access.NewOperationCatalog(securityv1.File_avalon_security_v1_security_proto, domainv1.File_avalon_domain_v1_domain_proto, battlev1.File_avalon_battle_v1_battle_proto, rpgv1.File_avalon_rpg_v1_rpg_proto, systemv1.File_avalon_system_v1_system_proto)
	if err != nil {
		return fmt.Errorf("编译玩家 RPC 安全目录: %w", err)
	}
	gameDataStore := gamedatastore.New(pool, identifierRuntime)
	// Team 在创建、更新和进入对战前直接校验当前启用资料。资料变更通过停机维护完成，
	// 因此在线读取无需维护门禁或跨查询的全局修订重试。
	elementService := element.NewService(gameDataStore, identifierRuntime, time.Now)
	abilityService := ability.NewService(gameDataStore, identifierRuntime, time.Now)
	itemService := item.NewService(gameDataStore, identifierRuntime, time.Now)
	skillService := skill.NewService(gameDataStore, identifierRuntime, time.Now)
	statService := stat.NewService(gameDataStore, identifierRuntime, time.Now)
	natureService := nature.NewService(gamedatastore.NewNatureStore(gameDataStore), statService, identifierRuntime, time.Now)
	elementEffectivenessService := elementeffectiveness.NewService(gamedatastore.NewElementEffectivenessStore(gameDataStore), identifierRuntime, time.Now)
	creatureMetadataService := creaturemetadata.NewService(gameDataStore)
	teamReferenceCatalog := teamcatalog.NewReader(
		elementService, abilityService, itemService, skillService, statService, natureService, creatureMetadataService,
	)
	// 规则注册表由当前二进制显式构造；Battle 只接受能够编译为有限强类型快照的 Clause、Restriction 和
	// Mechanic，绝不在运行时加载脚本、反射处理器或未注册的资料定义。
	effectRegistry, err := effect.NewDefaultRegistry()
	if err != nil {
		return fmt.Errorf("创建战斗效果注册表: %w", err)
	}
	battleRuleService := battleformat.NewService(gameDataStore, effectRegistry, identifierRuntime, time.Now)
	battleRuleCompiler := battle.NewBattleFormatRuleCompiler(battleRuleService, teamReferenceCatalog, effectRegistry)
	playerCharacterStore := playercharacterstore.New(pool, identifierRuntime)
	presenceRegistry := playercharacter.NewPresenceRegistry(90 * time.Second)
	activeBindingHub := playercharacter.NewActiveBindingHub()
	playerCharacterLifecycle := playercharacter.NewServiceWithPresence(playerCharacterStore, presenceRegistry, identifierRuntime, time.Now)
	playerCharacterQuery := playercharacter.NewQueryService(playerCharacterStore, presenceRegistry, time.Now)
	activePlayerCharacter := playercharacter.NewActiveService(playerCharacterStore, presenceRegistry, activeBindingHub, time.Now)
	playerPresence := playercharacter.NewPresenceService(playerCharacterStore, presenceRegistry, time.Now)
	playerCharacterService := playercharacterapi.NewKratosService(
		playerCharacterLifecycle, playerCharacterQuery, activePlayerCharacter, playerPresence, logger,
	)
	// Battle 存储拥有对局、回合、历史和账号占用的唯一写入边界。Runtime Registry 只保存活跃对局的
	// 进程内串行执行器；服务重启后由恢复协调器从已提交快照重建。
	rpgWorldStore := rpg.NewEntWorldStore(pool, identifierRuntime)
	battleRepository := battlestore.New(pool, identifierRuntime, rpgWorldStore)
	runtimeRegistry := battle.NewRuntimeRegistryWithRuntimeLeases(defaultBattleRuntimeCapacity, func(_ context.Context, failure battle.RuntimePanic) {
		// Runtime 已从 Registry 移除，但数据库中的 running Battle、账号占用和资料活跃计数仍必须同事务
		// 清理。使用独立的短生命周期上下文，保证客户端请求取消不会跳过这一关键终态转换。
		interruptCtx, cancelInterrupt := context.WithTimeout(context.Background(), runtimePanicInterruptTimeout)
		defer cancelInterrupt()
		if _, interruptErr := battleRepository.InterruptRuntime(
			interruptCtx, failure.Lease, battle.TerminalReasonRuntimePanic, time.Now().UTC(),
		); interruptErr != nil {
			logger.Error("中断发生 panic 的 Battle Runtime 失败", "battleId", failure.BattleID, "error", interruptErr.Error())
		}
	}, battleRepository, instanceID)
	if runtimeRegistry == nil {
		return errors.New("对战 Runtime 容量配置无效")
	}
	// Asynq Worker 独立于 Server 进程，无法直接删除本机 Runtime。受控监控循环会以 Battle 持久化状态
	// 为权威源回收已被超时结算或中断的 Runtime，防止本机容量被终局对局长期占用。
	runtimeRegistryReconciler := battle.NewRuntimeRegistryReconciler(runtimeRegistry, battleRepository)
	battleRealtimeHub := battle.NewRealtimeHub(battleRepository, 8)
	defer battleRealtimeHub.Close()
	battleFactsReader := battle.NewGameDataInitialStateFactsReaderWithRules(
		battleRuleService,
		elementService,
		elementEffectivenessService,
		abilityService,
		skillService,
		skilldamageclass.NewService(gameDataStore, identifierRuntime, time.Now),
		skillailment.NewService(gameDataStore, identifierRuntime, time.Now),
		skilltarget.NewService(gameDataStore, identifierRuntime, time.Now),
		skillstatchange.NewService(gameDataStore, identifierRuntime, time.Now),
		statService,
		natureService,
		creatureMetadataService,
		gameDataStore,
		battleRuleCompiler,
	)
	battleStarter := battle.NewStartService(
		battleRepository,
		runtimeRegistry,
		battleFactsReader,
		newBattleRandomSource,
		battleRealtimeHub,
		time.Now,
	)
	// 到期 Preview 由独立 Worker 持久化为等待 Runtime 的 running Battle；只有持有租约的 Server
	// 可以编译并激活 Runtime。协调器覆盖 Worker 与同步 RPC 启动之间的短暂并发。
	pendingRuntimeReconciler := battle.NewPendingRuntimeReconciler(battleRepository, battleStarter)
	runtimeRecoveryReconciler := battle.NewRuntimeRecoveryReconciler(battleRepository, runtimeRegistry, battleStarter, instanceID, time.Now)
	teamStore := teamstore.New(pool, identifierRuntime)
	teamQuery := team.NewQueryService(teamStore)
	teamValidator := team.NewCatalogValidator(teamReferenceCatalog)
	// Team 保存和首次分享导入在同一数据库事务中完成实时资料校验与持久化。
	teamWriteGate := teamcatalog.NewAvailabilityGate(pool)
	teamLifecycle := team.NewService(
		teamStore, teamValidator, teamWriteGate, identifierRuntime, time.Now, pool,
	)
	teamService := teamapi.NewKratosService(
		teamLifecycle,
		teamQuery,
		team.NewShareService(teamStore, teamValidator, teamWriteGate, identifierRuntime, team.NewShareCode, time.Now, pool),
		logger,
	)
	challengeApplication := battle.NewChallengeApplicationServiceWithRules(
		battleRepository,
		playerCharacterQuery,
		playerCharacterQuery,
		team.NewAdmissionService(teamQuery, teamValidator),
		gameDataStore,
		battleRuleCompiler,
		identifierRuntime,
		time.Now,
	)
	trainingApplication := battle.NewTrainingApplicationServiceWithRules(
		battleRepository,
		playerCharacterQuery,
		team.NewAdmissionService(teamQuery, teamValidator),
		gameDataStore,
		battle.NewPersistentTrainingBotCatalog(battleRepository, identifierRuntime),
		battleRuleCompiler,
		identifierRuntime,
		time.Now,
	)
	battleService := battleapi.NewKratosService(
		battleRepository, runtimeRegistry, playerCharacterQuery, battleRealtimeHub, challengeApplication, trainingApplication, battleStarter, time.Now, logger,
	)
	rpgWorldService := rpgapi.NewPlayerService(rpg.NewWorldService(rpgWorldStore), time.Now)
	playerGRPCServer := server.NewPlayerGRPCServer(
		cfg.GetServer().GetGrpcAddress(), cfg.GetServer().GetConnectAddress(),
		systemapi.NewService(systemapi.BuildInfo{
			Version: version, Commit: commit, APIMajorVersion: "v1",
		}),
		bearerSecurityService,
		playerCharacterService,
		teamService,
		gameapi.NewPlayerCatalogService(battleRuleService, logger),
		battleService,
		battleService,
		battleService,
		rpgWorldService,
		kratosLogger,
		[]middleware.Middleware{securityapi.NewBearerSecurityMiddleware(accessCatalog, playerAccessTokens)},
	)
	application := appruntime.NewApplication(
		appruntime.ApplicationInfo{
			ID:          instanceID,
			Name:        "avalon-server",
			Version:     version,
			Metadata:    map[string]string{"commit": commit, "proto.version": "v1"},
			StopTimeout: time.Duration(cfg.GetLifecycle().GetShutdownTimeoutSeconds()) * time.Second,
		},
		kratosLogger,
		playerGRPCServer,
		identifierLeaseMonitor,
	)

	monitorCtx, cancelMonitor := context.WithCancel(context.Background())
	defer cancelMonitor()
	leaseErrCh := make(chan error, 1)
	var monitor sync.WaitGroup
	monitor.Add(1)
	go func() {
		defer monitor.Done()
		leaseTicker := time.NewTicker(time.Second)
		defer leaseTicker.Stop()
		runtimeTicker := time.NewTicker(runtimeRegistryReconcileInterval)
		defer runtimeTicker.Stop()
		runtimeLeaseTicker := time.NewTicker(battlestore.RuntimeLeaseRenewInterval)
		defer runtimeLeaseTicker.Stop()
		for {
			select {
			case <-monitorCtx.Done():
				return
			case <-leaseTicker.C:
				checkCtx, cancel := context.WithTimeout(monitorCtx, 2*time.Second)
				err := lease.Ready(checkCtx)
				cancel()
				if err != nil {
					readiness.BeginDrain()
					leaseErrCh <- errors.New("数据库单实例租约丢失")
					_ = application.Stop()
					return
				}
			case <-runtimeTicker.C:
				checkCtx, cancel := context.WithTimeout(monitorCtx, 2*time.Second)
				recovered, recoveryErr := runtimeRecoveryReconciler.RecoverDue(checkCtx)
				if recoveryErr != nil {
					logger.Error("恢复待处理 Battle Runtime 失败", "error", recoveryErr.Error())
				}
				for _, battleID := range recovered {
					battleRealtimeHub.Publish(checkCtx, battleID)
				}
				started, startErr := pendingRuntimeReconciler.StartPending(checkCtx)
				if startErr != nil {
					logger.Error("启动待处理 Battle 失败", "error", startErr.Error())
				}
				for _, battleID := range started {
					battleRealtimeHub.Publish(checkCtx, battleID)
				}
				expired, expireErr := runtimeRegistry.ExpireTurnDeadlines(checkCtx, time.Now().UTC())
				if expireErr != nil {
					logger.Error("结算到期 Battle 回合失败", "error", expireErr.Error())
				}
				for _, battleID := range expired {
					battleRealtimeHub.Publish(checkCtx, battleID)
				}
				removed, reconcileErr := runtimeRegistryReconciler.PruneTerminal(checkCtx)
				cancel()
				if reconcileErr != nil {
					logger.Error("同步终局 Battle Runtime 失败", "error", reconcileErr.Error())
					continue
				}
				if removed > 0 {
					logger.Info("已回收终局 Battle Runtime", "count", removed)
				}
			case <-runtimeLeaseTicker.C:
				checkCtx, cancel := context.WithTimeout(monitorCtx, 2*time.Second)
				if err := runtimeRegistry.RenewRuntimeLeases(checkCtx); err != nil {
					logger.Error("续期 Battle Runtime Lease 失败", "error", err.Error())
				}
				cancel()
			}
		}
	}()

	runErr := application.Run()
	readiness.BeginDrain()
	cancelMonitor()
	monitor.Wait()
	select {
	case leaseErr := <-leaseErrCh:
		if runErr == nil {
			runErr = leaseErr
		}
	default:
	}
	return runErr
}

// newBattleRandomSource 为每场新启动的 Battle 从操作系统安全随机源生成独立确定性种子。
//
// 随机源本身使用版本化的 SplitMix64 算法，实际每次消耗都会随 Turn Record 写入随机轨迹；安全随机
// 仅用于避免不同 Battle 复用可预测的起点，重放不依赖再次访问系统熵源。
func newBattleRandomSource() (battleengine.RandomSource, error) {
	var seedBytes [8]byte
	if _, err := rand.Read(seedBytes[:]); err != nil {
		return battleengine.RandomSource{}, fmt.Errorf("读取 Battle 随机种子: %w", err)
	}
	return battleengine.NewRandomSource(battleengine.RandomAlgorithmSplitMix64V1, binary.LittleEndian.Uint64(seedBytes[:]))
}
