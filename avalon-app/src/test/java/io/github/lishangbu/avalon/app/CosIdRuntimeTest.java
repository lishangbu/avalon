package io.github.lishangbu.avalon.app;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

import me.ahoo.cosid.ClockBackwardsException;
import me.ahoo.cosid.runtime.CosIdRuntime;
import me.ahoo.cosid.runtime.GeneratorSpec;
import me.ahoo.cosid.runtime.MachineSpec;
import me.ahoo.cosid.runtime.RuntimeOptions;
import me.ahoo.cosid.runtime.RuntimeSpec;
import me.ahoo.cosid.time.ClockRegressionPolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class CosIdRuntimeTest {

	@TempDir
	Path directory;

	@Test
	void smallClockRegressionBorrowsTimeButLargeRegressionFails() {
		var clock = new AtomicLong(Instant.parse("2026-11-01T00:00:00Z").toEpochMilli());
		var spec = RuntimeSpec.builder("regression-test")
			.machine(MachineSpec.Static.builder(3).build())
			.generator("default",
					GeneratorSpec.Snowflake.builder()
						.regression(new ClockRegressionPolicy(Duration.ofMillis(5), Duration.ofSeconds(1)))
						.build())
			.build();
		try (var runtime = CosIdRuntime.start(spec, RuntimeOptions.builder().time(clock::get).build())) {
			long first = runtime.generator("default").generate();
			clock.addAndGet(-3);
			assertTrue(runtime.generator("default").generate() > first);
			clock.addAndGet(-2_000);
			assertThrows(ClockBackwardsException.class, () -> runtime.generator("default").generate());
		}
	}

	@Test
	void persistedStaticSlotPreventsReuseAfterRestart() {
		var state = directory.resolve("machine.state");
		var spec = RuntimeSpec.builder("restart-test")
			.machine(MachineSpec.Static.builder(4).stateFile(state).ahead(Duration.ofMillis(50)).build())
			.generator("default", GeneratorSpec.Snowflake.builder().build())
			.build();
		long first;
		try (var runtime = CosIdRuntime.start(spec, RuntimeOptions.defaults())) {
			first = runtime.generator("default").generate();
			assertTrue(Files.exists(state));
		}
		try (var runtime = CosIdRuntime.start(spec, RuntimeOptions.defaults())) {
			assertTrue(runtime.generator("default").generate() > first);
		}
	}

}
