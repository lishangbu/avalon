package io.github.lishangbu.avalon.identity.application.model;

/**
 * 设备列表查询结果，不含访问令牌、刷新令牌或数据库摘要。 id 为字符串雪花编号，供撤销设备请求使用；服务端仍会检查此设备属于当前账号。 label
 * 仅供辨认设备，current 根据当前请求验证过的会话编号计算，客户端不能指定。
 */
public record DeviceView(String id, String label, boolean current) {
}
