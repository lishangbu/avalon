package io.github.lishangbu.avalon.chat.application.command;

import io.github.lishangbu.avalon.character.application.model.CharacterBattleChanged;
import io.github.lishangbu.avalon.character.application.model.PlayerNotice;
import io.github.lishangbu.avalon.chat.application.model.ChatChanged;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 同步领域事件适配：奖励/任务不依赖聊天模块，监听器把可信通知加入原业务事务，禁止独立提交。 */
@Component
@RequiredArgsConstructor
public class PlayerNoticeObserver {

	private final ChatCommandService chat;

	private final ApplicationEventPublisher events;

	@EventListener
	@Transactional(propagation = Propagation.MANDATORY)
	public void notice(PlayerNotice event) {
		chat.notify(event.owner(), event.source(), event.text());
	}

	/** 与开战/结束同事务登记实时唤醒，数据库回滚则不会误隐藏或打开另一标签页的聊天。 */
	@EventListener
	@Transactional(propagation = Propagation.MANDATORY)
	public void battle(CharacterBattleChanged event) {
		events.publishEvent(new ChatChanged(event.characterId(), event.characterId()));
	}

}
