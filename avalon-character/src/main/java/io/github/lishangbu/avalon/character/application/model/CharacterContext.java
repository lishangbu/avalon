package io.github.lishangbu.avalon.character.application.model;

/**
 * 写事务内从持锁角色构建的上下文，内部 id 保留 Long，不经过浏览器字符串转换。 上下文只代表读取时刻的值，不自行携带数据库锁，也不能在事务结束后当作授权凭证复用。
 * 同事务先发经验再发伙伴时，后一个命令主动重新取得上下文以使用最新等级。
 */
public record CharacterContext(Long id, String profession, String location, int level) {
}
