package io.github.lishangbu.avalon.creature.application.command;

import io.github.lishangbu.avalon.creature.domain.CreatureStorageRules;

import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.character.application.command.CharacterFactCommandService;
import io.github.lishangbu.avalon.content.creature.CreatureCatalog;
import io.github.lishangbu.avalon.content.creature.CreatureAbilityRules;
import io.github.lishangbu.avalon.content.battle.GrowthRules;
import io.github.lishangbu.avalon.content.world.WorldCatalog;
import io.github.lishangbu.avalon.creature.domain.PartyRules;
import io.github.lishangbu.avalon.creature.persistence.entity.OwnedCreature;
import io.github.lishangbu.avalon.creature.persistence.mapper.CreatureStorageCodec;
import io.github.lishangbu.avalon.creature.persistence.repository.CreatureRepository;
import io.github.lishangbu.avalon.platform.BusinessException;
import org.springframework.stereotype.Component;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 伙伴领取服务。来源唯一，初始等级经验固定，不追补加入前已发的人物经验。
 */
@Component
@RequiredArgsConstructor
public class CreatureCommandService {

	private final CreatureRepository pets;

	private final GrowthRules growth;

	private final CreatureAbilityRules abilities;

	private final CreatureCatalog catalog;

	private final CreatureStorageCodec codec;

	private final CharacterCommandService characters;

	private final CollectionCommandService collection;

	private final CharacterFactCommandService facts;

	private final WorldCatalog world;

	/**
	 * 赠宠内部命令。锁定账号角色后读取最新等级并按来源防重；在相同锁内选择空位、创建实例。伙伴取模板等级和累计经验，不追赶人物；满位拒绝外层事务，不能丢弃前面已经扣除的资源。
	 */
	@Transactional(Transactional.TxType.MANDATORY)
	public void grant(long owner, String source, String species) {
		// 在效果执行当下读取等级，包含同一任务事务中先前刚发放的升级经验。
		var context = characters.lock(owner);
		long character = context.id();
		if (pets.hasSource(character, source))
			return;
		if (!catalog.obtainableDirectly(species))
			throw new BusinessException(409, "该形态不能通过赠宠获得");
		var definition = catalog.pet(species);
		OwnedCreature pet = new OwnedCreature();

		pet.setCharacterId(character);
		pet.setOriginCharacterId(character);
		pet.setOriginKind("GIFT");
		pet.setBound(true);
		pet.setSource(source);
		pet.setSpecies(species);
		pet.setName(definition.name());
		pet.setElement(definition.element());
		pet.setLevel(definition.level());
		pet.setExperience(growth.threshold(definition.level()));
		// 来源防重检查已完成，只有首次真正创建个体才抽取；重试直接返回原个体。
		var ability = abilities.draw(species, ThreadLocalRandom.current());
		pet.setAbilitySlot(ability.slot());
		pet.setAbility(ability.ability());
		Set<Integer> occupied = pets.positions(character);
		int position = PartyRules.firstFree(occupied, growth.carryingCapacity(context.level()));
		pet.setPartyPosition(position);
		pet.setSkills(codec.encode(definition.skills()));
		pets.save(pet);
		characters.invalidatePreparation(owner);
		collection.settle(owner);
	}

	/**
	 * 捕获保留遭遇生成的等级、槽位和技能。优先替补、携带，再仓库，绝不自动填出战位。
	 */
	@Transactional(Transactional.TxType.MANDATORY)
	public long capture(long owner, String source, String species, int level, String slot, String ability,
			List<String> skills) {
		var context = characters.lock(owner);
		var prior = pets.originated(context.id()).stream().filter(p -> p.getSource().equals(source)).findFirst();
		if (prior.isPresent())
			return prior.get().getId();
		if (!catalog.obtainableDirectly(species))
			throw new BusinessException(409, "该形态不能从野外捕获");
		var positions = pets.positions(context.id());
		int position = -1;
		if (level <= context.level())
			for (int i = 2; i < growth.carryingCapacity(context.level()); i++)
				if (!positions.contains(i)) {
					position = i;
					break;
				}
		if (position < 0)
			for (int i = 100; i < 130; i++)
				if (!positions.contains(i)) {
					position = i;
					break;
				}
		if (position < 0)
			throw new BusinessException(409, "携带及仓库容量不足");
		var definition = catalog.pet(species);
		var pet = new OwnedCreature();

		pet.setCharacterId(context.id());
		pet.setOriginCharacterId(context.id());
		pet.setCaptorCharacterId(context.id());
		pet.setOriginKind("CAPTURE");
		pet.setBound(false);
		pet.setSource(source);
		pet.setSpecies(species);
		pet.setName(definition.name());
		pet.setElement(definition.element());
		pet.setLevel(level);
		pet.setExperience(growth.threshold(level));
		pet.setAbilitySlot(slot);
		pet.setAbility(ability);
		pet.setSkills(codec.encode(skills));
		pet.setPartyPosition(position);
		pets.saveAndFlush(pet);
		characters.invalidatePreparation(owner);
		collection.settle(owner);
		// 仅真实新捕获发布目标事件，原来源重试已在方法入口返回；登记旧个体不能代替本次捕获。
		facts.record(owner, "CAPTURE:WILD");
		return pet.getId();
	}

