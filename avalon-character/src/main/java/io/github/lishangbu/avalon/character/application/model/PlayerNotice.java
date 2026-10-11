package io.github.lishangbu.avalon.character.application.model;

/**
 * 可信游戏事件生成的玩家通知。owner为账号内部编号，仅在服务间使用；null表示全服公告。
 * source须唯一标识实际业务行为，重放回执不再发布；正文为服务端生成的纯文字，不能接受玩家任意广播。
 * 监听器同步加入原事务，奖励回滚时通知一同回滚；对浏览器的实时更新另在提交后进行。
 */
public record PlayerNotice(Long owner, String source, String text) {
}
