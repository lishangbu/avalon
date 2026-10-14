package io.github.lishangbu.avalon.battle.replay;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** 离线逐回合重放与链完整性核验；不连接正式状态或资产数据库，不为缺失记录重新生成输入。 */
public final class BattleReplayVerifier {

	private BattleReplayVerifier() {
	}

	/** 核验一份文件，返回完整回合数；活动记录允许核验已完成前缀，结束记录必须有合法END尾行。 */
	public static int verify(Path file) throws IOException {
		var lines = Files.readAllLines(file, StandardCharsets.UTF_8);
		if (lines.isEmpty())
			throw new IllegalArgumentException("战斗记录为空");
		var header = BattleReplayCodec.read(lines.getFirst(), BattleReplayHeader.class);
		if (!header.implementation().equals(BattleImplementation.current()))
			throw new IllegalArgumentException("战斗实现标识不匹配，请使用记录对应提交及JDK的重放工具");
		var engine = BattleReplayRules.assemble(header.rules());
		String previous = BattleReplayCodec.digest(lines.getFirst());
		int expected = header.initial().getRound();
		int rounds = 0;
		boolean ended = false;
		var expectedState = header.initial();
		for (int index = 1; index < lines.size(); index++) {
			if (ended)
				throw new IllegalArgumentException("终局之后存在额外战斗记录");
			String line = lines.get(index);
			if (BattleReplayCodec.kind(line).equals("STATE")) {
				var event = BattleReplayCodec.read(line, BattleReplayState.class);
				if (!event.previous().equals(previous)
						|| !event.hash().equals(BattleReplayCodec.digest(BattleReplayCodec.write(event.unhashed())))
						|| !BattleReplayCodec.write(expectedState).equals(BattleReplayCodec.write(event.before())))
					throw new IllegalArgumentException("外部状态变化缺失或摘要不匹配");
				expectedState = event.after();
				expected = expectedState.getRound();
				previous = event.hash();
				continue;
			}
			// 所有数据记录明确携带kind，不能凭解析失败将残缺回合降级为结束标记。
			if (BattleReplayCodec.kind(line).equals("END")) {
				var end = BattleReplayCodec.read(line, BattleReplayEnd.class);
				if (!end.previous().equals(previous))
					throw new IllegalArgumentException("战斗结束摘要不匹配");
				ended = true;
				continue;
			}
			var round = BattleReplayCodec.read(line, BattleReplayRound.class);
			if (round.round() != expected || round.before().getRound() != expected
					|| !round.previous().equals(previous))
				throw new IllegalArgumentException("战斗回合缺失、乱序或链摘要不匹配：" + expected);
			if (!BattleReplayCodec.write(round.before()).equals(BattleReplayCodec.write(expectedState)))
				throw new IllegalArgumentException("回合输入没有承接上一条状态记录");
			String hash = BattleReplayCodec.digest(BattleReplayCodec.write(round.unhashed()));
			if (!hash.equals(round.hash()))
				throw new IllegalArgumentException("战斗回合文本已被修改：" + expected);
			var input = round.result().input();
			var actual = engine.advance(round.before(), input.orders(), input.surrender(), input.resources(),
					input.replacements());
			if (!BattleReplayCodec.write(actual).equals(BattleReplayCodec.write(round.result())))
				throw new IllegalArgumentException("战斗重放状态、资源、随机抽样或事件不匹配：" + expected);
			previous = hash;
			expected = actual.state().getRound();
			expectedState = actual.state();
			rounds++;
		}
		if (file.getFileName().toString().endsWith(".ended.jsonl") && !ended)
			throw new IllegalArgumentException("已结束战斗记录缺少终局尾行");
		return rounds;
	}

}
