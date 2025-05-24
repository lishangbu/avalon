/**
 * 调用任何API端点时，如果不指定资源ID或名称，将返回该API可用资源的分页列表。默认情况下，列表的每个"页面"最多包含20个资源。
 * 如果您想更改这一数量，只需在GET请求中添加'limit'查询参数，例如：?limit=60。您可以使用'offset'来移动到下一页，
 * 例如：?limit=60&offset=60。characteristic、contest-effect、evolution-chain、machine、super-contest-effect端点是未命名的，
 * 而其余端点则是命名的。
 *
 * @author lishangbu
 * @since 2025/5/24
 */
package io.github.lishangbu.avalon.pokeapi.model.resource;
