package io.github.lishangbu.avalon.character.application.command;

import io.github.lishangbu.avalon.character.persistence.entity.CharacterFact;
import io.github.lishangbu.avalon.character.application.model.CharacterActionRecorded;
import org.springframework.context.ApplicationEventPublisher;
import io.github.lishangbu.avalon.character.persistence.repository.CharacterFactRepository;
import org.springframework.stereotype.Component;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import java.time.Instant;

/**
 * 事实记录的内部入口，不提供 HTTP 写接口；事实与真实行为必须共同提交或共同回滚。
 */
@Component
@RequiredArgsConstructor
public class CharacterFactCommandService {

	private final CharacterCommandService characters;

	private final CharacterFactRepository facts;

	private final ApplicationEventPublisher events;

	/**
	 * 同一事实只记首次发生。MANDATORY 防止调用者脱离行为事务单独保存资格。
	 */
	@Transactional(Transactional.TxType.MANDATORY)
	public void record(long owner, String code) {
		if (code == null || code.isBlank() || code.length() > 255)
			throw new IllegalArgumentException("事实编号无效");
		long characterId = characters.lock(owner).id();
		if (!facts.contains(characterId, code)) {
			CharacterFact fact = new CharacterFact();
			fact.setCharacterId(characterId);
			fact.setCode(code);
			fact.setCreatedAt(Instant.now());
			facts.save(fact);
		}
		// 曾经击败不等于本次任务有效；即使永久事实已存在，也让进行中的任务核验这次行为。
		events.publishEvent(new CharacterActionRecorded(characterId, code));
	}

}
