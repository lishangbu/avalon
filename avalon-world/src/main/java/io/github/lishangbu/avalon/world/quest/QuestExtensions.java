package io.github.lishangbu.avalon.world.quest;

import io.github.lishangbu.avalon.quest.QuestDefinition;
import io.github.lishangbu.avalon.quest.model.QuestCondition;
import io.github.lishangbu.avalon.quest.model.QuestGrant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * 将扩展类型集中注册及校验。引擎执行无需switch，新条件或效果的实现和校验集中在同一个处理器。 重复类型或未知类型直接报错，避免配置拼写错误被默默忽略。
 */
@Component
public class QuestExtensions {

	private final Map<String, QuestEffectHandler> effects;

	private final Map<String, QuestConditionHandler> conditions;

	public QuestExtensions(List<QuestEffectHandler> effects, List<QuestConditionHandler> conditions) {
		this.effects = effects.stream()
			.collect(Collectors.toUnmodifiableMap(QuestEffectHandler::kind, Function.identity()));
		this.conditions = conditions.stream()
			.collect(Collectors.toUnmodifiableMap(QuestConditionHandler::kind, Function.identity()));
	}

	/** 游戏引用校验和核心结构校验互补，离线命令可启动同一套目录及处理器执行全部校验。 */
	public void validate(QuestDefinition definition) {
		for (var step : definition.steps()) {
			step.effects().forEach(effect -> effect(effect.kind()).validate(effect));
			step.conditions().forEach(condition -> condition(condition.kind()).validate(condition));
			step.activation().forEach(condition -> condition(condition.kind()).validate(condition));
		}
		definition.ringRewards().forEach(effect -> effect(effect.kind()).validate(effect));
	}

	public boolean test(long owner, QuestCondition condition) {
		return condition(condition.kind()).test(owner, condition);
	}

	public List<String> apply(long owner, QuestGrant grant) {
		return effect(grant.effect().kind()).apply(owner, grant);
	}

	private QuestEffectHandler effect(String kind) {
		var handler = effects.get(kind);
		if (handler == null)
			throw new IllegalArgumentException("未知任务效果类型：" + kind);
		return handler;
	}

	private QuestConditionHandler condition(String kind) {
		var handler = conditions.get(kind);
		if (handler == null)
			throw new IllegalArgumentException("未知任务条件类型：" + kind);
		return handler;
	}

}
