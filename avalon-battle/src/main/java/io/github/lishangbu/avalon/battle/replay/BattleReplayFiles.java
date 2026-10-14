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

	/** 完整回合提交后追加，缺失头或尾部损坏立即拒绝，不伪造缺失历史。 */
	public synchronized void append(String mode, long battle, String request, BattleState before,
			BattleRoundResult result) throws IOException {
		var file = active(mode, battle);
		var lines = Files.readAllLines(file, StandardCharsets.UTF_8);
		if (lines.isEmpty())
			throw new IOException("战斗日志缺少初始记录");
		var header = BattleReplayCodec.read(lines.getFirst(), BattleReplayHeader.class);
		if (!header.implementation().equals(BattleImplementation.current()))
			throw new IOException("战斗日志实现标识已变化");
		String previous = BattleReplayCodec.digest(lines.getFirst());
		int expected = header.initial().getRound();
		BattleState expectedState = header.initial();
		if (lines.size() > 1) {
			if (lines.getLast().contains("\"kind\":\"STATE\"")) {
				var last = BattleReplayCodec.read(lines.getLast(), BattleReplayState.class);
				if (!last.hash().equals(BattleReplayCodec.digest(BattleReplayCodec.write(last.unhashed()))))
					throw new IOException("战斗外部状态摘要错误");
				previous = last.hash();
				expected = last.after().getRound();
				expectedState = last.after();
			}
			else {
				var last = BattleReplayCodec.read(lines.getLast(), BattleReplayRound.class);
				if (!last.hash().equals(BattleReplayCodec.digest(BattleReplayCodec.write(last.unhashed()))))
					throw new IOException("战斗日志末轮摘要错误");
				previous = last.hash();
				expected = last.result().state().getRound();
				expectedState = last.result().state();
			}
		}
		if (before.getRound() != expected)
			throw new IOException("战斗日志缺失中间回合，拒绝续写");
		if (!BattleReplayCodec.write(before).equals(BattleReplayCodec.write(expectedState)))
			throw new IOException("战斗输入未承接已记录状态，拒绝续写");
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
		var lines = Files.readAllLines(file, StandardCharsets.UTF_8);
		if (lines.isEmpty())
			throw new IOException("战斗日志缺少初始记录");
		String previous = BattleReplayCodec.digest(lines.getFirst());
		BattleState recorded = BattleReplayCodec.read(lines.getFirst(), BattleReplayHeader.class).initial();
		if (lines.size() > 1) {
			if (lines.getLast().contains("\"kind\":\"STATE\"")) {
				var last = BattleReplayCodec.read(lines.getLast(), BattleReplayState.class);
				previous = last.hash();
				recorded = last.after();
			}
			else {
				var last = BattleReplayCodec.read(lines.getLast(), BattleReplayRound.class);
				previous = last.hash();
				recorded = last.result().state();
			}
		}
		if (!BattleReplayCodec.write(recorded).equals(BattleReplayCodec.write(before)))
			throw new IOException("外部变化之前的状态与日志不一致");
		var raw = new BattleReplayState("STATE", reason, before, after, previous, "");
		var event = new BattleReplayState("STATE", reason, before, after, previous,
				BattleReplayCodec.digest(BattleReplayCodec.write(raw)));
		Files.writeString(file, BattleReplayCodec.write(event) + "\n", StandardCharsets.UTF_8,
				StandardOpenOption.APPEND);
	}

	/** 终局标记落盘后才重命名为结束文件，未完成重命名的文件保守保留。 */
	public synchronized void finish(String mode, long battle, String status, Instant now) throws IOException {
		var file = active(mode, battle);
		var lines = Files.readAllLines(file, StandardCharsets.UTF_8);
		if (lines.isEmpty())
			throw new IOException("战斗日志缺少初始记录");
		String previous = lines.size() == 1 ? BattleReplayCodec.digest(lines.getFirst())
				: lines.getLast().contains("\"kind\":\"STATE\"")
						? BattleReplayCodec.read(lines.getLast(), BattleReplayState.class).hash()
						: BattleReplayCodec.read(lines.getLast(), BattleReplayRound.class).hash();
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

	/** 清理只验证文本结构与整条摘要链，不执行旧规则；损坏中间行的历史文件必须保留供排查。 */
	private void validateEndedChain(List<String> lines, BattleReplayEnd end) {
		if (lines.size() < 2)
			throw new IllegalArgumentException("结束记录缺少初始战局");
		var header = BattleReplayCodec.read(lines.getFirst(), BattleReplayHeader.class);
		String previous = BattleReplayCodec.digest(lines.getFirst());
		int expected = header.initial().getRound();
		for (int index = 1; index < lines.size() - 1; index++) {
			if (lines.get(index).contains("\"kind\":\"STATE\"")) {
				var event = BattleReplayCodec.read(lines.get(index), BattleReplayState.class);
				if (!event.previous().equals(previous)
						|| !event.hash().equals(BattleReplayCodec.digest(BattleReplayCodec.write(event.unhashed()))))
					throw new IllegalArgumentException("外部状态摘要链已损坏");
				previous = event.hash();
				expected = event.after().getRound();
				continue;
			}
			var round = BattleReplayCodec.read(lines.get(index), BattleReplayRound.class);
			if (round.round() != expected || round.before().getRound() != expected || !round.previous().equals(previous)
					|| !round.hash().equals(BattleReplayCodec.digest(BattleReplayCodec.write(round.unhashed()))))
				throw new IllegalArgumentException("结束记录的回合摘要链已损坏");
			previous = round.hash();
			expected = round.result().state().getRound();
		}
		if (!end.previous().equals(previous))
			throw new IllegalArgumentException("结束记录的终局摘要已损坏");
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
