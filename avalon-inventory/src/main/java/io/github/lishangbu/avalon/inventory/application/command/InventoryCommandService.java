package io.github.lishangbu.avalon.inventory.application.command;

import io.github.lishangbu.avalon.inventory.persistence.repository.InventoryGrantStore;

import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.character.application.command.CharacterFactCommandService;
import io.github.lishangbu.avalon.content.item.ItemCatalog;
import io.github.lishangbu.avalon.content.item.EquipmentCatalog;
import io.github.lishangbu.avalon.inventory.domain.EquipmentRules;
import io.github.lishangbu.avalon.inventory.domain.EquipmentLoadoutRules;
import io.github.lishangbu.avalon.inventory.application.model.EquipmentChanged;
import io.github.lishangbu.avalon.inventory.domain.BagCapacityRules;
import io.github.lishangbu.avalon.inventory.persistence.entity.OwnedItem;
import io.github.lishangbu.avalon.inventory.persistence.mapper.InventoryStorageCodec;
import io.github.lishangbu.avalon.inventory.persistence.repository.InventoryRepository;
import io.github.lishangbu.avalon.platform.BusinessException;
import org.springframework.stereotype.Component;
import org.springframework.context.ApplicationEventPublisher;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 库存写用例入口。内部发放/交付加入已有奖励事务，公开穿戴用例建立自己的事务。 每种变更均主动锁定账号角色并核验服务器状态，不依赖调用者口头约定或前端按钮禁用。
 * 此类只编排命令，列表查询由 InventoryQueryService 提供，JSON 存储格式交给 codec。
 */
@Component
@RequiredArgsConstructor
public class InventoryCommandService {

	private final InventoryGrantStore grants;

	private final InventoryRepository items;

	private final ItemCatalog catalog;

	private final EquipmentCatalog equipmentCatalog;

	private final InventoryStorageCodec codec;

	private final CharacterCommandService characters;

	private final ApplicationEventPublisher equipmentChanged;

	private final CharacterFactCommandService facts;

	private final EquipmentLoadoutRules loadouts;

	/**
	 * 任务奖励的内部命令，不是公开任意发物品接口。主动锁定 owner
	 * 的角色，读取真实职业，再按来源幂等键检查已有实例。设备模板属性与职业加成在校验后合并，任何保存失败都使外层奖励事务回滚。
	 */
	@Transactional(Transactional.TxType.MANDATORY)
	public void grant(long owner, String source, String code, int amount, boolean equipment) {
		grantWithBinding(owner, source, code, amount, equipment, true);
	}

	/**
	 * 来源决定绑定状态；任务沿用绑定发放，野外技能书保持未绑定，不由品质或客户端决定。
	 */
	@Transactional(Transactional.TxType.MANDATORY)
	public void grantWithBinding(long owner, String source, String code, int amount, boolean equipment, boolean bound) {
		grantInternal(owner, source, code, amount, equipment, bound, false);
	}

	/** 购买、合成等主动消费必须完整放入背包，否则整个外层事务回滚，不转入待领。 */
	@Transactional(Transactional.TxType.MANDATORY)
	public void grantStrict(long owner, String source, String code, int amount, boolean equipment, boolean bound) {
		grantInternal(owner, source, code, amount, equipment, bound, true);
	}

	/** 内部奖励装备保持固定模板，不依领取职业改变；来源指定的回收价随实例保存。 */
	@Transactional(Transactional.TxType.MANDATORY)
	public void grantRewardEquipment(long owner, String source, String code, int sellPrice) {
		if (sellPrice < 0)
			throw new IllegalArgumentException("回收价不能为负");
		long character = characters.lock(owner).id();
		if (grants.hasSource(character, source))
			return;
		grantInternal(owner, source, code, 1, true, true, false);
		items.source(character, source).setSellPrice(sellPrice);
	}

	private void grantInternal(long owner, String source, String code, int amount, boolean equipment, boolean bound,
			boolean strict) {
		var context = characters.lock(owner);
		long character = context.id();
		String profession = context.profession();
		if (amount <= 0)
			throw new IllegalArgumentException("发放数量必须为正");
		if (grants.hasSource(character, source))
			return;
		if (equipment ? !catalog.hasEquipment(code) : !catalog.hasItem(code))
			throw new IllegalArgumentException("物品不能进入正式库存：" + code);
		boolean task = !equipment && catalog.item(code).task();
		int accepted = BagCapacityRules.fits(items.available(character)
			.stream()
			.map(existing -> new BagCapacityRules.Entry(existing.getCode(), existing.isBound(), existing.getQuantity(),
					existing.getSlot() != null, existing.getSlot() == null && catalog.item(existing.getCode()).task()))
			.toList(), new BagCapacityRules.Entry(code, bound, amount, equipment, task));
		if (strict && accepted != amount)
			throw new BusinessException(409, "背包空间不足，操作未执行");
		OwnedItem item = new OwnedItem();

		item.setCharacterId(character);
		item.setSource(source);
		item.setCode(code);
		item.setQuantity(accepted);
		item.setPendingQuantity(amount - accepted);
		item.setBound(bound);
		Map<String, Integer> stats = new TreeMap<>();
		if (equipment) {
			var definition = catalog.equipment(code);
			item.setQuality(equipmentCatalog.metadata(code).quality());
			item.setName(definition.names().getOrDefault(profession, definition.name()));
			item.setSlot(definition.slot());
			item.setRequiredLevel(definition.level());
			stats.putAll(definition.stats());
			definition.professionStats()
				.getOrDefault(profession, Map.of())
				.forEach((k, v) -> stats.merge(k, v, Integer::sum));
		}
		else
			item.setName(catalog.item(code).name());
		item.setStats(codec.encode(stats));
		items.save(item);
		// persist 已分配雪花编号；出处与首次库存复用编号，后续拆分或转手保持出处不变。
		item.setOriginGrantId(item.getId());
		grants.rememberGrant(item.getId(), character, source);
	}

