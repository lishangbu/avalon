---
applyTo: "*Mapper.java,*.Mapper.xml"
---

# Mapper层 代码与注释编写规范

## Mapper 规范

- 命名以 *Mapper 结尾，放在 mapper 包，对应的xml放在resource目录的mapper文件夹下
- 继承 BaseMapper，除非实体包含复合主键
- 负责数据持久化操作，不包含业务逻辑
- 接口和方法必须编写 JavaDoc 注释，说明查询/操作目的、参数、返回值
- 使用 @Mapper 注解标记
- 注释结尾不使用中文句号，保持简洁

### 命名规约

- 优先按 Mybatis Plus 规范命名查询方法
- 单条查询：`selectByXXX`
- 列表查询：`selectListByXXX`
- 存在性检查：`existsByXXX`
- 计数查询：`countByXXX`
- 删除操作：`deleteByXXX`
- 批量操作：`deleteAllByIdIn`
- 排序查询：`selectListByXXXOrderByYYY`、`selectListByXXXOrderByYYYDesc`
- 限制结果：`selectFirstByXXX`、`selectTopNByXXX`

## 自定义查询规范

- 多个参数使用 @Param 注解明确指定
- 自定义查询方法也必须编写详细的 JavaDoc 注释

## 分页查询规范

- 分页参数使用 Page<T> 对象
- 返回类型使用 IPage<T>
- 排序参数包含在 Page 中
- 注释中说明分页参数的作用

## 事务注解使用

- 查询操作通常不需要 @Transactional
- 修改操作建议在 Service 层添加 @Transactional
- 只读操作可使用 @Transactional(readOnly = true)

## 其他建议

- 避免在 Mapper 层编写业务逻辑
- 复杂查询条件建议使用XML构造动态查询条件
- 注意查询性能，适当添加数据库索引
- 批量操作注意数据量大小，避免内存溢出
- 对于大数据量查询，考虑使用流式处理或分批处理
- SQL 查询要注意数据库兼容性问题