	/** 在幻兽中心登记真实捕获结果；登记只产生任务事实，不扣除幻兽或重复发放奖励。 */
	@Transactional
	public void registerCapture(long owner) {
		var context = characters.requireOutsideDungeon(owner);
		if (!context.location().equals("PET_CENTER"))
			throw new BusinessException(409, "请回到幻兽中心登记捕获结果");
		if (pets.originated(context.id()).stream().noneMatch(p -> "CAPTURE".equals(p.getOriginKind())))
			throw new BusinessException(409, "尚未发现可登记的野外捕获伙伴");
		facts.record(owner, "CAPTURE:BQ007_REGISTERED");
	}

	/**
	 * 编队只交换真实持有个体；先暂存空位并 flush，避开数据库位置唯一约束的更新顺序。
	 */
	@Transactional
	public void position(long owner, long petId, int position) {
		var context = characters.requireOutsideDungeon(owner);
		characters.requireCreatureNotDispatched(owner, petId);
		if (position < 0 || position >= growth.carryingCapacity(context.level()))
			throw new BusinessException(400, "随身位置无效");
		var pet = pets.get(petId);
		if (pet == null || pet.unavailable() || !pet.getCharacterId().equals(context.id()))
			throw new BusinessException(404, "伙伴不存在");
		int old = pet.getPartyPosition();
		CreatureStorageRules.requirePosition(old, position, growth.carryingCapacity(context.level()), pet.getLevel(),
				context.level(), world.provides(context.location(), "STORAGE"));
		if (old == position) {
			facts.record(owner, "ACTION:PARTY_SAVED");
			if (context.location().equals("ACADEMY"))
				facts.record(owner, "PARTY:BQ024_SAVED");
			return;
		}
		var other = pets.owned(context.id()).stream().filter(p -> p.getPartyPosition() == position).findFirst();
		other.ifPresent(value -> characters.requireCreatureNotDispatched(owner, value.getId()));
		pet.setPartyPosition(-1);
		pets.flush();
		other.ifPresent(p -> p.setPartyPosition(old));
		pets.flush();
		pet.setPartyPosition(position);
		characters.invalidatePreparation(owner);
		facts.record(owner, "ACTION:PARTY_SAVED");
		if (context.location().equals("ACADEMY"))
			facts.record(owner, "PARTY:BQ024_SAVED");
	}

	/** 玩家明确打开伙伴技能面板时记录查看事实；读取列表本身不会写进度。 */
	@Transactional
	public void viewedSkills(long owner, long petId) {
		var context = characters.requireOutsideDungeon(owner);
		var pet = pets.get(petId);
		if (pet == null || pet.unavailable() || !pet.getCharacterId().equals(context.id()))
			throw new BusinessException(404, "伙伴不存在");
		facts.record(owner, "ACTION:CREATURE_SKILLS_VIEWED");
	}

	/**
	 * 任务经验仅给当时出战或替补；人物先升级后，再计算幻兽经验存储上限。
	 */
	@Transactional(Transactional.TxType.MANDATORY)
	public void grantPartyExperience(long owner, int amount) {
		var context = characters.lock(owner);
		for (var pet : pets.owned(context.id()))
			if (pet.getPartyPosition() < 4)
				addExperience(pet, amount, context.level());
	}

	/**
	 * 战斗使用入场名单及各自入场等级，捕获的新个体不在该名单内。
	 */
	@Transactional(Transactional.TxType.MANDATORY)
	public void grantBattleExperience(long owner, Map<Long, Integer> rewards) {
		var context = characters.lock(owner);
		for (var pet : pets.owned(context.id()))
			if (rewards.containsKey(pet.getId()))
				addExperience(pet, rewards.get(pet.getId()), context.level());
	}

	private void addExperience(OwnedCreature pet, int amount, int playerLevel) {
		long cap = playerLevel == GrowthRules.LEVEL_CAP ? growth.threshold(playerLevel)
				: growth.threshold(playerLevel + 1) - 1;
		pet.setExperience(Math.min(cap, growth.addExperience(pet.getExperience(), amount)));
		pet.setLevel(Math.min(playerLevel, growth.level(pet.getExperience())));
	}

}
