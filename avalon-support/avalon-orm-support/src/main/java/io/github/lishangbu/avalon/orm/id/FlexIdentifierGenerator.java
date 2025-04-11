package io.github.lishangbu.avalon.orm.id;

import io.github.lishangbu.avalon.keygen.FlexKeyGenerator;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

/**
 * FLex Id生成器
 *
 * @author lishangbu
 * @since 2025/3/31
 */
public class FlexIdentifierGenerator implements IdentifierGenerator {
  @Override
  public Object generate(SharedSessionContractImplementor session, Object object) {
    return FlexKeyGenerator.getInstance().generate();
  }
}
