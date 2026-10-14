package io.github.lishangbu.avalon.battle.replay;

import java.util.Locale;
import java.util.List;
import java.util.Set;

import io.github.lishangbu.avalon.battle.domain.BattleRoundResult;
import io.github.lishangbu.avalon.battle.domain.BattleState;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.Duration;

/** JSONLines文件存储；只管理文本，不连接数据库。单实例文件操作串行，活动或残缺文件永不按年龄清理。 */
public final class BattleReplayFiles {

	/** 所有记录限定在持久化目录内，文件名只接收服务端模式与数字编号。 */
	private final Path directory;

	/** 明确目录，生产容器挂载到独立持久化卷。 */
	public BattleReplayFiles(Path directory) {
		this.directory = directory.toAbsolutePath().normalize();
	}

	/** 创建初始记录，重复编号拒绝覆盖，避免把已有历史误认为新战局。 */
	public synchronized void start(String mode, long battle, BattleState initial, Instant now) throws IOException {
		Files.createDirectories(directory);
		var header = new BattleReplayHeader("HEADER", Long.toString(battle), mode, now, BattleImplementation.current(),
				BattleReplayRules.capture(), initial);
		Files.writeString(active(mode, battle), BattleReplayCodec.write(header) + "\n", StandardCharsets.UTF_8,
				StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
	}

	/** 完整回合提交后追加，历史任一行损坏即拒绝，不伪造缺失历史。 */
	public synchronized void append(String mode, long battle, String request, BattleState before,
			BattleRoundResult result) throws IOException {
		var file = active(mode, battle);
		var tail = activeTail(file, mode, battle);
		if (before.getRound() != tail.state().getRound())
			throw new IOException("战斗日志缺失中间回合，拒绝续写");
		if (!BattleReplayCodec.write(before).equals(BattleReplayCodec.write(tail.state())))
			throw new IOException("战斗输入未承接已记录状态，拒绝续写");
		String previous = tail.hash();
		var raw = new BattleReplayRound("ROUND", before.getRound(), request, before, result, previous, "");
		var round = new BattleReplayRound("ROUND", raw.round(), request, before, result, previous,
				BattleReplayCodec.digest(BattleReplayCodec.write(raw)));
		Files.writeString(file, BattleReplayCodec.write(round) + "\n", StandardCharsets.UTF_8,
				StandardOpenOption.APPEND);
	}

	/** 显式追加宿主侧变化，前一状态必须与日志一致，不能凭当前数据库快照补造缺失历史。 */
	public synchronized void state(String mode, long battle, String reason, BattleState before, BattleState after)
			throws IOException {
		var file = active(mode, battle);
		var tail = activeTail(file, mode, battle);
		if (!BattleReplayCodec.write(tail.state()).equals(BattleReplayCodec.write(before)))
			throw new IOException("外部变化之前的状态与日志不一致");
		String previous = tail.hash();
		var raw = new BattleReplayState("STATE", reason, before, after, previous, "");
		var event = new BattleReplayState("STATE", reason, before, after, previous,
				BattleReplayCodec.digest(BattleReplayCodec.write(raw)));
		Files.writeString(file, BattleReplayCodec.write(event) + "\n", StandardCharsets.UTF_8,
				StandardOpenOption.APPEND);
	}

	/** 终局标记落盘后才重命名为结束文件，未完成重命名的文件保守保留。 */
	public synchronized void finish(String mode, long battle, String status, Instant now) throws IOException {
		var file = active(mode, battle);
		String previous = activeTail(file, mode, battle).hash();
		var end = new BattleReplayEnd("END", status, now, previous);
		Files.writeString(file, BattleReplayCodec.write(end) + "\n", StandardCharsets.UTF_8, StandardOpenOption.APPEND);
		Files.move(file, ended(mode, battle));
	}

	/** 仅按合法终局记录的结束时刻清理，不把文件修改时间当作战斗结束时刻。 */
	public synchronized int cleanup(Instant now, Duration retention) throws IOException {
		if (retention.isNegative() || retention.isZero())
			throw new IllegalArgumentException("战斗日志保留时间必须为正");
		if (!Files.isDirectory(directory))
			return 0;
		int removed = 0;
		try (var files = Files.list(directory)) {
			for (var file : files.filter(path -> path.getFileName().toString().endsWith(".ended.jsonl")).toList()) {
				var lines = Files.readAllLines(file, StandardCharsets.UTF_8);
				if (lines.isEmpty())
					continue;
				try {
					var end = BattleReplayCodec.read(lines.getLast(), BattleReplayEnd.class);
					validateEndedChain(lines, end);
					if (end.ended().isBefore(now.minus(retention))) {
						Files.delete(file);
						removed++;
					}
				}
				catch (RuntimeException exception) {
					// 残缺结束文件保守保留，不能由清理任务隐藏写入或内容损坏问题。
				}
			}
		}
		return removed;
	}

	/** 当前写入必须承接整条有效历史；结构校验失败转换为IO错误，交由宿主记录日志失败。 */
	private ChainTail activeTail(Path file, String mode, long battle) throws IOException {
		var lines = Files.readAllLines(file, StandardCharsets.UTF_8);
		try {
			var tail = validateChain(lines, lines.size());
			var header = BattleReplayCodec.read(lines.getFirst(), BattleReplayHeader.class);
			if (!header.implementation().equals(BattleImplementation.current()))
				throw new IllegalArgumentException("战斗日志实现标识已变化");
			if (!header.mode().equals(mode) || !header.battle().equals(Long.toString(battle)))
				throw new IllegalArgumentException("战斗日志头与文件编号不一致");
			return tail;
		}
		catch (RuntimeException exception) {
			throw new IOException("战斗历史已损坏，拒绝续写或结束", exception);
		}
	}

	/** 清理核对完整状态及摘要衔接，使用历史实现的文件也只校验结构，不执行旧规则。 */
	private void validateEndedChain(List<String> lines, BattleReplayEnd end) {
		if (lines.size() < 2)
			throw new IllegalArgumentException("结束记录缺少初始战局");
		var tail = validateChain(lines, lines.size() - 1);
		if (!end.previous().equals(tail.hash()))
			throw new IllegalArgumentException("结束记录的终局摘要已损坏");
	}

	/** 同一结构算法用于续写及清理；截止位置之前只允许头、外部状态和完整回合。 */
	private ChainTail validateChain(List<String> lines, int limit) {
		if (limit < 1)
			throw new IllegalArgumentException("战斗日志缺少初始记录");
		var header = BattleReplayCodec.read(lines.getFirst(), BattleReplayHeader.class);
		String previous = BattleReplayCodec.digest(lines.getFirst());
		BattleState expectedState = header.initial();
		for (int index = 1; index < limit; index++) {
			String line = lines.get(index);
			String kind = BattleReplayCodec.kind(line);
			if (kind.equals("STATE")) {
				var event = BattleReplayCodec.read(line, BattleReplayState.class);
				if (!event.previous().equals(previous)
						|| !event.hash().equals(BattleReplayCodec.digest(BattleReplayCodec.write(event.unhashed()))))
					throw new IllegalArgumentException("外部状态摘要链已损坏");
				if (!BattleReplayCodec.write(expectedState).equals(BattleReplayCodec.write(event.before())))
					throw new IllegalArgumentException("外部状态未承接上一条记录");
				previous = event.hash();
				expectedState = event.after();
			}
			else if (kind.equals("ROUND")) {
				var round = BattleReplayCodec.read(line, BattleReplayRound.class);
				if (round.round() != expectedState.getRound() || round.before().getRound() != round.round()
						|| !round.previous().equals(previous)
						|| !round.hash().equals(BattleReplayCodec.digest(BattleReplayCodec.write(round.unhashed()))))
					throw new IllegalArgumentException("战斗回合摘要链已损坏");
				if (!BattleReplayCodec.write(expectedState).equals(BattleReplayCodec.write(round.before())))
					throw new IllegalArgumentException("回合未承接上一条状态记录");
				previous = round.hash();
				expectedState = round.result().state();
			}
			else
				throw new IllegalArgumentException("战斗日志中间存在非法记录类型：" + kind);
		}
		return new ChainTail(previous, expectedState);
	}

	/** 校验所得尾部只在当前同步文件操作内使用，不缓存以免忽略外部文件改动。 */
	private record ChainTail(String hash, BattleState state) {
	}

	/** 管理工具显式导出原始文件，复制不改变活动标记及保留期限。 */
	public synchronized void export(String mode, long battle, Path destination) throws IOException {
		Path source = Files.exists(ended(mode, battle)) ? ended(mode, battle) : active(mode, battle);
		Files.copy(source, destination);
	}

	/** 活动文件无结束后缀，清理任务绝不读取或删除。 */
	private Path active(String mode, long battle) {
		return directory.resolve(name(mode, battle) + ".jsonl");
	}

	/** 只有写入终局标记的文件才有结束后缀。 */
	private Path ended(String mode, long battle) {
		return directory.resolve(name(mode, battle) + ".ended.jsonl");
	}

	/** 限制文件名空间，禁止客户端文本成为文件路径。 */
	private String name(String mode, long battle) {
		if (!Set.of("SINGLE", "DUNGEON").contains(mode) || battle <= 0)
			throw new IllegalArgumentException("战斗日志编号或模式无效");
		return mode.toLowerCase(Locale.ROOT) + "-" + battle;
	}

}
