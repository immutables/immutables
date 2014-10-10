package org.immutables.value.processor.meta;

import com.google.common.base.Joiner;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.SetMultimap;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class CaseStructure {
  private final DiscoveredValue nestingParent;
  private final List<DiscoveredValue> nestedChildren;
  private final ListMultimap<String, DiscoveredValue> subtyping;
  private final Map<String, DiscoveredValue> typeMap;
  private final SetMultimap<String, DiscoveredValue> occurencesSubtypingMapping = HashMultimap.create();
  private static final Joiner DOT_JOINER = Joiner.on('.').skipNulls();

  public CaseStructure(DiscoveredValue nestingParent, List<DiscoveredValue> nestedChildren) {
    this.nestingParent = nestingParent;
    this.nestedChildren = nestedChildren;
    this.typeMap = buildTypeMap(nestedChildren);
    this.subtyping = buildSubtyping(nestedChildren);
  }

  private Map<String, DiscoveredValue> buildTypeMap(List<DiscoveredValue> nestedChildren) {
    Map<String, DiscoveredValue> map = Maps.newHashMap();
    for (DiscoveredValue type : nestedChildren) {
      String abstractValueType = type.internalTypeElement().getQualifiedName().toString();
      String immutableImplementationType = DOT_JOINER.join(type.getPackageName(), type.getImmutableReferenceName());

      map.put(abstractValueType, type);
      map.put(immutableImplementationType, type);
    }
    return ImmutableMap.copyOf(map);
  }

  private ListMultimap<String, DiscoveredValue> buildSubtyping(List<DiscoveredValue> nestedChildren) {
    ImmutableListMultimap.Builder<String, DiscoveredValue> builder = ImmutableListMultimap.builder();

    for (DiscoveredValue type : nestedChildren) {
      for (String className : type.getExtendedClassesNames()) {
        builder.put(className, type);
      }
      for (String interfaceName : type.getImplementedInterfacesNames()) {
        builder.put(interfaceName, type);
      }
    }

    return builder.build();
  }

  public DiscoveredValue getNestingParent() {
    return nestingParent;
  }

  public List<DiscoveredValue> getImplementationTypes() {
    return nestedChildren;
  }

  public List<DiscoveredValue> knownSubtypes(String typeName) {
    return subtyping.get(typeName);
  }

  public DiscoveredValue knownImplementation(String typeName) {
    return typeMap.get(typeName);
  }

  public boolean isKnownType(String typeName) {
    return typeMap.containsKey(typeName) || subtyping.containsKey(typeName);
  }

  public String track(String usageType, DiscoveredValue subclassType) {
    occurencesSubtypingMapping.put(usageType, subclassType);
    return "";
  }

  public Collection<Entry<String, DiscoveredValue>> getTrackedUsageTypes() {
    return occurencesSubtypingMapping.entries();
  }
}
