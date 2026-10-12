package io.github.lishangbu.avalon.inventory.domain;

import java.util.Map;

/** 领域输入仅包含计算所需值，不依赖库存接口投影。 */
public record EquipmentLoadoutRulesPiece(String code, boolean equipped, int quantity, Map<String, Integer> stats) {
}
