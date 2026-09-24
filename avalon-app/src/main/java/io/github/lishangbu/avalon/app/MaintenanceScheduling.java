package io.github.lishangbu.avalon.app;

import java.time.Duration;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * 单实例周期扫描：内存只保存唤醒计划，截止时间、事务及幂等由业务数据库负责。 启动完成后才注册任务，AOT 训练的 onRefresh 退出不会扫描业务数据。
 */
@Configuration(proxyBeanMethods = false)
public class MaintenanceScheduling {

	/** 显式使用平台线程池，避免虚拟线程调度器将所有 fixedDelay 任务串在同一线程上。 */
	@Bean
	ThreadPoolTaskScheduler maintenanceScheduler() {
		var scheduler = new ThreadPoolTaskScheduler();
		scheduler.setPoolSize(4);
		scheduler.setThreadNamePrefix("avalon-maintenance-");
		scheduler.setWaitForTasksToCompleteOnShutdown(true);
		scheduler.setAwaitTerminationSeconds(30);
		scheduler.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
		scheduler.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
		return scheduler;
	}

	/** 固定维护周期，无运行配置开关；启动后立即扫描一次，再按完成后的间隔持续执行。 */
	@Bean
	ApplicationRunner maintenanceStartup(ThreadPoolTaskScheduler maintenanceScheduler, SessionMaintenance sessions,
			MarketMaintenance market, DungeonRoundMaintenance dungeon) {
		return arguments -> {
			maintenanceScheduler.scheduleWithFixedDelay(sessions::clean, Duration.ofHours(1));
			maintenanceScheduler.scheduleWithFixedDelay(market::expire, Duration.ofSeconds(10));
			maintenanceScheduler.scheduleWithFixedDelay(dungeon::expire, Duration.ofSeconds(1));
			maintenanceScheduler.scheduleWithFixedDelay(dungeon::expireLobbies, Duration.ofSeconds(3));
		};
	}

}
