package io.github.lishangbu.avalon.battle.application.command;

import io.github.lishangbu.avalon.battle.persistence.entity.*;
import io.github.lishangbu.avalon.battle.persistence.repository.*;
import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.inventory.application.command.InventoryCommandService;
import io.github.lishangbu.avalon.platform.BusinessException;
import io.github.lishangbu.avalon.world.domain.LifeClock;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 角色锁串行化使用免战牌与受袭；回执防止网络重试重复扣除。 */
@Component
@RequiredArgsConstructor
public class PvpProtectionService {

	public static final String ITEM = "PVP_TRUCE_TOKEN";

	public static final int SECONDS = 1800;

	private final CharacterCommandService characters;

	private final CharacterQueryService queries;

	private final InventoryCommandService inventory;

	private final PvpProtectionRepository protections;

	private final PvpProtectionReceiptRepository receipts;

	private final LifeClock clock;

	@Transactional
	public Instant use(long owner, UUID request) {
		if (request == null)
			throw new BusinessException(400, "缺少请求号");
		characters.lock(owner);
		long character = queries.requireId(owner);
		var prior = receipts.request(character, request.toString());
		if (prior != null)
			return prior.getExpiresAt();
		characters.requireOutsideDungeon(owner);
		var protection = protections.findById(character).orElseGet(PvpProtection::new);
		if (protection.getExpiresAt() != null && protection.getExpiresAt().isAfter(clock.now()))
			throw new BusinessException(409, "免战保护仍有效，无须重复使用");
		inventory.take(owner, ITEM, 1);
		protection.setId(character);
		protection.setExpiresAt(clock.now().plusSeconds(SECONDS));
		protections.save(protection);
		var receipt = new PvpProtectionReceipt();
		receipt.setCharacterId(character);
		receipt.setRequestId(request.toString());
		receipt.setExpiresAt(protection.getExpiresAt());
		receipts.save(receipt);
		return protection.getExpiresAt();
	}

	@Transactional
	public Instant current(long owner) {
		var protection = protections.findById(queries.requireId(owner)).orElse(null);
		return protection != null && protection.getExpiresAt().isAfter(clock.now()) ? protection.getExpiresAt() : null;
	}

	/** 调用方已持有双方角色锁；主动攻击解除攻击者保护，被保护目标拒绝攻击。 */
	@Transactional(Transactional.TxType.MANDATORY)
	public void attack(long attacker, long defender) {
		var target = protections.findById(defender).orElse(null);
		if (target != null && target.getExpiresAt().isAfter(clock.now()))
			throw new BusinessException(409, "目标处于免战保护中");
		var self = protections.findById(attacker).orElse(null);
		if (self != null)
			protections.delete(self);
	}

}
