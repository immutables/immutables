/*
 * Copyright 2019 Immutables Authors and Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.immutables.value.processor.meta;

import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.SimpleTypeVisitor6;

/**
 * Static util methods related to {@link javax.lang.model.type.TypeMirror}
 */
public class MoreTypes {

  /**
   * Returns a {@link ExecutableType} if the {@link TypeMirror} represents an executable type such
   * as a method, constructor, or initializer or throws an {@link IllegalArgumentException}.
   */
  public static ExecutableType asExecutable(TypeMirror maybeExecutableType) {
    return maybeExecutableType.accept(ExecutableTypeVisitor.INSTANCE, null);
  }

  private static final class ExecutableTypeVisitor extends AbstractVisitor<ExecutableType> {
    private static final ExecutableTypeVisitor INSTANCE = new ExecutableTypeVisitor();

    ExecutableTypeVisitor() {
      super("executable type");
    }

    @Override
    public ExecutableType visitExecutable(ExecutableType type, Void ignore) {
      return type;
    }
  }

  /**
   * Returns a {@link ArrayType} if the {@link TypeMirror} represents a primitive array or throws an
   * {@link IllegalArgumentException}.
   */
  public static ArrayType asArray(TypeMirror maybeArrayType) {
    return maybeArrayType.accept(ArrayTypeVisitor.INSTANCE, null);
  }

  private static final class ArrayTypeVisitor extends AbstractVisitor<ArrayType> {
    private static final ArrayTypeVisitor INSTANCE = new ArrayTypeVisitor();

    ArrayTypeVisitor() {
      super("primitive array");
    }

    @Override
    public ArrayType visitArray(ArrayType type, Void ignore) {
      return type;
    }
  }


  private abstract static class AbstractVisitor<T> extends SimpleTypeVisitor6<T, Void> {
    private final String label;

    AbstractVisitor(String label) {
      this.label = label;
    }

    @Override
    protected T defaultAction(TypeMirror e, Void v) {
      throw new IllegalArgumentException(e + " does not represent a " + label);
    }
  }

  /**
   * Returns a {@link DeclaredType} if the {@link TypeMirror} represents a declared type such as a
   * class, interface, union/compound, or enum or throws an {@link IllegalArgumentException}.
   */
  public static DeclaredType asDeclared(TypeMirror maybeDeclaredType) {
    return maybeDeclaredType.accept(DeclaredTypeVisitor.INSTANCE, null);
  }

  private static final class DeclaredTypeVisitor extends AbstractVisitor<DeclaredType> {
    private static final DeclaredTypeVisitor INSTANCE = new DeclaredTypeVisitor();

    DeclaredTypeVisitor() {
      super("declared type");
    }

    @Override
    public DeclaredType visitDeclared(DeclaredType type, Void ignore) {
      return type;
    }
  }


}
