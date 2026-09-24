package io.github.lishangbu.avalon.inventory.api;

import io.github.lishangbu.avalon.inventory.application.command.InventoryCommandService;
import io.github.lishangbu.avalon.inventory.application.command.BookRecyclingCommandService;
import io.github.lishangbu.avalon.inventory.application.query.BookRecyclingQueryService;
import io.github.lishangbu.avalon.inventory.application.command.DungeonChestCommandService;
import io.github.lishangbu.avalon.inventory.application.query.DungeonChestQueryService;
import io.github.lishangbu.avalon.inventory.application.command.EquipmentCommerceService;
import io.github.lishangbu.avalon.inventory.application.query.EquipmentShopQueryService;
import io.github.lishangbu.avalon.inventory.application.command.InventoryClaimService;
import io.github.lishangbu.avalon.inventory.application.model.PendingItemView;
import io.github.lishangbu.avalon.inventory.application.model.BagView;
import io.github.lishangbu.avalon.inventory.application.command.CraftingCommandService;
import io.github.lishangbu.avalon.inventory.application.model.CraftingResult;
import io.github.lishangbu.avalon.inventory.application.model.CraftingView;
import io.github.lishangbu.avalon.inventory.application.query.CraftingQueryService;
import io.github.lishangbu.avalon.inventory.application.model.InventoryItemView;
import io.github.lishangbu.avalon.inventory.application.query.InventoryQueryService;
import io.github.lishangbu.avalon.inventory.domain.EquipmentLoadoutRules;
import io.github.lishangbu.avalon.platform.CurrentAccount;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

import java.util.List;

/**
 * 只从认证身份读取账号，禁止客户端指定其他玩家的角色；同步 JDBC 运行于虚拟线程。
 */
@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryResource {

	private final InventoryCommandService application;

	private final InventoryQueryService queries;

	private final InventoryClaimService claims;

	private final CraftingQueryService craftingQueries;

	private final CraftingCommandService craftingCommands;

	private final CurrentAccount identity;

	private final EquipmentCommerceService commerce;

	private final EquipmentShopQueryService shops;

	private final DungeonChestCommandService chests;

	private final DungeonChestQueryService chestQueries;

	private final BookRecyclingQueryService recyclingQueries;

	private final BookRecyclingCommandService recyclingCommands;

	@GetMapping("/books/recycling")
	public BookRecyclingQueryService.View recycling() {
		return recyclingQueries.view(owner());
	}

	@PostMapping("/books/recycling")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void recycle(@RequestBody(required = false) BookRecyclingCommandService.Request request) {
		recyclingCommands.sell(owner(), request);
	}

	@GetMapping("/chests")
	public List<DungeonChestQueryService.View> chests() {
		return chestQueries.list(owner());
	}

	@PostMapping("/chests/{id}/claim")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void claimChest(@PathVariable("id") long id) {
		chests.claim(owner(), id);
	}

	@GetMapping("/equipment/loadout")
	public EquipmentLoadoutRules.Loadout loadout() {
		return queries.loadout(owner());
	}

	@GetMapping("/equipment/shop")
	public EquipmentShopQueryService.Shop shop() {
		return shops.view(owner());
	}

	@PostMapping("/equipment/commerce")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void trade(@RequestBody(required = false) EquipmentCommerceService.Request request) {
		commerce.execute(owner(), request);
	}

	private long owner() {
		return identity.id();
	}

	@GetMapping
	public List<InventoryItemView> list() {
		return queries.list(owner());
	}

	@GetMapping("/bag")
	public BagView bag() {
		return queries.bag(owner());
	}

	@GetMapping("/pending")
	public List<PendingItemView> pending() {
		return queries.pending(owner());
	}

	@PostMapping("/pending/{id}/claim")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void claim(@PathVariable("id") long id,
			@RequestBody(required = false) InventoryClaimService.Request request) {
		claims.claim(owner(), id, request);
	}

	@PostMapping("/equipment/{id}/equip")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void equip(@PathVariable("id") long id) {
		application.equip(owner(), id);
	}

	@PostMapping("/equipment/{id}/unequip")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void unequip(@PathVariable("id") long id) {
		application.unequip(owner(), id);
	}

	@GetMapping("/crafting")
	public CraftingView crafting() {
		return craftingQueries.preview(owner());
	}

	@PostMapping("/crafting")
	public CraftingResult craft(@RequestBody(required = false) CraftingCommandService.Request request) {
		return craftingCommands.craft(owner(), request);
	}

}