	/**
	 * 任务交付内部命令。按稳定物品编号读取当前角色正库存实例，先确认总量足够再依序扣除，避免不足时部分交付。数量归零保留记录，以维持奖励来源防重；不能接受零或负交付数量。
	 */
	@Transactional(Transactional.TxType.MANDATORY)
	public void take(long owner, String code, int amount) {
		long character = characters.lock(owner).id();
		if (amount <= 0)
			throw new IllegalArgumentException("交付数量必须为正");
		List<OwnedItem> matching = items.matching(character, code);
		if (matching.stream().mapToLong(OwnedItem::getQuantity).sum() < amount)
			throw new BusinessException(409, "缺少需要交付的任务物品");
		for (OwnedItem item : matching) {
			int used = Math.min(amount, item.getQuantity());
			item.setQuantity(item.getQuantity() - used);
			amount -= used;
			if (amount == 0)
				break;
		}
	}

	/**
	 * 精确消耗玩家选中的一个库存实例，不自动混用同名绑定／未绑定物品。 用于学习等需要确认具体物品的内部命令；所属角色、内容编号和剩余数量均由服务端核验。
	 */
	@Transactional(Transactional.TxType.MANDATORY)
	public void consume(long owner, long itemId, String expectedCode, int amount) {
		long character = characters.lock(owner).id();
		if (amount <= 0)
			throw new IllegalArgumentException("消耗数量必须为正");
		var item = items.get(itemId);
		if (item == null || item.getCharacterId() != character)
			throw new BusinessException(404, "物品不存在");
		if (!item.getCode().equals(expectedCode) || item.getSlot() != null || item.getQuantity() < amount)
			throw new BusinessException(409, "物品或数量已变化，请重新确认");
		item.setQuantity(item.getQuantity() - amount);
	}

	/** 生活商店回收成品；装备必须未穿戴、未强化，避免普通材料出售入口回收已投入强化的实例。 */
	@Transactional(Transactional.TxType.MANDATORY)
	public void consumeForSale(long owner, long itemId, String expectedCode, int amount) {
		long character = characters.lock(owner).id();
		var item = items.get(itemId);
		if (item == null || item.getCharacterId() != character)
			throw new BusinessException(404, "物品不存在");
		if (amount <= 0 || !item.getCode().equals(expectedCode) || item.getQuantity() < amount)
			throw new BusinessException(409, "物品或数量已变化，请重新确认");
		if (item.isEquipped() || item.getEnhancementLevel() > 0)
			throw new BusinessException(409, "生活商店仅回收未穿戴、未强化的成品");
		item.setQuantity(item.getQuantity() - amount);
	}

	/**
	 * 在已有事务内主动取得角色锁并校验物品归属，读取人物真实等级检查资格。已穿戴项幂等返回；替换装备先卸下并 flush，再设置新装备，避免 Hibernate
	 * 更新排序触发部位唯一冲突。
	 */
	@Transactional
	public void equip(long owner, long itemId) {
		var context = characters.requireOutsideBattle(owner);
		long character = context.id();
		OwnedItem item = items.get(itemId);
		if (item == null || item.getCharacterId() != character)
			throw new BusinessException(404, "物品不存在");
		EquipmentRules.requireWearable(item.getSlot(), item.getQuantity(), item.getRequiredLevel(), context.level());
		String profession = equipmentCatalog.metadata(item.getCode()).profession();
		if (profession != null && !profession.equals(context.profession()))
			throw new BusinessException(409, "当前职业不能穿戴这件装备");
		if (item.isEquipped())
			return;
		var previous = equippedStats(character, context.level());
		items.equipped(character, item.getSlot()).forEach(i -> i.setEquipped(false));
		items.flush(); // 先提交同事务内的卸下更新，避免装备唯一约束受 Hibernate 更新顺序影响。
		item.setEquipped(true);
		item.setBound(true);
		characters.invalidatePreparation(owner);
		equipmentChanged.publishEvent(new EquipmentChanged(owner, previous));
		facts.record(owner, "ACTION:EQUIPMENT_EQUIPPED");
	}

	/** 卸装是明确的目标状态，重试不反转；仍需检查归属、正库存和战外门禁。 */
	@Transactional
	public void unequip(long owner, long itemId) {
		var context = characters.requireOutsideBattle(owner);
		long character = context.id();
		var item = items.get(itemId);
		if (item == null || item.getCharacterId() != character)
			throw new BusinessException(404, "物品不存在");
		if (item.getSlot() == null || item.getQuantity() <= 0)
			throw new BusinessException(409, "该物品不能卸下");
		if (!item.isEquipped())
			return;
		var previous = equippedStats(character, context.level());
		item.setEquipped(false);
		characters.invalidatePreparation(owner);
		equipmentChanged.publishEvent(new EquipmentChanged(owner, previous));
	}

	private Map<String, Integer> equippedStats(long character, int level) {
		return loadouts
			.from(items.available(character)
				.stream()
				.map(codec::view)
				.map(item -> new EquipmentLoadoutRules.Piece(item.code(), item.equipped(), item.quantity(),
						item.stats()))
				.toList(), level)
			.stats();
	}

}
