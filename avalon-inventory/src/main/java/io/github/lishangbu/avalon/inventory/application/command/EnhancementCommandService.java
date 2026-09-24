package io.github.lishangbu.avalon.inventory.application.command;

import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.inventory.application.model.EquipmentChanged;
import io.github.lishangbu.avalon.inventory.application.model.InventoryItemView;
import io.github.lishangbu.avalon.inventory.application.command.EnhancementPreviewService;
import io.github.lishangbu.avalon.inventory.application.command.EnhancementPreviewService.Preview;
import io.github.lishangbu.avalon.inventory.application.query.InventoryQueryService;
import io.github.lishangbu.avalon.inventory.domain.EnhancementRoll;
import io.github.lishangbu.avalon.inventory.domain.EnhancementRules;
import io.github.lishangbu.avalon.inventory.persistence.entity.EnhancementReceipt;
import io.github.lishangbu.avalon.inventory.persistence.mapper.InventoryStorageCodec;
import io.github.lishangbu.avalon.inventory.persistence.repository.EnhancementReceiptRepository;
import io.github.lishangbu.avalon.inventory.persistence.repository.InventoryRepository;
import io.github.lishangbu.avalon.platform.BusinessException;
import org.springframework.stereotype.Component;
import org.springframework.context.ApplicationEventPublisher;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import java.util.Map;
import java.util.UUID;

/** 一次强化是一笔事务：扣费、扣料、绑定、掉级、资源同步和回执要么全部提交，要么全部回滚。 */
@Component
@RequiredArgsConstructor
public class EnhancementCommandService {

	private final CharacterCommandService characters;

	private final EnhancementPreviewService previews;

	private final InventoryQueryService queries;

	private final InventoryRepository inventory;

	private final InventoryStorageCodec codec;

	private final EnhancementReceiptRepository receipts;

	private final CultivationMaterialService materials;

	private final EnhancementRoll random;

	private final ApplicationEventPublisher changed;

	@Transactional
	public Result enhance(long owner, long id, Request request) {
		if (request == null || request.expected() == null || request.expected().mode() == null)
			throw new BusinessException(400, "缺少强化确认信息");
		try {
			if (!UUID.fromString(request.requestId()).toString().equals(request.requestId()))
				throw new IllegalArgumentException();
		}
		catch (IllegalArgumentException | NullPointerException error) {
			throw new BusinessException(400, "强化请求号无效");
		}
		var context = characters.lock(owner);
		var payload = new Payload(id, request.expected());
		var prior = receipts.prior(context.id(), request.requestId());
		if (prior != null) {
			if (!codec.decode(prior.getPayload(), Payload.class).equals(payload))
				throw new BusinessException(409, "请求号已用于其他强化操作");
			return codec.decode(prior.getResult(), Result.class);
		}
		var actual = previews.preview(owner, id, request.expected().mode());
		if (!actual.equals(request.expected()))
			throw new BusinessException(409, "强化信息已变化，请重新确认");
		var item = inventory.get(id);
		var previousStats = item.isEquipped() ? queries.loadout(owner).stats() : Map.<String, Integer>of();
		characters.changeGold(owner, -actual.cost().gold());
		materials.consume(owner, Map.of(actual.cost().stone(), actual.cost().quantity()), actual.mode(),
				actual.materials());
		int resultLevel = EnhancementRules.outcome(actual.cost(), random.next());
		item.setEnhancementLevel(resultLevel);
		item.setBound(actual.resultBound());
		if (item.isEquipped())
			changed.publishEvent(new EquipmentChanged(owner, previousStats));
		characters.invalidatePreparation(owner);
		var result = new Result(resultLevel == actual.cost().target(), codec.view(item));
		var receipt = new EnhancementReceipt();

		receipt.setCharacterId(context.id());
		receipt.setRequestId(request.requestId());
		receipt.setPayload(codec.encode(payload));
		receipt.setResult(codec.encode(result));
		receipts.saveAndFlush(receipt);
		return result;
	}

	public record Request(String requestId, Preview expected) {
	}

	public record Payload(long itemId, Preview expected) {
	}

	public record Result(boolean success, InventoryItemView item) {
	}

}
