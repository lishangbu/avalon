package io.github.lishangbu.avalon.jpa;

import io.github.lishangbu.avalon.keygen.FlexKeyGenerator;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

/**
 * flex id生成器
 *
 * @author lishangbu
 * @since 2025/9/14
 */
public class FlexIdentifierGenerator implements IdentifierGenerator {

  @Override
  public Object generate(SharedSessionContractImplementor session, Object object) {
    return FlexKeyGenerator.getInstance().generate();
  }
}
