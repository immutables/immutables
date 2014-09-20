package org.immutables.modeling.meta;

public interface MetaContributor {

  void contribute(Registry registry);

  public interface Registry {
    void facet(Class<?> targetType, Class<?> facetType);
  }
}
