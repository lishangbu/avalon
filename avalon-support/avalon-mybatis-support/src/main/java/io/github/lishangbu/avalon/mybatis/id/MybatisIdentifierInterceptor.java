package io.github.lishangbu.avalon.mybatis.id;

import io.github.lishangbu.avalon.mybatis.id.generator.IdentifierGeneratorFactory;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.*;
import org.springframework.util.ReflectionUtils;

/**
 * mybatis主键生成策略拦截器，属性有@Id注解时，优先匹配注解，否则匹配全局配置的主键策略【spring.datasource.idType=UUID】，两种都没有时默认SNID处理
 *
 * @author lishangbu
 * @since 2025/8/20
 */
@Slf4j
@Intercepts(
    value = {
      @Signature(
          type = Executor.class,
          method = "update",
          args = {MappedStatement.class, Object.class})
    })
@SuppressWarnings({"rawtypes", "unchecked"})
public class MybatisIdentifierInterceptor implements Interceptor {
  private static final String ID_TYPE = "idType";

  /** key为传入参数类型，value为参数的注解字段，如果没有注解， 则保存一个空的set对象，这个Map避免每次都要重新反射判断 */
  private Map<Class, List<Field>> filesMap = new ConcurrentHashMap();

  /** 全局主键策略 */
  private String globalIdType;

  /**
   * 拦截目标对象的方法执行
   *
   * @param invocation
   * @return
   * @throws Throwable
   */
  @Override
  public Object intercept(Invocation invocation) throws Throwable {

    MappedStatement mappedStatement = (MappedStatement) invocation.getArgs()[0];
    Object paramObj = invocation.getArgs()[1]; // 获取参数
    if (SqlCommandType.INSERT.equals(mappedStatement.getSqlCommandType())) { // 获取 SQL类型
      // 获取实体集合
      Set<Object> entitySet = getParamSet(paramObj);
      // 设置id
      for (Object object : entitySet) {
        process(object);
      }
    }
    // 执行目标方法
    return invocation.proceed();
  }

  /**
   * 为目标对象创建代理对象
   *
   * @param target
   * @return
   */
  @Override
  public Object plugin(Object target) {
    // 利用mybatis工具类创建代理对象
    if (target instanceof Executor) {
      return Plugin.wrap(target, this);
    }
    return target;
  }

  /**
   * 配置信息
   *
   * @param properties
   */
  @Override
  public void setProperties(Properties properties) {
    this.globalIdType = properties.getProperty(ID_TYPE);
  }

  /**
   * object是需要新增的实体数据,它可能是对象,也可能是批量插入的对象。 如果是单个对象,那么object就是当前对象
   * 如果是批量插入对象，那么object就是一个map集合,key值为"list",value为ArrayList集合对象
   */
  private Set<Object> getParamSet(Object object) {
    Set<Object> set = new HashSet<>();
    if (object instanceof Map) {
      // 批量插入对象
      Collection values = (Collection) ((Map) object).get("list");
      for (Object value : values) {
        if (value instanceof Collection) {
          set.addAll((Collection) value);
        } else {
          set.add(value);
        }
      }
    } else {
      // 单个插入对象
      set.add(object);
    }
    return set;
  }

  /**
   * 反射获取注解字段
   *
   * @param paramObj 参数
   * @throws Throwable
   */
  private void process(Object paramObj) throws Throwable {
    // 实体类型
    Class objClass = paramObj.getClass();
    List<Field> handlerList = filesMap.get(objClass);

    // TODO 性能优化，List参数（批量操作）的时候只遍历一次
    syn:
    if (handlerList == null) {
      synchronized (this) {
        handlerList = filesMap.get(objClass); // 多线程双重检测
        // 如果到这里map集合已经存在，则跳出syn标签
        if (handlerList != null) {
          break syn;
        }
        handlerList = new ArrayList<Field>();
        // 获取带有Id注解的所有属性字段
        List<Field> fieldList = getAllField(paramObj);
        if (!fieldList.isEmpty()) {
          for (Field field : fieldList) {
            // 是否包含主键注解
            if (field.isAnnotationPresent(Id.class)) {
              handlerList.add(field);
            }
          }
        }
        filesMap.put(objClass, handlerList);
      }
    }
    generatorKey(handlerList, paramObj);
  }

  /**
   * 获取所有属性包括父类
   *
   * @param model
   * @return
   */
  private List<Field> getAllField(Object model) {
    Class clazz = model.getClass();
    List<Field> fields = new ArrayList<>();
    while (clazz != null) {
      fields.addAll(new ArrayList<>(Arrays.asList(clazz.getDeclaredFields())));
      clazz = clazz.getSuperclass();
    }
    return fields;
  }

  /**
   * 主键生成
   *
   * @param paramObj
   * @throws Exception
   * @throws Throwable
   */
  private void generatorKey(List<Field> fieldList, Object paramObj) throws Exception {
    if (fieldList.isEmpty()) {
      // TODO 无主键注解时执行全局id生成策略
      log.debug("没有找到主键注解，使用全局主键策略: {}", globalIdType);
    } else {
      for (Field field : fieldList) {
        // TODO 完善冲突时的策略
        // 仅先处理没有值的情况
        field.setAccessible(true);
        if (field.get(paramObj) == null) {
          ReflectionUtils.setField(
              field, paramObj, IdentifierGeneratorFactory.nextId(field, paramObj));
        }
      }
    }
  }
}
