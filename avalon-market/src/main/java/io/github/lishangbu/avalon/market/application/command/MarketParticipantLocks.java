package io.github.lishangbu.avalon.market.application.command;

import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.character.application.model.CharacterContext;
import org.springframework.stereotype.Component;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * 市场命令统一先按账号升序取得角色锁，再取得挂单锁，禁止先锁买方再补锁卖方。 上架、取消、到期转待领及领取也须遵循角色→挂单顺序；查询不取得这些写锁。
 */
@Component
@RequiredArgsConstructor
public class MarketParticipantLocks {

	private final CharacterCommandService characters;

	@Transactional(Transactional.TxType.MANDATORY)
	public Participants acquire(long seller, long buyer) {
		if (seller == buyer)
			throw new IllegalArgumentException("成交参与者不能相同");
		var first = characters.lock(Math.min(seller, buyer));
		var second = characters.lock(Math.max(seller, buyer));
		return seller < buyer ? new Participants(first, second) : new Participants(second, first);
	}

	public record Participants(CharacterContext seller, CharacterContext buyer) {
	}

}
