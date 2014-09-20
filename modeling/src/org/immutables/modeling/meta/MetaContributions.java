package org.immutables.modeling.meta;

import com.google.common.collect.ImmutableMap;
import org.immutables.modeling.meta.MetaContributor.Registry;
import java.util.ServiceLoader;

final class MetaContributions {
  private static final ImmutableMap<String, String> FACETS;

  // TBD unfinished idea

  private MetaContributions() {}

  private static class RegistryBuilder implements Registry {
    ImmutableMap.Builder<String, String> facetsBuilder = ImmutableMap.builder();

    @Override
    public void facet(Class<?> targetType, Class<?> facetType) {
      facetsBuilder.put(targetType.getCanonicalName(), facetType.getCanonicalName());
    }
  }

/*

methods return regular values



create local generated facet wrappers
register facets by extension mechanism
facet returns regular value, extension determines wrapping into facetable?
 annotation here is irrelevant for the conversion into facetable
 object attribute methods should be present on the generated wrapper to be uniformal
 

Operates on immutable or generated facet after generated metadata

 */

  static {
    RegistryBuilder builder = new RegistryBuilder();
    for (MetaContributor contributor : ServiceLoader.load(MetaContributor.class)) {
      contributor.contribute(builder);
    }
    FACETS = builder.facetsBuilder.build();
  }
}
