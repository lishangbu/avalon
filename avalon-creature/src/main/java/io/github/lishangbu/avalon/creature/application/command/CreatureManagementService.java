package io.github.lishangbu.avalon.creature.application.command;

import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.content.world.WorldCatalog;
import io.github.lishangbu.avalon.creature.application.model.CreatureManagementRelease;
import io.github.lishangbu.avalon.creature.domain.CreatureStorageRules;
import io.github.lishangbu.avalon.creature.persistence.entity.CreatureReleaseReceipt;
import io.github.lishangbu.avalon.creature.persistence.entity.OwnedCreature;
import io.github.lishangbu.avalon.creature.persistence.mapper.CreatureStorageCodec;
import io.github.lishangbu.avalon.creature.persistence.repository.CreatureReleaseReceiptRepository;
import io.github.lishangbu.avalon.creature.persistence.repository.CreatureRepository;
import io.github.lishangbu.avalon.platform.BusinessException;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.HashSet;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 仓库与保护操作共用角色写锁。批量放生先检查完整名单，再同时释放占位并写审计， 任一个体状态变化都会拒绝整批，不会出现部分放生后才向玩家报错的情况。
 */
@Component
@RequiredArgsConstructor
public class CreatureManagementService {

	private final CharacterCommandService characters;

	private final CreatureRepository pets;

	private final CreatureReleaseReceiptRepository receipts;

	private final CreatureStorageCodec codec;

	private final WorldCatalog world;

	/** 锁定命令设置明确目标值，重试不会把保护状态反向切换。 */
	@Transactional
	public void lock(long owner, long petId, boolean locked) {
		var context = characters.requireOutsideBattle(owner);
		requirePet(context.id(), petId).setLocked(locked);
	}

	/** 城镇幻兽中心免费存仓，空位由服务端分配；重复存仓保持原位置。 */
	@Transactional
	public void deposit(long owner, long petId) {
		var context = characters.requireOutsideDungeon(owner);
		characters.requireCreatureNotDispatched(owner, petId);
		if (!world.provides(context.location(), "STORAGE"))
			throw new BusinessException(409, "请到幻兽中心存放伙伴");
		var pet = requirePet(context.id(), petId);
		if (pet.getPartyPosition() >= 100)
			return;
		pet.setPartyPosition(CreatureStorageRules.deposit(pet.getPartyPosition(), pets.positions(context.id())));
		characters.invalidatePreparation(owner);
	}

	/**
	 * 确认名单携带显示时的等级、特性、技能与位置，避免另一页面换位或学习后误放生。 已成功的相同请求直接返回；来源档案和审计均不删除，也不发放任何补偿资源。
	 */
	@Transactional
	public void release(long owner, CreatureManagementRelease request) {
		validate(request);
		var context = characters.lock(owner);
		String payload = codec.encode(request.expected());
		var prior = receipts.prior(context.id(), request.requestId());
		if (prior != null) {
			if (!prior.getPayload().equals(payload))
				throw new BusinessException(409, "请求号已用于其他放生名单");
			return;
		}
		characters.requireOutsideDungeon(owner);
		var selected = request.expected().stream().map(expected -> {
			characters.requireCreatureNotDispatched(owner, Long.parseLong(expected.id()));
			var pet = requirePet(context.id(), Long.parseLong(expected.id()));
			if (!codec.view(pet).equals(expected))
				throw new BusinessException(409, "伙伴信息已变化，请重新选择并确认");
			if (pet.isLocked() || pet.getPartyPosition() < 4)
				throw new BusinessException(409, "出战、替补或锁定伙伴不能放生");
			return pet;
		}).toList();
		// 放归不可逆；已倒下伙伴可免费恢复、仓库伙伴可免费取出，但高于人物等级的个体不能替代战斗能力。
		var selectedIds = selected.stream().map(OwnedCreature::getId).collect(Collectors.toSet());
		if (pets.owned(context.id())
			.stream()
			.noneMatch(pet -> !selectedIds.contains(pet.getId()) && pet.getLevel() <= context.level()))
			throw new BusinessException(409, "请至少保留一只等级不高于人物的伙伴，以便继续战斗与捕捉");
		if (selected.stream().anyMatch(pet -> pet.getSource().contains(":O002_RECEIVE:"))
				&& pets.originated(context.id()).stream().noneMatch(pet -> "CAPTURE".equals(pet.getOriginKind())))
			throw new BusinessException(409, "首次捕获野外伙伴前，请保留最初领取的伙伴");
		var now = Instant.now();
		for (var pet : selected) {
			pet.setReleasedAt(now);
			pet.setPartyPosition(null);
		}
		var receipt = new CreatureReleaseReceipt();

		receipt.setCharacterId(context.id());
		receipt.setRequestId(request.requestId());
		receipt.setPayload(payload);
		receipt.setCreatedAt(now);
		receipts.saveAndFlush(receipt);
	}

	private OwnedCreature requirePet(long character, long id) {
		var pet = pets.get(id);
		if (pet == null || pet.getCharacterId() != character || pet.unavailable())
			throw new BusinessException(404, "伙伴不存在");
		return pet;
	}

	/** 任务交付只能消费玩家明确确认的个体快照；任务宿主负责同事务保存奖励与幂等回执。 */
	@Transactional(Transactional.TxType.MANDATORY)
	public void handIn(long owner, String species,
			java.util.List<io.github.lishangbu.avalon.creature.application.model.CreatureView> selected) {
		var context = characters.requireOutsideDungeon(owner);
		var ids = new HashSet<Long>();
		var targets = selected.stream().map(expected -> {
			long id;
			try {
				id = Long.parseLong(expected.id());
			}
			catch (RuntimeException error) {
				throw new BusinessException(400, "交付伙伴编号无效");
			}
			if (!ids.add(id))
				throw new BusinessException(400, "不能重复选择同一伙伴");
			characters.requireCreatureNotDispatched(owner, id);
			var pet = requirePet(context.id(), id);
			if (!pet.getSpecies().equals(species) || !codec.view(pet).equals(expected))
				throw new BusinessException(409, "伙伴信息已变化，请重新选择并确认");
			if (pet.isLocked() || pet.getPartyPosition() < 4)
				throw new BusinessException(409, "出战、替补或锁定伙伴不能交付，请先调整编队和保护状态");
			return pet;
		}).toList();
		if (pets.owned(context.id())
			.stream()
			.noneMatch(pet -> !ids.contains(pet.getId()) && pet.getLevel() <= context.level()))
			throw new BusinessException(409, "请至少保留一只等级不高于人物的伙伴");
		for (var pet : targets) {
			pet.setReleasedAt(Instant.now());
			pet.setPartyPosition(null);
		}
	}

	private void validate(CreatureManagementRelease request) {
		if (request == null || request.requestId() == null || request.expected() == null || request.expected().isEmpty()
				|| request.expected().size() > 36)
			throw new BusinessException(400, "请选择要放生的伙伴");
		try {
			if (!UUID.fromString(request.requestId()).toString().equals(request.requestId()))
				throw new IllegalArgumentException();
			var selected = new HashSet<Long>();
			for (var pet : request.expected()) {
				if (pet == null || pet.id() == null)
					throw new IllegalArgumentException();
				long id = Long.parseLong(pet.id());
				if (id <= 0 || !selected.add(id))
					throw new IllegalArgumentException();
			}
		}
		catch (IllegalArgumentException error) {
			throw new BusinessException(400, "放生请求号或伙伴名单无效");
		}
	}

}
