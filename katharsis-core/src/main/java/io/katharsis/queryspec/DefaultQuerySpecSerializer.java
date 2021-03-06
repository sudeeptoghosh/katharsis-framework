package io.katharsis.queryspec;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.katharsis.core.internal.utils.StringUtils;
import io.katharsis.errorhandling.exception.RepositoryNotFoundException;
import io.katharsis.resource.RestrictedQueryParamsMembers;
import io.katharsis.resource.registry.RegistryEntry;
import io.katharsis.resource.registry.ResourceRegistry;

public class DefaultQuerySpecSerializer implements QuerySpecSerializer {

	private ResourceRegistry resourceRegistry;

	public DefaultQuerySpecSerializer(ResourceRegistry resourceRegistry) {
		this.resourceRegistry = resourceRegistry;
	}

	@Override
	public Map<String, Set<String>> serialize(QuerySpec querySpec) {
		Map<String, Set<String>> map = new HashMap<>();
		serialize(querySpec, map);
		return map;
	}

	private void serialize(QuerySpec querySpec, Map<String, Set<String>> map) {
		RegistryEntry entry = resourceRegistry.findEntry(querySpec.getResourceClass());
		if (entry == null) {
			throw new RepositoryNotFoundException(querySpec.getResourceClass());
		}
		String resourceType = entry.getResourceInformation().getResourceType();

		serializeFilters(querySpec, resourceType, map);
		serializeSorting(querySpec, resourceType, map);
		serializeIncludedFields(querySpec, resourceType, map);
		serializeIncludedRelations(querySpec, resourceType, map);
		serializePagination(querySpec, resourceType, map);

		for (QuerySpec relatedSpec : querySpec.getRelatedSpecs().values()) {
			serialize(relatedSpec, map);
		}
	}

	void serializeFilters(QuerySpec querySpec, String resourceType, Map<String, Set<String>> map) {
		for (FilterSpec filterSpec : querySpec.getFilters()) {
			if (filterSpec.hasExpressions()) {
				throw new UnsupportedOperationException("filter expressions like and and or not yet supported");
			}
			String attrKey = toKey(filterSpec.getAttributePath()) + "[" + filterSpec.getOperator().getName() + "]";
			String key = addResourceType(RestrictedQueryParamsMembers.filter, attrKey, resourceType);

			if (filterSpec.getValue() instanceof Collection) {
				Collection<?> col = (Collection<?>) filterSpec.getValue();
				Set<String> values = new HashSet<>();
				for (Object elem : col) {
					values.add(serializeValue(elem));
				}
				map.put(key, values);
			}
			else {
				String value = serializeValue(filterSpec.getValue());
				put(map, key, value);
			}
		}
	}

	public void serializeSorting(QuerySpec querySpec, String resourceType, Map<String, Set<String>> map) {
		if (!querySpec.getSort().isEmpty()) {
			String key = addResourceType(RestrictedQueryParamsMembers.sort, null, resourceType);

			StringBuilder builder = new StringBuilder();
			for (SortSpec filterSpec : querySpec.getSort()) {
				if (builder.length() > 0) {
					builder.append(",");
				}
				if (filterSpec.getDirection() == Direction.DESC) {
					builder.append("-");
				}
				builder.append(StringUtils.join(".", filterSpec.getAttributePath()));
			}
			put(map, key, builder.toString());
		}
	}

	void serializeIncludedFields(QuerySpec querySpec, String resourceType, Map<String, Set<String>> map) {
		if (!querySpec.getIncludedFields().isEmpty()) {
			String key = addResourceType(RestrictedQueryParamsMembers.fields, null, resourceType);

			StringBuilder builder = new StringBuilder();
			for (IncludeFieldSpec includedField : querySpec.getIncludedFields()) {
				if (builder.length() > 0) {
					builder.append(",");
				}
				builder.append(StringUtils.join(".", includedField.getAttributePath()));
			}
			put(map, key, builder.toString());
		}
	}

	void serializeIncludedRelations(QuerySpec querySpec, String resourceType, Map<String, Set<String>> map) {
		if (!querySpec.getIncludedRelations().isEmpty()) {
			String key = addResourceType(RestrictedQueryParamsMembers.include, null, resourceType);

			StringBuilder builder = new StringBuilder();
			for (IncludeRelationSpec includedField : querySpec.getIncludedRelations()) {
				if (builder.length() > 0) {
					builder.append(",");
				}
				builder.append(StringUtils.join(".", includedField.getAttributePath()));
			}
			put(map, key, builder.toString());
		}
	}

	public void serializePagination(QuerySpec querySpec, String resourceType, Map<String, Set<String>> map) {
		if (querySpec.getOffset() != 0) {
			put(map, "page[offset]", Long.toString(querySpec.getOffset()));
		}
		if (querySpec.getLimit() != null) {
			put(map, "page[limit]", Long.toString(querySpec.getLimit()));
		}
	}

	private static void put(Map<String, Set<String>> map, String key, String value) {
		map.put(key, new HashSet<String>(Arrays.asList(value)));
	}

	private static String toKey(List<String> attributePath) {
		return "[" + StringUtils.join("][", attributePath) + "]";
	}

	private static String addResourceType(RestrictedQueryParamsMembers type, String key, String resourceType) {
		return type.toString() + "[" + resourceType + "]" + (key != null ? key : "");
	}

	private static String serializeValue(Object value) {
		return "" + value;
	}
}
