package org.immutables.generate.internal.processing;

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
  private final GenerateType nestingParent;
  private final List<GenerateType> nestedChildren;
  private final ListMultimap<String, GenerateType> subtyping;
  private final Map<String, GenerateType> typeMap;
  private final SetMultimap<String, GenerateType> occurencesSubtypingMapping = HashMultimap.create();
  private static final Joiner DOT_JOINER = Joiner.on('.').skipNulls();

  public CaseStructure(GenerateType nestingParent, List<GenerateType> nestedChildren) {
    this.nestingParent = nestingParent;
    this.nestedChildren = nestedChildren;
    this.typeMap = buildTypeMap(nestedChildren);
    this.subtyping = buildSubtyping(nestedChildren);
  }

  private Map<String, GenerateType> buildTypeMap(List<GenerateType> nestedChildren) {
    Map<String, GenerateType> map = Maps.newHashMap();
    for (GenerateType type : nestedChildren) {
      String abstractValueType = type.internalTypeElement().getQualifiedName().toString();
      String immutableImplementationType = DOT_JOINER.join(type.getPackageName(), type.getImmutableReferenceName());

      map.put(abstractValueType, type);
      map.put(immutableImplementationType, type);
    }
    return ImmutableMap.copyOf(map);
  }

  private ListMultimap<String, GenerateType> buildSubtyping(List<GenerateType> nestedChildren) {
    ImmutableListMultimap.Builder<String, GenerateType> builder = ImmutableListMultimap.builder();

    for (GenerateType type : nestedChildren) {
      for (String className : type.getExtendedClassesNames()) {
        builder.put(className, type);
      }
      for (String interfaceName : type.getImplementedInterfacesNames()) {
        builder.put(interfaceName, type);
      }
    }

    return builder.build();
  }

  public GenerateType getNestingParent() {
    return nestingParent;
  }

  public List<GenerateType> getImplementationTypes() {
    return nestedChildren;
  }

  public List<GenerateType> knownSubtypes(String typeName) {
    return subtyping.get(typeName);
  }

  public GenerateType knownImplementation(String typeName) {
    return typeMap.get(typeName);
  }

  public boolean isKnownType(String typeName) {
    return typeMap.containsKey(typeName) || subtyping.containsKey(typeName);
  }

  public String track(String usageType, GenerateType subclassType) {
    occurencesSubtypingMapping.put(usageType, subclassType);
    return "";
  }

  public Collection<Entry<String, GenerateType>> getTrackedUsageTypes() {
    return occurencesSubtypingMapping.entries();
  }
}
