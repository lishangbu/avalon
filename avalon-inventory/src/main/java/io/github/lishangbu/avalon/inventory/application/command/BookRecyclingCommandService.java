package io.github.lishangbu.avalon.inventory.application.command;

import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.inventory.application.model.BookRecyclingRequest;
import io.github.lishangbu.avalon.inventory.application.query.BookRecyclingQueryService;
import io.github.lishangbu.avalon.inventory.persistence.entity.BookRecyclingReceipt;
import io.github.lishangbu.avalon.inventory.persistence.mapper.InventoryStorageCodec;
import io.github.lishangbu.avalon.inventory.persistence.repository.BookRecyclingReceiptRepository;
import io.github.lishangbu.avalon.inventory.persistence.repository.InventoryRepository;
import io.github.lishangbu.avalon.platform.BusinessException;
import jakarta.transaction.Transactional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 锁内重算资格与报价，按稳定来源次序消费未绑定书，不删除奖励来源防重记录。 */
@Component
@RequiredArgsConstructor
public class BookRecyclingCommandService {

	private final CharacterCommandService characters;

	private final BookRecyclingQueryService queries;

	private final InventoryRepository inventory;

	private final BookRecyclingReceiptRepository receipts;

	private final InventoryStorageCodec codec;

	@Transactional
	public void sell(long owner, BookRecyclingRequest request) {
		if (request == null || request.expected() == null || request.quantity() <= 0)
			throw new BusinessException(400, "请确认回收书目及数量");
		try {
			if (!UUID.fromString(request.requestId()).toString().equals(request.requestId()))
				throw new IllegalArgumentException();
		}
		catch (IllegalArgumentException | NullPointerException error) {
			throw new BusinessException(400, "回收请求号无效");
		}
		long character = characters.lock(owner).id();
		String payload = codec.encode(request);
		var prior = receipts.prior(character, request.requestId());
		if (prior != null) {
			if (!prior.getPayload().equals(payload))
				throw new BusinessException(409, "请求号已用于其他回收操作");
			return;
		}
		characters.requireOutsideDungeon(owner);
		var offer = queries.view(owner)
			.offers()
			.stream()
			.filter(o -> o.code().equals(request.expected().code()))
			.findFirst()
			.orElseThrow(() -> new BusinessException(409, "请在补给商店回收可出售的重复技能书"));
		if (!offer.equals(request.expected()) || request.quantity() > offer.maximum())
			throw new BusinessException(409, "库存或报价已变化，请重新确认回收数量");
		int remaining = request.quantity();
		for (var item : inventory.matching(character, offer.code())) {
			if (item.isBound())
				continue;
			int used = Math.min(remaining, item.getQuantity());
			item.setQuantity(item.getQuantity() - used);
			remaining -= used;
			if (remaining == 0)
				break;
		}
		if (remaining != 0)
			throw new IllegalStateException("回收库存与报价不一致");
		characters.changeGold(owner, Math.multiplyExact((long) offer.unitPrice(), request.quantity()));
		var receipt = new BookRecyclingReceipt();

		receipt.setCharacterId(character);
		receipt.setRequestId(request.requestId());
		receipt.setPayload(payload);
		receipts.saveAndFlush(receipt);
	}

}
