---
applyTo: "*Controller.java"
---

# Controller 代码与注释编写规范

## Controller 规范

- 命名以 *Controller 结尾，放在 controller 包
- 负责接收请求、参数校验、调用 Service 层，不包含业务逻辑
- 使用 @RestController 注解标记
- 控制器和方法必须编写 JavaDoc/Markdown 文档注释，说明接口用途、参数、返回值
- 统一使用 RESTful 风格的 URL 设计
- 参数校验使用 Bean Validation 注解
- 异常处理统一使用全局异常处理器
- 注释结尾不使用中文句号，保持简洁

## 命名规约

- 查询列表：`listXXX` 或 `getXXXs`
- 获取分页：`getXXXPage` 或 `pageXXX`
- 根据ID查询：`getXXXById`
- 新增：`createXXX` 或 `addXXX`
- 更新：`updateXXX` 或 `updateXXXById`
- 删除：`deleteXXXById` 或 `removeXXXById`
- 批量操作：`batchXXX`

## HTTP 方法映射

- `GET` - 查询操作
- `POST` - 新增操作
- `PUT` - 更新操作（全量更新）
- `PATCH` - 更新操作（部分更新）
- `DELETE` - 删除操作

## 参数校验规范

- 路径参数：`@PathVariable` 配合校验注解
- 查询参数：`@RequestParam` 配合校验注解
- 请求体：`@RequestBody @Valid` 进行校验
- 校验规则用 Bean Validation 注解描述长度、格式、必填等

## 其他规范

- 路由使用 RESTful 规范与语义化路径
- 响应状态码遵循 HTTP 约定
- API 版本通过 Spring 7 RequestMapping 的 version 参数管理
- 跨域处理通过 `@CrossOrigin` 或全局配置
- 接口文档使用 OpenAPI 3.0 注解生成
- 日志记录关键操作，便于审计与排查
- 统一响应处理：返回业务数据，由全局拦截器包装
- 统一异常处理：使用全局异常处理器
