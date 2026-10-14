package io.github.lishangbu.avalon.combat;

import io.github.lishangbu.avalon.combat.model.CombatEvent;
import io.github.lishangbu.avalon.combat.model.CombatLimits;
import io.github.lishangbu.avalon.combat.model.CombatPhase;
import java.util.*;

/** 单回合隔离上下文。阶段只能由引擎推进；所有扩展事件与随机抽样共用有限预算。 */
public final class CombatFrame<S, I> {

	private final S state;

	private final I input;

	private final CombatLimits limits;

	private final List<CombatEvent> events = new ArrayList<>();

	private final Map<String, CombatRandom> randoms = new HashMap<>();

	private final Map<String, Long> seeds = new HashMap<>();

	private CombatPhase phase;

	private int depth;

	/** 只允许核心创建，防止处理器自行重置事件计数或构造另一套阶段上下文。 */
	CombatFrame(S state, I input, CombatLimits limits) {
		this.state = state;
		this.input = input;
		this.limits = limits;
	}

	/** 供纯规则读取或修改本回合副本，禁止引用宿主的可变实体。 */
	public S state() {
		return state;
	}

	/** 指令、资源和规则快照等不可变外部输入由宿主在执行前收集。 */
	public I input() {
		return input;
	}

	/** 当前阶段只读；处理器不能自行改变阶段来绕过顺序约束。 */
	public CombatPhase phase() {
		return phase;
	}

	/** 核心按枚举顺序逐个调用；重复或倒退推进立即失败。 */
	void phase(CombatPhase next) {
		if (phase != null && next.ordinal() != phase.ordinal() + 1)
			throw new IllegalStateException("战斗阶段不能跳跃或回退");
		if (phase == null && next != CombatPhase.OPEN)
			throw new IllegalStateException("战斗必须从开放阶段开始");
		phase = next;
	}

	/** 记录机器事件；预算也包含随机抽样和阶段标记，因此循环不能通过隐藏事件绕过保护。 */
	public void event(String kind, Map<String, String> values) {
		if (events.size() >= limits.maximumEvents())
			throw new IllegalStateException("战斗事件超过回合预算");
		events.add(new CombatEvent(events.size(), Objects.requireNonNull(phase), kind, values));
	}

	/** 嵌套效果通过此入口执行；finally保证失败后深度正确复原，但本回合结果仍须整体丢弃。 */
	public void trigger(String kind, Runnable action) {
		if (depth >= limits.maximumDepth())
			throw new IllegalStateException("战斗触发超过最大深度");
		depth++;
		try {
			event("TRIGGER", Map.of("kind", kind, "depth", Integer.toString(depth)));
			action.run();
		}
		finally {
			depth--;
		}
	}

	/** 具名随机流保持现有派生种子算法，同一流中途改变种子视为处理器错误。 */
	public CombatRandom random(String stream, long seed) {
		if (stream == null || stream.isBlank())
			throw new IllegalArgumentException("随机流名称不能为空");
		Long previous = seeds.putIfAbsent(stream, seed);
		if (previous != null && previous != seed)
			throw new IllegalStateException("同一随机流不能更换种子");
		return randoms.computeIfAbsent(stream, key -> new CombatRandom(seed, key, this));
	}

	/** 对外返回不可变事件集合，后续规则不能改写已发生的抽样和效果。 */
	List<CombatEvent> events() {
		return List.copyOf(events);
	}

}
