package io.github.lishangbu.avalon.inventory.application.command;

import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.content.item.ItemCatalog;
import io.github.lishangbu.avalon.inventory.application.model.ProductionInventoryProduct;
import io.github.lishangbu.avalon.inventory.domain.BagCapacityRules;
import io.github.lishangbu.avalon.inventory.domain.BagCapacityRulesEntry;
import io.github.lishangbu.avalon.inventory.persistence.repository.InventoryRepository;
import io.github.lishangbu.avalon.platform.BusinessException;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 生产结算的库存边界：调用者验证配方，库存负责归属、绑定传播和扣料后的净空间。 */
@Component
@RequiredArgsConstructor
public class ProductionInventoryService {

	private final CharacterCommandService characters;

	private final InventoryRepository items;

	private final ItemCatalog catalog;

	private final InventoryCommandService commands;

	/** 外层生产命令持有回执事务；容量不足返回false，且完全不扣材料。 */
	@Transactional(Transactional.TxType.MANDATORY)
	public boolean exchange(long owner, String source, Map<Long, Integer> selected, String product, int quantity) {
		return exchange(owner, source, selected, product, quantity, false);
	}

	/** 延迟收成保留播种时的绑定属性；容量不足时将收成留在农田。 */
	@Transactional(Transactional.TxType.MANDATORY)
	public boolean harvest(long owner, String source, String product, int quantity, boolean bound) {
		return exchange(owner, source, Map.of(), product, quantity, bound);
	}

	private boolean exchange(long owner, String source, Map<Long, Integer> selected, String product, int quantity,
			boolean bound) {
		return exchangeAll(owner, source, selected, List.of(new ProductionInventoryProduct(product, quantity)), bound,
				false);
	}

	/** 先为本轮所有可能产物预留净空间，避免满包重试反复抽取稀有材料。 */
	@Transactional(Transactional.TxType.MANDATORY)
	public boolean fits(long owner, Map<Long, Integer> selected, List<ProductionInventoryProduct> products) {
		return exchangeAll(owner, "", selected, products, false, true);
	}

	@Transactional(Transactional.TxType.MANDATORY)
	public boolean exchangeAll(long owner, String source, Map<Long, Integer> selected,
			List<ProductionInventoryProduct> products) {
		return exchangeAll(owner, source, selected, products, false, false);
	}

	private boolean exchangeAll(long owner, String source, Map<Long, Integer> selected,
			List<ProductionInventoryProduct> products, boolean bound, boolean checkOnly) {
		long character = characters.lock(owner).id();
		// 采集可能空产出，但本轮补给仍须共同扣除；空列表不等于非法的零数量产物。
		if (products.stream().anyMatch(product -> product.quantity() < 1))
			throw new IllegalArgumentException("产出数量必须为正");
		var owned = items.available(character);
		var byId = owned.stream().collect(Collectors.toMap(item -> item.getId(), Function.identity()));
		for (var entry : selected.entrySet()) {
			var item = byId.get(entry.getKey());
			if (item == null)
				throw new BusinessException(409, "所选材料不存在");
			if (entry.getValue() == null || entry.getValue() < 1 || item.getQuantity() < entry.getValue()
					|| item.isEquipped())
				throw new BusinessException(409, "材料不足或装备正在穿戴");
			bound |= item.isBound();
		}
		var projection = new ArrayList<BagCapacityRulesEntry>();
		for (var item : owned)
			projection.add(new BagCapacityRulesEntry(item.getCode(), item.isBound(),
					item.getQuantity() - selected.getOrDefault(item.getId(), 0), item.getSlot() != null,
					item.getSlot() == null && catalog.item(item.getCode()).task()));
		for (var product : products) {
			boolean equipment = catalog.hasEquipment(product.code());
			if (!equipment && !catalog.hasItem(product.code()))
				throw new IllegalArgumentException("未知生产物品");
			projection.add(new BagCapacityRulesEntry(product.code(), bound, product.quantity(), equipment,
					!equipment && catalog.item(product.code()).task()));
		}
		if (BagCapacityRules.slots(projection) > BagCapacityRules.CAPACITY)
			return false;
		if (checkOnly)
			return true;
		for (var item : owned)
			item.setQuantity(item.getQuantity() - selected.getOrDefault(item.getId(), 0));
		items.flush();
		// 装备逐件存储，避免批量件共用强化、穿戴或交易状态。
		for (int productIndex = 0; productIndex < products.size(); productIndex++) {
			var product = products.get(productIndex);
			String receipt = products.size() == 1 ? source : source + ":PRODUCT:" + productIndex;
			if (catalog.hasEquipment(product.code())) {
				for (int index = 0; index < product.quantity(); index++)
					commands.grantStrict(owner, receipt + ":" + index, product.code(), 1, true, bound);
			}
			else
				commands.grantStrict(owner, receipt, product.code(), product.quantity(), false, bound);
		}
		return true;
	}

}
