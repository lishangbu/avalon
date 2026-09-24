/**
 * auth 领域的浏览器数据契约。类型只约束编译期，不代表运行时验证；对象来自服务端响应，不能作为授权凭据。雪花 ID 保持字符串，普通等级/数量为数字，禁止将 ID 转成 Number 后再传回。
 */
export type Tokens = {
    accessToken: string
    refreshToken: string
    expiresIn: number
}
export type Device = {id: string; label: string; current: boolean}
