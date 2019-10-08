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

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.SimpleElementVisitor7;

/**
 * Static util methods related to {@link Element}
 */
public final class MoreElements {

  private MoreElements() {}

  /**
   * Returns true if the given {@link Element} instance is a {@link TypeElement}.
   *
   * <p>This method is functionally equivalent to an {@code instanceof} check, but should
   * always be used over that idiom as instructed in the documentation for {@link Element}.
   */
  public static boolean isType(Element element) {
    return element.getKind().isClass() || element.getKind().isInterface();
  }

  /**
   * Returns the given {@link Element} instance as {@link TypeElement}.
   *
   * <p>This method is functionally equivalent to an {@code instanceof} check and a cast, but should
   * always be used over that idiom as instructed in the documentation for {@link Element}.
   *
   * @throws IllegalArgumentException if {@code element} isn't a {@link TypeElement}.
   */
  public static TypeElement asType(Element element) {
    return element.accept(TypeElementVisitor.INSTANCE, null);
  }

  /**
   * Returns the given {@link Element} instance as {@link PackageElement}.
   *
   * <p>This method is functionally equivalent to an {@code instanceof} check and a cast, but should
   * always be used over that idiom as instructed in the documentation for {@link Element}.
   *
   * @throws IllegalArgumentException if {@code element} isn't a {@link TypeElement}.
   */
  public static PackageElement asPackage(Element element) {
    return element.accept(PackageElementVisitor.INSTANCE, null);
  }

  /**
   * Returns the given {@link Element} instance as {@link ExecutableElement}.
   *
   * <p>This method is functionally equivalent to an {@code instanceof} check and a cast, but should
   * always be used over that idiom as instructed in the documentation for {@link Element}.
   *
   * @throws IllegalArgumentException if {@code element} isn't a {@link ExecutableElement}.
   */
  public static ExecutableElement asExecutable(Element element) {
    return element.accept(ExecutableElementVisitor.INSTANCE, null);
  }


  private static final class ExecutableElementVisitor
          extends AbstractVisitor<ExecutableElement> {
    private static final ExecutableElementVisitor INSTANCE = new ExecutableElementVisitor();

    ExecutableElementVisitor() {
      super("executable element");
    }

    @Override
    public ExecutableElement visitExecutable(ExecutableElement e, Void label) {
      return e;
    }
  }


  private static final class PackageElementVisitor extends AbstractVisitor<PackageElement> {
    private static final PackageElementVisitor INSTANCE = new PackageElementVisitor();

    PackageElementVisitor() {
      super("package element");
    }

    @Override
    public PackageElement visitPackage(PackageElement e, Void aVoid) {
      return e;
    }
  }


  private static final class TypeElementVisitor extends AbstractVisitor<TypeElement> {
    private static final TypeElementVisitor INSTANCE = new TypeElementVisitor();

    TypeElementVisitor() {
      super("type element");
    }

    @Override
    public TypeElement visitType(TypeElement e, Void ignore) {
      return e;
    }
  }

  private abstract static class AbstractVisitor<T> extends SimpleElementVisitor7<T, Void> {
    private final String label;

    AbstractVisitor(String label) {
      this.label = label;
    }

    @Override
    protected final T defaultAction(Element e, Void ignore) {
      throw new IllegalArgumentException(e + " does not represent a " + label);
    }
  }

}
