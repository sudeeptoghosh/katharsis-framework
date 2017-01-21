package io.katharsis.resource.registry;

import java.util.List;

/**
 * Using class of this type it's possible to build instances of repository entries, which can be used by other parts of
 * the library.
 */
public interface RepositoryEntryBuilder {

    ResourceEntry<?, ?> buildResourceRepository(ResourceLookup lookup, Class<?> resourceClass);

    List<ResponseRelationshipEntry<?, ?>> buildRelationshipRepositories(ResourceLookup lookup, Class<?> resourceClass);
}
