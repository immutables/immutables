/*
   Copyright 2014 Immutables Authors and Contributors

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
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.primitives.Primitives;
import java.io.Serializable;
import javax.lang.model.type.TypeMirror;
import org.immutables.generator.TypeHierarchyCollector;

public abstract class TypeIntrospectionBase {
  protected static final Predicate<CharSequence> UNDEFINABLE_PATTERN = Predicates.containsPattern("\\.Undefinable$");
  protected static final String ORDINAL_VALUE_INTERFACE_TYPE = "org.immutables.ordinal.OrdinalValue";

  protected static final ImmutableBiMap<String, String> BOXED_TO_PRIMITIVE_TYPES;

  static {
    ImmutableBiMap.Builder<String, String> builder = ImmutableBiMap.builder();
    for (Class<?> primitive : Primitives.allPrimitiveTypes()) {
      builder.put(Primitives.wrap(primitive).getName(), primitive.getName());
    }
    BOXED_TO_PRIMITIVE_TYPES = builder.build();
  }

  protected static boolean isPrimitiveType(String typeName) {
    return BOXED_TO_PRIMITIVE_TYPES.containsValue(typeName);
  }

  protected static boolean isPrimitiveOrWrapped(String name) {
    return BOXED_TO_PRIMITIVE_TYPES.containsKey(name)
        || BOXED_TO_PRIMITIVE_TYPES.containsValue(name);
  }

  protected static String wrapType(String typeName) {
    return MoreObjects.firstNonNull(BOXED_TO_PRIMITIVE_TYPES.inverse().get(typeName), typeName);
  }

  protected static String unwrapType(String typeName) {
    return MoreObjects.firstNonNull(BOXED_TO_PRIMITIVE_TYPES.get(typeName), typeName);
  }

  private volatile boolean introspected;
  protected ImmutableList<String> extendedClassesNames;
  protected ImmutableSet<String> implementedInterfacesNames;

  protected abstract TypeMirror internalTypeMirror();

  protected void ensureTypeIntrospected() {
    if (!introspected) {
      introspectType();
      introspected = true;
    }
  }

  public ImmutableList<String> getExtendedClassesNames() {
    ensureTypeIntrospected();
    return extendedClassesNames;
  }

  public ImmutableSet<String> getImplementedInterfacesNames() {
    ensureTypeIntrospected();
    return implementedInterfacesNames;
  }

  public boolean isComparable() {
    ensureTypeIntrospected();
    return implementedInterfacesNames.contains(Comparable.class.getName());
  }

  public boolean isOrdinalValue() {
    ensureTypeIntrospected();
    return implementedInterfacesNames.contains(ORDINAL_VALUE_INTERFACE_TYPE);
  }

  public boolean isEnumType() {
    ensureTypeIntrospected();
    return extendedClassesNames.contains(Enum.class.getName());
  }

  public boolean isSerializable() {
    ensureTypeIntrospected();
    return implementedInterfacesNames.contains(Serializable.class.getName());
  }

  public boolean isUndefinable() {
    ensureTypeIntrospected();
    return FluentIterable.from(implementedInterfacesNames).anyMatch(UNDEFINABLE_PATTERN);
  }

  public String getDirectSupertype() {
    ensureTypeIntrospected();
    return Iterables.getFirst(extendedClassesNames, null);
  }

  protected void introspectType() {
    intospectTypeMirror(internalTypeMirror());
  }

  protected void intospectTypeMirror(TypeMirror typeMirror) {
    TypeHierarchyCollector collector = collectTypeHierarchy(typeMirror);
    this.extendedClassesNames = collector.extendedClassNames();
    this.implementedInterfacesNames = collector.implementedInterfaceNames();
  }

  protected TypeHierarchyCollector collectTypeHierarchy(TypeMirror typeMirror) {
    TypeHierarchyCollector collector = new TypeHierarchyCollector();
    collector.collectFrom(typeMirror);
    return collector;
  }
}
