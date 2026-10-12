package io.github.lishangbu.avalon.world.application.model;

/**
 * 可用交互的查询投影。id是提交命令的步骤编号，quest用于主线归属，location/npc用于地图提示。
 * title和label只负责显示，不能据此决定奖励。查询到可用操作后位置仍可能变化，执行时必须重新验证。
 */
public record QuestAction(String id, String quest, String title, String location, String npc, String label,
		boolean interactable, String npcId, boolean acceptRequired, boolean accepted, boolean acceptAvailable,
		String guide, String instance, int round) {
	/** 旧调用不携带实例，浏览器只对有实例的交付使用新命令。 */
	public QuestAction(String id, String quest, String title, String location, String npc, String label,
			boolean interactable, String npcId, boolean acceptRequired, boolean accepted, boolean acceptAvailable,
			String guide) {
		this(id, quest, title, location, npc, label, interactable, npcId, acceptRequired, accepted, acceptAvailable,
				guide, null, 1);
	}

	public QuestAction(String id, String quest, String title, String location, String npc, String label,
			boolean interactable, String npcId) {
		this(id, quest, title, location, npc, label, interactable, npcId, false, false, false, null);
	}

	public QuestAction(String id, String quest, String title, String location, String npc, String label,
			boolean interactable) {
		this(id, quest, title, location, npc, label, interactable, null);
	}
}
