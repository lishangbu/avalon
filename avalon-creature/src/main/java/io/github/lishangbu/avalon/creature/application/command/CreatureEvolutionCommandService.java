package io.github.lishangbu.avalon.creature.application.command;

import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.content.creature.EvolutionCatalog;
import io.github.lishangbu.avalon.content.creature.AbilitySlotsCatalog;
import io.github.lishangbu.avalon.creature.application.model.CreatureFormChanged;
import io.github.lishangbu.avalon.creature.application.command.CreatureEvolutionPreviewService;
import io.github.lishangbu.avalon.creature.application.command.CreatureEvolutionPreviewService.Preview;
import io.github.lishangbu.avalon.creature.persistence.entity.CreatureEvolutionReceipt;
import io.github.lishangbu.avalon.creature.persistence.mapper.CreatureStorageCodec;
import io.github.lishangbu.avalon.creature.persistence.repository.CreatureDiscoveryRepository;
import io.github.lishangbu.avalon.creature.persistence.repository.CreatureEvolutionReceiptRepository;
import io.github.lishangbu.avalon.creature.persistence.repository.CreatureRepository;
import io.github.lishangbu.avalon.inventory.application.command.CultivationMaterialService;
import io.github.lishangbu.avalon.platform.BusinessException;
import org.springframework.stereotype.Component;
import org.springframework.context.ApplicationEventPublisher;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

/** 进化提交只变更原个体形态；材料、绑定、资源、发现奖励和操作凭证共用一个事务。 */
@Component
@RequiredArgsConstructor
public class CreatureEvolutionCommandService {

	private final CharacterCommandService characters;

	private final CreatureEvolutionPreviewService previews;

	private final CreatureEvolutionReceiptRepository receipts;

	private final CreatureRepository pets;

	private final CreatureDiscoveryRepository discoveries;

	private final CreatureStorageCodec codec;

	private final EvolutionCatalog catalog;

	private final AbilitySlotsCatalog abilities;

	private final CultivationMaterialService materials;

	private final CollectionCommandService collection;

	private final ApplicationEventPublisher changed;

	@Transactional
	public Result evolve(long owner, long id, Request request) {
		if (request == null || request.expected() == null || request.expected().mode() == null)
			throw new BusinessException(400, "缺少进化确认信息");
		try {
			if (!UUID.fromString(request.requestId()).toString().equals(request.requestId()))
				throw new IllegalArgumentException();
		}
		catch (IllegalArgumentException | NullPointerException error) {
			throw new BusinessException(400, "进化请求号无效");
		}
		var context = characters.lock(owner);
		var payload = new Payload(id, request.expected());
		var prior = receipts.prior(context.id(), request.requestId());
		if (prior != null) {
			// 解码后按值比较，JSON 对象键顺序改变不应把同一次请求误认为新内容。
			if (!codec.decode(prior.getPayload(), Payload.class).equals(payload))
				throw new BusinessException(409, "请求号已用于其他进化操作");
			return codec.decode(prior.getResult(), Result.class);
		}
		var actual = previews.preview(owner, id, request.expected().mode());
		if (!actual.equals(request.expected()))
			throw new BusinessException(409, "进化信息已变化，请重新确认");
		var pet = pets.get(id);
		var route = catalog.route(pet.getSpecies());
		discoveries.remember(pet);
		materials.consume(owner, route.materials(), actual.mode(), actual.materials());
		String previous = pet.getSpecies();
		pet.setSpecies(route.target());
		// 继承的是槽位；目标形态该槽位可配置不同效果，不能携带源形态名称直接过关。
		pet.setAbility(abilities.ability(route.target(), pet.getAbilitySlot()));
		pet.setName(actual.name());
		pet.setBound(actual.resultBound());
		// 不重置等级、经验、技能、A/B/H 槽位、原捕获者、昵称次数或锁定状态。
		changed.publishEvent(new CreatureFormChanged(context.id(), id, pet.getLevel(), previous, pet.getSpecies()));
		collection.settle(owner);
		characters.invalidatePreparation(owner);
		var result = new Result(Long.toString(id), pet.getSpecies(), pet.getName(), pet.isBound());
		var receipt = new CreatureEvolutionReceipt();

		receipt.setCharacterId(context.id());
		receipt.setRequestId(request.requestId());
		receipt.setPayload(codec.encode(payload));
		receipt.setResult(codec.encode(result));
		receipts.saveAndFlush(receipt);
		return result;
	}

	public record Request(String requestId, Preview expected) {
	}

	public record Result(String id, String species, String name, boolean bound) {
	}

	public record Payload(long creatureId, Preview expected) {
	}

}
