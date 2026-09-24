package io.github.lishangbu.avalon.market.application.command;

import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.character.application.command.CharacterFactCommandService;
import io.github.lishangbu.avalon.character.application.query.CharacterFactQueryService;
import io.github.lishangbu.avalon.content.item.MarketCatalog;
import io.github.lishangbu.avalon.platform.BusinessException;
import org.springframework.stereotype.Component;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/** 交易教学和主动办理使用同一服务端门槛，卖方异步收款不调用此门槛。 */
@Component
@RequiredArgsConstructor
public class MarketAccess {

	private final CharacterCommandService characters;

	private final CharacterFactCommandService facts;

	private final CharacterFactQueryService queries;

	private final MarketCatalog catalog;

	@Transactional(Transactional.TxType.MANDATORY)
	public void require(long owner) {
		facility(owner);
		if (!queries.codes(owner).contains("MARKET:TUTORIAL"))
			throw new BusinessException(409, "请先完成交易教学");
	}

	@Transactional
	public void teach(long owner, boolean accepted) {
		if (!accepted)
			throw new BusinessException(400, "请确认交易税与托管退货规则");
		facility(owner);
		facts.record(owner, "MARKET:TUTORIAL");
	}

	private void facility(long owner) {
		var character = characters.requireOutsideDungeon(owner);
		if (character.level() < catalog.definition().playerLevel()
				|| !catalog.definition().locations().contains(character.location()))
			throw new BusinessException(409, "请达到开放等级后到城镇交易所办理");
	}

}
