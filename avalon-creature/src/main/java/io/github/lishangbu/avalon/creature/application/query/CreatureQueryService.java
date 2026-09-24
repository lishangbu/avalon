package io.github.lishangbu.avalon.creature.application.query;

import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.content.creature.CreatureCatalog;
import io.github.lishangbu.avalon.content.creature.EvolutionCatalog;
import io.github.lishangbu.avalon.platform.BusinessException;
import io.github.lishangbu.avalon.creature.application.model.EncyclopediaView;
import io.github.lishangbu.avalon.creature.application.model.CreatureView;
import io.github.lishangbu.avalon.creature.persistence.mapper.CreatureStorageCodec;
import io.github.lishangbu.avalon.creature.persistence.repository.CreatureRepository;
import io.github.lishangbu.avalon.creature.persistence.repository.CollectionReceiptRepository;
import io.github.lishangbu.avalon.creature.persistence.repository.CreatureDiscoveryRecords;

import org.springframework.stereotype.Component;
import jakarta.transaction.Transactional;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Stream;

/**
 * 当前领域只读入口。归属查询和实例查询不取得角色写锁。 角色归属在当前版本不可变、角色不可删除，列表由单条 SELECT 返回； 不宣称多个独立 HTTP
 * 查询构成同一个跨领域快照。
 */
@Component
@RequiredArgsConstructor
public class CreatureQueryService {

	private final CharacterQueryService characters;

	private final CreatureRepository pets;

	private final CreatureStorageCodec codec;

	private final CreatureCatalog catalog;

	private final EvolutionCatalog evolutions;

	private final CollectionReceiptRepository collections;

	private final CreatureDiscoveryRecords discoveryRecords;

	/** 永久发现与当前持有分开计算；导入旧档尚未补领时只读兼容，不在查询中补写。 */
	@Transactional
	public List<EncyclopediaView> encyclopedia(long owner) {
		var history = pets.history(characters.requireId(owner));
		var discovered = discoveryRecords.discovered(characters.requireId(owner));
		var credited = collections.owned(owner);
		int level = characters.find(owner).level();
		// 未开放进化的开篇仍展示原八种；曾获得的新形态不会因等级门槛而从永久图鉴消失。
		return catalog.all()
			.stream()
			.filter(species -> catalog.obtainableDirectly(species.id())
					|| level >= evolutions.definition().playerLevel()
					|| history.stream().anyMatch(p -> p.getSpecies().equals(species.id()))
					|| discovered.stream().anyMatch(p -> p.getSpecies().equals(species.id())))
			.map(species -> {
				var found = history.stream().filter(p -> p.getSpecies().equals(species.id())).toList();
				var remembered = discovered.stream().filter(p -> p.getSpecies().equals(species.id())).toList();
				return new EncyclopediaView(species.id(), species.name(), species.element(),
						!found.isEmpty() || !remembered.isEmpty(),
						found.stream().filter(p -> p.getReleasedAt() == null).count(),
						Stream
							.concat(found.stream()
								.map(p -> new EncyclopediaView.Ability(p.getAbilitySlot(), p.getAbility())),
									remembered.stream()
										.map(p -> new EncyclopediaView.Ability(p.getAbilitySlot(), p.getAbility())))
							.distinct()
							.toList(),
						credited.stream().anyMatch(receipt -> receipt.getCode().equals("SPECIES:" + species.id())),
						species.image(), species.description());
			})
			.toList();
	}

	/**
	 * 根据认证账号的角色编号读取伙伴列表，顺序与实例编号保持稳定。JSON解码在存储 mapper 内完成；此查询不补经验、不重生成特性，也不自动调整出战位置。
	 */
	@Transactional
	public List<CreatureView> list(long owner) {
		Long id = characters.requireId(owner);
		return pets.owned(id).stream().map(codec::view).toList();
	}

	/** 来源只供当前持有者查看；托管和放归后不能借旧编号读取个体。进化、转手不改写原始身份。 */
	@Transactional
	public Provenance provenance(long owner, long creature) {
		long character = characters.requireId(owner);
		var pet = pets.get(creature);
		if (pet == null || pet.unavailable() || pet.getCharacterId() != character)
			throw new BusinessException(404, "伙伴不存在");
		Long origin = pet.getOriginCharacterId(), captor = pet.getCaptorCharacterId();
		return new Provenance(pet.isBound(), pet.getOriginKind(), origin == null ? null : origin.toString(),
				origin == null ? null : characters.publicName(origin), captor == null ? null : captor.toString(),
				captor == null ? null : characters.publicName(captor));
	}

	/** 雪花编号使用字符串；未知旧档保持空值，不把当前持有者猜成原捕获者。 */
	public record Provenance(boolean bound, String originKind, String originId, String originName, String captorId,
			String captorName) {
	}

	/**
	 * 任务检查来源及精确编队位，避免用普通同种个体替代指定教学捕获事件。
	 */
	@Transactional
	public boolean hasPlacement(long owner, String sourcePrefix, int position) {
		return pets.owned(characters.requireId(owner))
			.stream()
			.anyMatch(p -> p.getSource().startsWith(sourcePrefix)
					&& (p.getOriginCharacterId() == null || p.getOriginCharacterId().equals(p.getCharacterId()))
					&& p.getPartyPosition() == position);
	}

	/**
	 * 一次查询检查捕获来源，编队位置变化不会丢失捕获事实。
	 */
	@Transactional
	public boolean hasSourcePrefix(long owner, String prefix) {
		return pets.originated(characters.requireId(owner)).stream().anyMatch(p -> p.getSource().startsWith(prefix));
	}

}
