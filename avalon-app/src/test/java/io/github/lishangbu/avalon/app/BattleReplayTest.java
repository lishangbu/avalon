package io.github.lishangbu.avalon.app;

import java.io.IOException;
import java.util.HashMap;

import io.github.lishangbu.avalon.battle.domain.*;
import io.github.lishangbu.avalon.battle.replay.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** 正式规则离线重放、文本完整性及文件生命周期测试，不访问数据库。 */
class BattleReplayTest {

	/** 测试记录只能写入JUnit隔离目录。 */
	@TempDir
	Path directory;

	/** 同一文本快照离线重建规则后逐回合核对全部状态、资源、抽样和事件。 */
	@Test
	void completeReplayUsesRecordedRulesAndDetectsTampering() throws Exception {
		var fixture = new RuleTestFixture();
		var initial = fixture.training.createAcademy("BQ020", "MAGE", List.of(), 71L);
		var files = new BattleReplayFiles(directory);
		files.start("SINGLE", 1, initial, Instant.EPOCH);
		var first = round(fixture, initial);
		files.append("SINGLE", 1, "request-one", initial, first);
		var second = round(fixture, first.state());
		files.append("SINGLE", 1, "request-two", first.state(), second);
		Path active = directory.resolve("single-1.jsonl");
		assertEquals(2, BattleReplayVerifier.verify(active));
		var lines = Files.readAllLines(active);
		lines.set(1, lines.get(1).replace("request-one", "changed-request"));
		Path modified = directory.resolve("modified.jsonl");
		Files.write(modified, lines);
		assertThrows(IllegalArgumentException.class, () -> BattleReplayVerifier.verify(modified));
		Files.write(modified, List.of(lines.getFirst(), lines.get(2)));
		assertThrows(IllegalArgumentException.class, () -> BattleReplayVerifier.verify(modified));
		Files.writeString(modified, "");
		assertThrows(IllegalArgumentException.class, () -> BattleReplayVerifier.verify(modified));
		assertThrows(IOException.class, () -> BattleReplayVerifier.verify(directory.resolve("missing.jsonl")));
	}

	/** 实现不匹配或缺一份规则均拒绝；不会从当前正式目录补齐历史配置。 */
	@Test
	void explicitHostStateChangesAreRequiredBetweenRounds() throws Exception {
		var fixture = new RuleTestFixture();
		var initial = fixture.training.createAcademy("BQ020", "MAGE", List.of(), 71L);
		var files = new BattleReplayFiles(directory);
		files.start("SINGLE", 1, initial, Instant.EPOCH);
		var first = round(fixture, initial);
		files.append("SINGLE", 1, "first", initial, first);
		var changed = BattleReplayCodec.read(BattleReplayCodec.write(first.state()), BattleState.class);
		changed.getLog().add("宿主记录成员结算后的状态变化");
		files.state("SINGLE", 1, "HOST_SETTLEMENT", first.state(), changed);
		files.append("SINGLE", 1, "second", changed, round(fixture, changed));
		Path file = directory.resolve("single-1.jsonl");
		assertEquals(2, BattleReplayVerifier.verify(file));
		var lines = Files.readAllLines(file);
		lines.remove(2);
		Files.write(file, lines);
		assertThrows(IllegalArgumentException.class, () -> BattleReplayVerifier.verify(file));
	}

	/** 实现不匹配或缺一份规则均拒绝；不会从当前正式目录补齐历史配置。 */
	@Test
	void mismatchedImplementationAndMissingRulesAreRejected() throws Exception {
		var fixture = new RuleTestFixture();
		var initial = fixture.training.createAcademy("BQ020", "MAGE", List.of(), 71L);
		var content = BattleReplayRules.capture();
		var header = new BattleReplayHeader("HEADER", "1", "SINGLE", Instant.EPOCH, "old-implementation", content,
				initial);
		Path file = directory.resolve("different.jsonl");
		Files.writeString(file, BattleReplayCodec.write(header) + "\n");
		assertThrows(IllegalArgumentException.class, () -> BattleReplayVerifier.verify(file));
		var missing = new HashMap<>(content);
		missing.remove("training");
		assertThrows(IllegalArgumentException.class, () -> BattleReplayRules.assemble(missing));
		Path finished = directory.resolve("single-1.ended.jsonl");
		Files.writeString(finished, BattleReplayCodec.write(new BattleReplayHeader("HEADER", "1", "SINGLE",
				Instant.EPOCH, BattleImplementation.current(), content, initial)) + "\n");
		assertThrows(IllegalArgumentException.class, () -> BattleReplayVerifier.verify(finished));
	}

