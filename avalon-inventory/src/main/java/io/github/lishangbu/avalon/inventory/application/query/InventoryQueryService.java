package io.github.lishangbu.avalon.inventory.application.query;

import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.inventory.application.model.InventoryItemView;
import io.github.lishangbu.avalon.inventory.application.model.PendingItemView;
import io.github.lishangbu.avalon.inventory.application.model.BagProjection;
import io.github.lishangbu.avalon.inventory.application.model.BagView;
import io.github.lishangbu.avalon.inventory.domain.EquipmentLoadoutRules;
import io.github.lishangbu.avalon.inventory.persistence.mapper.InventoryStorageCodec;
import io.github.lishangbu.avalon.inventory.persistence.repository.InventoryRepository;

import org.springframework.stereotype.Component;
import jakarta.transaction.Transactional;

import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * 当前领域只读入口。归属查询和实例查询不取得角色写锁。 角色归属在当前版本不可变、角色不可删除，列表由单条 SELECT 返回； 不宣称多个独立 HTTP
 * 查询构成同一个跨领域快照。
 */
@Component
@RequiredArgsConstructor
public class InventoryQueryService {

	private final CharacterQueryService characters;

	private final InventoryRepository items;

	private final InventoryStorageCodec codec;

	private final BagProjection bags;

	private final EquipmentLoadoutRules loadouts;

	@Transactional
	public EquipmentLoadoutRules.Loadout loadout(long owner) {
		return loadouts.from(list(owner).stream()
			.map(item -> new EquipmentLoadoutRules.Piece(item.code(), item.equipped(), item.quantity(), item.stats()))
			.toList(), characters.find(owner).level());
	}

	/**
	 * 先解析认证账号的角色编号，再以一次库存 SELECT 返回正数量实例。投影由存储 mapper
	 * 完成，查询不改装备状态、不发奖励、不取得角色写锁。其他接口同时更新时只保证本条SQL读取已提交数据。
	 */
	@Transactional
	public List<InventoryItemView> list(long owner) {
		Long id = characters.requireId(owner);
		return items.available(id).stream().map(codec::view).toList();
	}

	@Transactional
	public BagView bag(long owner) {
		return bags.from(list(owner));
	}

	@Transactional
	public List<PendingItemView> pending(long owner) {
		return items.pending(characters.requireId(owner))
			.stream()
			.map(item -> new PendingItemView(item.getId().toString(), item.getName(), item.getPendingQuantity(),
					item.isBound(), sourceLabel(item.getSource()), item.getCode()))
			.toList();
	}

	/** 面向玩家说明奖励来源类别，不把内部请求号或数据库来源键直接放进游戏页面。 */
	private String sourceLabel(String source) {
		if (source.startsWith("DUNGEON_CHEST:"))
			return "副本宝箱";
		if (source.startsWith("BATTLE:"))
			return "战斗战利品";
		if (source.startsWith("A"))
			return "冒险任务奖励";
		if (source.startsWith("CRAFT:"))
			return "技能书合成";
		if (source.startsWith("PURCHASE:"))
			return "商店购买";
		return "冒险奖励";
	}

}
