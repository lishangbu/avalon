package io.github.lishangbu.avalon.platform.persistence;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.hibernate.MappingException;
import org.hibernate.boot.ResourceStreamLocator;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.spi.AdditionalMappingContributions;
import org.hibernate.boot.spi.AdditionalMappingContributor;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Table;

/** 将实体声明的标量外键和部分索引加入 Hibernate 结构模型，建库和更新共用同一份元数据。 */
public class RelationalMappingContributor implements AdditionalMappingContributor {

	@Override
	public void contribute(AdditionalMappingContributions contributions, InFlightMetadataCollector metadata,
			ResourceStreamLocator resources, MetadataBuildingContext context) {
		Map<String, PersistentClass> tables = metadata.getEntityBindings()
			.stream()
			.collect(Collectors.toMap(binding -> binding.getTable().getName(), Function.identity()));
		for (var binding : tables.values()) {
			var table = binding.getTable();
			for (var reference : binding.getMappedClass().getAnnotationsByType(DatabaseForeignKey.class)) {
				var target = tables.get(reference.referencedTable());
				if (target == null) {
					throw new MappingException("未知外键目标表：" + reference.referencedTable());
				}
				var columns = Arrays.stream(reference.columns()).map(name -> column(table, name)).toList();
				var referenced = Arrays.stream(reference.referencedColumns())
					.map(name -> column(target.getTable(), name))
					.toList();
				var key = table.createForeignKey(reference.name(), columns, target.getEntityName(), null,
						reference.options(), referenced);
				key.setReferencedTable(target.getTable());
			}
			for (var definition : binding.getMappedClass().getAnnotationsByType(PartialIndex.class)) {
				var index = table.getOrCreateIndex(definition.name());
				index.setUnique(definition.unique());
				index.setOptions("where " + definition.predicate());
				for (var name : definition.columns()) {
					var parts = name.split("\\s+");
					index.addColumn(column(table, parts[0]), parts.length > 1 ? parts[1] : null);
				}
			}
		}
	}

	private static Column column(Table table, String name) {
		var column = table.getColumn(Identifier.toIdentifier(name));
		if (column == null) {
			throw new MappingException("未知外键或索引列：" + table.getName() + "." + name);
		}
		return column;
	}

}
