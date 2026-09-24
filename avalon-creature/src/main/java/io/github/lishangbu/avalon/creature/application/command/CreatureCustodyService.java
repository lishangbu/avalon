package io.github.lishangbu.avalon.creature.application.command;

import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.creature.application.model.CreatureView;
import io.github.lishangbu.avalon.creature.application.model.CreatureOwnershipChanged;
import io.github.lishangbu.avalon.creature.persistence.mapper.CreatureStorageCodec;
import io.github.lishangbu.avalon.creature.persistence.repository.CreatureDiscoveryRepository;
import io.github.lishangbu.avalon.creature.persistence.repository.CreatureRepository;
import io.github.lishangbu.avalon.platform.BusinessException;
import org.springframework.stereotype.Component;
import org.springframework.context.ApplicationEventPublisher;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * 伙伴模块提供的托管边界。调用方必须在同一事务中建立市场挂单和请求凭证， 不能把此服务作为独立 HTTP 操作，以免产生没有挂单的托管个体。
 */
@Component
@RequiredArgsConstructor
public class CreatureCustodyService {

	private final CharacterCommandService characters;

	private final CreatureRepository pets;

	private final CreatureStorageCodec codec;

	private final CreatureDiscoveryRepository discoveries;

	private final CollectionCommandService collection;

	private final ApplicationEventPublisher ownershipChanged;

	/**
	 * 在角色锁内核对完整确认资料，再释放占位；个体并未放生，来源和发现不会被删除。 初始／赠送／未知来源保守拒绝，教学捕获仍用于指定任务，不作为普通野生商品。
	 */
	@Transactional(Transactional.TxType.MANDATORY)
	public long deposit(long owner, long petId, CreatureView expected) {
		var context = characters.requireOutsideDungeon(owner);
		characters.requireCreatureNotDispatched(owner, petId);
		long character = context.id();
		var pet = pets.get(petId);
		if (pet == null || pet.getCharacterId() != character || pet.unavailable())
			throw new BusinessException(409, "上架伙伴已变化");
		if (!pet.canEnterCustody())
			throw new BusinessException(409, "绑定、锁定、出战、替补或剧情伙伴不能上架");
		if (!codec.view(pet).equals(expected))
			throw new BusinessException(409, "伙伴详情已变化，请重新确认");
		if (pets.owned(character)
			.stream()
			.noneMatch(other -> other.getId() != petId && other.getLevel() <= context.level()))
			throw new BusinessException(409, "请至少保留一只等级不高于人物的伙伴，以便继续战斗与捕捉");
		discoveries.remember(pet);
		pet.setInCustody(true);
		pet.setPartyPosition(null);
		characters.invalidatePreparation(owner);
		return petId;
	}

	/**
	 * 成交和退货统一入仓。市场调用方须先按账号升序锁住双方角色，再锁挂单； 此处重复取得的是已有锁，不能由任意外部入口绕过市场锁序调用。
	 * 买方等级不影响存仓资格，容量不足整笔失败，不生成普通待领。
	 */
	@Transactional(Transactional.TxType.MANDATORY)
	public String receive(long seller, long recipient, long petId) {
		long previous = characters.lock(seller).id();
		long target = characters.lock(recipient).id();
		var pet = pets.get(petId);
		if (pet == null || pet.getCharacterId() != previous || !pet.isInCustody() || pet.getReleasedAt() != null
				|| pet.isBound() || pet.isLocked() || pet.getPartyPosition() != null)
			throw new BusinessException(409, "托管伙伴已变化");
		var occupied = pets.positions(target);
		int position = 100;
		while (position < 130 && occupied.contains(position))
			position++;
		if (position == 130)
			throw new BusinessException(409, "伙伴仓库已满");
		// 保留卖方发现后再改归属；原捕获者、来源、昵称、技能和剩余资源均不重新生成。
		discoveries.remember(pet);
		pet.setCharacterId(target);
		pet.setPartyPosition(position);
		pet.setInCustody(false);
		ownershipChanged.publishEvent(new CreatureOwnershipChanged(previous, target, petId));
		collection.settle(recipient);
		characters.invalidatePreparation(recipient);
		return pet.getId().toString();
	}

}
