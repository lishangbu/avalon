package io.github.lishangbu.avalon.world.application.query;

import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.world.persistence.repository.QuestAcceptReceiptRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.time.Instant;

/** 对其他领域仅公开本角色首次接取时间，不暴露JPA实体或补给写入口。 */
@Component
@RequiredArgsConstructor
public class QuestAcceptQueryService {

	private final CharacterQueryService characters;

	private final QuestAcceptReceiptRepository receipts;

	@Transactional
	public Instant acceptedAt(long owner, String step) {
		var accepted = receipts.accepted(characters.requireId(owner), step);
		return accepted.isEmpty() ? null : accepted.getFirst().getAcceptedAt();
	}

}
