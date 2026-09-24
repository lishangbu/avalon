package io.github.lishangbu.avalon.inventory.application.query;

import io.github.lishangbu.avalon.inventory.application.model.InventoryItemView;
import io.github.lishangbu.avalon.inventory.persistence.mapper.InventoryStorageCodec;
import io.github.lishangbu.avalon.inventory.persistence.repository.InventoryRepository;
import io.github.lishangbu.avalon.platform.BusinessException;
import org.springframework.stereotype.Component;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/** 仅由市场已校验挂单后的内部查询调用，不提供按任意库存编号读取他人背包的 HTTP 入口。 */
@Component
@RequiredArgsConstructor
public class ItemCustodyQueryService {

	private final InventoryRepository inventory;

	private final InventoryStorageCodec codec;

	@Transactional
	public InventoryItemView detail(long asset) {
		var item = inventory.get(asset);
		if (item == null || item.getCustodyQuantity() <= 0)
			throw new BusinessException(409, "商品已离开托管，请刷新挂单");
		var view = codec.view(item);
		// 商品详情数量使用托管量；背包查询仍只看到 quantity，不能因展示商品而恢复可用库存。
		return new InventoryItemView(view.id(), view.name(), view.code(), item.getCustodyQuantity(), view.slot(),
				view.requiredLevel(), view.stats(), view.equipped(), view.bound(), view.quality(), view.sellPrice(),
				view.replacementPrice(), view.source(), view.requiredProfession(), view.enhancementLevel());
	}

}