	/** 默认30天只删除明确结束的过期文件，活动文件及损坏文件保持原样；导出不修改原记录。 */
	@Test
	void cleanupKeepsActiveAndMalformedFilesAndExportsEndedRecords() throws Exception {
		var initial = new RuleTestFixture().training.createAcademy("BQ020", "MAGE", List.of(), 71L);
		var files = new BattleReplayFiles(directory);
		files.start("SINGLE", 1, initial, Instant.EPOCH);
		files.start("SINGLE", 2, initial, Instant.EPOCH);
		files.finish("SINGLE", 2, "SURRENDERED", Instant.EPOCH);
		Path exported = directory.resolve("export.jsonl");
		files.export("SINGLE", 2, exported);
		assertEquals(0, BattleReplayVerifier.verify(exported));
		Path malformed = directory.resolve("single-3.ended.jsonl");
		Files.writeString(malformed, "broken\n");
		assertEquals(1, files.cleanup(Instant.EPOCH.plus(Duration.ofDays(31)), Duration.ofDays(30)));
		assertTrue(Files.exists(directory.resolve("single-1.jsonl")));
		assertTrue(Files.exists(malformed));
		assertTrue(Files.exists(exported));
		assertFalse(Files.exists(directory.resolve("single-2.ended.jsonl")));
	}

	/** 即使终局行可解析，中间回合篡改或删除仍属于损坏文件，保留期限不能将其自动抹掉。 */
	@Test
	void cleanupPreservesBrokenMiddleWithValidEnd() throws Exception {
		var fixture = new RuleTestFixture();
		var initial = fixture.training.createAcademy("BQ020", "MAGE", List.of(), 71L);
		var files = new BattleReplayFiles(directory);
		files.start("SINGLE", 1, initial, Instant.EPOCH);
		files.append("SINGLE", 1, "original-request", initial, round(fixture, initial));
		files.finish("SINGLE", 1, "SURRENDERED", Instant.EPOCH);
		Path ended = directory.resolve("single-1.ended.jsonl");
		var lines = Files.readAllLines(ended);
		lines.set(1, lines.get(1).replace("original-request", "modified-request"));
		Files.write(ended, lines);
		assertEquals(0, files.cleanup(Instant.EPOCH.plus(Duration.ofDays(31)), Duration.ofDays(30)));
		assertTrue(Files.exists(ended));
		Files.write(ended, List.of(lines.getFirst(), lines.getLast()));
		assertEquals(0, files.cleanup(Instant.EPOCH.plus(Duration.ofDays(31)), Duration.ofDays(30)));
		assertTrue(Files.exists(ended));
	}

	/** 文件缺失及无法创建目录的失败明确暴露，不能自动从当前战局重建虚假初始记录。 */
	@Test
	void absentHeaderAndUnwritableDirectoryNeverProduceSyntheticHistory() throws Exception {
		var fixture = new RuleTestFixture();
		var initial = fixture.training.createAcademy("BQ020", "MAGE", List.of(), 71L);
		var result = round(fixture, initial);
		assertThrows(IOException.class,
				() -> new BattleReplayFiles(directory).append("SINGLE", 1, "request", initial, result));
		Path occupied = directory.resolve("occupied");
		Files.writeString(occupied, "ordinary-file");
		assertThrows(IOException.class,
				() -> new BattleReplayFiles(occupied).start("SINGLE", 1, initial, Instant.EPOCH));
		assertThrows(IllegalArgumentException.class,
				() -> new BattleReplayFiles(directory).start("../", 1, initial, Instant.EPOCH));
	}

	/** 全员防御保持训练活动，连续两轮用于验证随机流和回合号链。 */
	private BattleRoundResult round(RuleTestFixture fixture, BattleState state) {
		var orders = state.getUnits()
			.stream()
			.filter(unit -> !unit.isEnemy() && unit.alive() && unit.active())
			.map(unit -> new BattleStateOrder(unit.getId(), "DEFEND", unit.getId()))
			.toList();
		return fixture.engine.advance(state, orders, false, new BattleResourceSnapshot(Map.of(), Set.of()), Map.of());
	}

}
