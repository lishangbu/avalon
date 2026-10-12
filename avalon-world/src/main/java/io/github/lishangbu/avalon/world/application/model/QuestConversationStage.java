package io.github.lishangbu.avalon.world.application.model;

/** 服务端判定的对白阶段，客户端不能用阶段值绕过接取或交付检查。 */
public enum QuestConversationStage {

	OFFER, PROGRESS, READY, REVISIT, RESUME

}
