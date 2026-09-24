package io.github.lishangbu.avalon.inventory.application.command;

import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.inventory.persistence.entity.OwnedItem;
import io.github.lishangbu.avalon.inventory.persistence.repository.InventoryRepository;
import io.github.lishangbu.avalon.platform.BusinessException;
import org.springframework.stereotype.Component;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * 强化与进化共用的绑定材料选择。只暴露不可变扣除计划，不向其他模块传库存实体。 调用者提供受服务端配方校验的需求；目标未绑定时必须由玩家明确允许绑定，不能因库存不足擅自改选。
 * 预览与实际消耗使用同一算法；提交持角色锁重算并精确比对，库存变化要求重新确认。
 */
@Component
@RequiredArgsConstructor
public class CultivationMaterialService {

	private final CharacterCommandService characters;

	private final InventoryRepository inventory;

	public enum Mode {

		UNBOUND_ONLY, BOUND_FIRST

	}

	public record Plan(Map<Long, Integer> quantities, boolean bindsTarget) {
		public Plan {
			quantities = Map.copyOf(quantities);
		}
	}

	/** 由角色锁内的培养报价调用，未领库存与装备不能充当培养材料。 */
	@Transactional(Transactional.TxType.MANDATORY)
	public Plan preview(long owner, Map<String, Integer> requirements, Mode mode) {
		if (requirements == null || requirements.isEmpty() || mode == null
				|| requirements.entrySet()
					.stream()
					.anyMatch(e -> e.getKey() == null || e.getValue() == null || e.getValue() <= 0))
			throw new BusinessException(400, "培养材料需求无效");
		long character = characters.lock(owner).id();
		var candidates = inventory.available(character)
			.stream()
			.filter(item -> item.getSlot() == null)
			.filter(item -> mode == Mode.BOUND_FIRST || !item.isBound())
			.sorted(Comparator.comparing(OwnedItem::isBound).reversed().thenComparing(OwnedItem::getId))
			.toList();
		var selected = new LinkedHashMap<Long, Integer>();
		boolean bound = false;
		for (var requirement : new TreeMap<>(requirements).entrySet()) {
			int remaining = requirement.getValue();
			for (var item : candidates) {
				if (!item.getCode().equals(requirement.getKey()))
					continue;
				int quantity = Math.min(remaining, item.getQuantity());
				selected.put(item.getId(), quantity);
				bound |= item.isBound();
				remaining -= quantity;
				if (remaining == 0)
					break;
			}
			if (remaining > 0)
				throw new BusinessException(409, mode == Mode.UNBOUND_ONLY ? "未绑定培养材料不足，请重新选择材料使用方式" : "培养材料不足");
		}
		return new Plan(selected, bound);
	}

	/** 计划的绑定结果参与比较；失败强化仍须调用消耗并按返回结果绑定目标。幂等由外层培养凭证负责。 */
	@Transactional(Transactional.TxType.MANDATORY)
	public boolean consume(long owner, Map<String, Integer> requirements, Mode mode, Plan expected) {
		var actual = preview(owner, requirements, mode);
		if (!actual.equals(expected))
			throw new BusinessException(409, "培养材料已变化，请重新确认");
		actual.quantities().forEach((id, quantity) -> {
			var item = inventory.get(id);
			item.setQuantity(item.getQuantity() - quantity);
		});
		return actual.bindsTarget();
	}

}
