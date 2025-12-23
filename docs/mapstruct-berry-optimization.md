# MapStruct 优化 Berry 类型转换

## 问题背景

在 `BerryServiceImpl.importBerries()` 方法中，原本使用手动的 lambda 表达式进行类型转换：

```java
berryData -> {
  Berry berry = new Berry();
  berry.setInternalName(berryData.name());
  berry.setId(berryData.id().longValue());
  berry.setName(berryData.name());
  // ... 其他字段设置
  return berry;
}
```

这种方式存在以下问题：
- 代码冗长，重复性高
- 容易出错，维护困难
- 类型转换逻辑分散在业务代码中

## 解决方案

### 1. 创建 MapStruct Mapper

创建 `BerryMapper` 接口，使用 MapStruct 注解进行类型映射：

```java
@Mapper(componentModel = "spring")
public abstract class BerryMapper {

  @Autowired
  protected PokeApiService pokeApiService;

  @Mapping(target = "id", expression = "java(pokeApiBerry.id().longValue())")
  @Mapping(target = "internalName", source = "name")
  @Mapping(target = "name", source = "pokeApiBerry", qualifiedByName = "resolveLocalizedName")
  // ... 其他映射配置
  public abstract Berry toDatasetBerry(Berry pokeApiBerry);

  @Named("resolveLocalizedName")
  protected String resolveLocalizedName(Berry pokeApiBerry) {
    // 复杂的本地化名称解析逻辑
  }
}
```

### 2. 更新服务类

在 `BerryServiceImpl` 中注入 MapStruct mapper 并使用：

```java
@Service
@RequiredArgsConstructor
public class BerryServiceImpl implements BerryService {

  private final DatasetBerryMapper berryMapper;
  private final PokeApiService pokeApiService;
  private final BerryMapper berryConversionMapper; // MapStruct mapper

  @Override
  public List<Berry> importBerries() {
    return pokeApiService.importData(
        PokeDataTypeEnum.BERRY,
        berryConversionMapper::toDatasetBerry, // 使用 MapStruct mapper
        berryMapper::insert,
        io.github.lishangbu.avalon.pokeapi.model.berry.Berry.class);
  }
}
```

## 映射配置详解

### 基本字段映射

```java
@Mapping(target = "id", expression = "java(pokeApiBerry.id().longValue())")
@Mapping(target = "internalName", source = "name")
@Mapping(target = "growthTime", source = "growthTime")
@Mapping(target = "maxHarvest", source = "maxHarvest")
@Mapping(target = "bulk", source = "size") // 字段名不同
@Mapping(target = "smoothness", source = "smoothness")
@Mapping(target = "soilDryness", source = "soilDryness")
@Mapping(target = "naturalGiftPower", source = "naturalGiftPower")
```

### 嵌套对象映射

```java
@Mapping(target = "firmnessInternalName", source = "firmness.name")
@Mapping(target = "naturalGiftTypeInternalName", source = "naturalGiftType.name")
```

### 自定义逻辑映射

```java
@Mapping(target = "name", source = "pokeApiBerry", qualifiedByName = "resolveLocalizedName")
```

## 自定义转换逻辑

### 本地化名称解析

```java
@Named("resolveLocalizedName")
protected String resolveLocalizedName(Berry pokeApiBerry) {
  try {
    Item item = pokeApiService.getEntityFromUri(
        PokeDataTypeEnum.ITEM,
        NamedApiResourceUtils.getId(pokeApiBerry.item())
    );

    if (item != null) {
      return LocalizationUtils.getLocalizationName(item.names())
          .map(name -> name.name())
          .orElse(pokeApiBerry.name());
    }
  } catch (Exception e) {
    // 如果获取本地化名称失败，使用原始名称
  }

  return pokeApiBerry.name();
}
```

这个方法：
1. 从 PokeAPI Berry 的 `item` 字段获取对应的 Item 数据
2. 从 Item 的 `names` 列表中获取本地化名称
3. 如果获取失败，回退到使用原始名称

## 优势对比

### ❌ 手动转换的缺点

- **代码冗长**：每个字段都需要手动赋值
- **维护困难**：字段变更时需要修改多处代码
- **错误易发**：手动赋值容易遗漏或出错
- **逻辑分散**：转换逻辑混在业务代码中

### ✅ MapStruct 的优势

- **代码简洁**：通过注解声明映射关系
- **自动生成**：编译时自动生成转换代码
- **类型安全**：编译时检查映射正确性
- **易维护**：集中管理映射配置
- **性能优异**：生成的代码性能与手动编写相当

## 文件结构

```
avalon-admin-server/
├── src/main/java/io/github/lishangbu/avalon/admin/
│   ├── mapper/
│   │   └── BerryMapper.java          # MapStruct mapper
│   └── service/dataset/impl/
│       └── BerryServiceImpl.java     # 使用 MapStruct mapper
```

## 验证结果

- ✅ 编译通过：MapStruct 正确生成转换代码
- ✅ 测试通过：类型转换逻辑正常工作
- ✅ 功能完整：保留了所有原有功能，包括本地化名称解析

## 使用建议

1. **统一命名**：MapStruct mapper 使用 `XxxMapper` 命名
2. **包结构**：放在对应的业务包下的 `mapper` 子包中
3. **依赖注入**：使用 Spring 组件模型，自动注入依赖
4. **测试覆盖**：为复杂的自定义转换逻辑编写单元测试

## 扩展应用

这种模式可以扩展到其他数据导入场景：
- Type 导入
- Pokemon 导入
- Move 导入
- 等等

只需要创建对应的 MapStruct mapper 即可实现类型转换的统一管理。

