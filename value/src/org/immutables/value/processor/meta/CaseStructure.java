package org.immutables.value.processor.meta;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.*;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

public class CaseStructure {
  public final List<DiscoveredValue> implementationTypes;
  public final ListMultimap<String, DiscoveredValue> subtyping;
  public final Set<String> implementationTypeNames;
  public final SetMultimap<String, DiscoveredValue> subtypeUsages = HashMultimap.create();
  public final SetMultimap<String, DiscoveredValue> abstractUsages = HashMultimap.create();

  public CaseStructure(DiscoveredValue discoveredValue) {
    this.implementationTypes = discoveredValue.getNestedChildren();
    this.implementationTypeNames = buildImplementationType(implementationTypes);
    this.subtyping = buildSubtyping(implementationTypes);
  }

  private static Set<String> buildImplementationType(List<DiscoveredValue> implementationTypes) {
    ImmutableSet.Builder<String> builder = ImmutableSet.builder();
    for (DiscoveredValue discoveredValue : implementationTypes) {
      builder.add(discoveredValue.implementationTypeName());
    }
    return builder.build();
  }

  private static ListMultimap<String, DiscoveredValue> buildSubtyping(List<DiscoveredValue> implementationTypes) {
    ImmutableListMultimap.Builder<String, DiscoveredValue> builder = ImmutableListMultimap.builder();

    for (DiscoveredValue type : implementationTypes) {
      builder.put(type.internalTypeElement().getQualifiedName().toString(), type);

      for (String className : type.getExtendedClassesNames()) {
        builder.put(className, type);
      }
      for (String interfaceName : type.getImplementedInterfacesNames()) {
        builder.put(interfaceName, type);
      }
    }

    return builder.build();
  }

  public final Predicate<String> isImplementationType = new Predicate<String>() {
    @Override
    public boolean apply(String input) {
      return implementationTypeNames.contains(input);
    }
  };

  public final Function<String, List<DiscoveredValue>> knownSubtypes = new Function<String, List<DiscoveredValue>>() {
    @Override
    public List<DiscoveredValue> apply(@Nullable String typeName) {
      List<DiscoveredValue> subtypes = subtyping.get(typeName);
      subtypeUsages.putAll(typeName, subtypes);
      for (DiscoveredValue subtype : subtypes) {
        subtypeUsages.put(subtype.valueTypeName(), subtype);
      }
      return subtypes;
    }
  };
}
