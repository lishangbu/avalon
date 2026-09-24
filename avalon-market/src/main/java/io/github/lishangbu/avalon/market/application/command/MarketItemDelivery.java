package io.github.lishangbu.avalon.market.application.command;

import io.github.lishangbu.avalon.inventory.application.command.ItemCustodyService;
import io.github.lishangbu.avalon.market.persistence.entity.MarketListing;
import io.github.lishangbu.avalon.platform.BusinessException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 多批次挂单的成交和退货共用同一交付路径，任一批容量不足时整笔回滚。 */
@Component
@RequiredArgsConstructor
public class MarketItemDelivery {

	private final ItemCustodyService custody;

	@Transactional(Transactional.TxType.MANDATORY)
	public String receive(MarketListing listing, long recipient, int quantity, String source) {
		int remaining = quantity;
		String first = null;
		// 固定实例顺序，部分购买与重试不依赖集合加载顺序。
		var assets = listing.getItemAssets().keySet().stream().sorted().toList();
		for (long asset : assets) {
			if (remaining == 0)
				break;
			int available = listing.getItemAssets().get(asset);
			int count = Math.min(remaining, available);
			String delivered = custody.receive(listing.getSellerAccountId(), recipient, asset, count,
					source + ":" + asset);
			if (first == null)
				first = delivered;
			if (count == available)
				listing.getItemAssets().remove(asset);
			else
				listing.getItemAssets().put(asset, available - count);
			remaining -= count;
		}
		if (remaining != 0 || first == null)
			throw new BusinessException(409, "托管物品数量已变化");
		if (!listing.getItemAssets().isEmpty())
			listing.setAssetId(listing.getItemAssets().keySet().stream().min(Long::compareTo).orElseThrow());
		return first;
	}

}
