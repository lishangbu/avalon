---
applyTo: "*Controller.java"
---

# Controller 代码与注释编写规范

## Controller 规范

## 基本要求

- 类名以 Controller 结尾，放在 controller 包
- 仅负责请求接入、参数校验与调用 Service
- 使用 @RestController
- 类与方法必须有 JavaDoc/Markdown 注释，说明用途、参数、返回值
- RESTful URL
- Bean Validation 做参数校验
- 使用全局异常处理器
- 注释结尾不使用中文句号

## 命名

- list/get: listXXX 或 getXXXs
- page: getXXXPage 或 pageXXX
- byId: getXXXById
- create: createXXX 或 addXXX
- update: updateXXX 或 updateXXXById
- delete: deleteXXXById 或 removeXXXById
- batch: batchXXX

## HTTP 方法

- GET 查询
- POST 新增
- PUT 全量更新
- PATCH 部分更新
- DELETE 删除

## 其他

- 版本号用 RequestMapping version
- OpenAPI 3.0 注解
- 关键操作记录日志
- 返回业务数据，由全局拦截器包装
