/*
   Copyright 2015 Immutables Authors and Contributors

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package org.immutables.value.processor.meta;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;

public enum AttributeTypeKind {
  REGULAR(""),
  ARRAY(""),
  LIST("List",
      java.util.List.class.getName(),
      UnshadeGuava.typeString("collect.ImmutableList")),
  SET("Set",
      java.util.Set.class.getName(),
      UnshadeGuava.typeString("collect.ImmutableSet")),
  ENUM_SET("Set"),
  SORTED_SET(
      "SortedSet",
      java.util.SortedSet.class.getName(),
      java.util.NavigableSet.class.getName(),
      UnshadeGuava.typeString("collect.ImmutableSortedSet")),
  MAP("Map",
      java.util.Map.class.getName(),
      UnshadeGuava.typeString("collect.ImmutableMap")),
  ENUM_MAP("Map"),
  SORTED_MAP(
      "SortedMap",
      java.util.SortedMap.class.getName(),
      java.util.NavigableMap.class.getName(),
      UnshadeGuava.typeString("collect.ImmutableSortedMap")),
  MULTISET(
      "Multiset",
      UnshadeGuava.typeString("collect.Multiset"),
      UnshadeGuava.typeString("collect.ImmutableMultiset")),
  MULTIMAP(
      "Multimap",
      UnshadeGuava.typeString("collect.Multimap"),
      UnshadeGuava.typeString("collect.ImmutableMultimap")),
  LIST_MULTIMAP(
      "ListMultimap",
      UnshadeGuava.typeString("collect.ListMultimap"),
      UnshadeGuava.typeString("collect.ImmutableListMultimap")),
  SET_MULTIMAP(
      "SetMultimap",
      UnshadeGuava.typeString("collect.SetMultimap"),
      UnshadeGuava.typeString("collect.ImmutableSetMultimap")),
  BI_MAP("BiMap",
      UnshadeGuava.typeString("collect.BiMap"),
      UnshadeGuava.typeString("collect.ImmutableBiMap")),
  OPTIONAL_JDK(
      "Optional",
      "java.util.Optional"),
  OPTIONAL_INT_JDK(
      "OptionalInt",
      "java.util.OptionalInt"),
  OPTIONAL_LONG_JDK(
      "OptionalLong",
      "java.util.OptionalLong"),
  OPTIONAL_DOUBLE_JDK(
      "OptionalDouble",
      "java.util.OptionalDouble"),
  OPTIONAL_GUAVA(
      "Optional",
      UnshadeGuava.typeString("base.Optional")),
  OPTION_FUGUE(
      "Option",
      "com.atlassian.fugue.Option",
      "io.atlassian.fugue.Option"),
  OPTION_JAVASLANG(
      "Option",
      "javaslang.control.Option");

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

  AttributeTypeKind havingEnumFirstTypeParameter(boolean isEnum) {
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
    case MULTISET:
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

  public boolean isMapKind() {
    switch (this) {
    case MAP:
    case ENUM_MAP:
    case SORTED_MAP:
    case BI_MAP:
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
    case BI_MAP:
      return true;
    default:
      return false;
    }
  }

  public boolean isKeyedKind() {
    return isMappingKind() || isSetKind();
  }

  public boolean isJdkOnlyContainerKind() {
    switch (this) {
    case OPTIONAL_JDK:
    case OPTIONAL_INT_JDK:
    case OPTIONAL_LONG_JDK:
    case OPTIONAL_DOUBLE_JDK:
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
    case BI_MAP:
      return true;
    default:
      return false;
    }
  }

  public boolean isOptionalJdk() {
    return this == OPTIONAL_JDK;
  }

  public boolean isOptionalSpecializedJdk() {
    switch (this) {
    case OPTIONAL_INT_JDK:
    case OPTIONAL_LONG_JDK:
    case OPTIONAL_DOUBLE_JDK:
      return true;
    default:
      return false;
    }
  }

  public boolean isOptionalGuava() {
    return this == OPTIONAL_GUAVA;
  }

  public boolean isOptionFugue() {
    return this == OPTION_FUGUE;
  }

  public boolean isOptionJavaslang() {
    return this == OPTION_JAVASLANG;
  }

  public boolean isOptionalKind() {
    switch (this) {
    case OPTIONAL_GUAVA:
    case OPTIONAL_JDK:
    case OPTIONAL_INT_JDK:
    case OPTIONAL_LONG_JDK:
    case OPTIONAL_DOUBLE_JDK:
    case OPTION_FUGUE:
    case OPTION_JAVASLANG:
      return true;
    default:
      return false;
    }
  }

  public boolean isEnumKeyed() {
    switch (this) {
    case ENUM_MAP:
    case ENUM_SET:
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

  public boolean isPlainMapKind() {
    return this == MAP
        || this == ENUM_MAP;
  }

  public boolean isBiMap() {
    return this == BI_MAP;
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
