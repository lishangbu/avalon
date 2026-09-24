package io.github.lishangbu.avalon.app;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** 验证实际内存调度的异常恢复、固定周期和生命周期边界，不依赖任务表。 */
class MaintenanceSchedulingTest {

	@Test
	void registersFixedTasksAfterApplicationStartup() throws Exception {
		var scheduler = mock(ThreadPoolTaskScheduler.class);
		var sessions = mock(SessionMaintenance.class);
		var market = mock(MarketMaintenance.class);
		var dungeon = mock(DungeonRoundMaintenance.class);
		var startup = new MaintenanceScheduling().maintenanceStartup(scheduler, sessions, market, dungeon);
		verifyNoInteractions(scheduler);
		startup.run(null);
		verify(scheduler).scheduleWithFixedDelay(any(Runnable.class), eq(Duration.ofHours(1)));
		verify(scheduler).scheduleWithFixedDelay(any(Runnable.class), eq(Duration.ofSeconds(10)));
		verify(scheduler).scheduleWithFixedDelay(any(Runnable.class), eq(Duration.ofSeconds(1)));
		verify(scheduler).scheduleWithFixedDelay(any(Runnable.class), eq(Duration.ofSeconds(3)));
		verifyNoMoreInteractions(scheduler);
	}

	@Test
	void failureDoesNotCancelFutureScansAndShutdownStopsTheScheduler() throws Exception {
		var configuration = new MaintenanceScheduling();
		var scheduler = configuration.maintenanceScheduler();
		var dungeon = mock(DungeonRoundMaintenance.class);
		var attempts = new AtomicInteger();
		var recovered = new CountDownLatch(1);
		doAnswer(invocation -> {
			if (attempts.incrementAndGet() == 1)
				throw new IllegalStateException("模拟一次扫描失败");
			recovered.countDown();
			return null;
		}).when(dungeon).expire();
		scheduler.initialize();
		try {
			configuration
				.maintenanceStartup(scheduler, mock(SessionMaintenance.class), mock(MarketMaintenance.class), dungeon)
				.run(null);
			assertTrue(recovered.await(5, TimeUnit.SECONDS), "扫描异常后应继续重试");
		}
		finally {
			scheduler.destroy();
		}
		assertTrue(scheduler.getScheduledThreadPoolExecutor().isTerminated());
	}

}
