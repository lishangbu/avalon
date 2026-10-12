package io.github.lishangbu.avalon.chat.application.model;

/** 角色级陌生人拒收开关，缺省为允许；设置写入保留原发送时间，不能用其重置限频。 */
public record ChatSettings(boolean rejectStrangers) {
}
