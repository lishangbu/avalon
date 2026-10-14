package io.github.lishangbu.avalon.combat;

import java.util.Random;
import java.util.Map;

/** 与JDK Random算法相同的可追踪随机流。记录底层每次抽样，可覆盖shuffle及拒绝采样的额外抽取。 */
public final class CombatRandom extends Random {

	private final CombatFrame<?, ?> frame;

	private final String stream;

	private long draws;

	/** 每个具名流只创建一次，种子来自战局；重试重新从相同状态创建整回合即可。 */
	CombatRandom(long seed, String stream, CombatFrame<?, ?> frame) {
		super(seed);
		this.stream = stream;
		this.frame = frame;
	}

	/** 记录实际抽样位数及无符号结果，避免只记最终骰子而漏掉算法内部的抽取。 */
	@Override
	protected int next(int bits) {
		int value = super.next(bits);
		frame.event("RANDOM", Map.of("stream", stream, "draw", Long.toString(draws++), "bits", Integer.toString(bits),
				"value", Integer.toUnsignedString(value)));
		return value;
	}

}
