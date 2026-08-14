// avalon-worker 是 Avalon 的独立 Asynq 后台任务进程入口。
package main

import (
	"context"
	"errors"
	"flag"
	"fmt"
	"log/slog"
	"os"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/hibiken/asynq"
	appruntime "github.com/lishangbu/avalon/internal/app/runtime"
	battle "github.com/lishangbu/avalon/internal/battle"
	battlepersistence "github.com/lishangbu/avalon/internal/battle/persistence"
	"github.com/lishangbu/avalon/internal/platform/audit"
	"github.com/lishangbu/avalon/internal/platform/config"
	"github.com/lishangbu/avalon/internal/platform/database"
	platformlogging "github.com/lishangbu/avalon/internal/platform/logging"
	"github.com/lishangbu/avalon/internal/rpg"
	"github.com/lishangbu/avalon/internal/verification"
	"github.com/lishangbu/avalon/internal/worker"
)

const (
	workerStartupTimeout = 15 * time.Second
)

var (
	// version 由发布构建注入；开发构建固定使用 dev。
	version = "dev"
	// commit 由发布构建注入，用于关联运行实例与源码修订。
	commit = "unknown"
)

func main() {
	logger := slog.New(slog.NewJSONHandler(os.Stdout, nil))
	if err := run(os.Args[1:]); err != nil {
		logger.Error("Avalon worker 退出", "error", err.Error())
		os.Exit(1)
	}
}

func run(args []string) error {
	flags := flag.NewFlagSet("avalon-worker", flag.ContinueOnError)
	flags.SetOutput(os.Stderr)
	configPath := flags.String("config", config.DefaultWorkerPath, "avalon-worker YAML 配置文件路径")
	if err := flags.Parse(args); err != nil {
		return err
	}
	if flags.NArg() != 0 {
		return errors.New("不接受位置参数")
	}
	cfg, err := config.LoadWorker(*configPath)
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
		"service.name", "avalon-worker",
		"service.version", version,
	)
	if warning := config.PermissionWarning(*configPath); warning != "" {
		logger.Warn(warning)
	}

	startupCtx, cancelStartup := context.WithTimeout(context.Background(), workerStartupTimeout)
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

	rpgWorldStore := rpg.NewEntWorldStore(pool, identifierRuntime)
	matchRepository := battlepersistence.NewAdapters(pool, identifierRuntime, rpgWorldStore)
	lifecycle := battle.NewLifecycleService(matchRepository, time.Now)
	analytics := worker.NewBattleAnalyticsWorker(matchRepository, time.Now)
	replayVerification := verification.NewBattleReplayService(matchRepository)
	auditVerification := verification.NewAuditHashService(audit.NewVerifier(pool))
	registry, err := worker.NewDefaultRegistry(
		worker.NewBattleLifecycleWorker(lifecycle),
		analytics,
		worker.NewBattleReplayVerificationWorker(replayVerification),
		worker.NewAuditHashVerificationWorker(auditVerification),
	)
	if err != nil {
		return fmt.Errorf("创建后台任务白名单: %w", err)
	}
	valkey := cfg.GetValkey()
	asynqServer, err := worker.NewAsynqServer(pool.Persistence(), registry, identifierRuntime, worker.AsynqServerConfig{
		Redis: asynq.RedisClientOpt{
			Addr: valkey.GetAddress(), Username: valkey.GetUsername(), Password: valkey.GetPassword(),
			DB: int(valkey.GetDatabase()),
		},
		Concurrency:     int(cfg.GetWorker().GetConcurrency()),
		ShutdownTimeout: time.Duration(cfg.GetLifecycle().GetShutdownTimeoutSeconds()) * time.Second,
		WorkerID:        instanceID,
	}, logger)
	if err != nil {
		return fmt.Errorf("创建 Asynq Worker: %w", err)
	}

	application := appruntime.NewApplication(
		appruntime.ApplicationInfo{
			ID:          instanceID,
			Name:        "avalon-worker",
			Version:     version,
			Metadata:    map[string]string{"commit": commit},
			StopTimeout: time.Duration(cfg.GetLifecycle().GetShutdownTimeoutSeconds()) * time.Second,
		},
		logger,
		asynqServer,
		identifierLeaseMonitor,
	)
	return application.Run()
}
