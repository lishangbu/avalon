package io.github.lishangbu.avalon.world.application.model;

/** 地图最多突出一项主线与一项支线。空槽位不意味着没有其他可推进的任务。 */
public record QuestTrackingView(String main, String side, long revision) {
}
