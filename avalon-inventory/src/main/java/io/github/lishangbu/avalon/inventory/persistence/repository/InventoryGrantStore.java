package io.github.lishangbu.avalon.inventory.persistence.repository;

import io.github.lishangbu.avalon.inventory.persistence.entity.InventoryGrant;
import org.springframework.stereotype.Repository;
import lombok.RequiredArgsConstructor;

/** 奖励出处独立于库存归属；转手前保留最初发放凭证。 */
@Repository
@RequiredArgsConstructor
public class InventoryGrantStore {

	private final InventoryGrantRepository grants;

	private final InventoryRepository items;

	public boolean hasSource(long character, String source) {
		return grants.existsSource(character, source) || items.existsSource(character, source);
	}

	public Long grantId(long character, String source) {
		return grants.grantId(character, source);
	}

	public void rememberGrant(long id, long character, String source) {
		var grant = new InventoryGrant();
		grant.setId(id);
		grant.setCharacterId(character);
		grant.setSource(source);
		grants.save(grant);
	}

}
