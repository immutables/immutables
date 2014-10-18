/*
    Copyright 2014 Ievgen Lukash

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
package org.immutables.generator;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import java.util.IdentityHashMap;
import javax.lang.model.element.Element;
import org.eclipse.jdt.internal.compiler.apt.model.ElementImpl;
import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.FieldDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.lookup.SourceTypeBinding;

/**
 * Utility that abstracts away hacks to retreive elements in source order. Currently, Javac returns
 * elements in proper source order, but EJC returns elements in alphabetical order.
 * <ul>
 * <li><a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=300408">Bug 300408 -
 * TypeElement.getEnclosedElements does not respect source order</a>
 * <li><a href="http://bugs.sun.com/view_bug.do?bug_id=6884227">JDK-6884227 : Clarify ordering
 * requirements of javax.lang.model.TypeElement.getEnclosedElements</a>
 * </ul>
 * <p>
 * <em>Based on a workaround idea provided by Christian Humer</em>
 */
public final class SourceOrdering {
  private SourceOrdering() {}

  interface OrderingProvider {
    Ordering<Element> enclosedBy(Element element);
  }

  private static final OrderingProvider DEFAULT_PROVIDER = new OrderingProvider() {
    // it's safe to cast ordering because it handles elements without regards of actual types.
    @SuppressWarnings("unckecked")
    @Override
    public Ordering<Element> enclosedBy(Element element) {
      return (Ordering<Element>) Ordering.explicit(element.getEnclosedElements());
    }
  };

  private static final OrderingProvider PROVIDER = createProvider();

  // it's safe to cast immutable list of <? extends Element> to a list of <Element>
  @SuppressWarnings("unchecked")
  public static ImmutableList<Element> getEnclosingElements(Element element) {
    return (ImmutableList<Element>) enclosedBy(element).immutableSortedCopy(element.getEnclosedElements());
  }

  public static Ordering<Element> enclosedBy(Element element) {
    return PROVIDER.enclosedBy(element);
  }

  private static OrderingProvider createProvider() {
    try {
      return new EclipseCompilerOrderingProvider();
    } catch (Throwable ex) {
      return DEFAULT_PROVIDER;
    }
  }

  /**
   * This inner static class will fail to load if Eclipse compliler classes will not be in
   * classpath.
   * If annotation processor is executed by Javac compiler in presence of ECJ classes, then
   * instanceof checks will fail with fallback to defaults (Javac).
   */
  private static class EclipseCompilerOrderingProvider
      implements OrderingProvider, Function<Element, Object> {

    // Triggers loading of class that may be absent in classpath
    static {
      ElementImpl.class.getCanonicalName();
    }

    @Override
    public Object apply(Element input) {
      return ((ElementImpl) input)._binding;
    }

    @Override
    public Ordering<Element> enclosedBy(Element element) {
      if (element instanceof ElementImpl &&
          Iterables.all(element.getEnclosedElements(), Predicates.instanceOf(ElementImpl.class))) {

        ElementImpl implementation = (ElementImpl) element;
        if (!(implementation._binding instanceof SourceTypeBinding)) {
          SourceTypeBinding sourceBinding = ((SourceTypeBinding) implementation._binding);

          return Ordering.natural().onResultOf(
              Functions.compose(bindingsToSourceOrder(sourceBinding), this));
        }
      }

      return DEFAULT_PROVIDER.enclosedBy(element);
    }

    private Function<Object, Integer> bindingsToSourceOrder(SourceTypeBinding sourceBinding) {
      IdentityHashMap<Object, Integer> bindings = Maps.newIdentityHashMap();

      for (AbstractMethodDeclaration declaration : sourceBinding.scope.referenceContext.methods) {
        bindings.put(declaration.binding, declaration.declarationSourceStart);
      }
      for (FieldDeclaration declaration : sourceBinding.scope.referenceContext.fields) {
        bindings.put(declaration.binding, declaration.declarationSourceStart);
      }
      for (TypeDeclaration declaration : sourceBinding.scope.referenceContext.memberTypes) {
        bindings.put(declaration.binding, declaration.declarationSourceStart);
      }
      return Functions.forMap(bindings);
    }
  }
}
