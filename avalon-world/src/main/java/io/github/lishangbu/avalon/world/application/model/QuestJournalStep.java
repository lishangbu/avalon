package io.github.lishangbu.avalon.world.application.model;

/** 步骤状态由服务器回执派生；地点名称仅用于展示，不能作为交付授权。 */
public record QuestJournalStep(String id, String title, String location, boolean completed) {
}
