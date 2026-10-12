package io.github.lishangbu.avalon.inventory.application.command;

import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.content.item.ItemCatalog;
import io.github.lishangbu.avalon.inventory.application.model.InventoryItemView;
import io.github.lishangbu.avalon.inventory.domain.BagCapacityRules;
import io.github.lishangbu.avalon.inventory.domain.BagCapacityRulesEntry;
import io.github.lishangbu.avalon.inventory.persistence.entity.OwnedItem;
import io.github.lishangbu.avalon.inventory.persistence.mapper.InventoryStorageCodec;
import io.github.lishangbu.avalon.inventory.persistence.repository.InventoryGrantStore;
import io.github.lishangbu.avalon.inventory.persistence.repository.InventoryRepository;
import io.github.lishangbu.avalon.platform.BusinessException;
import jakarta.transaction.Transactional;
import me.ahoo.cosid.IdGenerator;
import me.ahoo.cosid.provider.IdGeneratorProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

/**
 * 库存对市场提供的实物边界，不返回实体。调用方负责挂单、金额、凭证和双方锁序。 此服务只加入已有事务；任一容量或状态校验失败，市场外层必须整笔回滚。
 */
@Component
@DependsOn("snowflakeIdBeanRegistrar")
public class ItemCustodyService {

	private final InventoryGrantStore grants;

	private final CharacterCommandService characters;

	private final InventoryRepository inventory;

	private final InventoryStorageCodec codec;

	private final ItemCatalog catalog;

	private final IdGenerator ids;

	/** Starter 动态注册共享生成器，先等待注册完成，再按名称注入当前上下文中的实例。 */
	public ItemCustodyService(InventoryGrantStore grants, CharacterCommandService characters,
			InventoryRepository inventory, InventoryStorageCodec codec, ItemCatalog catalog,
			@Qualifier(IdGeneratorProvider.SHARE + "SnowflakeId") IdGenerator ids) {
		this.grants = grants;
		this.characters = characters;
		this.inventory = inventory;
		this.codec = codec;
		this.catalog = catalog;
		this.ids = ids;
	}

	@Transactional(Transactional.TxType.MANDATORY)
	public long deposit(long owner, long itemId, int quantity, InventoryItemView expected) {
		long character = characters.requireOutsideDungeon(owner).id();
		var item = inventory.get(itemId);
		if (item == null || item.getCharacterId() != character || item.getQuantity() < quantity || quantity <= 0
				|| item.getCustodyQuantity() != 0)
			throw new BusinessException(409, "上架物品或数量已变化");
		if (item.isBound() || item.isEquipped() || item.getPendingQuantity() != 0
				|| (item.getSlot() == null && catalog.item(item.getCode()).task()))
			throw new BusinessException(409, "该物品不能上架");
		if (!codec.view(item).equals(expected))
			throw new BusinessException(409, "物品详情已变化，请重新确认");
		if (item.getSlot() != null && (quantity != 1 || item.getQuantity() != 1))
			throw new BusinessException(409, "装备必须逐件上架");
		// 旧导入实例在第一次离开原归属前补建出处，不让后续转手恢复旧领奖资格。
		if (item.getOriginGrantId() == null) {
			Long grant = grants.grantId(character, item.getSource());
			if (grant == null) {
				grant = ids.generate();
				grants.rememberGrant(grant, character, item.getSource());
			}
			item.setOriginGrantId(grant);
		}
		if (quantity == item.getQuantity()) {
			item.setQuantity(0);
			item.setCustodyQuantity(quantity);
			return itemId;
		}
		item.setQuantity(item.getQuantity() - quantity);
		var split = copy(item, character, "MARKET:SPLIT:" + ids.generate());
		split.setCustodyQuantity(quantity);
		inventory.save(split);
		return split.getId();
	}

	/** 成交和退货共用接收容量校验，严禁主动购买进入普通待领。 */
	@Transactional(Transactional.TxType.MANDATORY)
	public String receive(long seller, long recipient, long assetId, int quantity, String source) {
		long sellerCharacter = characters.lock(seller).id();
		long target = characters.lock(recipient).id();
		var asset = inventory.get(assetId);
		if (asset == null || asset.getCharacterId() != sellerCharacter || quantity <= 0
				|| quantity > asset.getCustodyQuantity() || asset.isBound() || asset.isEquipped()
				|| asset.getQuantity() != 0 || asset.getPendingQuantity() != 0
				|| (asset.getSlot() != null && quantity != 1))
			throw new BusinessException(409, "托管物品已变化");
		var current = inventory.available(target)
			.stream()
			.map(item -> new BagCapacityRulesEntry(item.getCode(), item.isBound(), item.getQuantity(),
					item.getSlot() != null, item.getSlot() == null && catalog.item(item.getCode()).task()))
			.toList();
		if (BagCapacityRules.fits(current, new BagCapacityRulesEntry(asset.getCode(), false, quantity,
				asset.getSlot() != null, false)) != quantity)
			throw new BusinessException(409, "背包空间不足");
		OwnedItem received;
		if (quantity == asset.getCustodyQuantity()) {
			received = asset;
			asset.setCustodyQuantity(0);
			asset.setCharacterId(target);
			asset.setSource(source);
		}
		else {
			asset.setCustodyQuantity(asset.getCustodyQuantity() - quantity);
			received = copy(asset, target, source);
			inventory.save(received);
		}
		received.setQuantity(quantity);
		return received.getId().toString();
	}

	/** 拆分只适用于可堆叠物品；复制真实实例数据和出处，不从模板重新生成属性。 */
	private OwnedItem copy(OwnedItem original, long character, String source) {
		var item = new OwnedItem();

		item.setCharacterId(character);
		item.setOriginGrantId(original.getOriginGrantId());
		item.setSource(source);
		item.setCode(original.getCode());
		item.setName(original.getName());
		item.setRequiredLevel(original.getRequiredLevel());
		item.setStats(original.getStats());
		item.setEnhancementLevel(original.getEnhancementLevel());
		item.setQuality(original.getQuality());
		item.setSellPrice(original.getSellPrice());
		return item;
	}

}
