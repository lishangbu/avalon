package io.github.lishangbu.avalon.creature.application.model;

import java.util.List;

/**
 * 伙伴实例读模型。id 与 species 分别标识个体和物种，level/experience 来自个体持久化状态。 abilitySlot
 * 保留生成时槽位语义，ability 为当前效果名称，skills 保留实例技能顺序。 partyPosition
 * 是从零开始的随身位置，不能单凭该编号改变队伍，未来编队仍需独立命令校验。
 */
public record CreatureView(String id, String species, String name, String element, int level, long experience,
		String abilitySlot, String ability, List<String> skills, int partyPosition, boolean locked) {
}
