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
package org.immutables.generator;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import org.eclipse.jdt.internal.compiler.apt.model.ElementImpl;
import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.FieldDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.lookup.SourceTypeBinding;

/**
 * Utility that abstracts away hacks to retrieve elements in source order. Currently, Javac returns
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

  private interface OrderingProvider {
    Ordering<Element> enclosedBy(Element element);
  }

  private static final OrderingProvider DEFAULT_PROVIDER = new OrderingProvider() {
    // it's safe to cast ordering because it handles elements without regards of actual types.
    @SuppressWarnings("unchecked")
    @Override
    public Ordering<Element> enclosedBy(Element element) {
      return (Ordering<Element>) Ordering.explicit(element.getEnclosedElements());
    }
  };

  private static final OrderingProvider PROVIDER = createProvider();

  // it's safe to cast immutable list of <? extends Element> to a list of <Element>
  @SuppressWarnings("unchecked")
  public static ImmutableList<Element> getEnclosedElements(Element element) {
    return (ImmutableList<Element>) enclosedBy(element).immutableSortedCopy(element.getEnclosedElements());
  }

  public static Ordering<Element> enclosedBy(Element element) {
    return PROVIDER.enclosedBy(element);
  }

  private static OrderingProvider createProvider() {
    if (Compiler.ECJ.isPresent()) {
      return new EclipseCompilerOrderingProvider();
    }
    return DEFAULT_PROVIDER;
  }

  /**
   * This inner static class will fail to load if Eclipse compliler classes will not be in
   * classpath.
   * If annotation processor is executed by Javac compiler in presence of ECJ classes, then
   * instanceof checks will fail with fallback to defaults (Javac).
   */
  private static class EclipseCompilerOrderingProvider
      implements OrderingProvider, Function<Element, Object> {

    @Override
    public Object apply(Element input) {
      return ((ElementImpl) input)._binding;
    }

    @Override
    public Ordering<Element> enclosedBy(Element element) {
      if (element instanceof ElementImpl &&
          Iterables.all(element.getEnclosedElements(), Predicates.instanceOf(ElementImpl.class))) {

        ElementImpl implementation = (ElementImpl) element;
        if (implementation._binding instanceof SourceTypeBinding) {
          SourceTypeBinding sourceBinding = (SourceTypeBinding) implementation._binding;

          return Ordering.natural().onResultOf(
              Functions.compose(bindingsToSourceOrder(sourceBinding), this));
        }
      }

      return DEFAULT_PROVIDER.enclosedBy(element);
    }

    private Function<Object, Integer> bindingsToSourceOrder(SourceTypeBinding sourceBinding) {
      IdentityHashMap<Object, Integer> bindings = Maps.newIdentityHashMap();

      if (sourceBinding.scope.referenceContext.methods != null) {
        for (AbstractMethodDeclaration declaration : sourceBinding.scope.referenceContext.methods) {
          bindings.put(declaration.binding, declaration.declarationSourceStart);
        }
      }
      if (sourceBinding.scope.referenceContext.fields != null) {
        for (FieldDeclaration declaration : sourceBinding.scope.referenceContext.fields) {
          bindings.put(declaration.binding, declaration.declarationSourceStart);
        }
      }
      if (sourceBinding.scope.referenceContext.memberTypes != null) {
        for (TypeDeclaration declaration : sourceBinding.scope.referenceContext.memberTypes) {
          bindings.put(declaration.binding, declaration.declarationSourceStart);
        }
      }
      return Functions.forMap(bindings);
    }
  }

  /**
   * While we have {@link SourceOrdering}, there's still a problem: We have inheritance hierarchy
   * and
   * we want to have all defined or inherited accessors returned as members of target type, like
   * {@link Elements#getAllMembers(TypeElement)}, but we need to have them properly and stably
   * sorted.
   * This implementation doesn't try to correctly resolve order for accessors inherited from
   * different supertypes(interfaces), just something that stable and reasonable wrt source ordering
   * without handling complex cases.
   * @param elements the elements utility
   * @param types the types utility
   * @param type the type to traverse
   * @return all accessors in source order
   */
  public static ImmutableList<ExecutableElement> getAllAccessors(
      final Elements elements,
      final Types types,
      final TypeElement type) {

    class CollectedOrdering
        extends Ordering<Element> {

      class Intratype {
        Ordering<String> ordering;
        int rank;
      }

      final Map<String, Intratype> accessorOrderings = Maps.newLinkedHashMap();
      final List<TypeElement> linearizedTypes = Lists.newArrayList();
      final Predicate<String> accessorNotYetInOrderings =
          Predicates.not(Predicates.in(accessorOrderings.keySet()));

      CollectedOrdering() {
        traverse(type);
        traverseObjectForInterface();
      }

      private void traverseObjectForInterface() {
        if (type.getKind() == ElementKind.INTERFACE) {
          traverse(elements.getTypeElement(Object.class.getName()));
        }
      }

      void traverse(@Nullable TypeElement element) {
        if (element == null) {
          return;
        }
        collectEnclosing(element);
        traverse(asTypeElement(element.getSuperclass()));
        for (TypeMirror implementedInterface : element.getInterfaces()) {
          traverse(asTypeElement(implementedInterface));
        }
      }

      @Nullable
      TypeElement asTypeElement(TypeMirror type) {
        if (type.getKind() == TypeKind.DECLARED) {
          return (TypeElement) ((DeclaredType) type).asElement();
        }
        return null;
      }

      void collectEnclosing(TypeElement type) {
        List<String> accessors =
            FluentIterable.from(SourceOrdering.getEnclosedElements(type))
                .filter(IsAccessor.PREDICATE)
                .transform(ToSimpleName.FUNCTION)
                .filter(accessorNotYetInOrderings)
                .toList();

        Intratype intratype = new Intratype();
        intratype.rank = linearizedTypes.size();
        intratype.ordering = Ordering.explicit(accessors);

        for (String name : accessors) {
          accessorOrderings.put(name, intratype);
        }

        linearizedTypes.add(type);
      }

      @Override
      public int compare(Element left, Element right) {
        String leftKey = ToSimpleName.FUNCTION.apply(left);
        String rightKey = ToSimpleName.FUNCTION.apply(right);
        Intratype leftIntratype = accessorOrderings.get(leftKey);
        Intratype rightIntratype = accessorOrderings.get(rightKey);
        if (leftIntratype == null || rightIntratype == null) {
          // FIXME figure out why it happens (null)
          return Boolean.compare(leftIntratype == null, rightIntratype == null);
        }
        return leftIntratype == rightIntratype
            ? leftIntratype.ordering.compare(leftKey, rightKey)
            : Integer.compare(leftIntratype.rank, rightIntratype.rank);
      }
    }

    return FluentIterable.from(ElementFilter.methodsIn(elements.getAllMembers(type)))
        .filter(IsAccessor.PREDICATE)
        .toSortedList(new CollectedOrdering());
  }

  private enum ToSimpleName implements Function<Element, String> {
    FUNCTION;
    @Override
    public String apply(Element input) {
      return input.getSimpleName().toString();
    }
  }

  private enum IsAccessor implements Predicate<Element> {
    PREDICATE;
    @Override
    public boolean apply(Element input) {
      if (input.getKind() != ElementKind.METHOD) {
        return false;
      }
      ExecutableElement element = (ExecutableElement) input;
      boolean parameterless = element.getParameters().isEmpty();
      boolean nonstatic = !element.getModifiers().contains(Modifier.STATIC);
      return parameterless && nonstatic;
    }
  }
}
