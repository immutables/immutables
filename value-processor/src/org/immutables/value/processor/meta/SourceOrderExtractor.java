package org.immutables.value.processor.meta;

import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import org.immutables.generator.SourceOrdering;

/**
 * While we have {@link SourceOrdering}, there's still a problem: We have inheritance hierarchy and
 * we want to have all defined or iherited accessors returned as members of target type, like
 * {@link Elements#getAllMembers(TypeElement)}, but we need to have them properly and stably sorted.
 * This implementation doen't try to correctly resolve order for accessors inherited from different
 * supertypes(interfaces), just something that stable and reasonable wrt source ordering without
 * handling complex cases. There could be more straightforward ways to do this, if you found, let me
 * know.
 */
public final class SourceOrderExtractor {
  private SourceOrderExtractor() {}

  public static ImmutableList<Element> getOrderedAccessors(
      final Elements elements,
      final TypeElement type) {

    class CollectedOrdering extends Ordering<Element> {
      class Intra {
        Ordering<Element> ordering;
        int rank;
      }

      Map<String, Intra> accessorOrderings = Maps.newLinkedHashMap();
      List<TypeElement> linearizedTypes = Lists.newArrayList();

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

      TypeElement asTypeElement(TypeMirror type) {
        if (type instanceof DeclaredType) {
          return (TypeElement) ((DeclaredType) type).asElement();
        }
        return null;
      }

      void collectEnclosing(TypeElement type) {
        Intra intratype = new Intra();
        intratype.rank = linearizedTypes.size();
        intratype.ordering = SourceOrdering.enclosedBy(type);

        for (Element accessor : Iterables.filter(
            type.getEnclosedElements(),
            IsAccessor.PREDICATE)) {
          String key = key(accessor);
          if (!accessorOrderings.containsKey(key)) {
            accessorOrderings.put(key, intratype);
          }
        }

        linearizedTypes.add(type);
      }

      public String key(Element input) {
        return input.getSimpleName().toString();
      }

      @Override
      public int compare(Element left, Element right) {
        Intra leftIntratype = accessorOrderings.get(key(left));
        Intra rightIntratype = accessorOrderings.get(key(right));
        return leftIntratype == rightIntratype
            ? leftIntratype.ordering.compare(left, right)
            : Integer.compare(leftIntratype.rank, rightIntratype.rank);
      }
    }

    return FluentIterable.from(ImmutableList.<Element>of())
        .append(elements.getAllMembers(type))
        .filter(IsAccessor.PREDICATE)
        .toSortedList(new CollectedOrdering());
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
