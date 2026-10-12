package io.github.lishangbu.avalon.market.application.query;

import io.github.lishangbu.avalon.character.application.query.CharacterFactQueryService;
import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.content.item.EquipmentSetCatalog;
import io.github.lishangbu.avalon.content.item.MarketCatalog;
import io.github.lishangbu.avalon.creature.application.model.CreatureView;
import io.github.lishangbu.avalon.creature.application.query.CreatureCustodyQueryService;
import io.github.lishangbu.avalon.inventory.application.model.InventoryItemView;
import io.github.lishangbu.avalon.inventory.application.query.ItemCustodyQueryService;
import io.github.lishangbu.avalon.market.application.model.MarketCreatureFilter;
import io.github.lishangbu.avalon.market.application.model.MarketDetail;
import io.github.lishangbu.avalon.market.application.model.MarketPage;
import io.github.lishangbu.avalon.market.application.model.MarketAccessView;
import io.github.lishangbu.avalon.market.application.model.MarketListingView;
import io.github.lishangbu.avalon.market.persistence.entity.MarketListing;
import io.github.lishangbu.avalon.market.persistence.repository.MarketListingRepository;
import io.github.lishangbu.avalon.platform.BusinessException;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

/** 市场读取不取得角色写锁，不在查询时推进到期状态；展示和命令均按实际截止时间判断。 */
@Component
@RequiredArgsConstructor
public class MarketQueryService {

	private final CharacterQueryService characters;

	private final CharacterFactQueryService facts;

	private final MarketCatalog catalog;

	private final MarketListingRepository listings;

	private final ItemCustodyQueryService custody;

	private final EquipmentSetCatalog sets;

	private final CreatureCustodyQueryService creatureCustody;

	@Transactional
	public List<CreatureView> eligibleCreatures(long owner) {
		var permission = access(owner);
		if (!permission.available() || !permission.taught())
			throw new BusinessException(409, "请在交易所完成教学后查看");
		return creatureCustody.eligible(owner);
	}

	/** 参数化筛选，排序字段由枚举分支选择，不把客户端文本拼成 HQL。 */
	@Transactional
	public MarketPage search(long owner, String name, String category, String slot, String quality, Integer minimum,
			Integer maximum, String sort, int page, int size) {
		return search(owner, name, category, slot, quality, minimum, maximum, sort, page, size, null);
	}

	@Transactional
	public MarketPage search(long owner, String name, String category, String slot, String quality, Integer minimum,
			Integer maximum, String sort, int page, int size, MarketCreatureFilter creature) {
		return search(owner, name, category, slot, quality, minimum, maximum, sort, page, size, creature, null);
	}

	@Transactional
	public MarketPage search(long owner, String name, String category, String slot, String quality, Integer minimum,
			Integer maximum, String sort, int page, int size, MarketCreatureFilter creature, Integer enhancementLevel) {
		validatePage(page, size);
		if (enhancementLevel != null && (enhancementLevel < 0 || enhancementLevel > 7))
			throw new BusinessException(400, "强化等级筛选无效");
		var permission = access(owner);
		if (!permission.available() || !permission.taught())
			throw new BusinessException(409, "请在交易所完成教学后浏览");
		if (name == null || name.length() > 100 || category == null
				|| !Set.of("ALL", "EQUIPMENT", "BOOK", "FRAGMENT", "MATERIAL", "CREATURE").contains(category)
				|| (minimum != null && minimum < 0) || (maximum != null && maximum < 0)
				|| (minimum != null && maximum != null && minimum > maximum))
			throw new BusinessException(400, "商品筛选参数无效");
		if (creature != null && ((creature.species() != null && creature.species().length() > 100)
				|| (creature.skill() != null && creature.skill().length() > 100)))
			throw new BusinessException(400, "伙伴筛选参数无效");
		String order = switch (sort == null ? "" : sort) {
			case "NEWEST" -> "createdAt desc, id desc";
			case "PRICE_ASC" -> "unitPrice asc, id desc";
			case "PRICE_DESC" -> "unitPrice desc, id desc";
			default -> throw new BusinessException(400, "商品排序无效");
		};
		Instant now = Instant.now();
		Specification<MarketListing> specification = (root, query, cb) -> {
			var predicates = new ArrayList<Predicate>();
			predicates.add(cb.equal(root.get("state"), "ACTIVE"));
			predicates.add(cb.greaterThan(root.get("expiresAt"), now));
			if (!name.isBlank())
				predicates.add(cb.greaterThan(cb.locate(root.get("itemName"), name.strip()), 0));
			if (!category.equals("ALL"))
				predicates.add(cb.equal(root.get("itemCategory"), category));
			if (slot != null && !slot.isBlank())
				predicates.add(cb.equal(root.get("itemSlot"), slot));
			if (quality != null && !quality.isBlank())
				predicates.add(cb.equal(root.get("itemQuality"), quality));
			if (minimum != null)
				predicates.add(cb.ge(root.get("itemLevel"), minimum));
			if (maximum != null)
				predicates.add(cb.le(root.get("itemLevel"), maximum));
			if (enhancementLevel != null)
				predicates.add(cb.equal(root.get("enhancementLevel"), enhancementLevel));
			if (creature != null) {
				if (creature.species() != null && !creature.species().isBlank()) {
					predicates.add(cb.equal(root.get("kind"), "CREATURE"));
					predicates.add(cb.equal(root.get("itemCode"), creature.species()));
				}
				if (creature.hidden() != null)
					predicates.add(cb.equal(root.get("creatureHidden"), creature.hidden()));
				if (creature.skill() != null && !creature.skill().isBlank())
					predicates.add(cb.isMember(creature.skill(), root.get("creatureSkills")));
			}
			return cb.and(predicates.toArray(Predicate[]::new));
		};
		var sorting = sort.equals("PRICE_ASC") ? Sort.by("unitPrice").ascending()
				: Sort.by(sort.equals("PRICE_DESC") ? "unitPrice" : "createdAt").descending();
		var slice = listings.findBy(specification,
				query -> query.slice(PageRequest.of(page, size, sorting.and(Sort.by("id").descending()))));
		var rows = slice.getContent();
		return new MarketPage(page, size, slice.hasNext(),
				rows.stream().limit(size).map(row -> view(row, now)).toList());
	}

