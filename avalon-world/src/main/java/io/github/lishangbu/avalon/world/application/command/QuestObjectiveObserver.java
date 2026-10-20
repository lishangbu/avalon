package io.github.lishangbu.avalon.world.application.command;

import io.github.lishangbu.avalon.character.application.model.CharacterActionRecorded;
import io.github.lishangbu.avalon.quest.model.QuestEvent;
import io.github.lishangbu.avalon.world.quest.QuestRuntimeService;
import io.github.lishangbu.avalon.world.quest.QuestClock;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** 真实行为仅进入实例引擎，资格、事件防重和数量全部由统一状态管理。 */
@Component
@RequiredArgsConstructor
public class QuestObjectiveObserver {

	private final QuestRuntimeService runtime;

	private final QuestClock clock;

	@EventListener
	@Transactional(Transactional.TxType.MANDATORY)
	public void recorded(CharacterActionRecorded event) {
		if (event.owner() > 0)
			// 事件可能由其他模块用系统时钟创建，而任务引擎使用可替换的 QuestClock；
			// 统一采用同一处理时刻，避免测试时钟或时钟精度差异制造“事件来自未来”。
			runtime.record(event.owner(), new QuestEvent(event.eventId(), event.code(), event.amount(), clock.now()));
	}

}
