package io.github.lishangbu.avalon.creature.application.command;

import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.content.creature.EvolutionResearchCatalog;
import io.github.lishangbu.avalon.creature.application.query.EvolutionResearchQueryService;
import io.github.lishangbu.avalon.creature.persistence.entity.ResearchExchangeReceipt;
import io.github.lishangbu.avalon.creature.persistence.mapper.CreatureStorageCodec;
import io.github.lishangbu.avalon.creature.persistence.repository.ResearchExchangeReceiptRepository;
import io.github.lishangbu.avalon.inventory.application.command.CultivationMaterialService;
import io.github.lishangbu.avalon.inventory.application.command.CultivationMaterialService.Mode;
import io.github.lishangbu.avalon.inventory.application.command.InventoryCommandService;
import io.github.lishangbu.avalon.platform.BusinessException;
import org.springframework.stereotype.Component;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.UUID;

/** 扣凭证后校验发货容量；容量不足整个事务回滚，不扣费后转待领，也不重复兑换。 */
@Component
@RequiredArgsConstructor
public class ResearchExchangeCommandService {

	private final CharacterCommandService characters;

	private final EvolutionResearchQueryService research;

	private final EvolutionResearchCatalog catalog;

	private final CultivationMaterialService materials;

	private final InventoryCommandService inventory;

	private final ResearchExchangeReceiptRepository receipts;

	private final CreatureStorageCodec codec;

	@Transactional
	public void exchange(long owner, Request request) {
		if (request == null || request.quantity() <= 0 || !request.acceptBinding())
			throw new BusinessException(400, "请确认兑换数量及物品绑定状态");
		try {
			if (!UUID.fromString(request.requestId()).toString().equals(request.requestId()))
				throw new IllegalArgumentException();
		}
		catch (IllegalArgumentException | NullPointerException error) {
			throw new BusinessException(400, "兑换请求号无效");
		}
		var context = characters.lock(owner);
		var prior = receipts.prior(context.id(), request.requestId());
		if (prior != null) {
			if (!codec.decode(prior.getPayload(), Request.class).equals(request))
				throw new BusinessException(409, "请求号已用于其他兑换");
			return;
		}
		characters.requireOutsideDungeon(owner);
		var view = research.view(owner);
		if (!view.available() || !view.taught())
			throw new BusinessException(409, "请在研究所完成教学后兑换");
		var offer = catalog.offer(request.code());
		if (offer == null || offer.price() != request.unitPrice()
				|| view.offers().stream().noneMatch(available -> available.code().equals(request.code())))
			throw new BusinessException(409, "兑换项目或费用已变化，请重新确认");
		int cost;
		try {
			cost = Math.multiplyExact(offer.price(), request.quantity());
		}
		catch (ArithmeticException error) {
			throw new BusinessException(400, "兑换数量超出支持范围");
		}
		var requirement = Map.of(catalog.definition().token(), cost);
		var plan = materials.preview(owner, requirement, Mode.BOUND_FIRST);
		materials.consume(owner, requirement, Mode.BOUND_FIRST, plan);
		inventory.grantStrict(owner, "RESEARCH:EXCHANGE:" + request.requestId(), request.code(), request.quantity(),
				false, true);
		var receipt = new ResearchExchangeReceipt();

		receipt.setCharacterId(context.id());
		receipt.setRequestId(request.requestId());
		receipt.setPayload(codec.encode(request));
		receipts.saveAndFlush(receipt);
	}

	public record Request(String requestId, String code, int quantity, int unitPrice, boolean acceptBinding) {
	}

}