	@Transactional
	public MarketDetail detail(long owner, long id) {
		var permission = access(owner);
		if (!permission.available() || !permission.taught())
			throw new BusinessException(409, "请在交易所完成教学后查看");
		var listing = listings.get(id);
		Instant now = Instant.now();
		if (listing == null || (listing.getSellerAccountId() != owner
				&& (!listing.getState().equals("ACTIVE") || !now.isBefore(listing.getExpiresAt()))))
			throw new BusinessException(404, "商品不存在或挂单已结束");
		if (listing.getKind().equals("CREATURE"))
			return new MarketDetail(view(listing, now), null, List.of(), listing.getSellerAccountId() == owner,
					creatureCustody.detail(listing.getSellerAccountId(), listing.getAssetId()));
		var item = custody.detail(listing.getAssetId());
		item = new InventoryItemView(item.id(), item.name(), item.code(), listing.getRemaining(), item.slot(),
				item.requiredLevel(), item.stats(), item.equipped(), item.bound(), item.quality(), item.sellPrice(),
				item.replacementPrice(), item.source(), item.requiredProfession(), item.enhancementLevel());
		String code = item.code();
		return new MarketDetail(view(listing, now), item,
				sets.sets()
					.stream()
					.filter(set -> set.pieces().contains(code) || set.weapons().contains(code))
					.toList(),
				listing.getSellerAccountId() == owner, null);
	}

	private void validatePage(int page, int size) {
		if (page < 0 || size < 1 || size > 50 || (long) page * size > Integer.MAX_VALUE - 51L)
			throw new BusinessException(400, "分页参数无效");
	}

	@Transactional
	public MarketAccessView access(long owner) {
		var character = characters.find(owner);
		if (character == null)
			return new MarketAccessView(false, false, 0, false, catalog.definition().playerLevel());
		var adventure = characters.adventure(owner);
		boolean available = character.level() >= catalog.definition().playerLevel()
				&& catalog.definition().locations().contains(character.location()) && adventure.battle() == null
				&& adventure.dungeon() == null;
		if (!available)
			return new MarketAccessView(false, false, 0, false, catalog.definition().playerLevel());
		Instant now = Instant.now();
		long active = listings.unexpired(owner, now);
		boolean pending = listings.hasReturns(owner) || listings.overdue(owner, now) > 0;
		return new MarketAccessView(true, facts.codes(owner).contains("MARKET:TUTORIAL"), active, pending,
				catalog.definition().playerLevel());
	}

	/** 每页最多50条，额外读取一条判断后续页；不把全量历史载入内存后切片。 */
	@Transactional
	public MarketPage mine(long owner, String state, int page, int size) {
		if (page < 0 || size < 1 || size > 50 || (long) page * size > Integer.MAX_VALUE - 51L)
			throw new BusinessException(400, "分页参数无效");
		if (state == null || !Set.of("ALL", "ACTIVE", "RETURN_PENDING", "SOLD", "RETURNED").contains(state))
			throw new BusinessException(400, "挂单状态筛选无效");
		var access = access(owner);
		if (!access.available() || !access.taught())
			throw new BusinessException(409, "请在交易所完成教学后查看挂单");
		Instant now = Instant.now();
		Specification<MarketListing> specification = (root, query, cb) -> {
			var owned = cb.equal(root.get("sellerAccountId"), owner);
			var active = cb.equal(root.get("state"), "ACTIVE");
			var filter = switch (state) {
				case "ACTIVE" -> cb.and(active, cb.greaterThan(root.get("expiresAt"), now));
				case "RETURN_PENDING" -> cb.or(cb.equal(root.get("state"), "RETURN_PENDING"),
						cb.and(active, cb.lessThanOrEqualTo(root.get("expiresAt"), now)));
				case "SOLD", "RETURNED" -> cb.equal(root.get("state"), state);
				default -> cb.conjunction();
			};
			return cb.and(owned, filter);
		};
		var slice = listings.findBy(specification,
				query -> query.slice(PageRequest.of(page, size, Sort.by("createdAt", "id").descending())));
		var rows = slice.getContent();
		return new MarketPage(page, size, slice.hasNext(),
				rows.stream().limit(size).map(listing -> view(listing, now)).toList());
	}

	private MarketListingView view(MarketListing listing, Instant now) {
		String state = listing.getState();
		if (state.equals("ACTIVE") && !now.isBefore(listing.getExpiresAt()))
			state = "RETURN_PENDING";
		return new MarketListingView(listing.getId().toString(), listing.getAssetId().toString(), listing.getKind(),
				state, Long.toString(listing.getUnitPrice()), listing.getOriginalQuantity(), listing.getRemaining(),
				listing.getExpiresAt(), listing.getItemName(), listing.getItemCategory(),
				listing.getEnhancementLevel());
	}

}
