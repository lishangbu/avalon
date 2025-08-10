package io.github.lishangbu.avalon.dataset.callback;

import io.github.lishangbu.avalon.dataset.entity.ItemAttributeRelation;
import io.github.lishangbu.avalon.keygen.FlexKeyGenerator;
import org.springframework.data.relational.core.mapping.event.BeforeConvertCallback;
import org.springframework.stereotype.Component;

/**
 * 物品属性关系保存前回调
 *
 * @author lishangbu
 * @since 2025/8/10
 */
@Component
public class ItemAttributeRelationBeforeConvertCallback
    implements BeforeConvertCallback<ItemAttributeRelation> {

  @Override
  public ItemAttributeRelation onBeforeConvert(ItemAttributeRelation aggregate) {
    if (aggregate.getId() == null) {
      aggregate.setId(FlexKeyGenerator.getInstance().generate());
    }
    return aggregate;
  }
}
