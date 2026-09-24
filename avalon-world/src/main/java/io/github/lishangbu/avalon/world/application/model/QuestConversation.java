package io.github.lishangbu.avalon.world.application.model;

/** 当前地点可阅读的一段任务对白。stage为服务端选择结果，不接受客户端指定目标阶段。 */
public record QuestConversation(String step, String title, String speaker, Stage stage, String text, String npcId) {

	public enum Stage {

		OFFER, PROGRESS, READY, REVISIT, RESUME

	}
}
