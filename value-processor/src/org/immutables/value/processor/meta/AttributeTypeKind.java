package org.immutables.value.processor.meta;

import com.google.common.collect.ImmutableMap;
import org.immutables.value.internal.google.common.base.MoreObjects;

enum AttributeTypeKind {
  REGULAR(""),
  ARRAY(""),
  LIST("List", java.util.List.class.getName()),
  SET("Set", java.util.Set.class.getName()),
  ENUM_SET("Set"),
  SORTED_SET("SortedSet", java.util.SortedSet.class.getName(), java.util.NavigableSet.class.getName()),
  MAP("Map", java.util.Map.class.getName()),
  ENUM_MAP("Map"),
  SORTED_MAP("SortedMap", java.util.SortedMap.class.getName(), java.util.NavigableMap.class.getName()),
  MULTISET("Multiset", UnshadeGuava.typeString("collect.Multiset")),
  MULTIMAP("Multimap", UnshadeGuava.typeString("collect.Multimap")),
  LIST_MULTIMAP("ListMultimap", UnshadeGuava.typeString("collect.ListMultimap")),
  SET_MULTIMAP("SetMultimap", UnshadeGuava.typeString("collect.SetMultimap")),
  OPTIONAL_JDK("Optional", "java.util.Optional"),
  OPTIONAL_GUAVA("Optional", UnshadeGuava.typeString("base.Optional"));

  private final String[] rawTypes;
  private final String rawSimpleName;

  AttributeTypeKind(String rawSimpleName, String... rawTypes) {
    this.rawSimpleName = rawSimpleName;
    this.rawTypes = rawTypes;
  }

  public String rawSimpleName() {
    return rawSimpleName;
  }

  static AttributeTypeKind forRawType(String rawType) {
    return MoreObjects.firstNonNull(
        rawTypeMapping.get(rawType),
        REGULAR);
  }

  AttributeTypeKind withHasEnumFirstTypeParameter(boolean isEnum) {
    if (isEnum) {
      switch (this) {
      case SET:
        return ENUM_SET;
      case MAP:
        return ENUM_MAP;
      default:
      }
    }
    return this;
  }

  private static final ImmutableMap<String, AttributeTypeKind> rawTypeMapping;
  static {
    ImmutableMap.Builder<String, AttributeTypeKind> builder = ImmutableMap.builder();
    for (AttributeTypeKind k : values()) {
      for (String r : k.rawTypes) {
        builder.put(r, k);
      }
    }
    rawTypeMapping = builder.build();
  }

  public boolean isSortedKind() {
    switch (this) {
    case SORTED_MAP:
    case SORTED_SET:
      return true;
    default:
      return false;
    }
  }

  public boolean isSetKind() {
    switch (this) {
    case SET:
    case ENUM_SET:
    case SORTED_SET:
      return true;
    default:
      return false;
    }
  }

  public boolean isContainerKind() {
    switch (this) {
    case REGULAR:
    case ARRAY:
      return false;
    default:
      return true;
    }
  }

  public boolean isCollectionKind() {
    switch (this) {
    case LIST:
    case SET:
    case ENUM_SET:
    case SORTED_SET:
    case MULTISET:
      return true;
    default:
      return false;
    }
  }

  public boolean isMappingKind() {
    switch (this) {
    case MAP:
    case ENUM_MAP:
    case SORTED_MAP:
    case MULTIMAP:
    case LIST_MULTIMAP:
    case SET_MULTIMAP:
      return true;
    default:
      return false;
    }
  }

  public boolean isGuavaContainerKind() {
    switch (this) {
    case MULTISET:
    case MULTIMAP:
    case LIST_MULTIMAP:
    case SET_MULTIMAP:
      return true;
    default:
      return false;
    }
  }

  public boolean isOptionalJdk() {
    return this == OPTIONAL_JDK;
  }

  public boolean isOptionalGuava() {
    return this == OPTIONAL_GUAVA;
  }

  public boolean isOptionalKind() {
    switch (this) {
    case OPTIONAL_GUAVA:
    case OPTIONAL_JDK:
      return true;
    default:
      return false;
    }
  }

  public boolean isMultimapKind() {
    switch (this) {
    case MULTIMAP:
    case LIST_MULTIMAP:
    case SET_MULTIMAP:
      return true;
    default:
      return false;
    }
  }

  public boolean isSet() {
    return this == SET;
  }

  public boolean isEnumSet() {
    return this == ENUM_SET;
  }

  public boolean isSortedSet() {
    return this == SORTED_SET;
  }

  public boolean isList() {
    return this == LIST;
  }

  public boolean isMap() {
    return this == MAP;
  }

  public boolean isSortedMap() {
    return this == SORTED_MAP;
  }

  public boolean isArray() {
    return this == ARRAY;
  }

  public boolean isRegular() {
    return this == REGULAR;
  }

  public boolean isMultiset() {
    return this == MULTISET;
  }

  public boolean isMultimap() {
    return this == MULTIMAP;
  }

  public boolean isSetMultimap() {
    return this == SET_MULTIMAP;
  }

  public boolean isListMultimap() {
    return this == SET_MULTIMAP;
  }

  public boolean isEnumMap() {
    return this == ENUM_MAP;
  }
}
